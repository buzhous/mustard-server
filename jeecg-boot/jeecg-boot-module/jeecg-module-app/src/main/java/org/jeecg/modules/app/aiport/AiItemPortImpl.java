package org.jeecg.modules.app.aiport;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.ai.bean.dto.AiItemQuery;
import org.jeecg.modules.ai.bean.dto.AiItemRow;
import org.jeecg.modules.ai.port.ItemReadPort;
import org.jeecg.modules.ai.port.ItemWritePort;
import org.jeecg.modules.app.bean.enums.SyncStatusEnum;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.ItemSync;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.service.AppOpenService;
import org.jeecg.modules.app.service.IItemInfoService;
import org.jeecg.modules.app.service.IItemSyncService;
import org.jeecg.modules.app.service.impl.OpenAppCredentialServiceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemWritePort / ItemReadPort 桥接实现：
 * 写路径内部完成原 AiAgentService.resolveCredential 全部逻辑（readwrite + ai_assistant 标记优先，
 * 其次任意启用 readwrite，无则自动创建 AI 专属凭据）并生成幂等键，透传 AppOpenService.addItem；
 * 读路径透传 AppOpenService.stats/itemDetail/lineage 与 IItemInfoService.listByCondition/queryItemInfoByOriId，
 * 并把 Page&lt;ItemInfo&gt;/ItemInfo 映射为 Page&lt;AiItemRow&gt;/AiItemRow。
 */
@Slf4j
public class AiItemPortImpl implements ItemWritePort, ItemReadPort {

    /** AI 专属凭据标记（与原 AiAgentService.AI_CREDENTIAL_MARK 一致） */
    private static final String AI_CREDENTIAL_MARK = "ai_assistant";

    private final AppOpenService appOpenService;
    private final IItemInfoService itemInfoService;
    private final IItemSyncService itemSyncService;
    private final OpenAppCredentialServiceImpl credentialService;
    private final org.jeecg.modules.app.service.ItemSearchIndexService searchIndexService;
    /** 查询意图缓存（P4）：物品写路径成功后按 uid 批量失效 */
    private final org.jeecg.modules.ai.service.AiQueryCacheService queryCacheService;

    public AiItemPortImpl(AppOpenService appOpenService,
                          IItemInfoService itemInfoService,
                          IItemSyncService itemSyncService,
                          OpenAppCredentialServiceImpl credentialService,
                          org.jeecg.modules.app.service.ItemSearchIndexService searchIndexService,
                          org.jeecg.modules.ai.service.AiQueryCacheService queryCacheService) {
        this.appOpenService = appOpenService;
        this.itemInfoService = itemInfoService;
        this.itemSyncService = itemSyncService;
        this.credentialService = credentialService;
        this.searchIndexService = searchIndexService;
        this.queryCacheService = queryCacheService;
    }

    // ================================ ItemWritePort ================================

    @Override
    public Result<JSONObject> addItem(String uid, JSONObject body) {
        OpenAppCredential credential = resolveCredential(uid);
        Result<JSONObject> result = appOpenService.addItem(credential, body, IdUtil.fastSimpleUUID());
        // 意图缓存写失效（P4）：创建成功即失效该 uid 全部查询缓存，异常吞掉不影响写主流程
        if (result != null && result.isSuccess()) {
            queryCacheService.evictUid(uid);
        }
        return result;
    }

    @Override
    public Result<JSONObject> updateItem(String uid, String itemId, JSONObject body) {
        OpenAppCredential credential = resolveCredential(uid);
        if (body == null) {
            body = new JSONObject();
        }
        // AI 局部补丁语义（P5）：服务端读存量 fields 合并，restricted 原值保留 + patch restricted 丢弃
        body.put("itemId", itemId);
        body.put("patch", true);
        Result<JSONObject> result = appOpenService.updateItem(credential, body, IdUtil.fastSimpleUUID());
        // 意图缓存写失效：更新成功同样失效该 uid 全部查询缓存
        if (result != null && result.isSuccess()) {
            queryCacheService.evictUid(uid);
        }
        return result;
    }

    /**
     * 凭据解析：readwrite 优先（ai_assistant 标记 > 任意启用），无则自动创建 AI 专属凭据
     * （原样迁移自 AiAgentService.resolveCredential）
     */
    private OpenAppCredential resolveCredential(String uid) {
        List<OpenAppCredential> credentials = credentialService.listByUid(uid);
        OpenAppCredential marked = null;
        OpenAppCredential anyReadWrite = null;
        for (OpenAppCredential c : credentials) {
            if (c.getStatus() == null || c.getStatus() != 1) {
                continue;
            }
            boolean markedAi = StrUtil.isNotBlank(c.getRemark()) && c.getRemark().contains(AI_CREDENTIAL_MARK);
            if (markedAi && "readwrite".equals(c.getScope())) {
                marked = c;
                break;
            }
            if (anyReadWrite == null && "readwrite".equals(c.getScope())) {
                anyReadWrite = c;
            }
        }
        if (marked != null) {
            return marked;
        }
        if (anyReadWrite != null) {
            return anyReadWrite;
        }
        // 自动创建：appName=芥灵AI + scope=readwrite + remark 标记（§5.6 密钥自动带入）
        OpenAppCredential created = credentialService.addByUser(uid, "芥灵AI");
        created.setScope("readwrite");
        created.setRemark("source:" + AI_CREDENTIAL_MARK);
        credentialService.updateById(created);
        return created;
    }

    // ================================ ItemReadPort ================================

    @Override
    public JSONObject stats(String uid) {
        return appOpenService.stats(uid);
    }

    @Override
    public JSONObject itemDetail(String uid, String itemId) {
        JSONObject detail = appOpenService.itemDetail(uid, itemId);
        // 回收站/已销毁物品不参与检索：get_item 详情直查同样排除（检索列表已在 listByCondition 过滤）
        if (detail != null && !isItemAlive(uid, itemId)) {
            return null;
        }
        return detail;
    }

    @Override
    public JSONObject lineage(String uid, String itemId) {
        return appOpenService.lineage(uid, itemId);
    }

    @Override
    public Page<AiItemRow> listByCondition(AiItemQuery query) {
        ItemInfoQueryVO queryVO = new ItemInfoQueryVO()
                .setUserId(query.getUserId())
                .setKeyword(query.getKeyword())
                .setCategoryList(query.getCategoryList())
                .setTag(query.getTag())
                .setLevel(query.getLevel())
                .setStatus(query.getStatus())
                .setPriceMin(query.getPriceMin())
                .setPriceMax(query.getPriceMax())
                .setCreateTimeStart(query.getCreateTimeStart())
                .setCreateTimeEnd(query.getCreateTimeEnd())
                .setStationIds(query.getStationIds())
                .setOrderBy(query.getOrderBy())
                .setAscending(query.getAscending())
                .setPageNo(query.getPageNo())
                .setPageSize(query.getPageSize());
        Page<ItemInfo> page = itemInfoService.listByCondition(queryVO);
        return toPage(page);
    }

    @Override
    public List<String> aliasExpand(String term) {
        return searchIndexService.aliasExpand(term);
    }

    @Override
    public Page<AiItemRow> searchIndexQuery(String uid, List<String> terms, AiItemQuery filter) {
        List<String> clean = terms == null ? List.of()
                : terms.stream().filter(s -> s != null && !s.isBlank()).toList();
        if (clean.isEmpty()) {
            return new Page<>(filter.getPageNo() == null ? 1 : filter.getPageNo(),
                    filter.getPageSize() == null ? 20 : filter.getPageSize(), 0);
        }
        // 全文索引圈定主键（任一词命中），其余过滤条件复用 listByCondition 同一套逻辑
        List<String> ids = searchIndexService.searchIdsByTerms(uid, String.join(" ", clean));
        if (ids.isEmpty()) {
            return new Page<>(filter.getPageNo() == null ? 1 : filter.getPageNo(),
                    filter.getPageSize() == null ? 20 : filter.getPageSize(), 0);
        }
        ItemInfoQueryVO queryVO = new ItemInfoQueryVO()
                .setUserId(uid)
                .setIdList(ids)
                .setCategoryList(filter.getCategoryList())
                .setTag(filter.getTag())
                .setLevel(filter.getLevel())
                .setStatus(filter.getStatus())
                .setPriceMin(filter.getPriceMin())
                .setPriceMax(filter.getPriceMax())
                .setCreateTimeStart(filter.getCreateTimeStart())
                .setCreateTimeEnd(filter.getCreateTimeEnd())
                .setStationIds(filter.getStationIds())
                .setOrderBy(filter.getOrderBy())
                .setAscending(filter.getAscending())
                .setPageNo(filter.getPageNo())
                .setPageSize(filter.getPageSize());
        return toPage(itemInfoService.listByCondition(queryVO));
    }

    private Page<AiItemRow> toPage(Page<ItemInfo> page) {
        Page<AiItemRow> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AiItemRow> rows = new ArrayList<>(page.getRecords().size());
        for (ItemInfo item : page.getRecords()) {
            rows.add(toRow(item));
        }
        result.setRecords(rows);
        return result;
    }

    @Override
    public AiItemRow queryByItemId(String uid, String itemId) {
        ItemInfo item = itemInfoService.queryItemInfo(itemId);
        // 回收站/已销毁物品不参与检索：itemId 直查同样排除
        if (item == null || !uid.equals(item.getUserId()) || !isItemAlive(uid, String.valueOf(item.getId()))) {
            return null;
        }
        return toRow(item);
    }

    /**
     * 物品是否在库（可检索）：存在未销毁（status<>DESTROYED）的同步记录；
     * 与 ItemInfoServiceImpl.listByCondition 的 exists 过滤口径一致
     */
    private boolean isItemAlive(String uid, String itemId) {
        if (StrUtil.isEmpty(itemId)) {
            return false;
        }
        Long count = itemSyncService.count(new LambdaQueryWrapper<ItemSync>()
                .eq(ItemSync::getUserId, uid)
                .eq(ItemSync::getItemId, itemId)
                .and(q -> q.isNull(ItemSync::getStatus).or().ne(ItemSync::getStatus, SyncStatusEnum.DESTROYED.getCode())));
        return count != null && count > 0;
    }

    @Override
    public List<String> listItemCategories(String uid) {
        // 与原 AiQueryService.distinctItemCategories 的查询条件完全一致：
        // select(category).eq(userId).isNotNull(category).ne(category,"")，无排序
        List<ItemInfo> items = itemInfoService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItemInfo>()
                .select(ItemInfo::getCategory)
                .eq(ItemInfo::getUserId, uid)
                .isNotNull(ItemInfo::getCategory)
                .ne(ItemInfo::getCategory, ""));
        List<String> categories = new ArrayList<>(items.size());
        for (ItemInfo it : items) {
            categories.add(it.getCategory());
        }
        return categories;
    }

    private AiItemRow toRow(ItemInfo item) {
        return new AiItemRow()
                .setId(item.getId())
                .setOriId(item.getOriId())
                .setName(item.getName())
                .setCategory(item.getCategory())
                .setIcon(item.getIcon())
                .setQuantity(item.getQuantity())
                .setStatus(item.getStatus())
                .setLevel(item.getLevel())
                .setPrice(item.getPrice())
                .setStationId(item.getStationId())
                .setStationType(item.getStationType())
                .setCreateTime(item.getCreateTime());
    }
}
