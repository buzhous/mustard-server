package org.jeecg.modules.app.controller.mip;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.Topic;
import org.jeecg.modules.app.mip.MipAttributesCodec;
import org.jeecg.modules.app.mip.MipProfileBuilder;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.ITopicService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MIP v1 存量数据迁移执行器（协议 10.4 T2/T3/T4）
 * <p>
 * 使用约束：
 * - 仅限<strong>凌晨 02:00-05:00 停写窗口</strong>执行（协议 10.1 升级顺序铁律）；
 * - 执行前先在数据库运行 sql/mustard_mip_upgrade_v1.sql（含备份与回滚脚本）；
 * - 所有接口<strong>幂等可重复执行</strong>：fields 补级按注册表重算覆盖，等级列以列为准回写组件。
 * <p>
 * 接口顺序：① regenProfiles（模板 100% 生成 profile）→ ② upgradeItemFields（存量 100% 带 sensitivity、等级对齐）。
 * content_hash 不落库，出域序列化时由 MipHashUtil 实时计算。
 */
@Slf4j
@Tag(name = "MIP管理-存量迁移")
@RestController
@RequestMapping("/app/mip/admin")
public class AppMipAdminController {

    private static final int BATCH_SIZE = 500;

    private final IItemInfoService itemInfoService;
    private final IItemSyncService itemSyncService;
    private final ITopicService topicService;
    private final org.jeecg.modules.app.mip.LineageService lineageService;

    public AppMipAdminController(IItemInfoService itemInfoService,
                                 IItemSyncService itemSyncService,
                                 ITopicService topicService,
                                 org.jeecg.modules.app.mip.LineageService lineageService) {
        this.itemInfoService = itemInfoService;
        this.itemSyncService = itemSyncService;
        this.topicService = topicService;
        this.lineageService = lineageService;
    }

    @Operation(summary = "全量重刷模板 mip_profile（T4，幂等）")
    @PostMapping("/regenProfiles")
    public Result<JSONObject> regenProfiles() {
        long count = 0;
        long lastId = 0;
        while (true) {
            List<Topic> batch = topicService.list(new LambdaQueryWrapper<Topic>()
                    .gt(Topic::getId, String.valueOf(lastId))
                    .orderByAsc(Topic::getId)
                    .last("limit " + BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (Topic topic : batch) {
                String schemaId = "topic_" + topic.getId();
                JSONObject profile = MipProfileBuilder.build(topic.getFields(), schemaId, null);
                topic.setMipProfile(profile.toJSONString());
                topicService.updateById(topic);
                lastId = parseId(topic.getId());
                count++;
            }
        }
        return Result.ok(stat("regenProfiles", count));
    }

    @Operation(summary = "存量物品 fields 补级 + 等级列对齐（T2/T3，幂等，覆盖 tbl_item_info 与 tbl_item_sync）")
    @PostMapping("/upgradeItemFields")
    public Result<JSONObject> upgradeItemFields() {
        JSONObject stat = stat("upgradeItemFields", 0);
        long[] info = migrateItemInfo();
        long[] sync = migrateItemSync();
        stat.put("item_info", info[0]);
        stat.put("item_info_skipped", info[1]);
        stat.put("item_sync", sync[0]);
        stat.put("item_sync_skipped", sync[1]);
        return Result.ok(stat);
    }

    @Operation(summary = "存量物品 import 锚点事件（T10：无账本事件的存量档案诚实起始，幂等）")
    @PostMapping("/anchorImports")
    public Result<JSONObject> anchorImports() {
        long count = 0;
        long lastId = 0;
        while (true) {
            List<ItemInfo> batch = itemInfoService.list(new LambdaQueryWrapper<ItemInfo>()
                    .gt(ItemInfo::getId, lastId)
                    .orderByAsc(ItemInfo::getId)
                    .last("limit " + BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (ItemInfo item : batch) {
                lastId = item.getId();
                // 已有账本事件的档案不补锚点（不可伪造历史）
                if (!lineageService.chain(item.getOriId()).isEmpty()) {
                    continue;
                }
                lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                        .setOriId(item.getOriId())
                        .setItemId(String.valueOf(item.getId()))
                        .setEventType(org.jeecg.modules.app.mip.MipEventType.IMPORT.getCode())
                        .setToUid(org.jeecg.modules.app.mip.LineageService.hashUid(item.getUserId()))
                        .setOperator(org.jeecg.modules.app.mip.LineageService.OPERATOR_SYSTEM)
                        .setPayload("{\"source\":\"legacy_backfill\"}")
                        .setIdempotencyKey("import:" + item.getId()));
                count++;
            }
        }
        return Result.ok(stat("anchorImports", count));
    }

    // ================================ 私有方法 ================================

    /** 返回 [成功数, 跳过数]：单条存量脏数据跳过不中断全量迁移 */
    private long[] migrateItemInfo() {
        long count = 0;
        long skipped = 0;
        long lastId = 0;
        while (true) {
            List<ItemInfo> batch = itemInfoService.list(new LambdaQueryWrapper<ItemInfo>()
                    .gt(ItemInfo::getId, lastId)
                    .orderByAsc(ItemInfo::getId)
                    .last("limit " + BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (ItemInfo item : batch) {
                lastId = item.getId();
                try {
                    String upgraded = upgradeOne(item.getFields());
                    upgraded = alignLevel(upgraded, item.getLevel());
                    ItemInfo patch = new ItemInfo();
                    patch.setId(item.getId());
                    patch.setFields(upgraded);
                    itemInfoService.updateById(patch);
                    count++;
                } catch (Exception e) {
                    log.warn("migrateItemInfo skip id={}: {}", item.getId(), e.getMessage());
                    skipped++;
                }
            }
        }
        return new long[]{count, skipped};
    }

    private long[] migrateItemSync() {
        long count = 0;
        long skipped = 0;
        long lastId = 0;
        while (true) {
            List<ItemSync> batch = itemSyncService.list(new LambdaQueryWrapper<ItemSync>()
                    .gt(ItemSync::getId, lastId)
                    .orderByAsc(ItemSync::getId)
                    .last("limit " + BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (ItemSync sync : batch) {
                lastId = sync.getId();
                try {
                    String upgraded = upgradeOne(sync.getFields());
                    upgraded = alignLevel(upgraded, sync.getLevel());
                    ItemSync patch = new ItemSync();
                    patch.setId(sync.getId());
                    patch.setFields(upgraded);
                    itemSyncService.updateById(patch);
                    count++;
                } catch (Exception e) {
                    log.warn("migrateItemSync skip id={}: {}", sync.getId(), e.getMessage());
                    skipped++;
                }
            }
        }
        return new long[]{count, skipped};
    }

    /**
     * 单条 fields 升级：按模板 fields 现场推导 Profile（证件 12 提级等规则自动生效）后补级。
     * 模板 mip_profile 已由 regenProfiles 生成，此处现场推导可保证接口独立幂等。
     */
    private String upgradeOne(String fieldsJson) {
        JSONObject profile = MipProfileBuilder.build(fieldsJson, null, null);
        return MipAttributesCodec.upgradeFields(fieldsJson, profile);
    }

    /**
     * 等级列对齐（协议 10.3 衔接点⑤）：以 level 列为准回写组件 4 的 content.level
     */
    private String alignLevel(String fieldsJson, Integer level) {
        if (fieldsJson == null || fieldsJson.isBlank() || level == null) {
            return fieldsJson;
        }
        try {
            JSONArray fields = JSONArray.parseArray(fieldsJson);
            if (fields == null) {
                return fieldsJson;
            }
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                Integer el = MipAttributesCodec.elementOf(f);
                if (el == null || el != 4) {
                    continue;
                }
                JSONObject content = f.getJSONObject("content");
                if (content == null) {
                    content = new JSONObject();
                    f.put("content", content);
                }
                content.put("level", String.valueOf(level));
                return fields.toJSONString();
            }
            return fieldsJson;
        } catch (Exception e) {
            return fieldsJson;
        }
    }

    private JSONObject stat(String name, long count) {
        JSONObject o = new JSONObject();
        o.put("task", name);
        o.put("count", count);
        o.put("window", "仅限凌晨02:00-05:00停写窗口执行");
        return o;
    }

    private long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }
}
