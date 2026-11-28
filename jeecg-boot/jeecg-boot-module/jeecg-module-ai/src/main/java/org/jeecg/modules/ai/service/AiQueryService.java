package org.jeecg.modules.ai.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.adapter.AiGlobalSemaphore;
import org.jeecg.modules.ai.adapter.AiModelAdapter;
import org.jeecg.modules.ai.adapter.AiRateLimiter;
import org.jeecg.modules.ai.bean.dto.AiCategoryRef;
import org.jeecg.modules.ai.bean.dto.AiItemQuery;
import org.jeecg.modules.ai.bean.dto.AiItemRow;
import org.jeecg.modules.ai.bean.dto.AiStationRef;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.jeecg.modules.ai.entity.AiChatSession;
import org.jeecg.modules.ai.mip.MipEventType;
import org.jeecg.modules.ai.port.ItemReadPort;
import org.jeecg.modules.ai.port.ObservabilityPort;
import org.jeecg.modules.ai.port.TaxonomyPort;
import org.jeecg.modules.ai.service.impl.AiChatMessageServiceImpl;
import org.jeecg.modules.ai.service.impl.AiChatSessionServiceImpl;
import org.jeecg.modules.ai.stream.AiStreamSink;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * C2 对话式查询编排器（两段式：意图解析 → 服务端执行 → 结果回答）
 * <p>
 * 职责边界与 C1 建档编排器一致：AI 模型只产出结构化意图（第一段）与自然语言回答（第二段），
 * 查询执行/参数白名单校验/所有权圈定（uid）/限流/持久化全部由本编排器完成，
 * 模型无网络自由度；提示词与输出契约来自技能包 skills/mustard-item-query-v2。
 * <p>
 * 会话与消息复用 C1 双表（tbl_ai_chat_session/message）与 /app/ai/chat/send 端点，
 * 结果卡快照复用 draft_snapshot 列（msgType=query_result 时为查询结果 JSON）。
 * <p>
 * v2 变更：意图白名单新增 get_item（单物品定位→详情卡）；过滤器扩展（价格区间/建档时间
 * 区间/仓位/排序）；pageSize 上限 10→20、页数 5→10。
 */
@Slf4j
@Service
public class AiQueryService {

    /** 技能包两段契约分隔标记（全文唯一；代码取 lastIndexOf 兜底防说明文字误命中） */
    private static final String STAGE_SPLIT_MARKER = "<!-- ==== STAGE_SPLIT · 以下为第二段契约 ==== -->";

    /** 意图类型白名单（chat 为兜底类型，不在执行分支内） */
    private static final List<String> INTENT_TYPES = List.of("search_items", "get_stats", "get_item", "get_item_lineage");
    /** 单次返回条目上限（v2：10 → 20） */
    private static final int MAX_PAGE_SIZE = 20;
    /** 翻页上限（v2：5 → 10） */
    private static final int MAX_PAGE = 10;
    /** 关键词长度上限 */
    private static final int MAX_KEYWORD = 50;
    /** 流转事件注入上限 */
    private static final int MAX_LINEAGE_EVENTS = 20;
    /** 详情卡属性注入上限 */
    private static final int MAX_DETAIL_PROPS = 10;
    /** 排序字段白名单 */
    private static final List<String> ORDER_BY_WHITELIST = List.of("created", "name", "price", "quantity", "status");
    /** 详情卡元素注入上限（1图标/2名称/3类别/11属性组/12图片等大对象不在属性摘要中展开） */
    private static final Set<Integer> DETAIL_PROP_EXCLUDE = Set.of(1, 2, 3, 12, 13, 14, 15, 16, 19, 20);
    /** 状态码 → 展示名（0临时 1正常 2使用中） */
    private static final Map<Integer, String> STATUS_LABELS = Map.of(0, "临时", 1, "正常", 2, "使用中");

    /** 强查询语型：命中即走查询管线（含图片也生效） */
    private static final Pattern STRONG_QUERY = Pattern.compile(
            "多少|几[件双本支台个套枚张对]|列出|列出来|列一下|清单|哪些|统计|盘点|查一下|查询|搜一下|找一下|找出|查找|搜索");
    /** 弱查询语型：仅纯文本时走查询管线（带图多为建档/识别诉求，保持 C1 行为） */
    private static final Pattern WEAK_QUERY = Pattern.compile("有什么|都有什么|帮我看看|看看我|找找");

    private final AiChatSessionServiceImpl sessionService;
    private final AiChatMessageServiceImpl messageService;
    private final TaxonomyPort taxonomyPort;
    private final ItemReadPort itemReadPort;
    private final AiModelAdapter modelAdapter;
    private final AiRateLimiter rateLimiter;
    private final ObservabilityPort observabilityPort;
    private final AiGlobalSemaphore semaphore;
    /** 调优配置读路径（P4：DB enabled 提示词/检索开关/缓存参数优先于内置，异常降级内置） */
    private final org.jeecg.modules.ai.config.TuningConfigService tuningConfigService;
    /** 查询意图缓存（P4：search_items 结果卡 Redis TTL 缓存，写路径失效在 app 侧） */
    private final AiQueryCacheService queryCacheService;

    private volatile String promptTemplate;

    /** L2 conv 槽位等待上限（毫秒）：超时抛 QUEUE_TIMEOUT */
    private static final long CONV_WAIT_MS = 3000;

    /** conv 槽位最大并发数（配置 ai.semaphore.conv-max，默认 300） */
    @org.springframework.beans.factory.annotation.Value("${ai.semaphore.conv-max:300}")
    private int convMax;

    /** 检索新链灰度开关（P2-6）：别名扩展 / 索引直查，默认开启 */
    @org.springframework.beans.factory.annotation.Value("${ai.search.alias-enabled:true}")
    private volatile boolean aliasEnabled;
    @org.springframework.beans.factory.annotation.Value("${ai.search.index-enabled:true}")
    private volatile boolean indexEnabled;

    public AiQueryService(AiChatSessionServiceImpl sessionService,
                          AiChatMessageServiceImpl messageService,
                          TaxonomyPort taxonomyPort,
                          ItemReadPort itemReadPort,
                          AiModelAdapter modelAdapter,
                          AiRateLimiter rateLimiter,
                          ObservabilityPort observabilityPort,
                          AiGlobalSemaphore semaphore,
                          org.jeecg.modules.ai.config.TuningConfigService tuningConfigService,
                          AiQueryCacheService queryCacheService) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.taxonomyPort = taxonomyPort;
        this.itemReadPort = itemReadPort;
        this.modelAdapter = modelAdapter;
        this.rateLimiter = rateLimiter;
        this.observabilityPort = observabilityPort;
        this.semaphore = semaphore;
        this.tuningConfigService = tuningConfigService;
        this.queryCacheService = queryCacheService;
    }

    /** yml 内置默认灰度开关（admin 调优页展示「当前生效值+来源」用） */
    public boolean defaultAliasEnabled() {
        return aliasEnabled;
    }

    public boolean defaultIndexEnabled() {
        return indexEnabled;
    }

    // ================================ 意图路由 ================================

    /**
     * 查询语型路由：强语型命中 → C2 管线；弱语型仅纯文本生效（带图走 C1 建档/视觉识别）。
     * 未命中保持 C1 建档管线，默认行为不变。
     */
    public static boolean isQueryText(String text, List<String> images) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        if (STRONG_QUERY.matcher(text).find()) {
            return true;
        }
        return (images == null || images.isEmpty()) && WEAK_QUERY.matcher(text).find();
    }

    // ================================ 两段式管线 ================================

    /**
     * 对话式查询：用户消息落库 → 第一段意图解析（白名单校验）→ 服务端执行查询
     * → 第二段结果回答（模型失败走确定性模板兜底）→ query_result 消息落库。
     */
    public AiChatSendVO send(String uid, String sessionId, String text, List<String> images) {
        return send(uid, sessionId, text, images, null);
    }

    /**
     * 对话式查询（可带流式事件汇）：sink 非空为 SSE 通道——assistant 消息落库即推、解析阶段事件推送，
     * 限流由端点前置（此处跳过防双重计数）；sink 为空为旧一次性 POST 行为，完全不变。
     * <p>
     * L2 全局并发（P2-8）：一次 send 处理占一个 conv 槽（try/finally 包裹，编排轮次间空闲不占槽）；
     * 未立即得槽且为 SSE 通道时推 queued 阶段事件（B.5 排队 stepper），等待 ≤3s 超时抛 QUEUE_TIMEOUT。
     */
    public AiChatSendVO send(String uid, String sessionId, String text, List<String> images, AiStreamSink sink) {
        semaphore.beginRequest();
        boolean acquired = false;
        try {
            acquired = acquireConvSlot(sink);
            return doSend(uid, sessionId, text, images, sink);
        } finally {
            if (acquired) {
                semaphore.release(AiGlobalSemaphore.SEM_CONV);
            }
            semaphore.endRequest();
        }
    }

    /** L2 conv 槽位获取：未立即得槽且 SSE 通道先推 queued，再等待 ≤3s，超时抛 QUEUE_TIMEOUT；返回是否获得槽位 */
    private boolean acquireConvSlot(AiStreamSink sink) {
        boolean ok = semaphore.tryAcquire(AiGlobalSemaphore.SEM_CONV, convMax, 0);
        if (!ok) {
            if (sink != null) {
                sink.progress("queued");
            }
            ok = semaphore.tryAcquire(AiGlobalSemaphore.SEM_CONV, convMax, CONV_WAIT_MS);
        }
        if (!ok) {
            throw new AppException(AiErrorEnum.QUEUE_TIMEOUT);
        }
        return true;
    }

    private AiChatSendVO doSend(String uid, String sessionId, String text, List<String> images, AiStreamSink sink) {
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        if (sink == null) {
            String limitError = rateLimiter.tryAcquireChat(uid);
            if (limitError != null) {
                throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, limitError);
            }
        }
        List<String> imgs = images == null ? List.of() : images.stream().limit(3).toList();
        AiChatMessage userMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_USER,
                AiChatMessage.TYPE_TEXT, text, imgs, null, null, null, sink);

        List<AiCategoryRef> categories = taxonomyPort.listUnifiedCategories(uid);
        // 用户仓位清单（虚拟仓 + 绑定真实站点仓）：station 参数解析为 stationIds 组查询
        List<AiStationRef> stations = taxonomyPort.listStations(uid);

        // 物品实际在用的分类值（建档字典名等）：items.category 存的是分类名/id 混合值，
        // 分类字典 = 用户自定义分类 ∪ 物品在用值，供模型做泛称映射与过滤解析
        Set<String> usedCategoryValues = distinctItemCategories(uid);

        // 第一段：意图解析（技能包 stage-1 契约）
        JSONArray categoryView = new JSONArray();
        Set<String> knownNames = new LinkedHashSet<>();
        for (AiCategoryRef c : categories) {
            JSONObject o = new JSONObject(true);
            o.put("id", c.getId());
            o.put("name", c.getName());
            categoryView.add(o);
            if (c.getName() != null) {
                knownNames.add(c.getName());
            }
        }
        for (String v : usedCategoryValues) {
            if (!knownNames.contains(v) && categoryView.size() < 40) {
                JSONObject o = new JSONObject(true);
                o.put("name", v);
                categoryView.add(o);
            }
        }
        JSONArray stationView = new JSONArray();
        for (AiStationRef s : stations) {
            JSONObject o = new JSONObject(true);
            o.put("id", s.getId());
            o.put("name", s.getName());
            o.put("type", s.getType());
            stationView.add(o);
        }
        if (sink != null) {
            sink.progress("parsing");
        }
        JSONObject intent = callStructured(stage1Prompt()
                .replace("{{USER_CATEGORIES}}", categoryView.toJSONString())
                .replace("{{USER_STATIONS}}", stationView.toJSONString())
                .replace("{{HISTORY}}", recentHistory(uid, sessionId))
                .replace("{{USER_TEXT}}", text), text);
        String type = intent == null ? null : StrUtil.trimToNull(intent.getString("type"));
        log.info("[ai-query] stage1 intent={}", intent == null ? "null" : intent.toJSONString());

        // chat 兜底：非查询意图（含解析失败），自然回应后结束
        if (!INTENT_TYPES.contains(type)) {
            String reply = intent == null ? null : StrUtil.trimToNull(intent.getString("reply"));
            if (reply == null) {
                reply = "我可以帮你查物品，比如「我有多少双鞋」或「帮我把漫画分类的物品列出来」";
            }
            return finishText(uid, sessionId, session, userMsg, reply, sink);
        }

        // 参数白名单校验 + 服务端执行（uid 圈定所有权，仅输出安全摘要字段）
        JSONObject params = intent.getJSONObject("params");
        JSONObject card;
        switch (type) {
            case "get_stats" -> card = statsCard(uid, categories);
            case "get_item" -> card = getItemCard(uid, params, categories, stations);
            case "get_item_lineage" -> card = lineageCard(uid, params);
            default -> card = searchCard(uid, categories, usedCategoryValues, params, stations, text);
        }
        if (card == null) {
            // get_item 未命中（0/多件）→ 回退 search_items 关键词卡，避免空回复
            if ("get_item".equals(type) && params != null && StrUtil.isNotBlank(params.getString("itemName"))) {
                JSONObject fallback = new JSONObject(true);
                fallback.put("keyword", params.getString("itemName"));
                fallback.put("category", params.getString("category"));
                card = searchCard(uid, categories, usedCategoryValues, fallback, stations, text);
                if (card != null) {
                    card.put("fallbackFrom", "get_item");
                }
            }
        }
        if (card == null) {
            return finishText(uid, sessionId, session, userMsg,
                    "没有找到这件物品，说下它的名字我帮你查查看", sink);
        }

        // 第二段：结果回答（模型失败走确定性模板兜底）
        String reply = safeReply(stage2Prompt()
                .replace("{{USER_TEXT}}", text)
                .replace("{{RESULT}}", answerJson(card)));
        if (reply == null) {
            reply = fallbackReply(type, card);
        }

        AiChatMessage resultMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_QUERY_RESULT, clip(reply, 200), null, null, card.toJSONString(), null, sink);
        touchSession(session, text, reply);
        return new AiChatSendVO().setSessionId(sessionId)
                .setSessionStatus(session.getStatus())
                .setMessages(List.of(userMsg, resultMsg));
    }

    // ================================ 查询执行 ================================

    /** search_items：keyword/category/categories/tag/level/status + v2 过滤器（价格/时间/仓位/排序）+ 分页，仅输出安全摘要字段 */
    private JSONObject searchCard(String uid, List<AiCategoryRef> categories,
                                  Set<String> usedCategoryValues, JSONObject params,
                                  List<AiStationRef> stations, String userText) {
        AiItemQuery query = new AiItemQuery().setUserId(uid);
        if (params != null) {
            query.setKeyword(clipStr(params.getString("keyword"), MAX_KEYWORD));
            // 组查询优先：categories 数组（泛称映射多分类，如「证件」→[身份证,护照,驾驶证]）
            Set<String> categoryValues = resolveCategoryValues(uid, params, categories, usedCategoryValues);
            if (!categoryValues.isEmpty()) {
                query.setCategoryList(new ArrayList<>(categoryValues));
            } else if (query.getKeyword() == null && StrUtil.isNotBlank(params.getString("category"))) {
                // 模型给的分类在字典中完全对不上（技能包规则4）：降级为关键词，避免无过滤返回全部
                query.setKeyword(clipStr(params.getString("category"), MAX_KEYWORD));
            }
            query.setTag(clipStr(params.getString("tag"), 20));
            Integer level = saneInt(params, "level", 1, 10);
            if (level != null) {
                query.setLevel(level);
            }
            Integer status = saneInt(params, "status", 0, 2);
            if (status != null) {
                query.setStatus(status);
            }
            // v2 过滤器：价格区间
            BigDecimal priceMin = saneDecimal(params, "price_min");
            if (priceMin != null) {
                query.setPriceMin(priceMin);
            }
            BigDecimal priceMax = saneDecimal(params, "price_max");
            if (priceMax != null) {
                query.setPriceMax(priceMax);
            }
            // v2 过滤器：建档时间区间（yyyy-MM-dd；end 取当日 23:59:59 含端）
            Date timeStart = saneDate(params.getString("time_start"), false);
            if (timeStart != null) {
                query.setCreateTimeStart(timeStart);
            }
            Date timeEnd = saneDate(params.getString("time_end"), true);
            if (timeEnd != null) {
                query.setCreateTimeEnd(timeEnd);
            }
            // v2 过滤器：仓位（名称精确/包含匹配用户仓位清单 → stationIds 组查询）
            List<String> stationIds = resolveStationIds(params.getString("station"), stations);
            if (!stationIds.isEmpty()) {
                query.setStationIds(stationIds);
            }
            // v2 过滤器：排序（白名单 + 方向）
            String orderBy = StrUtil.trimToNull(params.getString("order_by"));
            if (orderBy != null && ORDER_BY_WHITELIST.contains(orderBy)) {
                query.setOrderBy(orderBy);
            }
            Boolean ascending = params.getBoolean("ascending");
            if (ascending != null) {
                query.setAscending(ascending);
            }
        }
        Integer page = saneInt(params, "page", 1, MAX_PAGE);
        Integer pageSize = saneInt(params, "pageSize", 1, MAX_PAGE_SIZE);
        query.setPageNo(page == null ? 1 : page);
        query.setPageSize(pageSize == null ? MAX_PAGE_SIZE : pageSize);

        // 意图缓存（P4）：search_items 结果卡 Redis TTL 缓存，命中直接返回（cached 标记入埋点）；
        // 写失效由 app 侧物品写路径 evictUid 保证，缓存异常一律视为未命中不影响查询
        String qcacheKey = null;
        if (queryCacheService.enabled()) {
            try {
                qcacheKey = queryCacheService.cacheKey(uid, query, userText);
                JSONObject cachedCard = queryCacheService.get(qcacheKey);
                if (cachedCard != null) {
                    JSONObject traceData = new JSONObject(true);
                    traceData.put("cached", true);
                    traceData.put("stages", List.of(stage("cache", true, 0)));
                    traceData.put("queryText", query.getKeyword());
                    traceData.put("categoryWords", query.getCategoryList());
                    traceData.put("finalTotal", cachedCard.getIntValue("total"));
                    traceData.put("totalMs", 0);
                    observabilityPort.fire("ai_search_trace", uid, traceData);
                    return cachedCard;
                }
            } catch (Exception ignore) {
                qcacheKey = null;
            }
        }

        // 检索新链（P2-6）：仅 keyword 非空时执行 ① 别名扩展 → 索引查询 ② 索引直查；
        // 未命中回退原兜底链 ③④⑤（keyword 转分类 / 分类漂移 / 渐进放宽），行为不变
        Page<AiItemRow> result = null;
        String hitSource = null;
        long traceStart = System.nanoTime();
        List<Map<String, Object>> traceStages = new ArrayList<>();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            if (tuningConfigService.boolOpt(org.jeecg.modules.ai.config.TuningConfigService.KEY_SEARCH_ALIAS_ENABLED, aliasEnabled)) {
                long t0 = System.nanoTime();
                List<String> terms = itemReadPort.aliasExpand(query.getKeyword());
                if (terms.size() > 1) {
                    Page<AiItemRow> aliasResult = itemReadPort.searchIndexQuery(uid, terms, query);
                    boolean hit = aliasResult.getTotal() > 0;
                    traceStages.add(stage("alias", hit, (System.nanoTime() - t0) / 1_000_000));
                    if (hit) {
                        result = aliasResult;
                        hitSource = "alias:" + terms.get(1);
                    }
                } else {
                    traceStages.add(stage("alias", false, (System.nanoTime() - t0) / 1_000_000));
                }
            }
            if (result == null && tuningConfigService.boolOpt(org.jeecg.modules.ai.config.TuningConfigService.KEY_SEARCH_INDEX_ENABLED, indexEnabled)) {
                long t1 = System.nanoTime();
                Page<AiItemRow> indexResult = itemReadPort.searchIndexQuery(uid, List.of(query.getKeyword()), query);
                boolean hitIdx = indexResult.getTotal() > 0;
                traceStages.add(stage("index", hitIdx, (System.nanoTime() - t1) / 1_000_000));
                if (hitIdx) {
                    result = indexResult;
                    hitSource = "index";
                }
            }
        }
        if (result == null) {
            result = itemReadPort.listByCondition(query);
        }
        log.info("[ai-query] search keyword={}, categoryList={}, firstTotal={}, hitSource={}, userCategories={}",
                query.getKeyword(), query.getCategoryList(), result.getTotal(), hitSource,
                categories.stream().filter(AiCategoryRef::isUser).map(AiCategoryRef::getName).toList());
        // keyword 转分类兜底：模型把泛称（如「证件」）当关键词且未给任何分类参数时，
        // 首查 0 行则尝试将 keyword 按分类名/备注解析（复用三级放宽匹配），
        // 命中分类后按分类重查物品，补齐「查询词命中分类维度」的召回路径
        if (result.getTotal() == 0 && query.getKeyword() != null
                && (query.getCategoryList() == null || query.getCategoryList().isEmpty())) {
            long tKac = System.nanoTime();
            JSONObject keywordAsCategory = new JSONObject();
            keywordAsCategory.put("category", query.getKeyword());
            Set<String> fallback = resolveCategoryValues(uid, keywordAsCategory, categories, usedCategoryValues);
            log.info("[ai-query] keyword-as-category fallback: keyword={}, resolved={}, usedValues={}",
                    query.getKeyword(), fallback, usedCategoryValues);
            if (!fallback.isEmpty()) {
                result = itemReadPort.listByCondition(new AiItemQuery(query)
                        .setKeyword(null)
                        .setCategoryList(new ArrayList<>(fallback)));
                log.info("[ai-query] keyword-as-category retryTotal={}", result.getTotal());
                traceStages.add(stage("keyword_as_category", result.getTotal() > 0,
                        (System.nanoTime() - tKac) / 1_000_000));
            } else {
                traceStages.add(stage("keyword_as_category", false, (System.nanoTime() - tKac) / 1_000_000));
            }
        }
        // 分类漂移扩展：LLM 输出的近名分类（如「证件票据」）精确命中字典，但物品实际
        // 挂在细粒度在用值（「身份证」「护照」等建档自由词）下，按分类 id/名称过滤必 0 行。
        // 两级召回：①分类名/备注 ↔ 物品在用值双向包含（实例化分类继承系统分类 remark）
        // ②remark 词表覆盖不到的自由词 → LLM 在用值挑选（白名单内）
        if (result.getTotal() == 0 && query.getCategoryList() != null
                && !query.getCategoryList().isEmpty()) {
            Set<String> expanded = new LinkedHashSet<>(query.getCategoryList());
            for (String v : query.getCategoryList()) {
                expanded.addAll(expandRelatedCategories(v, categories));
            }
            for (String v : usedCategoryValues) {
                for (String q : query.getCategoryList()) {
                    if (looseContains(q, v)) {
                        expanded.add(v);
                    }
                }
            }
            List<AiCategoryRef> matched = new ArrayList<>();
            for (AiCategoryRef c : categories) {
                if (c.isUser() && c.getName() != null
                        && query.getCategoryList().contains(c.getName())) {
                    matched.add(c);
                }
            }
            for (String v : usedCategoryValues) {
                for (AiCategoryRef c : matched) {
                    if (looseContains(v, c.getName()) || looseContains(v, c.getRemark())) {
                        expanded.add(v);
                        break;
                    }
                }
            }
            // remark 词表只覆盖部分建档自由词（如含「身份证」但无「护照」「驾驶证」），
            // LLM 挑值不受重查结果短路：总是执行并与 remark 匹配合并为一次重查
            long tLlm = System.nanoTime();
            Set<String> picked = pickValuesByLlm(userText, query.getCategoryList(), usedCategoryValues);
            log.info("[ai-query] llm-value-pick picked={}", picked);
            traceStages.add(stage("llm_pick", !picked.isEmpty(), (System.nanoTime() - tLlm) / 1_000_000));
            expanded.addAll(picked);
            if (expanded.size() > query.getCategoryList().size()) {
                long tDrift = System.nanoTime();
                log.info("[ai-query] category-drift expand: {} -> {}", query.getCategoryList(), expanded);
                result = itemReadPort.listByCondition(new AiItemQuery(query)
                        .setCategoryList(new ArrayList<>(expanded)));
                traceStages.add(stage("category_drift", result.getTotal() > 0,
                        (System.nanoTime() - tDrift) / 1_000_000));
            }
        }
        // 渐进放宽：keyword 与 categoryList 同时生效时 AND 语义易双空（如模型把泛称
        // 「证件」同时塞进 keyword 与 categories，物品名「身份证」不含「证件」）。
        // 全量 0 行时依次丢 keyword → 丢 categoryList 重试，最大化召回
        if (result.getTotal() == 0 && query.getKeyword() != null
                && query.getCategoryList() != null && !query.getCategoryList().isEmpty()) {
            long tRel = System.nanoTime();
            result = itemReadPort.listByCondition(new AiItemQuery(query)
                    .setKeyword(null));
            if (result.getTotal() == 0) {
                result = itemReadPort.listByCondition(new AiItemQuery(query)
                        .setCategoryList(null)
                        .setKeyword(relaxedKeyword(query.getKeyword())));
            }
            traceStages.add(stage("relaxed", result.getTotal() > 0, (System.nanoTime() - tRel) / 1_000_000));
        }

        Map<String, String> categoryNames = categoryNames(categories);
        JSONArray items = new JSONArray();
        for (AiItemRow item : result.getRecords()) {
            JSONObject row = new JSONObject(true);
            row.put("itemId", String.valueOf(item.getId()));
            row.put("oriId", item.getOriId());
            row.put("name", item.getName());
            row.put("category", displayCategory(item.getCategory(), categoryNames));
            row.put("icon", item.getIcon());
            row.put("quantity", item.getQuantity());
            row.put("status", item.getStatus());
            row.put("statusLabel", STATUS_LABELS.getOrDefault(item.getStatus(), "未知"));
            row.put("price", item.getPrice());
            row.put("createTime", item.getCreateTime() == null ? null : item.getCreateTime().getTime());
            items.add(row);
        }
        // 兜底链埋点（P2-7 + P4 增强）：单查询 1 条 ai_search_trace
        // {stages:[{stage,hit,ms}], totalMs, queryText, categoryWords, finalTotal}
        // queryText/finalTotal 为样本归因与低命中样本自动入库（观测→样本池闭环）必需
        if (!traceStages.isEmpty()) {
            JSONObject traceData = new JSONObject(true);
            traceData.put("stages", traceStages);
            traceData.put("totalMs", (System.nanoTime() - traceStart) / 1_000_000);
            traceData.put("queryText", query.getKeyword());
            traceData.put("categoryWords", query.getCategoryList());
            traceData.put("finalTotal", result.getTotal());
            observabilityPort.fire("ai_search_trace", uid, traceData);
        }
        JSONObject card = new JSONObject(true);
        card.put("card", "query");
        card.put("type", "search_items");
        if (hitSource != null) {
            card.put("hitSource", hitSource);
        }
        card.put("total", result.getTotal());
        card.put("page", query.getPageNo());
        card.put("pageSize", query.getPageSize());
        card.put("items", items);
        // 意图缓存回填（P4）：仅在进入缓存判定且未命中时写入，异常吞掉
        if (qcacheKey != null) {
            queryCacheService.put(qcacheKey, card);
        }
        return card;
    }

    /**
     * 关键词检索卡（P5 update 定位失败回退）：复用 searchCard 全兜底链与意图缓存，
     * 按 keyword（可选 category 消歧）构造查询参数。userText 供 LLM 挑值与缓存键归一。
     */
    public JSONObject keywordSearchCard(String uid, String keyword, String category, String userText) {
        List<AiCategoryRef> categories = taxonomyPort.listUnifiedCategories(uid);
        Set<String> usedCategoryValues = distinctItemCategories(uid);
        JSONObject params = new JSONObject(true);
        params.put("keyword", clipStr(keyword, MAX_KEYWORD));
        if (StrUtil.isNotBlank(category)) {
            params.put("category", category);
        }
        return searchCard(uid, categories, usedCategoryValues, params, null, userText);
    }

    /** 埋点 stage 结构：{stage, hit, ms} */
    private Map<String, Object> stage(String name, boolean hit, long ms) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("stage", name);
        s.put("hit", hit);
        s.put("ms", ms);
        return s;
    }

    /**
     * 分类漂移扩展：查询分类词与全部用户分类做双向包含匹配，
     * 收集所有相似分类（不 return，区别于 collectCategoryValues 的单命中），
     * 如「证件票据」→ 同时并入「证件」「票据」等，避免近名分类漂移漏召回
     */
    private Set<String> expandRelatedCategories(String raw, List<AiCategoryRef> categories) {
        String name = StrUtil.trimToNull(raw);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (name == null) {
            return out;
        }
        for (AiCategoryRef c : categories) {
            if (!c.isUser()) {
                continue;
            }
            if (name.equals(c.getName()) || fuzzyCategoryHit(name, c)) {
                out.add(String.valueOf(c.getId()));
                if (c.getName() != null) {
                    out.add(c.getName());
                }
            }
        }
        return out;
    }

    /** LLM 在用值挑选 prompt：分类 0 行且 remark 词表未覆盖时，让模型从物品在用值清单挑相关值 */
    private static final String VALUE_PICK_PROMPT = """
            你是物品检索助手。用户按分类查询物品但没有命中，需要从该用户物品实际使用的分类值清单中挑选与查询相关的值。
            用户查询原文：{{USER_TEXT}}
            查询分类词：{{CATEGORY_WORDS}}
            物品在用分类值清单（JSON 数组）：{{USED_VALUES}}
            规则：只从清单中选择语义相关的值（如查询证件相关时选「身份证」「护照」等证件类值，查询数码相关时选「手机」「相机」等）；语义无关或不确定就不选；最多 10 个。
            仅输出 JSON：{"values": ["值1","值2"]}，无关时输出 {"values": []}""";

    /**
     * LLM 在用值挑选：把物品在用值清单给模型选择与查询相关的值。
     * 结果做白名单过滤（仅收清单内值，防注入越权），上限 10 个防卡片膨胀
     */
    private Set<String> pickValuesByLlm(String userText, List<String> categoryWords, Set<String> usedValues) {
        if (StrUtil.isBlank(userText) || usedValues.isEmpty() || usedValues.size() > 80) {
            return Set.of();
        }
        String prompt = VALUE_PICK_PROMPT
                .replace("{{USER_TEXT}}", userText)
                .replace("{{CATEGORY_WORDS}}", String.join("、", categoryWords))
                .replace("{{USED_VALUES}}", JSON.toJSONString(usedValues));
        JSONObject out = callStructured(prompt, userText);
        JSONArray arr = out == null ? null : out.getJSONArray("values");
        Set<String> picked = new LinkedHashSet<>();
        if (arr != null) {
            for (int i = 0; i < arr.size() && picked.size() < 10; i++) {
                String v = arr.getString(i);
                if (StrUtil.isNotBlank(v) && usedValues.contains(v.trim())) {
                    picked.add(v.trim());
                }
            }
        }
        return picked;
    }

    /** 泛称后缀剥离：「游戏装备」→「游戏」、「化妆品类」→「化妆品」，剥离后过短则返回原词 */
    private String relaxedKeyword(String keyword) {
        String relaxed = keyword.trim();
        for (String suffix : new String[]{"装备", "相关", "东西", "物品", "用品", "类"}) {
            while (relaxed.endsWith(suffix) && relaxed.length() - suffix.length() >= 2) {
                relaxed = relaxed.substring(0, relaxed.length() - suffix.length());
            }
        }
        return relaxed;
    }

    /** get_stats：聚合统计（总数/按状态/按分类/总价值），分类 id 映射为展示名 */
    private JSONObject statsCard(String uid, List<AiCategoryRef> categories) {
        JSONObject stats = itemReadPort.stats(uid);
        Map<String, String> categoryNames = categoryNames(categories);
        JSONObject byCategoryNamed = new JSONObject(true);
        JSONObject byCategory = stats.getJSONObject("byCategory");
        if (byCategory != null) {
            for (String id : byCategory.keySet()) {
                String name = displayCategory(id, categoryNames);
                int prev = byCategoryNamed.getIntValue(name);
                byCategoryNamed.put(name, prev + byCategory.getIntValue(id));
            }
        }
        JSONObject view = new JSONObject(true);
        view.put("total", stats.get("total"));
        view.put("totalValue", stats.get("totalValue"));
        view.put("byStatus", stats.get("byStatus"));
        // byStatus 的中文标签版（0临时 1正常 2使用中），供第二段模型准确转述
        JSONObject byStatusLabeled = new JSONObject(true);
        JSONObject byStatusRaw = stats.getJSONObject("byStatus");
        if (byStatusRaw != null) {
            for (String code : byStatusRaw.keySet()) {
                Integer key = StrUtil.isNumeric(code) ? Integer.valueOf(code) : null;
                byStatusLabeled.put(key == null ? code : STATUS_LABELS.getOrDefault(key, code), byStatusRaw.get(code));
            }
        }
        view.put("byStatusLabels", byStatusLabeled);
        view.put("byCategory", byCategoryNamed);
        JSONObject card = new JSONObject(true);
        card.put("card", "query");
        card.put("type", "get_stats");
        card.put("stats", view);
        return card;
    }

    /** get_item_lineage：流转账本（itemId 直查，缺省用 itemName 精确解析；去哈希仅保留用户可读事件） */
    private JSONObject lineageCard(String uid, JSONObject params) {
        String itemId = params == null ? null : StrUtil.trimToNull(params.getString("itemId"));
        String itemName = params == null ? null : StrUtil.trimToNull(params.getString("itemName"));
        if (itemId == null && itemName == null) {
            return null;
        }
        String resolvedId = null;
        if (itemId != null) {
            AiItemRow row = itemReadPort.queryByItemId(uid, itemId);
            if (row != null) {
                resolvedId = String.valueOf(row.getId());
            }
        }
        if (resolvedId == null && itemName != null) {
            // 名称兜底：唯一命中才解析（0 件/多件均返回 null，让用户说得更具体）
            Page<AiItemRow> byName = itemReadPort.listByCondition(new AiItemQuery()
                    .setUserId(uid)
                    .setKeyword(itemName)
                    .setPageNo(1)
                    .setPageSize(2));
            if (byName.getTotal() == 1) {
                resolvedId = String.valueOf(byName.getRecords().get(0).getId());
            }
        }
        if (resolvedId == null) {
            return null;
        }
        JSONObject lineage = itemReadPort.lineage(uid, resolvedId);
        if (lineage == null) {
            return null;
        }
        JSONArray raw = lineage.getJSONArray("events");
        JSONArray events = new JSONArray();
        if (raw != null) {
            Map<String, String> opLabels = new LinkedHashMap<>();
            for (MipEventType t : MipEventType.values()) {
                opLabels.put(t.getCode(), t.getLabel());
            }
            for (int i = Math.max(0, raw.size() - MAX_LINEAGE_EVENTS); i < raw.size(); i++) {
                JSONObject e = raw.getJSONObject(i);
                JSONObject ev = new JSONObject(true);
                String opType = e.getString("opType");
                ev.put("opType", opType);
                ev.put("opLabel", opLabels.getOrDefault(opType, opType));
                ev.put("operator", e.getString("operator"));
                ev.put("qty", e.get("qty"));
                ev.put("occurredAt", e.get("occurredAt"));
                events.add(ev);
            }
        }
        JSONObject card = new JSONObject(true);
        card.put("card", "query");
        card.put("type", "get_item_lineage");
        card.put("itemId", resolvedId);
        card.put("eventCount", lineage.getIntValue("event_count"));
        card.put("chainValid", Boolean.TRUE.equals(lineage.getBoolean("chainValid")));
        card.put("events", events);
        return card;
    }

    // ================================ v2 · get_item 详情卡 ================================

    /**
     * get_item：单物品定位（itemId / itemName 二选一，可选 category 消歧）→ 详情卡。
     * 属性摘要取自 itemDetail 出域实例（MipRedlineFilter 已过滤 restricted，第二重防线兜底）；
     * 0/多件返回 null，由调用方回退 search_items 关键词卡。
     */
    private JSONObject getItemCard(String uid, JSONObject params, List<AiCategoryRef> categories,
                                   List<AiStationRef> stations) {
        if (params == null) {
            return null;
        }
        String itemId = StrUtil.trimToNull(params.getString("itemId"));
        String itemName = StrUtil.trimToNull(params.getString("itemName"));
        String categoryHint = StrUtil.trimToNull(params.getString("category"));

        AiItemRow row = null;
        if (itemId != null) {
            row = itemReadPort.queryByItemId(uid, itemId);
        }
        if (row == null && itemName != null) {
            // 名称唯一命中才出详情卡（0/多件回退 search 卡）；category 消歧：命中分类值后重新限定查询
            AiItemQuery q = new AiItemQuery().setUserId(uid).setKeyword(itemName).setPageNo(1).setPageSize(2);
            Set<String> hint = new LinkedHashSet<>();
            collectCategoryValues(uid, categoryHint, categories, distinctItemCategories(uid), hint);
            if (!hint.isEmpty()) {
                q.setCategoryList(new ArrayList<>(hint));
            }
            Page<AiItemRow> byName = itemReadPort.listByCondition(q);
            if (byName.getTotal() == 1) {
                row = byName.getRecords().get(0);
            }
        }
        if (row == null) {
            return null;
        }

        // 出域实例：属性摘要（restricted 已被 MipRedlineFilter 过滤）+ 流转计数
        JSONObject detail = itemReadPort.itemDetail(uid, String.valueOf(row.getId()));
        JSONArray properties = new JSONArray();
        if (detail != null) {
            JSONArray attributes = detail.getJSONArray("attributes");
            if (attributes != null) {
                for (int i = 0; i < attributes.size() && properties.size() < MAX_DETAIL_PROPS; i++) {
                    JSONObject a = attributes.getJSONObject(i);
                    Integer el = a == null ? null : a.getInteger("element");
                    if (el == null || DETAIL_PROP_EXCLUDE.contains(el)) {
                        continue;
                    }
                    if (StrUtil.isBlank(a.getString("name"))) {
                        continue;
                    }
                    JSONObject p = new JSONObject(true);
                    p.put("name", a.getString("name"));
                    p.put("value", a.get("value"));
                    properties.add(p);
                }
            }
        }
        int eventCount = 0;
        try {
            JSONObject lineage = itemReadPort.lineage(uid, String.valueOf(row.getId()));
            if (lineage != null) {
                eventCount = lineage.getIntValue("event_count");
            }
        } catch (Exception e) {
            log.warn("[ai-query] get_item lineage 统计失败 itemId={}: {}", row.getId(), e.getMessage());
        }

        JSONObject item = new JSONObject(true);
        item.put("itemId", String.valueOf(row.getId()));
        item.put("oriId", row.getOriId());
        item.put("name", row.getName());
        item.put("category", displayCategory(row.getCategory(), categoryNames(categories)));
        item.put("icon", row.getIcon());
        item.put("level", row.getLevel());
        item.put("quantity", row.getQuantity());
        item.put("status", row.getStatus());
        item.put("statusLabel", STATUS_LABELS.getOrDefault(row.getStatus(), "未知"));
        item.put("station", stationName(row.getStationId(), stations));
        item.put("price", row.getPrice());
        item.put("createTime", row.getCreateTime() == null ? null : row.getCreateTime().getTime());
        item.put("properties", properties);
        item.put("eventCount", eventCount);

        JSONObject card = new JSONObject(true);
        card.put("card", "query");
        card.put("type", "get_item");
        card.put("item", item);
        return card;
    }

    /** 仓位 id → 展示名（虚拟仓/真实站点仓统一映射；未命中返回 null） */
    private String stationName(String stationId, List<AiStationRef> stations) {
        if (StrUtil.isBlank(stationId) || stations == null) {
            return null;
        }
        for (AiStationRef s : stations) {
            if (stationId.equals(s.getId())) {
                return s.getName();
            }
        }
        return null;
    }

    /** station 名称 → stationIds（精确优先，其次包含匹配；未命中返回空集合 = 不过滤） */
    private List<String> resolveStationIds(String raw, List<AiStationRef> stations) {
        String name = StrUtil.trimToNull(raw);
        List<String> ids = new ArrayList<>();
        if (name == null || stations == null || stations.isEmpty()) {
            return ids;
        }
        for (AiStationRef s : stations) {
            if (name.equals(s.getName())) {
                ids.add(s.getId());
            }
        }
        if (ids.isEmpty()) {
            for (AiStationRef s : stations) {
                if (s.getName() != null && (s.getName().contains(name) || name.contains(s.getName()))) {
                    ids.add(s.getId());
                }
            }
        }
        return ids;
    }

    // ================================ 私有：解析与组装 ================================

    /** 第二段回答生成：失败返回 null 走模板兜底 */
    private String safeReply(String systemPrompt) {
        try {
            JSONObject out = callStructured(systemPrompt, "请按契约生成回答");
            return out == null ? null : StrUtil.trimToNull(out.getString("reply"));
        } catch (Exception e) {
            log.warn("[ai-query] 第二段回答生成失败，走模板兜底: {}", e.getMessage());
            return null;
        }
    }

    /** 第二段兜底模板：模型不可用时仍能给出确定性回答 */
    private String fallbackReply(String type, JSONObject card) {
        switch (type) {
            case "get_stats": {
                JSONObject stats = card.getJSONObject("stats");
                return "你共有 " + stats.get("total") + " 件物品，总价值 " + stats.get("totalValue") + " 元";
            }
            case "get_item": {
                JSONObject item = card.getJSONObject("item");
                if (item == null) {
                    return "没有找到这件物品，换个说法试试？";
                }
                StringBuilder sb = new StringBuilder("「").append(item.getString("name")).append("」")
                        .append("，分类 ").append(item.getString("category"))
                        .append("，数量 ").append(item.get("quantity"))
                        .append("，状态 ").append(item.getString("statusLabel"));
                if (item.get("station") != null) {
                    sb.append("，在 ").append(item.getString("station"));
                }
                return sb.toString();
            }
            case "get_item_lineage": {
                String suffix = card.getBooleanValue("chainValid")
                        ? "，账本校验通过" : "，账本校验异常，建议在 App 内核查";
                return "该物品共 " + card.getIntValue("eventCount") + " 条流转记录" + suffix;
            }
            default: {
                long total = card.getLongValue("total");
                if (total <= 0) {
                    return "没有找到相关物品，换个说法试试？";
                }
                StringBuilder sb = new StringBuilder("共找到 ").append(total).append(" 件");
                JSONArray items = card.getJSONArray("items");
                if (items != null && !items.isEmpty()) {
                    sb.append("，比如");
                    for (int i = 0; i < Math.min(3, items.size()); i++) {
                        if (i > 0) {
                            sb.append("、");
                        }
                        sb.append(StrUtil.blankToDefault(items.getJSONObject(i).getString("name"), "…"));
                    }
                }
                return sb.toString();
            }
        }
    }

    /** 注入第二段的结果 JSON：剥离 icon/oriId 等展示字段，减少上下文噪音 */
    private String answerJson(JSONObject card) {
        JSONObject copy = new JSONObject(true);
        copy.putAll(card);
        JSONArray items = copy.getJSONArray("items");
        if (items != null) {
            JSONArray slim = new JSONArray();
            for (int i = 0; i < items.size(); i++) {
                JSONObject it = items.getJSONObject(i);
                JSONObject c = new JSONObject(true);
                c.putAll(it);
                c.remove("icon");
                c.remove("oriId");
                slim.add(c);
            }
            copy.put("items", slim);
        }
        return copy.toJSONString();
    }

    /** 组查询：categories 分类名数组（≤10 个）+ 单数 category 兜底，解析为物品分类值集合（id/名字混合存储，均需匹配） */
    private Set<String> resolveCategoryValues(String uid, JSONObject params, List<AiCategoryRef> categories,
                                              Set<String> usedCategoryValues) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        JSONArray arr = params == null ? null : params.getJSONArray("categories");
        if (arr != null && !arr.isEmpty()) {
            for (int i = 0; i < arr.size() && out.size() < 10; i++) {
                collectCategoryValues(uid, arr.getString(i), categories, usedCategoryValues, out);
            }
        }
        if (out.isEmpty()) {
            collectCategoryValues(uid, params == null ? null : params.getString("category"),
                    categories, usedCategoryValues, out);
        }
        return out;
    }

    /**
     * 单个分类名/泛称 → 物品分类值（三级，逐级放宽）：
     * ① 用户分类命中（id/名称精确，或名称/备注与泛称双向包含——「游戏装备」→「游戏道具」、「身份证件」→备注含「身份证」）收 id/名称；
     * ② 系统分类命中（精确或双向包含，备注参与）→ 聚合该系统分类下该用户已实例化的用户分类值；
     * ③ 最后对物品在用值做精确/包含匹配（兼容历史清单外分类）。
     * 包含匹配要求两侧 ≥2 字，防单字误命中
     */
    private void collectCategoryValues(String uid, String raw, List<AiCategoryRef> categories,
                                       Set<String> usedCategoryValues, Set<String> out) {
        String name = StrUtil.trimToNull(raw);
        if (name == null || out.size() >= 10) {
            return;
        }
        for (AiCategoryRef c : categories) {
            if (c.isUser() && (name.equals(String.valueOf(c.getId())) || name.equalsIgnoreCase(c.getName()))) {
                out.add(String.valueOf(c.getId()));
                if (c.getName() != null) {
                    out.add(c.getName());
                }
                return;
            }
        }
        for (AiCategoryRef c : categories) {
            if (c.isUser() && fuzzyCategoryHit(name, c)) {
                out.add(String.valueOf(c.getId()));
                if (c.getName() != null) {
                    out.add(c.getName());
                }
                return;
            }
        }
        for (AiCategoryRef s : categories) {
            if (!s.isUser() && (name.equals(String.valueOf(s.getId())) || name.equalsIgnoreCase(s.getName()))) {
                out.addAll(taxonomyPort.categoryIdsBySystemName(uid, s.getName()));
                return;
            }
        }
        for (AiCategoryRef s : categories) {
            if (!s.isUser() && fuzzyCategoryHit(name, s)) {
                out.addAll(taxonomyPort.categoryIdsBySystemName(uid, s.getName()));
                return;
            }
        }
        for (String v : usedCategoryValues) {
            if (v.equalsIgnoreCase(name) || v.contains(name) || name.contains(v)) {
                out.add(v);
            }
        }
    }

    /** 泛称 ↔ 分类名/备注 双向包含命中（两侧 ≥2 字）：「游戏装备」↔「游戏」「证件」↔备注「身份证/护照等」 */
    private boolean fuzzyCategoryHit(String query, AiCategoryRef c) {
        if (looseContains(query, c.getName())) {
            return true;
        }
        return looseContains(query, c.getRemark());
    }

    /** 双向包含：a 包含 b 或 b 包含 a，且被包含侧 ≥2 字（防单字误命中） */
    private boolean looseContains(String a, String b) {
        if (StrUtil.isBlank(a) || StrUtil.isBlank(b)) {
            return false;
        }
        String x = a.trim();
        String y = b.trim();
        if (y.length() < 2 || x.length() < 2) {
            return false;
        }
        return x.contains(y) || y.contains(x);
    }

    /** 物品实际在用的分类值去重集合（含用户分类 id 与建档字典名），上限 40 防止提示词膨胀 */
    private Set<String> distinctItemCategories(String uid) {
        List<String> items = itemReadPort.listItemCategories(uid);
        Set<String> values = new LinkedHashSet<>();
        for (String it : items) {
            if (values.size() >= 40) {
                break;
            }
            if (StrUtil.isNotBlank(it)) {
                values.add(it);
            }
        }
        return values;
    }

    private Map<String, String> categoryNames(List<AiCategoryRef> categories) {
        Map<String, String> names = new LinkedHashMap<>();
        for (AiCategoryRef c : categories) {
            names.put(c.getId(), c.getName());
        }
        return names;
    }

    /** 分类展示名：空白/占位 id「0」/未映射 id 统一显示「未分类」 */
    private String displayCategory(String categoryId, Map<String, String> names) {
        if (StrUtil.isBlank(categoryId) || "0".equals(categoryId)) {
            return "未分类";
        }
        return names.getOrDefault(categoryId, categoryId);
    }

    private String recentHistory(String uid, String sessionId) {
        List<AiChatMessage> recent = messageService.listBySession(uid, sessionId);
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, recent.size() - 6);
        for (int i = from; i < recent.size(); i++) {
            AiChatMessage m = recent.get(i);
            sb.append(AiChatMessage.ROLE_USER.equals(m.getRole()) ? "用户" : "AI")
                    .append(": ").append(StrUtil.blankToDefault(m.getContent(), "（结果卡/过程条目）")).append('\n');
        }
        return sb.toString();
    }

    private JSONObject callStructured(String systemPrompt, String userText) {
        String raw = modelAdapter.structured(systemPrompt, userText);
        JSONObject out = tryParse(raw);
        if (out == null) {
            // 重试 ≤1 次：强调只输出 JSON
            raw = modelAdapter.structured(systemPrompt,
                    userText + "\n\n（重要：你上一条输出不是合法 JSON。请只输出一个合法 JSON 对象，不要任何解释或 markdown。）");
            out = tryParse(raw);
        }
        return out;
    }

    private JSONObject tryParse(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```(json)?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        try {
            return JSON.parseObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    // ================================ 私有：技能包与会话 ================================

    private String stage1Prompt() {
        String template = template();
        int split = template.lastIndexOf(STAGE_SPLIT_MARKER);
        return split < 0 ? template : template.substring(0, split);
    }

    private String stage2Prompt() {
        String template = template();
        int split = template.lastIndexOf(STAGE_SPLIT_MARKER);
        return split < 0 ? template : template.substring(split + STAGE_SPLIT_MARKER.length());
    }

    private String template() {
        // P4：DB enabled 配置优先于内置技能包文件（两段契约 STAGE_SPLIT 由保存侧校验完整性）
        return tuningConfigService.queryPrompt(builtinTemplate());
    }

    /** 内置技能包懒加载（保留双检锁单例缓存） */
    private String builtinTemplate() {
        if (promptTemplate == null) {
            synchronized (this) {
                if (promptTemplate == null) {
                    try (InputStream in = getClass().getClassLoader()
                            .getResourceAsStream("skills/mustard-item-query-v2/system_prompt.md")) {
                        if (in == null) {
                            throw new IllegalStateException("技能包缺失：skills/mustard-item-query-v2/system_prompt.md");
                        }
                        promptTemplate = IoUtil.read(in, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        throw new IllegalStateException("技能包加载失败", e);
                    }
                }
            }
        }
        return promptTemplate;
    }

    private AiChatSendVO finishText(String uid, String sessionId, AiChatSession session,
                                    AiChatMessage userMsg, String reply, AiStreamSink sink) {
        AiChatMessage msg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_TEXT, clip(reply, 200), null, null, null, null, sink);
        touchSession(session, null, reply);
        return new AiChatSendVO().setSessionId(sessionId)
                .setSessionStatus(session.getStatus())
                .setMessages(List.of(userMsg, msg));
    }

    private void touchSession(AiChatSession session, String userText, String summary) {
        if (userText != null && "新对话".equals(session.getTitle())) {
            session.setTitle(clip(userText, 20));
        }
        session.setLastSummary(clip(summary, 60));
        sessionService.touch(session);
    }

    private AiChatMessage appendMessage(String uid, String sessionId, String role, String type,
                                        String content, List<String> images, String processEntries,
                                        String draftSnapshot, Integer confidence, AiStreamSink sink) {
        AiChatMessage msg = new AiChatMessage()
                .setSessionId(sessionId)
                .setUid(uid)
                .setRole(role)
                .setMsgType(type)
                .setContent(content)
                .setImages(images == null || images.isEmpty() ? null : JSON.toJSONString(images))
                .setProcessEntries(processEntries)
                .setDraftSnapshot(draftSnapshot)
                .setConfidence(confidence);
        AiChatMessage saved = messageService.append(msg);
        // SSE 通道：assistant 消息落库即推（用户消息不推，App 端有乐观气泡）
        if (sink != null && AiChatMessage.ROLE_ASSISTANT.equals(role)) {
            sink.message(saved);
        }
        return saved;
    }

    // ================================ 私有：通用 ================================

    private Integer saneInt(JSONObject params, String key, int min, int max) {
        if (params == null) {
            return null;
        }
        Integer v = params.getInteger(key);
        if (v == null || v < min || v > max) {
            return null;
        }
        return v;
    }

    /** 价格参数（数字或数字字符串；负数/非法返回 null） */
    private BigDecimal saneDecimal(JSONObject params, String key) {
        if (params == null) {
            return null;
        }
        Object v = params.get(key);
        if (v instanceof Number n) {
            return n.doubleValue() >= 0 ? new BigDecimal(String.valueOf(n)) : null;
        }
        String s = StrUtil.trimToNull(String.valueOf(v == null ? "" : v));
        if (s == null) {
            return null;
        }
        try {
            BigDecimal d = new BigDecimal(s);
            return d.signum() >= 0 ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 时间参数（yyyy-MM-dd / yyyy-MM-dd HH:mm:ss；endOfDay 时取当日 23:59:59 含端） */
    private Date saneDate(String raw, boolean endOfDay) {
        String s = StrUtil.trimToNull(raw);
        if (s == null) {
            return null;
        }
        try {
            Date d = DateUtil.parse(s);
            return endOfDay ? DateUtil.endOfDay(d) : DateUtil.beginOfDay(d);
        } catch (Exception e) {
            return null;
        }
    }

    private String clipStr(String raw, int max) {
        String s = StrUtil.trimToNull(raw);
        return s == null ? null : clip(s, max);
    }

    private String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
