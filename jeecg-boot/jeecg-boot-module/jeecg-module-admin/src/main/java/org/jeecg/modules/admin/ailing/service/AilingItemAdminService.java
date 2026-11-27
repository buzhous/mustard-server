package org.jeecg.modules.admin.ailing.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.admin.ailing.mapper.AilingItemAdminMapper;
import org.jeecg.modules.admin.ailing.mapper.AilingItemAdminMapper.PageArgs;
import org.jeecg.modules.admin.ailing.mapper.AilingLineageAdminMapper;
import org.jeecg.modules.admin.ailing.mapper.AilingSearchIndexAdminMapper;
import org.jeecg.modules.app.service.ItemSearchIndexService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 芥灵AI 物品只读工作台（P2-4）：列表（normal/recycle）/ 详情（restricted 脱敏）/
 * 时间线（item_id 直查 + spawn 父子链 + chainValid）/ 统计聚合 / 检索索引异步全量重建。
 * 全部只读（唯一写动作 = 重建索引快照）。
 */
@Slf4j
@Service
public class AilingItemAdminService {

    /** restricted 兜底清单（与 MipComponentRegistry.LOCKED_RESTRICTED 同步） */
    private static final Set<Integer> RESTRICTED_FALLBACK = Set.of(17, 18, 21, 22, 25, 26, 33);

    private final AilingItemAdminMapper itemMapper;
    private final AilingLineageAdminMapper lineageMapper;
    private final AilingSearchIndexAdminMapper indexMapper;
    private final ItemSearchIndexService searchIndexService;

    /** 异步全量重建：单线程 executor + 内存进度（total/done/fail/running） */
    private final ExecutorService rebuildExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ailing-index-rebuild");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean rebuildRunning = new AtomicBoolean(false);
    private volatile long rebuildTotal = 0;
    private final AtomicLong rebuildDone = new AtomicLong();
    private final AtomicLong rebuildFail = new AtomicLong();

    public AilingItemAdminService(AilingItemAdminMapper itemMapper,
                                  AilingLineageAdminMapper lineageMapper,
                                  AilingSearchIndexAdminMapper indexMapper,
                                  ItemSearchIndexService searchIndexService) {
        this.itemMapper = itemMapper;
        this.lineageMapper = lineageMapper;
        this.indexMapper = indexMapper;
        this.searchIndexService = searchIndexService;
    }

    // ==================== 列表 ====================

    /**
     * 物品分页：view=normal（在库）/ view=recycle（回收站直查）
     */
    public Map<String, Object> list(String view, String itemId, String keyword, String category, String tag,
                                    Integer level, Integer status, String stationId,
                                    BigDecimal priceMin, BigDecimal priceMax,
                                    Date createTimeStart, Date createTimeEnd, String source,
                                    String uid, String oriId, String orderBy, String orderDir,
                                    long pageNo, long pageSize) {
        Map<String, Object> out = new LinkedHashMap<>();
        PageArgs page = new PageArgs(pageNo, pageSize);
        if ("recycle".equalsIgnoreCase(view)) {
            List<Map<String, Object>> rows = itemMapper.selectRecyclePage(page, itemId, keyword, category,
                    uid, oriId, createTimeStart, createTimeEnd);
            out.put("records", rows);
            out.put("total", rows.size() >= page.getSize() ? null : null); // 回收站分页行数少，前端按返回行数判断
            out.put("pageNo", pageNo);
            out.put("pageSize", page.getSize());
            out.put("view", "recycle");
            return out;
        }
        // 排序方向防注入：仅 asc/desc（默认 desc，最新建档在前）
        String dir = "asc".equalsIgnoreCase(orderDir) ? "asc" : "desc";
        List<Map<String, Object>> rows = itemMapper.selectNormalPage(page, itemId, keyword, category, tag,
                level, status, stationId, priceMin, priceMax, createTimeStart, createTimeEnd,
                source, uid, oriId, orderBy, dir);
        out.put("records", rows);
        out.put("pageNo", pageNo);
        out.put("pageSize", page.getSize());
        out.put("view", "normal");
        return out;
    }

    // ==================== 详情（restricted 脱敏） ====================

    /**
     * 物品全字段详情：restricted 组件 content → 「手动补录」占位（sensitivity 键为准 + 兜底清单）；
     * oriId 仅追溯辅助展示。回收站物品返回 recycle 附加信息。
     */
    public Map<String, Object> detail(String itemId) {
        Map<String, Object> item = itemMapper.selectItemDetail(itemId);
        if (item == null) {
            Map<String, Object> recycle = itemMapper.selectRecycleByItemId(itemId);
            if (recycle == null) {
                return null;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("view", "recycle");
            out.put("recycle", recycle);
            return out;
        }
        Map<String, Object> out = new LinkedHashMap<>(item);
        out.put("view", "normal");
        Object fieldsRaw = item.get("fields");
        if (fieldsRaw != null) {
            try {
                JSONArray fields = JSONArray.parseArray(String.valueOf(fieldsRaw));
                for (int i = 0; i < fields.size(); i++) {
                    JSONObject f = fields.getJSONObject(i);
                    if (f == null) {
                        continue;
                    }
                    Integer element = f.getInteger("element");
                    String sensitivity = f.getString("sensitivity");
                    boolean restricted = "restricted".equals(sensitivity)
                            || (sensitivity == null && element != null && RESTRICTED_FALLBACK.contains(element));
                    if (restricted) {
                        JSONObject masked = new JSONObject(true);
                        masked.put("text", "手动补录");
                        f.put("content", masked);
                        f.put("masked", true);
                    }
                }
                out.put("fields", fields);
            } catch (Exception e) {
                // fields 非法 JSON：原样透出，不脱敏（脏数据兜底）
            }
        }
        return out;
    }

    // ==================== 时间线（账本 item_id 直查 + spawn 父子链） ====================

    /**
     * 物品流转时间线（P3-3 双源归一）：账本 tbl_item_lineage 按 item_id 直查（payload 交叉校验保留），
     * spawn 派生子链按 parent_ori 关联；chainValid 由 prev_hash 衔接 + 首事件 genesis 校验得出。
     * aiEvents：tbl_ai_chat_message 中 itemIds 命中该物品的 AI 操作事件（process→filed / receipt→receipt），
     * 与账本 events 分开返回（前端按时间归一渲染，AI 事件不参与哈希链校验）。
     */
    public Map<String, Object> timeline(String itemId) {
        Map<String, Object> item = itemMapper.selectItemDetail(itemId);
        List<Map<String, Object>> events = lineageMapper.selectByItemId(itemId);
        boolean chainValid = checkChainLink(events);
        String oriId = item == null ? null : String.valueOf(item.get("ori_id"));
        List<Map<String, Object>> spawnChildren = oriId == null ? List.of()
                : lineageMapper.selectSpawnChildren(oriId);
        List<Map<String, Object>> aiEvents = normalizeAiEvents(lineageMapper.selectAiEventsByItemId(itemId));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("itemId", itemId);
        out.put("oriId", oriId);
        out.put("chainValid", chainValid);
        out.put("events", events);
        out.put("spawnChildren", spawnChildren);
        out.put("aiEvents", aiEvents);
        out.put("eventCount", events.size());
        out.put("aiEventCount", aiEvents.size());
        out.put("mergedCount", events.size() + aiEvents.size());
        return out;
    }

    /**
     * AI 事件归一：{source:"ai", msgId, sessionId, uid, time, kind(filed|receipt), qty, express}
     * qty：receipt 取 count，process 取 ok；express 从 process_entries 提取（缺省 false）
     */
    private List<Map<String, Object>> normalizeAiEvents(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            try {
                JSONObject box = JSONObject.parseObject(String.valueOf(row.get("process_entries")));
                if (box == null) {
                    continue;
                }
                boolean receipt = "receipt".equals(row.get("msg_type"));
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("source", "ai");
                e.put("msgId", String.valueOf(row.get("msg_id")));
                e.put("sessionId", String.valueOf(row.get("session_id")));
                e.put("uid", row.get("uid"));
                e.put("time", row.get("create_time"));
                e.put("kind", receipt ? "receipt" : "filed");
                e.put("qty", receipt ? box.getInteger("count") : box.getInteger("ok"));
                e.put("express", Boolean.TRUE.equals(box.getBoolean("express")));
                out.add(e);
            } catch (Exception ex) {
                // 单条脏数据跳过，不影响其余事件装配
                log.warn("ailing AI 事件归一失败 msgId={}", row.get("msg_id"), ex);
            }
        }
        return out;
    }

    /** 链校验（展示级）：首事件 prev_hash=genesis、逐事件 prev_hash 与上一事件 hash 衔接 */
    private boolean checkChainLink(List<Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return true;
        }
        String expectedPrev = org.jeecg.modules.app.mip.MipHashUtil.GENESIS_PREV_HASH;
        for (Map<String, Object> e : events) {
            Object prev = e.get("prev_hash");
            if (prev == null || !expectedPrev.equals(String.valueOf(prev))) {
                return false;
            }
            Object hash = e.get("hash");
            if (hash == null) {
                return false;
            }
            expectedPrev = String.valueOf(hash);
        }
        return true;
    }

    // ==================== 统计 ====================

    /**
     * 统计聚合：总量/建档用户数/今日新增/AI 建档占比 + 分类 TOP（category_norm）/
     * 标签 TOP（tag_norms Java 侧拆分）/等级分布/建档趋势（30d）/来源分布
     */
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        Long total = itemMapper.countItems();
        Long users = itemMapper.countDistinctUsers();
        Date todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date thirtyDaysAgo = Date.from(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Long todayNew = itemMapper.countCreatedSince(todayStart);
        Long aiCount = itemMapper.countAiCreated();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("userCount", users);
        summary.put("todayNew", todayNew);
        summary.put("aiCount", aiCount);
        summary.put("aiRatio", (total == null || total == 0) ? BigDecimal.ZERO
                : BigDecimal.valueOf(aiCount).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, java.math.RoundingMode.HALF_UP));
        out.put("summary", summary);
        out.put("categoryTop", itemMapper.categoryTop());
        out.put("tagTop", tagTop(itemMapper.selectAllTagNorms(), 10));
        out.put("levelDist", itemMapper.levelDist());
        out.put("createdTrend", itemMapper.createdTrend(thirtyDaysAgo));
        out.put("sourceDist", itemMapper.sourceDist());
        out.put("indexRowCount", indexMapper.countIndexRows());
        return out;
    }

    /** tag_norms 空格拆分 → TOP N */
    private List<Map<String, Object>> tagTop(List<String> tagNorms, int limit) {
        Map<String, Long> counter = new HashMap<>();
        if (tagNorms != null) {
            for (String row : tagNorms) {
                if (row == null || row.isBlank()) {
                    continue;
                }
                for (String tag : row.trim().split("\\s+")) {
                    if (!tag.isBlank()) {
                        counter.merge(tag, 1L, Long::sum);
                    }
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        counter.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("k", e.getKey());
                    m.put("c", e.getValue());
                    out.add(m);
                });
        return out;
    }

    // ==================== 检索索引异步全量重建 ====================

    /**
     * 异步全量重建（单线程 executor）：逐 itemId upsert（幂等），已在跑时返回 false 防重入
     */
    public boolean startRebuild() {
        if (!rebuildRunning.compareAndSet(false, true)) {
            return false;
        }
        rebuildDone.set(0);
        rebuildFail.set(0);
        rebuildExecutor.submit(() -> {
            try {
                long lastId = 0;
                long total = 0;
                while (true) {
                    List<Long> ids = indexMapper.selectItemIdsAfter(lastId, 200);
                    if (ids == null || ids.isEmpty()) {
                        break;
                    }
                    // 先记账本批次投影总量，避免 done 短暂大于 total 的进度显示毛刺
                    rebuildTotal = total + ids.size();
                    for (Long id : ids) {
                        lastId = id;
                        total++;
                        try {
                            searchIndexService.rebuild(id);
                        } catch (Exception e) {
                            rebuildFail.incrementAndGet();
                            log.warn("ailing 全量重建单物品失败 itemId={}", id, e);
                        }
                        rebuildDone.incrementAndGet();
                    }
                }
                rebuildTotal = total;
            } finally {
                rebuildRunning.set(false);
            }
        });
        return true;
    }

    /** 重建进度（内存状态）：total/done/fail/running */
    public Map<String, Object> rebuildProgress() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("running", rebuildRunning.get());
        out.put("total", rebuildTotal);
        out.put("done", rebuildDone.get());
        out.put("fail", rebuildFail.get());
        return out;
    }
}
