package org.jeecg.modules.app.aiport;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.dto.AiCategoryRef;
import org.jeecg.modules.ai.bean.dto.AiStationRef;
import org.jeecg.modules.ai.bean.dto.AiTagRef;
import org.jeecg.modules.ai.bean.dto.AiTopicRef;
import org.jeecg.modules.ai.port.TaxonomyPort;
import org.jeecg.modules.app.entity.Topic;
import org.jeecg.modules.app.entity.setting.UserCategory;
import org.jeecg.modules.app.entity.setting.UserTag;
import org.jeecg.modules.app.entity.warehouse.UserStationBind;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.app.entity.warehouse.WarehouseStation;
import org.jeecg.modules.app.service.IWarehouseStationService;
import org.jeecg.modules.app.service.IUserCategoryService;
import org.jeecg.modules.app.service.IUserStationBindService;
import org.jeecg.modules.app.service.IUserTagService;
import org.jeecg.modules.app.service.IUserVirtualStationService;
import org.jeecg.modules.app.service.ITopicService;

import java.util.ArrayList;
import java.util.List;

/**
 * TaxonomyPort 桥接实现：
 * 查询与创建均直接走既有 Service（IUserCategoryService/IUserTagService/ITopicService/仓位三服务），
 * 条件与原 AiAgentService/AiQueryService 中的 LambdaQueryWrapper 完全一致。
 * 自动创建（createCategory/createTag）为 R4 受控工具落库通道：调用方（TaxonomyTools）
 * 已完成 ≤4 字裁剪与查重，这里只负责落库 tbl_user_category / tbl_user_tag。
 */
@Slf4j
public class AiTaxonomyPortImpl implements TaxonomyPort {

    /** 主题启用状态（App 广场/建档入口可见：topicStatus=0） */
    private static final int TOPIC_STATUS_ENABLED = 0;
    /** 提示词注入主题数上限（防上下文膨胀） */
    private static final int MAX_TOPICS = 60;
    /** 绑定状态：1已绑定 */
    private static final int BIND_STATUS_BOUND = 1;
    /** 仓位类型 */
    private static final int STATION_TYPE_VIRTUAL = 1;
    private static final int STATION_TYPE_REAL = 2;

    private final IUserCategoryService userCategoryService;
    private final IUserTagService userTagService;
    private final ITopicService topicService;
    private final IUserVirtualStationService virtualStationService;
    private final IUserStationBindService stationBindService;
    private final IWarehouseStationService warehouseStationService;

    public AiTaxonomyPortImpl(IUserCategoryService userCategoryService,
                              IUserTagService userTagService,
                              ITopicService topicService,
                              IUserVirtualStationService virtualStationService,
                              IUserStationBindService stationBindService,
                              IWarehouseStationService warehouseStationService) {
        this.userCategoryService = userCategoryService;
        this.userTagService = userTagService;
        this.topicService = topicService;
        this.virtualStationService = virtualStationService;
        this.stationBindService = stationBindService;
        this.warehouseStationService = warehouseStationService;
    }

    @Override
    public List<AiCategoryRef> listCategories(String uid) {
        List<UserCategory> categories = userCategoryService.list(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, uid));
        List<AiCategoryRef> refs = new ArrayList<>(categories.size());
        for (UserCategory c : categories) {
            refs.add(new AiCategoryRef().setId(c.getId()).setName(c.getName()));
        }
        return refs;
    }

    @Override
    public AiCategoryRef createCategory(String uid, String name, String icon) {
        UserCategory category = new UserCategory()
                .setUserId(uid)
                .setName(name)
                .setIcon(StrUtil.blankToDefault(icon, "box"));
        userCategoryService.save(category);
        return new AiCategoryRef().setId(category.getId()).setName(category.getName());
    }

    @Override
    public List<AiTagRef> listTags(String uid) {
        List<UserTag> tags = userTagService.list(new LambdaQueryWrapper<UserTag>()
                .eq(UserTag::getUserId, uid));
        List<AiTagRef> refs = new ArrayList<>(tags.size());
        for (UserTag t : tags) {
            refs.add(new AiTagRef().setId(t.getId()).setName(t.getName()));
        }
        return refs;
    }

    @Override
    public AiTagRef createTag(String uid, String name) {
        UserTag tag = new UserTag().setUserId(uid).setName(name);
        userTagService.save(tag);
        return new AiTagRef().setId(tag.getId()).setName(tag.getName());
    }

    @Override
    public List<AiTopicRef> listTopics() {
        List<Topic> topics = topicService.list(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getTopicStatus, TOPIC_STATUS_ENABLED)
                .last("limit " + MAX_TOPICS));
        List<AiTopicRef> refs = new ArrayList<>(topics.size());
        for (Topic t : topics) {
            AiTopicRef ref = new AiTopicRef()
                    .setId(t.getId())
                    .setName(t.getTopicName())
                    .setTopicType(t.getTopicType())
                    .setElements(parseElements(t.getFields()));
            refs.add(ref);
        }
        return refs;
    }

    @Override
    public List<AiStationRef> listStations(String uid) {
        List<AiStationRef> refs = new ArrayList<>();
        // 虚拟仓：普通用户固定 1 个（customName 空则展示「虚拟仓」）
        UserVirtualStation virtual = virtualStationService.getOne(new LambdaQueryWrapper<UserVirtualStation>()
                .eq(UserVirtualStation::getUserId, uid), false);
        if (virtual != null) {
            refs.add(new AiStationRef()
                    .setId(virtual.getId())
                    .setName(StrUtil.blankToDefault(virtual.getCustomName(), "虚拟仓"))
                    .setType(STATION_TYPE_VIRTUAL));
        }
        // 真实站点仓：已绑定站点（customName 空则展示站点名）
        List<UserStationBind> binds = stationBindService.list(new LambdaQueryWrapper<UserStationBind>()
                .eq(UserStationBind::getUserId, uid)
                .eq(UserStationBind::getStatus, BIND_STATUS_BOUND));
        if (!binds.isEmpty()) {
            List<String> stationIds = binds.stream().map(UserStationBind::getStationId).distinct().toList();
            List<WarehouseStation> stations = warehouseStationService.listByIds(stationIds);
            for (UserStationBind bind : binds) {
                WarehouseStation station = stations.stream()
                        .filter(s -> s.getId() != null && s.getId().equals(bind.getStationId()))
                        .findFirst().orElse(null);
                if (station == null) {
                    continue;
                }
                refs.add(new AiStationRef()
                        .setId(station.getId())
                        .setName(StrUtil.blankToDefault(bind.getCustomName(), station.getStationName()))
                        .setType(STATION_TYPE_REAL));
            }
        }
        return refs;
    }

    /** 主题 fields JSON → 组件编号集（[{"element":1,...}]；仅保留已知编号，解析失败返回空列表） */
    private List<Integer> parseElements(String fieldsJson) {
        List<Integer> elements = new ArrayList<>();
        if (StrUtil.isBlank(fieldsJson)) {
            return elements;
        }
        try {
            JSONArray fields = JSON.parseArray(fieldsJson);
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                Integer element = f == null ? null : f.getInteger("element");
                if (element != null && element >= 1 && element <= 34) {
                    elements.add(element);
                }
            }
        } catch (Exception e) {
            log.warn("[ai-port] 主题 fields 解析失败: {}", e.getMessage());
        }
        return elements;
    }
}
