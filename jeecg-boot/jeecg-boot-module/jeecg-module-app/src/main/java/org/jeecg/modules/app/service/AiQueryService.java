package org.jeecg.modules.app.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.ai.AiModelAdapter;
import org.jeecg.modules.app.ai.AiRateLimiter;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.app.bean.vo.item.ItemInfoQueryVO;
import org.jeecg.modules.app.entity.AiChatMessage;
import org.jeecg.modules.app.entity.AiChatSession;
import org.jeecg.modules.app.entity.ItemInfo;
import org.jeecg.modules.app.entity.setting.UserCategory;
import org.jeecg.modules.app.service.impl.AiChatMessageServiceImpl;
import org.jeecg.modules.app.service.impl.AiChatSessionServiceImpl;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 * 模型无网络自由度；提示词与输出契约来自技能包 skills/mustard-item-query-v1。
 * <p>
 * 会话与消息复用 C1 双表（tbl_ai_chat_session/message）与 /app/ai/chat/send 端点，
 * 结果卡快照复用 draft_snapshot 列（msgType=query_result 时为查询结果 JSON）。
 */
@Slf4j
@Service
public class AiQueryService {

    /** 技能包两段契约分隔标记（全文唯一；代码取 lastIndexOf 兜底防说明文字误命中） */
    private static final String STAGE_SPLIT_MARKER = "<!-- ==== STAGE_SPLIT · 以下为第二段契约 ==== -->";

    /** 意图类型白名单（chat 为兜底类型，不在执行分支内） */
    private static final List<String> INTENT_TYPES = List.of("search_items", "get_stats", "get_item_lineage");
    /** 单次返回条目上限 */
    private static final int MAX_PAGE_SIZE = 10;
    /** 翻页上限 */
    private static final int MAX_PAGE = 5;
    /** 关键词长度上限 */
    private static final int MAX_KEYWORD = 50;
    /** 流转事件注入上限 */
    private static final int MAX_LINEAGE_EVENTS = 20;
    /** 状态码 → 展示名（0临时 1正常 2使用中） */
    private static final Map<Integer, String> STATUS_LABELS = Map.of(0, "临时", 1, "正常", 2, "使用中");

    /** 强查询语型：命中即走查询管线（含图片也生效） */
    private static final Pattern STRONG_QUERY = Pattern.compile(
            "多少|几[件双本支台个套枚张对]|列出|列出来|列一下|清单|哪些|统计|盘点|查一下|查询|搜一下|找一下|找出|查找|搜索");
    /** 弱查询语型：仅纯文本时走查询管线（带图多为建档/识别诉求，保持 C1 行为） */
    private static final Pattern WEAK_QUERY = Pattern.compile("有什么|都有什么|帮我看看|看看我|找找");

    private final AiChatSessionServiceImpl sessionService;
    private final AiChatMessageServiceImpl messageService;
    private final IUserCategoryService userCategoryService;
    private final IItemInfoService itemInfoService;
    private final AppOpenService appOpenService;
    private final AiModelAdapter modelAdapter;
    private final AiRateLimiter rateLimiter;

    private volatile String promptTemplate;

    public AiQueryService(AiChatSessionServiceImpl sessionService,
                          AiChatMessageServiceImpl messageService,
                          IUserCategoryService userCategoryService,
                          IItemInfoService itemInfoService,
                          AppOpenService appOpenService,
                          AiModelAdapter modelAdapter,
                          AiRateLimiter rateLimiter) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.userCategoryService = userCategoryService;
        this.itemInfoService = itemInfoService;
        this.appOpenService = appOpenService;
        this.modelAdapter = modelAdapter;
        this.rateLimiter = rateLimiter;
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
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        String limitError = rateLimiter.tryAcquireChat(uid);
        if (limitError != null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR, limitError);
        }
        List<String> imgs = images == null ? List.of() : images.stream().limit(3).toList();
        AiChatMessage userMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_USER,
                AiChatMessage.TYPE_TEXT, text, imgs, null, null, null);

        List<UserCategory> categories = userCategoryService.list(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, uid));

        // 物品实际在用的分类值（建档字典名等）：items.category 存的是分类名/id 混合值，
        // 分类字典 = 用户自定义分类 ∪ 物品在用值，供模型做泛称映射与过滤解析
        Set<String> usedCategoryValues = distinctItemCategories(uid);

        // 第一段：意图解析（技能包 stage-1 契约）
        JSONArray categoryView = new JSONArray();
        Set<String> knownNames = new LinkedHashSet<>();
        for (UserCategory c : categories) {
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
        JSONObject intent = callStructured(stage1Prompt()
                .replace("{{USER_CATEGORIES}}", categoryView.toJSONString())
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
            return finishText(uid, sessionId, session, userMsg, reply);
        }

        // 参数白名单校验 + 服务端执行（uid 圈定所有权，仅输出安全摘要字段）
        JSONObject params = intent.getJSONObject("params");
        JSONObject card;
        switch (type) {
            case "get_stats" -> card = statsCard(uid, categories);
            case "get_item_lineage" -> card = lineageCard(uid, params);
            default -> card = searchCard(uid, categories, usedCategoryValues, params);
        }
        if (card == null) {
            return finishText(uid, sessionId, session, userMsg,
                    "没有找到这件物品，说下它的名字我帮你查查看");
        }

        // 第二段：结果回答（模型失败走确定性模板兜底）
        String reply = safeReply(stage2Prompt()
                .replace("{{USER_TEXT}}", text)
                .replace("{{RESULT}}", answerJson(card)));
        if (reply == null) {
            reply = fallbackReply(type, card);
        }

        AiChatMessage resultMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_QUERY_RESULT, clip(reply, 200), null, null, card.toJSONString(), null);
        touchSession(session, text, reply);
        return new AiChatSendVO().setSessionId(sessionId)
                .setSessionStatus(session.getStatus())
                .setMessages(List.of(userMsg, resultMsg));
    }

    // ================================ 查询执行 ================================

    /** search_items：keyword/category/categories/tag/level/status + 分页，仅输出安全摘要字段（MIP 红线外不放属性明细） */
    private JSONObject searchCard(String uid, List<UserCategory> categories,
                                  Set<String> usedCategoryValues, JSONObject params) {
        ItemInfoQueryVO query = new ItemInfoQueryVO().setUserId(uid);
        if (params != null) {
            query.setKeyword(clipStr(params.getString("keyword"), MAX_KEYWORD));
            // 组查询优先：categories 数组（泛称映射多分类，如「证件」→[身份证,护照,驾驶证]）
            Set<String> categoryValues = resolveCategoryValues(params, categories, usedCategoryValues);
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
        }
        Integer page = saneInt(params, "page", 1, MAX_PAGE);
        Integer pageSize = saneInt(params, "pageSize", 1, MAX_PAGE_SIZE);
        query.setPageNo(page == null ? 1 : page);
        query.setPageSize(pageSize == null ? MAX_PAGE_SIZE : pageSize);

        Page<ItemInfo> result = itemInfoService.listByCondition(query);
        Map<String, String> categoryNames = categoryNames(categories);
        JSONArray items = new JSONArray();
        for (ItemInfo item : result.getRecords()) {
            JSONObject row = new JSONObject(true);
            row.put("itemId", String.valueOf(item.getId()));
            row.put("oriId", item.getOriId());
            row.put("name", item.getName());
            row.put("category", displayCategory(item.getCategory(), categoryNames));
            row.put("icon", item.getIcon());
            row.put("quantity", item.getQuantity());
            row.put("status", item.getStatus());
            row.put("statusLabel", STATUS_LABELS.getOrDefault(item.getStatus(), "未知"));
            row.put("createTime", item.getCreateTime() == null ? null : item.getCreateTime().getTime());
            items.add(row);
        }
        JSONObject card = new JSONObject(true);
        card.put("card", "query");
        card.put("type", "search_items");
        card.put("total", result.getTotal());
        card.put("page", query.getPageNo());
        card.put("pageSize", query.getPageSize());
        card.put("items", items);
        return card;
    }

    /** get_stats：聚合统计（总数/按状态/按分类/总价值），分类 id 映射为展示名 */
    private JSONObject statsCard(String uid, List<UserCategory> categories) {
        JSONObject stats = appOpenService.stats(uid);
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

    /** get_item_lineage：流转账本（itemId 支持 id/oriId，缺省用 itemName 精确解析；去哈希仅保留用户可读事件） */
    private JSONObject lineageCard(String uid, JSONObject params) {
        String itemId = params == null ? null : StrUtil.trimToNull(params.getString("itemId"));
        String itemName = params == null ? null : StrUtil.trimToNull(params.getString("itemName"));
        if (itemId == null && itemName == null) {
            return null;
        }
        String resolvedId = null;
        if (itemId != null) {
            if (appOpenService.itemDetail(uid, itemId) != null) {
                resolvedId = itemId;
            } else {
                ItemInfo byOriId = itemInfoService.queryItemInfoByOriId(uid, itemId);
                if (byOriId != null) {
                    resolvedId = String.valueOf(byOriId.getId());
                }
            }
        }
        if (resolvedId == null && itemName != null) {
            // 名称兜底：唯一命中才解析（0 件/多件均返回 null，让用户说得更具体）
            Page<ItemInfo> byName = itemInfoService.listByCondition(new ItemInfoQueryVO()
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
        JSONObject lineage = appOpenService.lineage(uid, resolvedId);
        if (lineage == null) {
            return null;
        }
        JSONArray raw = lineage.getJSONArray("events");
        JSONArray events = new JSONArray();
        if (raw != null) {
            Map<String, String> opLabels = new LinkedHashMap<>();
            for (org.jeecg.modules.app.mip.MipEventType t : org.jeecg.modules.app.mip.MipEventType.values()) {
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
    private Set<String> resolveCategoryValues(JSONObject params, List<UserCategory> categories,
                                              Set<String> usedCategoryValues) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        JSONArray arr = params == null ? null : params.getJSONArray("categories");
        if (arr != null && !arr.isEmpty()) {
            for (int i = 0; i < arr.size() && out.size() < 10; i++) {
                collectCategoryValues(arr.getString(i), categories, usedCategoryValues, out);
            }
        }
        if (out.isEmpty()) {
            collectCategoryValues(params == null ? null : params.getString("category"),
                    categories, usedCategoryValues, out);
        }
        return out;
    }

    /** 单个分类名 → 物品分类值：用户分类命中时 id/名称都收（历史数据两种都存），否则对物品在用值做精确/包含匹配 */
    private void collectCategoryValues(String raw, List<UserCategory> categories,
                                       Set<String> usedCategoryValues, Set<String> out) {
        String name = StrUtil.trimToNull(raw);
        if (name == null || out.size() >= 10) {
            return;
        }
        for (UserCategory c : categories) {
            if (name.equals(String.valueOf(c.getId())) || name.equalsIgnoreCase(c.getName())) {
                out.add(String.valueOf(c.getId()));
                if (c.getName() != null) {
                    out.add(c.getName());
                }
                return;
            }
        }
        for (String v : usedCategoryValues) {
            if (v.equalsIgnoreCase(name) || v.contains(name) || name.contains(v)) {
                out.add(v);
            }
        }
    }

    /** 物品实际在用的分类值去重集合（含用户分类 id 与建档字典名），上限 40 防止提示词膨胀 */
    private Set<String> distinctItemCategories(String uid) {
        List<ItemInfo> items = itemInfoService.list(new LambdaQueryWrapper<ItemInfo>()
                .select(ItemInfo::getCategory)
                .eq(ItemInfo::getUserId, uid)
                .isNotNull(ItemInfo::getCategory)
                .ne(ItemInfo::getCategory, ""));
        Set<String> values = new LinkedHashSet<>();
        for (ItemInfo it : items) {
            if (values.size() >= 40) {
                break;
            }
            if (StrUtil.isNotBlank(it.getCategory())) {
                values.add(it.getCategory());
            }
        }
        return values;
    }

    private Map<String, String> categoryNames(List<UserCategory> categories) {
        Map<String, String> names = new LinkedHashMap<>();
        for (UserCategory c : categories) {
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
        if (promptTemplate == null) {
            synchronized (this) {
                if (promptTemplate == null) {
                    try (InputStream in = getClass().getClassLoader()
                            .getResourceAsStream("skills/mustard-item-query-v1/system_prompt.md")) {
                        if (in == null) {
                            throw new IllegalStateException("技能包缺失：skills/mustard-item-query-v1/system_prompt.md");
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
                                    AiChatMessage userMsg, String reply) {
        AiChatMessage msg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_TEXT, clip(reply, 200), null, null, null, null);
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
                                        String draftSnapshot, Integer confidence) {
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
        return messageService.append(msg);
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
