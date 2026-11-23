package org.jeecg.modules.app.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftReqVO;
import org.jeecg.modules.app.bean.vo.gift.ItemGiftRspVO;
import org.jeecg.modules.app.bean.vo.inventory.ItemInventoryVO;
import org.jeecg.modules.app.bean.vo.item.ItemInfoCreateVO;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.bean.vo.fields.FieldElementVO;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.mip.LineageService;
import org.jeecg.modules.app.mip.MipAttributesCodec;
import org.jeecg.modules.app.mip.MipComponentRegistry;
import org.jeecg.modules.app.mip.MipEventType;
import org.jeecg.modules.app.mip.MipInstanceBuilder;
import org.jeecg.modules.app.mip.MipProfileBuilder;
import org.jeecg.modules.app.mip.MipRedlineFilter;
import org.jeecg.modules.app.mip.MipValidator;
import org.jeecg.modules.app.mip.MipWriteSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 开放平台 service 内核（设计方案：一份 service 内核，两个出口）
 * <p>
 * REST（AppOpenApiController）与 MCP（AppOpenMcpController）共用：
 * MIP 出域装配红线（profile→instance→redlineFilter）与 MIP 写入链（enrich→validate→置信度门槛→mint）。
 * 鉴权/scope/限流/审计在 Filter 与 MCP dispatch 层完成，本类不重复做横切。
 */
@Slf4j
@Service
public class AppOpenService {

    /** 写接口日配额默认值（credential.daily_quota 为空时） */
    private static final int DEFAULT_DAILY_QUOTA = 5000;

    /** 置信度门槛：≥85 直接入库（status=1），否则草稿确认态（status=0） */
    private static final int CONFIDENCE_THRESHOLD = 85;

    /** 幂等键 Redis 前缀（TTL 24h） */
    private static final String KEY_IDEM = "open:idem:";
    /** giftToken Redis 前缀（TTL 300s，一次性） */
    private static final String KEY_GIFT_TOKEN = "open:gift:";
    private static final long GIFT_TOKEN_TTL_SECONDS = 300;
    private static final long IDEM_TTL_SECONDS = 86400;

    @Autowired
    private IItemInfoService itemInfoService;

    @Autowired
    private IItemGiftService itemGiftService;

    @Autowired
    private ItemFlowService itemFlowService;

    @Autowired
    private IItemUserInventoryService itemUserInventoryService;

    @Autowired
    private IItemSyncService itemSyncService;

    @Autowired
    private LineageService lineageService;

    @Autowired
    private RedisUtil redisUtil;

    // ================================ 只读内核 ================================

    /**
     * 物品分页查询（红线过滤 + 出域摘要装配：schemaId/contentHash）
     */
    public Page<ItemInfo> pageItems(String boundUid, ItemInfoQueryVO queryVO) {
        queryVO.setUserId(boundUid);
        if (queryVO.getPageSize() == null || queryVO.getPageSize() > 100) {
            queryVO.setPageSize(Math.min(queryVO.getPageSize() == null ? 10 : queryVO.getPageSize(), 100));
        }
        Page<ItemInfo> page = itemInfoService.listByCondition(queryVO);
        page.getRecords().forEach(this::fillOpenDigest);
        return page;
    }

    /**
     * 物品详情（MIP 实例视图，红线过滤后出域）
     */
    public ItemInfo getOwnedItem(String boundUid, String itemId) {
        ItemInfo itemInfo = itemInfoService.queryItemInfo(itemId);
        if (ObjectUtil.isEmpty(itemInfo) || !boundUid.equals(itemInfo.getUserId())) {
            return null;
        }
        return itemInfo;
    }

    /**
     * 物品详情 MIP 出域实例（含 lineage_digest）
     */
    public JSONObject itemDetail(String boundUid, String itemId) {
        ItemInfo item = getOwnedItem(boundUid, itemId);
        if (item == null) {
            return null;
        }
        return buildOutboundInstance(item, boundUid, true);
    }

    /**
     * 物品搜索（轻量摘要，keyword 必填语义由出口层校验；limit 上限 50）
     */
    public JSONArray searchItems(String boundUid, String keyword, Integer limit, String fieldsParam) {
        int size = limit == null ? 20 : Math.min(Math.max(limit, 1), 50);
        Page<ItemInfo> page = itemInfoService.listByCondition(new ItemInfoQueryVO()
                .setUserId(boundUid)
                .setKeyword(keyword)
                .setPageNo(1)
                .setPageSize(size));
        Set<Integer> wanted = parseElementFilter(fieldsParam);
        JSONArray out = new JSONArray();
        for (ItemInfo item : page.getRecords()) {
            JSONObject instance = buildOutboundInstance(item, boundUid, false);
            JSONObject summary = new JSONObject(true);
            summary.put("itemId", String.valueOf(item.getId()));
            summary.put("oriId", item.getOriId());
            summary.put("name", item.getName());
            summary.put("category", item.getCategory());
            summary.put("icon", item.getIcon());
            summary.put("level", item.getLevel());
            summary.put("quantity", item.getQuantity());
            summary.put("status", item.getStatus());
            summary.put("stationId", item.getStationId());
            summary.put("createTime", item.getCreateTime() == null ? null : item.getCreateTime().getTime());
            summary.put("schemaId", instance.getJSONObject("header").getString("schema_id"));
            summary.put("contentHash", instance.getJSONObject("integrity").getString("content_hash"));
            if (wanted != null) {
                summary.put("attributes", filterAttributesByElements(instance.getJSONArray("attributes"), wanted));
            }
            out.add(summary);
        }
        return out;
    }

    /**
     * 物品统计（仅聚合值，设计方案 4.3）
     */
    public JSONObject stats(String boundUid) {
        Map<String, Long> byStatus = new TreeMap<>();
        Map<String, Long> byCategory = new TreeMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        long total = 0;
        int pageNo = 1;
        while (pageNo <= 20) {
            Page<ItemInfo> page = itemInfoService.listByCondition(new ItemInfoQueryVO()
                    .setUserId(boundUid)
                    .setPageNo(pageNo)
                    .setPageSize(500));
            total = page.getTotal();
            for (ItemInfo item : page.getRecords()) {
                byStatus.merge(String.valueOf(item.getStatus() == null ? "null" : item.getStatus()), 1L, Long::sum);
                if (StrUtil.isNotBlank(item.getCategory())) {
                    byCategory.merge(item.getCategory(), 1L, Long::sum);
                }
                if (item.getPrice() != null) {
                    totalValue = totalValue.add(item.getPrice());
                }
            }
            if (page.getRecords().size() < 500) {
                break;
            }
            pageNo++;
        }
        JSONObject out = new JSONObject(true);
        out.put("total", total);
        out.put("byStatus", byStatus);
        out.put("byCategory", byCategory);
        out.put("totalValue", totalValue.setScale(2).toPlainString());
        return out;
    }

    /**
     * 流转记录（逐事件重算哈希，chainValid=false 表示账本被篡改，设计方案 4.3）
     */
    public JSONObject lineage(String boundUid, String itemId) {
        ItemInfo item = getOwnedItem(boundUid, itemId);
        if (item == null) {
            return null;
        }
        return lineageService.verify(item.getOriId());
    }

    // ================================ 写内核 ================================

    /**
     * Agent 新增物品（设计方案 4.4 处理链）：
     * attributes→fields 转换 → enrich 补级 → computeIdentitySync 冗余派生 → MipValidator 全量校验
     * → 置信度门槛（≥85 status=1，否则草稿 status=0）→ mint（operator=agent:尾4位）→ MIP 出域实例
     *
     * @param idempotencyKey 必填（出口层保证非空）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result<JSONObject> addItem(OpenAppCredential credential, JSONObject body, String idempotencyKey) {
        JSONArray attributes = body == null ? null : body.getJSONArray("attributes");
        if (attributes == null || attributes.isEmpty()) {
            return Result.error(400, "attributes 不能为空");
        }
        Integer confidence = body.getInteger("confidence");
        if (confidence == null || confidence < 0 || confidence > 100) {
            return Result.error(400, "confidence 必填且取值 0-100");
        }
        // 幂等回放：Redis 缓存首次响应 24h（手动重建 Result，避免泛型反序列化差异）
        String idemKey = KEY_IDEM + credential.getAppKey() + ":" + idempotencyKey;
        Object replay = redisUtil.get(idemKey);
        if (replay != null) {
            JSONObject cached = JSON.parseObject(String.valueOf(replay));
            Result<JSONObject> replayResult = new Result<>();
            replayResult.setCode(cached.getIntValue("code"));
            replayResult.setSuccess(cached.getBooleanValue("success"));
            replayResult.setMessage(cached.getString("message"));
            replayResult.setResult(cached.getJSONObject("result"));
            return replayResult;
        }
        // attributes [{element,value}] → fields [{element,name,sort,isShow,content}]
        Integer quantity = null;
        JSONArray fields = new JSONArray();
        int sort = 0;
        for (int i = 0; i < attributes.size(); i++) {
            JSONObject attr = attributes.getJSONObject(i);
            Integer element = attr == null ? null : attr.getInteger("element");
            if (element == null || !MipComponentRegistry.isKnown(element)) {
                return Result.error(400, "element " + element + " 非法组件");
            }
            Object value = attr.get("value");
            if (element == 7 && value != null) {
                quantity = Integer.parseInt(String.valueOf(value));
            }
            MipComponentRegistry.Component def = MipComponentRegistry.get(element);
            JSONObject f = new JSONObject(true);
            f.put("element", element);
            f.put("name", def == null ? String.valueOf(element) : def.name());
            f.put("sort", ++sort);
            f.put("isShow", 1);
            f.put("content", contentFor(element, value));
            fields.add(f);
        }
        // MIP 写入链：enrich 补级（sensitivity 服务端单点，请求方传入无效）
        String fieldsJson = MipWriteSupport.enrich(fields.toJSONString());
        MipWriteSupport.IdentitySync sync = MipWriteSupport.computeIdentitySync(fieldsJson);
        // 全量校验（必填 / element 合法性 / URL / identity 同步）
        JSONObject profile = MipProfileBuilder.build(fieldsJson, null, null);
        JSONObject identity = new JSONObject(true);
        identity.put("oriId", null);
        identity.put("name", sync.name());
        identity.put("category", sync.category());
        identity.put("icon", sync.icon());
        identity.put("level", sync.level());
        List<String> errors = MipValidator.validate(MipAttributesCodec.buildAttributes(fieldsJson, profile), identity);
        if (!errors.isEmpty()) {
            JSONObject errorBody = new JSONObject(true);
            errorBody.put("errors", errors);
            Result<JSONObject> fail = new Result<>();
            fail.setCode(422);
            fail.setSuccess(false);
            fail.setMessage("MIP 校验失败：attributes 不满足必填/类型/同步约束");
            fail.setResult(errorBody);
            return fail;
        }
        // 入库（oriId 由服务端生成；未传仓位自动落默认仓）
        String oriId = StrUtil.blankToDefault(body.getString("oriId"),
                IdUtil.fastSimpleUUID());
        ItemInfoCreateVO createVO = new ItemInfoCreateVO()
                .setOriId(oriId)
                .setUserId(credential.getBoundUid())
                .setName(sync.name())
                .setIcon(sync.icon())
                .setCategory(sync.category())
                .setLevel(sync.level())
                .setQuantity(quantity)
                .setStatus(confidence >= CONFIDENCE_THRESHOLD ? 1 : 0)
                .setFields(JSON.parseArray(fieldsJson, FieldElementVO.class));
        boolean saved = itemInfoService.createItemInfo(createVO, LineageService.operatorAgent(credential.getAppKey()));
        if (!saved) {
            return Result.error(500, "物品入库失败");
        }
        // 同步建库存记录（赠送三步流校验库存而非物品表；无数量组件默认 1）
        ItemInventoryVO inventoryVO = new ItemInventoryVO()
                .setItemId(createVO.getItemId())
                .setUserId(credential.getBoundUid())
                .setQuantity(quantity == null || quantity <= 0 ? 1 : quantity);
        itemUserInventoryService.createItemUserInventory(inventoryVO);
        // 回写同步队列表：App 增量下载仅读 tbl_item_sync，缺失会导致服务端创建的物品在物品栏不可见
        syncItemToQueue(oriId, credential.getBoundUid(), String.valueOf(createVO.getItemId()),
                inventoryVO.getId() == null ? null : String.valueOf(inventoryVO.getId()),
                sync.category(), sync.name(), sync.icon(), sync.level(), quantity,
                confidence >= CONFIDENCE_THRESHOLD ? 1 : 0, fieldsJson);
        // 返回 MIP 出域实例 + 入库状态（normal/draft）
        ItemInfo created = itemInfoService.queryItemInfo(createVO.getItemId());
        JSONObject instance = buildOutboundInstance(created, credential.getBoundUid(), true);
        Result<JSONObject> result = Result.OK(instance);
        result.setMessage(confidence >= CONFIDENCE_THRESHOLD ? "normal" : "draft");
        redisUtil.set(idemKey, JSON.toJSONString(result), IDEM_TTL_SECONDS);
        return result;
    }

    /**
     * 回写同步队列表（tbl_item_sync）：open/AI 链路创建/编辑的物品补写同步记录。
     * App 的 syncDownloadIncremental 仅读该表（user_id + update_time 增量），
     * 缺失记录会导致物品在 App 物品栏不可见。回写失败不影响创建/编辑主链路。
     */
    private void syncItemToQueue(String oriId, String userId, String itemId, String inventoryId,
                                 String categoryId, String name, String icon, Integer level,
                                 Integer quantity, Integer status, String fieldsJson) {
        try {
            java.util.Date now = new java.util.Date();
            ItemSync exist = itemSyncService.getOne(new LambdaQueryWrapper<ItemSync>()
                    .eq(ItemSync::getItemId, itemId)
                    .eq(ItemSync::getUserId, userId)
                    .last("limit 1"));
            if (exist != null) {
                ItemSync syncPatch = new ItemSync();
                syncPatch.setId(exist.getId());
                syncPatch.setName(name);
                syncPatch.setIcon(icon);
                syncPatch.setCategoryId(StrUtil.blankToDefault(categoryId, "0"));
                syncPatch.setLevel(level);
                syncPatch.setQuantity(quantity);
                syncPatch.setStatus(status);
                syncPatch.setFields(fieldsJson);
                syncPatch.setVersion(ObjectUtil.defaultIfNull(exist.getVersion(), 0) + 1);
                syncPatch.setSyncStatus(SyncStatusEnum.SUCCESS.getCode());
                syncPatch.setUpdateTime(now);
                itemSyncService.updateById(syncPatch);
            } else {
                ItemSync itemSync = new ItemSync();
                itemSync.setOriId(oriId);
                itemSync.setUserId(userId);
                itemSync.setItemId(itemId);
                itemSync.setInventoryId(inventoryId);
                itemSync.setCategoryId(StrUtil.blankToDefault(categoryId, "0"));
                itemSync.setName(name);
                itemSync.setIcon(icon);
                itemSync.setLevel(level);
                itemSync.setQuantity(quantity == null ? 1 : quantity);
                itemSync.setStatus(status);
                itemSync.setFields(fieldsJson);
                itemSync.setVersion(1);
                itemSync.setSyncStatus(SyncStatusEnum.SUCCESS.getCode());
                itemSync.setCreateTime(now);
                itemSync.setUpdateTime(now);
                itemSyncService.save(itemSync);
            }
        } catch (Exception e) {
            log.warn("[open] 同步队列表回写失败 itemId={}", itemId, e);
        }
    }

    /**
     * Agent 编辑物品（处理链与新增一致）：
     * 归属校验 → attributes→fields → enrich → computeIdentitySync → MipValidator(422)
     * → 置信度门槛（≥85 恢复 normal，<85 转草稿，未传保持原状态）→ 账本 update 事件 → MIP 出域实例
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result<JSONObject> updateItem(OpenAppCredential credential, JSONObject body, String idempotencyKey) {
        String itemId = body == null ? null : body.getString("itemId");
        JSONArray attributes = body == null ? null : body.getJSONArray("attributes");
        if (StrUtil.isEmpty(itemId) || attributes == null || attributes.isEmpty()) {
            return Result.error(400, "itemId 与 attributes 不能为空");
        }
        Integer confidence = body.getInteger("confidence");
        if (confidence != null && (confidence < 0 || confidence > 100)) {
            return Result.error(400, "confidence 取值 0-100");
        }
        ItemInfo exists = getOwnedItem(credential.getBoundUid(), itemId);
        if (exists == null) {
            return Result.error(404, "物品不存在或不在凭据数据范围内");
        }
        // 幂等回放（写接口 24h 回放首次响应）
        String idemKey = KEY_IDEM + credential.getAppKey() + ":" + idempotencyKey;
        Object replay = redisUtil.get(idemKey);
        if (replay != null) {
            JSONObject cached = JSON.parseObject(String.valueOf(replay));
            Result<JSONObject> replayResult = new Result<>();
            replayResult.setCode(cached.getIntValue("code"));
            replayResult.setSuccess(cached.getBooleanValue("success"));
            replayResult.setMessage(cached.getString("message"));
            replayResult.setResult(cached.getJSONObject("result"));
            return replayResult;
        }
        // attributes → fields（与 addItem 同规则）
        JSONArray fields = new JSONArray();
        int sort = 0;
        for (int i = 0; i < attributes.size(); i++) {
            JSONObject attr = attributes.getJSONObject(i);
            Integer element = attr == null ? null : attr.getInteger("element");
            if (element == null || !MipComponentRegistry.isKnown(element)) {
                return Result.error(400, "element " + element + " 非法组件");
            }
            MipComponentRegistry.Component def = MipComponentRegistry.get(element);
            JSONObject f = new JSONObject(true);
            f.put("element", element);
            f.put("name", def == null ? String.valueOf(element) : def.name());
            f.put("sort", ++sort);
            f.put("isShow", 1);
            f.put("content", contentFor(element, attr.get("value")));
            fields.add(f);
        }
        // 流转锁复核：锁定期拒编辑（D1 定向流转）
        if (itemFlowService.locked(String.valueOf(exists.getId()))) {
            return Result.error(409, "物品正在流转中（存在待响应要约），暂不可编辑");
        }
        // MIP 写入链：enrich → identity 同步 → 全量校验（422 对齐新增）
        String fieldsJson = MipWriteSupport.enrich(fields.toJSONString());
        MipWriteSupport.IdentitySync sync = MipWriteSupport.computeIdentitySync(fieldsJson);
        JSONObject profile = MipProfileBuilder.build(fieldsJson, null, null);
        JSONObject identity = new JSONObject(true);
        identity.put("oriId", exists.getOriId());
        identity.put("name", sync.name());
        identity.put("category", sync.category());
        identity.put("icon", sync.icon());
        identity.put("level", sync.level());
        List<String> errors = MipValidator.validate(MipAttributesCodec.buildAttributes(fieldsJson, profile), identity);
        if (!errors.isEmpty()) {
            JSONObject errorBody = new JSONObject(true);
            errorBody.put("errors", errors);
            Result<JSONObject> fail = new Result<>();
            fail.setCode(422);
            fail.setSuccess(false);
            fail.setMessage("MIP 校验失败：attributes 不满足必填/类型/同步约束");
            fail.setResult(errorBody);
            return fail;
        }
        // 更新（列级 patch，oriId/UserId/仓位不变）
        ItemInfo patch = new ItemInfo();
        patch.setId(exists.getId());
        patch.setName(sync.name());
        patch.setIcon(sync.icon());
        patch.setCategory(sync.category());
        patch.setLevel(sync.level());
        patch.setFields(fieldsJson);
        patch.setUpdateTime(new java.util.Date());
        if (confidence != null) {
            patch.setStatus(confidence >= CONFIDENCE_THRESHOLD ? 1 : 0);
        }
        boolean ok = itemInfoService.updateById(patch);
        if (!ok) {
            return Result.error(500, "物品更新失败");
        }
        // 回写同步队列表（编辑同样补写/升版，保证 App 增量可见）
        org.jeecg.modules.app.entity.ItemUserInventory openInv = itemUserInventoryService
                .queryUserItem(credential.getBoundUid(), String.valueOf(exists.getId()));
        syncItemToQueue(exists.getOriId(), credential.getBoundUid(), String.valueOf(exists.getId()),
                openInv == null ? null : String.valueOf(openInv.getId()),
                sync.category(), sync.name(), sync.icon(), sync.level(), null,
                confidence == null ? exists.getStatus()
                        : (confidence >= CONFIDENCE_THRESHOLD ? 1 : 0),
                fieldsJson);
        // 账本：update（自持事件，from=to=当前用户）
        lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                .setOriId(exists.getOriId())
                .setEventType(MipEventType.UPDATE.getCode())
                .setFromUid(LineageService.hashUid(credential.getBoundUid()))
                .setToUid(LineageService.hashUid(credential.getBoundUid()))
                .setOperator(LineageService.operatorAgent(credential.getAppKey()))
                .setIdempotencyKey("update:" + String.valueOf(exists.getId()) + ":" + idempotencyKey));
        ItemInfo updated = itemInfoService.queryItemInfo(String.valueOf(exists.getId()));
        JSONObject instance = buildOutboundInstance(updated, credential.getBoundUid(), true);
        Result<JSONObject> result = Result.OK(instance);
        result.setMessage("updated");
        redisUtil.set(idemKey, JSON.toJSONString(result), IDEM_TTL_SECONDS);
        return result;
    }

    /**
     * Agent 删除物品：归属校验 → 库存移除 → 物品删除 → 账本 recycle（历史保留，oriId 事件链可追溯）。
     * 高危操作：彻底移除，不可恢复（账本事件仍在）。
     */
    public Result<JSONObject> deleteItem(OpenAppCredential credential, JSONObject body, String idempotencyKey) {
        String itemId = body == null ? null : body.getString("itemId");
        if (StrUtil.isEmpty(itemId)) {
            return Result.error(400, "itemId 不能为空");
        }
        // 幂等回放必须先于归属校验：物品删除后重复请求仍回放首次响应而非 404
        String idemKey = KEY_IDEM + credential.getAppKey() + ":" + idempotencyKey;
        Object replay = redisUtil.get(idemKey);
        if (replay != null) {
            JSONObject cached = JSON.parseObject(String.valueOf(replay));
            Result<JSONObject> replayResult = new Result<>();
            replayResult.setCode(cached.getIntValue("code"));
            replayResult.setSuccess(cached.getBooleanValue("success"));
            replayResult.setMessage(cached.getString("message"));
            replayResult.setResult(cached.getJSONObject("result"));
            return replayResult;
        }
        ItemInfo exists = getOwnedItem(credential.getBoundUid(), itemId);
        if (exists == null) {
            return Result.error(404, "物品不存在或不在凭据数据范围内");
        }
        // 流转锁复核：锁定期拒删除（D1 定向流转）
        if (itemFlowService.locked(String.valueOf(exists.getId()))) {
            return Result.error(409, "物品正在流转中（存在待响应要约），暂不可删除");
        }
        // 库存记录移除（存在才删）
        org.jeecg.modules.app.entity.ItemUserInventory inventory =
                itemUserInventoryService.queryUserItem(credential.getBoundUid(), itemId);
        if (inventory != null) {
            itemUserInventoryService.removeById(inventory.getId());
        }
        boolean ok = itemInfoService.removeById(exists.getId());
        if (!ok) {
            return Result.error(500, "物品删除失败");
        }
        // 账本：recycle（账本终止语义，事件历史保留）
        lineageService.append(new org.jeecg.modules.app.entity.ItemLineage()
                .setOriId(exists.getOriId())
                .setEventType(MipEventType.RECYCLE.getCode())
                .setFromUid(LineageService.hashUid(credential.getBoundUid()))
                .setOperator(LineageService.operatorAgent(credential.getAppKey()))
                .setIdempotencyKey("recycle:" + String.valueOf(exists.getId()) + ":" + idempotencyKey));
        JSONObject resultBody = new JSONObject(true);
        resultBody.put("itemId", itemId);
        resultBody.put("oriId", exists.getOriId());
        resultBody.put("deleted", true);
        Result<JSONObject> result = Result.OK(resultBody);
        result.setMessage("deleted");
        redisUtil.set(idemKey, JSON.toJSONString(result), IDEM_TTL_SECONDS);
        return result;
    }

    /**
     * 赠送预览（第一步）：签发一次性 giftToken（Redis TTL 300s，绑定 appKey+itemId）+ 物品摘要 + 不可撤销告知
     */
    public Result<JSONObject> giftPreview(OpenAppCredential credential, String itemId) {
        ItemInfo item = getOwnedItem(credential.getBoundUid(), itemId);
        if (item == null) {
            return Result.error(404, "item not found");
        }
        // 流转锁复核：锁定期拒赠送（preview 一并拦截，避免用户确认后才撞锁）
        if (itemFlowService.locked(String.valueOf(item.getId()))) {
            return Result.error(409, "物品正在流转中（存在待响应要约），暂不可赠送");
        }
        String token = IdUtil.fastSimpleUUID();
        JSONObject payload = new JSONObject(true);
        payload.put("appKey", credential.getAppKey());
        payload.put("itemId", String.valueOf(item.getId()));
        payload.put("oriId", item.getOriId());
        payload.put("boundUid", credential.getBoundUid());
        payload.put("quantity", item.getQuantity() == null || item.getQuantity() <= 0 ? 1 : item.getQuantity());
        redisUtil.set(KEY_GIFT_TOKEN + token, payload.toJSONString(), GIFT_TOKEN_TTL_SECONDS);
        // 物品摘要（公开 attributes，红线过滤）
        JSONObject instance = buildOutboundInstance(item, credential.getBoundUid(), true);
        JSONObject summary = new JSONObject(true);
        summary.put("itemId", String.valueOf(item.getId()));
        summary.put("name", item.getName());
        summary.put("category", item.getCategory());
        summary.put("icon", item.getIcon());
        summary.put("quantity", payload.getInteger("quantity"));
        summary.put("attributes", instance.getJSONArray("attributes"));
        JSONObject out = new JSONObject(true);
        out.put("giftToken", token);
        out.put("expiresIn", GIFT_TOKEN_TTL_SECONDS);
        out.put("item", summary);
        out.put("warning", "赠送不可撤销。请将接收方与物品清单完整展示给用户确认后，再调用 gift/send 执行赠送。");
        return Result.OK(out);
    }

    /**
     * 赠送执行（第三步）：校验一次性 token（绑定 appKey）→ createGift（GIFT_OUT 记账 operator=agent:尾4位）
     */
    public Result<JSONObject> giftSend(OpenAppCredential credential, String giftToken, String toUserId) {
        if (StrUtil.isBlank(giftToken)) {
            return Result.error(400, "giftToken 不能为空");
        }
        if (StrUtil.isBlank(toUserId)) {
            return Result.error(400, "toUserId 不能为空");
        }
        String payloadJson = (String) redisUtil.get(KEY_GIFT_TOKEN + giftToken);
        if (payloadJson == null) {
            return Result.error(404, "giftToken 无效或已过期");
        }
        JSONObject payload = JSON.parseObject(payloadJson);
        if (!credential.getAppKey().equals(payload.getString("appKey"))
                || !credential.getBoundUid().equals(payload.getString("boundUid"))) {
            return Result.error(403, "giftToken 与当前凭据不匹配");
        }
        // 流转锁复核：锁定期拒赠送（D1 定向流转）
        if (itemFlowService.locked(payload.getString("itemId"))) {
            return Result.error(409, "物品正在流转中（存在待响应要约），暂不可赠送");
        }
        // 一次性：立即删除（防并发重放）
        redisUtil.del(KEY_GIFT_TOKEN + giftToken);
        ItemGiftReqVO reqVO = new ItemGiftReqVO()
                .setGiftType(1)
                .setReceiverUserId(toUserId)
                .setItemId(payload.getString("itemId"))
                .setOriId(payload.getString("oriId"))
                .setQuantity(payload.getInteger("quantity"))
                .setRemark("开放平台赠送（" + LineageService.operatorAgent(credential.getAppKey()) + "）");
        ItemGiftRspVO rsp = itemGiftService.createGift(reqVO, credential.getBoundUid(),
                LineageService.operatorAgent(credential.getAppKey()));
        return Result.OK(JSON.parseObject(JSON.toJSONString(rsp)));
    }

    // ================================ 私有工具 ================================

    /**
     * MIP 出域实例装配 + 红线过滤（出域唯一通道，出口层禁止手写字段过滤）
     *
     * @param withDigest 是否装配 lineage_digest（detail 需要，list/search 省查询）
     */
    private JSONObject buildOutboundInstance(ItemInfo item, String boundUid, boolean withDigest) {
        JSONObject profile = MipProfileBuilder.build(item.getFields(), null, null);
        JSONObject instance = MipInstanceBuilder.build(
                item.getOriId(), profile.getString("schema_id"), profile.getString("profile_version"),
                item.getFields(), profile.toJSONString(),
                item.getName(), item.getCategory(), item.getIcon(), item.getLevel(),
                LineageService.hashUid(boundUid), item.getStationId(), item.getCreateTime(),
                withDigest ? lineageService.digest(item.getOriId()) : null);
        return MipRedlineFilter.filter(instance, false);
    }

    /**
     * 列表/摘要出域信息装配：schemaId + contentHash（剥离视图 integrity.content_hash）
     */
    private void fillOpenDigest(ItemInfo item) {
        if (StrUtil.isBlank(item.getFields())) {
            return;
        }
        try {
            JSONObject profile = MipProfileBuilder.build(item.getFields(), null, null);
            item.setSchemaId(profile.getString("schema_id"));
            JSONObject instance = MipInstanceBuilder.build(
                    item.getOriId(), profile.getString("schema_id"), profile.getString("profile_version"),
                    item.getFields(), profile.toJSONString(),
                    item.getName(), item.getCategory(), item.getIcon(), item.getLevel(),
                    null, item.getStationId(), item.getCreateTime(), null);
            MipRedlineFilter.filter(instance, false);
            item.setContentHash(instance.getJSONObject("integrity").getString("content_hash"));
        } catch (Exception e) {
            log.warn("[open-digest] 装配出域摘要失败 itemId={}: {}", item.getId(), e.getMessage());
        }
    }

    /**
     * attributes element 过滤参数解析："1,3,7" → Set；空/非法返回 null（不过滤）
     */
    private Set<Integer> parseElementFilter(String fieldsParam) {
        if (StrUtil.isBlank(fieldsParam)) {
            return null;
        }
        Set<Integer> wanted = new HashSet<>();
        for (String s : fieldsParam.split(",")) {
            try {
                wanted.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法项
            }
        }
        return wanted.isEmpty() ? null : wanted;
    }

    private JSONArray filterAttributesByElements(JSONArray attributes, Set<Integer> wanted) {
        JSONArray out = new JSONArray();
        if (attributes == null) {
            return out;
        }
        for (int i = 0; i < attributes.size(); i++) {
            JSONObject a = attributes.getJSONObject(i);
            Integer el = MipAttributesCodec.elementOf(a);
            if (el != null && wanted.contains(el)) {
                out.add(a);
            }
        }
        return out;
    }

    /**
     * MIP attributes value → fields content 对象（按 MipAttributesCodec.contentValue 的键优先级反推写入键）
     */
    private JSONObject contentFor(int element, Object value) {
        JSONObject content = new JSONObject(true);
        String type = MipComponentRegistry.valueType(element);
        if (value instanceof JSONArray arr) {
            switch (type == null ? "" : type) {
                case "arrstr" -> {
                    // 规范形态 [{name,...}]（ElementContentVO.tags 为对象数组）：纯字符串归一为 {name}
                    JSONArray tagList = new JSONArray();
                    for (Object t : arr) {
                        tagList.add(t instanceof JSONObject o ? o
                                : new JSONObject(true).fluentPut("name", String.valueOf(t)));
                    }
                    content.put("tags", tagList);
                }
                case "arrobj" -> content.put("attributes", arr);
                case "arrurl" -> {
                    JSONArray medias = new JSONArray();
                    for (Object u : arr) {
                        medias.add(new JSONObject(true).fluentPut("url", u));
                    }
                    content.put("medias", medias);
                }
                case "daterange" -> content.put("date", arr);
                default -> content.put("value", value);
            }
            return content;
        }
        if (value instanceof JSONObject obj) {
            if (element == 18) {
                content.put("latitude", obj.get("lat"));
                content.put("longitude", obj.get("lng"));
                content.put("address", obj.get("address"));
            } else {
                content.put("value", obj);
            }
            return content;
        }
        switch (type == null ? "" : type) {
            case "html" -> content.put("rich", String.valueOf(value));
            case "string" -> content.put("text", String.valueOf(value));
            case "url" -> content.put("url", String.valueOf(value));
            case "int" -> content.put(element == 4 ? "level" : "value", value);
            case "number" -> content.put("price", value);
            case "date", "daterange" -> content.put("date", String.valueOf(value));
            default -> content.put("value", value);
        }
        return content;
    }
}
