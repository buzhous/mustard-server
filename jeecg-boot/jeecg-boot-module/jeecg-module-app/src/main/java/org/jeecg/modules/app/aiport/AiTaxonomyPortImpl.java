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
import org.jeecg.modules.app.entity.SystemCategory;
import org.jeecg.modules.app.entity.Topic;
import org.jeecg.modules.app.entity.setting.UserCategory;
import org.jeecg.modules.app.entity.setting.UserTag;
import org.jeecg.modules.app.entity.warehouse.UserStationBind;
import org.jeecg.modules.app.entity.warehouse.UserVirtualStation;
import org.jeecg.modules.app.entity.warehouse.WarehouseStation;
import org.jeecg.modules.app.service.IWarehouseStationService;
import org.jeecg.modules.app.service.ISystemCategoryService;
import org.jeecg.modules.app.service.IUserCategoryService;
import org.jeecg.modules.app.service.IUserStationBindService;
import org.jeecg.modules.app.service.IUserTagService;
import org.jeecg.modules.app.service.IUserVirtualStationService;
import org.jeecg.modules.app.service.ITopicService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    /** 统一分类清单中系统分类注入上限（防上下文膨胀） */
    private static final int MAX_SYSTEM_CATEGORIES = 40;
    /** 绑定状态：1已绑定 */
    private static final int BIND_STATUS_BOUND = 1;
    /** 仓位类型 */
    private static final int STATION_TYPE_VIRTUAL = 1;
    private static final int STATION_TYPE_REAL = 2;

    private final IUserCategoryService userCategoryService;
    private final IUserTagService userTagService;
    private final ITopicService topicService;
    private final ISystemCategoryService systemCategoryService;
    private final IUserVirtualStationService virtualStationService;
    private final IUserStationBindService stationBindService;
    private final IWarehouseStationService warehouseStationService;

    public AiTaxonomyPortImpl(IUserCategoryService userCategoryService,
                              IUserTagService userTagService,
                              ITopicService topicService,
                              ISystemCategoryService systemCategoryService,
                              IUserVirtualStationService virtualStationService,
                              IUserStationBindService stationBindService,
                              IWarehouseStationService warehouseStationService) {
        this.userCategoryService = userCategoryService;
        this.userTagService = userTagService;
        this.topicService = topicService;
        this.systemCategoryService = systemCategoryService;
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
            refs.add(new AiCategoryRef()
                    .setId(c.getId())
                    .setName(c.getName())
                    .setUser(true)
                    .setUserCategoryId(c.getId()));
        }
        return refs;
    }

    @Override
    public List<AiCategoryRef> listUnifiedCategories(String uid) {
        List<AiCategoryRef> refs = new ArrayList<>(listCategories(uid));
        // 启用系统分类（sort 升序，限流防上下文膨胀）；已被该用户实例化的系统分类带 userCategoryId
        List<SystemCategory> systemCategories = systemCategoryService.list(new LambdaQueryWrapper<SystemCategory>()
                .eq(SystemCategory::getStatus, 1)
                .orderByAsc(SystemCategory::getSort)
                .last("limit " + MAX_SYSTEM_CATEGORIES));
        if (systemCategories.isEmpty()) {
            return refs;
        }
        // 该用户已实例化的映射：system_id → 用户分类 id（顺带记录用户分类→系统分类，备注继承用）
        Map<String, String> instantiated = new HashMap<>();
        Map<String, String> systemIdByUserCategoryId = new HashMap<>();
        for (UserCategory c : userCategoryService.list(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, uid)
                .isNotNull(UserCategory::getSystemId))) {
            if (StrUtil.isNotBlank(c.getSystemId())) {
                instantiated.putIfAbsent(c.getSystemId(), c.getId());
                systemIdByUserCategoryId.putIfAbsent(c.getId(), c.getSystemId());
            }
        }
        // 系统分类备注透出（泛称/别名匹配用，如「证件」备注「身份证、护照、驾驶证等」）；
        // 由系统分类实例化的用户分类继承备注，保证解析层在用户分类段即可命中
        Map<String, String> remarkBySystemId = new HashMap<>();
        for (SystemCategory s : systemCategories) {
            if (StrUtil.isNotBlank(s.getRemark())) {
                remarkBySystemId.put(s.getId(), s.getRemark());
            }
        }
        for (AiCategoryRef ref : refs) {
            String systemId = systemIdByUserCategoryId.get(ref.getId());
            if (systemId != null) {
                ref.setRemark(remarkBySystemId.get(systemId));
            }
        }
        for (SystemCategory s : systemCategories) {
            refs.add(new AiCategoryRef()
                    .setId(s.getId())
                    .setName(s.getName())
                    .setUser(false)
                    .setUserCategoryId(instantiated.get(s.getId()))
                    .setRemark(StrUtil.blankToDefault(s.getRemark(), null)));
        }
        return refs;
    }

    @Override
    public AiCategoryRef createCategory(String uid, String name, String icon, String systemId) {
        UserCategory category = new UserCategory()
                .setUserId(uid)
                .setName(name)
                .setIcon(StrUtil.blankToDefault(icon, "box"))
                .setIsSys(StrUtil.isNotBlank(systemId) ? 1 : 0)
                .setSystemId(StrUtil.isNotBlank(systemId) ? systemId : null);
        userCategoryService.save(category);
        return new AiCategoryRef()
                .setId(category.getId())
                .setName(category.getName())
                .setUser(true)
                .setUserCategoryId(category.getId());
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

    @Override
    public List<String> categoryIdsBySystemName(String uid, String systemName) {
        String name = StrUtil.trimToNull(systemName);
        if (name == null) {
            return List.of();
        }
        // 系统分类精确匹配（模型提名来自统一清单，名称精确）；无实例化行返回空
        SystemCategory sys = systemCategoryService.getOne(new LambdaQueryWrapper<SystemCategory>()
                .eq(SystemCategory::getName, name)
                .eq(SystemCategory::getStatus, 1)
                .last("limit 1"), false);
        if (sys == null) {
            return List.of();
        }
        List<UserCategory> rows = userCategoryService.list(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, uid)
                .eq(UserCategory::getSystemId, sys.getId()));
        // 物品 category 历史数据 id/名称两种都存，与 collectCategoryValues 口径一致
        List<String> out = new ArrayList<>(rows.size() * 2);
        for (UserCategory c : rows) {
            out.add(c.getId());
            if (StrUtil.isNotBlank(c.getName())) {
                out.add(c.getName());
            }
        }
        return out;
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
