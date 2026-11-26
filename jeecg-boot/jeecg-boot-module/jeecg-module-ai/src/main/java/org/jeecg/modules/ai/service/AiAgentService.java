package org.jeecg.modules.ai.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.adapter.AiModelAdapter;
import org.jeecg.modules.ai.adapter.AiRateLimiter;
import org.jeecg.modules.ai.bean.dto.AiCategoryRef;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.jeecg.modules.ai.bean.vo.ai.AiChatCreateVO;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.catalog.AiComponentCatalog;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.jeecg.modules.ai.entity.AiChatSession;
import org.jeecg.modules.ai.port.TaxonomyPort;
import org.jeecg.modules.ai.service.impl.AiChatMessageServiceImpl;
import org.jeecg.modules.ai.service.impl.AiChatSessionServiceImpl;
import org.jeecg.modules.ai.stream.AiStreamSink;
import org.jeecg.modules.ai.tooling.ItemTools;
import org.jeecg.modules.ai.tooling.TaxonomyTools;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AI 建档编排器 v2（方案 4.1 skill v2 契约 / 4.2 express / 4.3 自动分类标签）
 * <p>
 * 职责边界：AI 模型只产出结构化意图（单轮决策），创建/查重/自动建分类标签/持久化/限流
 * 全部由本编排器 + 受控工具层（ItemTools/TaxonomyTools）执行，模型无网络自由度；
 * 技能提示词/输出契约来自技能包 skills/mustard-item-filing-v2（声明式资源）。
 * <p>
 * v2 变更：通用 fields[] 解析（element 白名单 + restricted 拦截转手动补录）、
 * proposed 分类/标签提名（创建时经 TaxonomyTools 执行）、express 免确认创建
 * （direct=true 且 overall≥85）、intent 三分类路由兜底（AiIntentRouter）。
 */
@Slf4j
@Service
public class AiAgentService {

    /** 置信度门限：≥85 出草稿可直接创建 */
    private static final int CONFIDENCE_GATE = 85;
    /** 单会话追问上限 */
    private static final int MAX_ASKS = 3;
    /** 单会话图片上限 */
    private static final int MAX_IMAGES = 3;
    /** 标签上限（MIP 校验 E-TAGS：≤3） */
    private static final int MAX_TAGS = 3;
    /** 单次创建件数上限 */
    private static final int MAX_QTY = 99;
    /** 模式：普通对话建档 */
    public static final String MODE_NORMAL = "normal";
    /** 模式：express 快速建档（一轮直达，免确认自动创建） */
    public static final String MODE_EXPRESS = "express";
    /** 模式：mock 模拟建档（虚构补齐完整草稿，免追问，不走免确认创建） */
    public static final String MODE_MOCK = "mock";

    private final AiChatSessionServiceImpl sessionService;
    private final AiChatMessageServiceImpl messageService;
    private final TaxonomyPort taxonomyPort;
    private final AiModelAdapter modelAdapter;
    private final AiRateLimiter rateLimiter;
    /** C2 对话式查询编排器（意图分流后委托） */
    private final AiQueryService queryService;
    /** R4 受控工具：分类/标签自动创建 */
    private final TaxonomyTools taxonomyTools;
    /** R1/R3 受控工具：attributes 组装 + N 件创建 */
    private final ItemTools itemTools;

    private volatile String promptTemplate;

    public AiAgentService(AiChatSessionServiceImpl sessionService,
                          AiChatMessageServiceImpl messageService,
                          TaxonomyPort taxonomyPort,
                          AiModelAdapter modelAdapter,
                          AiRateLimiter rateLimiter,
                          AiQueryService queryService,
                          TaxonomyTools taxonomyTools,
                          ItemTools itemTools) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.taxonomyPort = taxonomyPort;
        this.modelAdapter = modelAdapter;
        this.rateLimiter = rateLimiter;
        this.queryService = queryService;
        this.taxonomyTools = taxonomyTools;
        this.itemTools = itemTools;
    }

    // ================================ S3/S4 · 发送与解析 ================================

    /**
     * 聊天发送：意图路由（正则快路径 + intent 兜底）→ 解析管线（vision 降级口述 / 通用 fields /
     * 置信度）→ 追问槽位 → 草稿快照；express 模式免确认自动创建。
     *
     * @param mode normal（默认，对话建档）| express（快速建档）| mock（模拟建档）
     */
    public AiChatSendVO send(String uid, String sessionId, String text, List<String> images, String mode) {
        return send(uid, sessionId, text, images, mode, null);
    }

    /**
     * 聊天发送（可带流式事件汇）：sink 非空为 SSE 通道——阶段/消息事件经 sink 推送，
     * 限流由端点前置（此处跳过防双重计数）；sink 为空为旧一次性 POST 行为，完全不变。
     *
     * @param mode normal（默认，对话建档）| express（快速建档）| mock（模拟建档）
     */
    public AiChatSendVO send(String uid, String sessionId, String text, List<String> images,
                             String mode, AiStreamSink sink) {
        if (StrUtil.isBlank(text) && (images == null || images.isEmpty())) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        boolean express = MODE_EXPRESS.equals(mode);
        // mock 模拟建档：显式 mode=mock 或自然语言模拟话术（express 显式模式优先；模拟话术不转查询管线）
        boolean mock = MODE_MOCK.equals(mode) || (!express && AiIntentRouter.isMockText(text));
        // C2 意图快路径：查询语型走对话式查询管线（复用同会话/限流/消息表）
        if (!mock && AiIntentRouter.routeToQuery(text, images, null, express)) {
            return AiIntentRouter.delegateQuery(queryService, uid, sessionId, text, images, sink);
        }
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        List<String> imgs = images == null ? List.of() : images.stream().limit(MAX_IMAGES).toList();

        // 解析管线（先于消息落库：intent 兜底路由命中查询时，由查询管线自行落库，避免消息重复）
        if (sink != null && !imgs.isEmpty()) {
            sink.progress("vision");
        }
        String visionDesc = resolveVision(imgs, text);
        String userText = StrUtil.blankToDefault(text, "（仅图片，无文字）");
        List<AiCategoryRef> categories = taxonomyPort.listUnifiedCategories(uid);
        String draftState = StrUtil.blankToDefault(session.getDraftSnapshot(), "null");
        int askBudget = (express || mock) ? 0 : Math.max(0, MAX_ASKS - nz(session.getAskCount()));
        String history = recentHistory(uid, sessionId);
        String topicsJson = JSON.toJSONString(taxonomyPort.listTopics());
        String systemPrompt = prompt()
                .replace("{{VISION_DESCRIPTION}}", StrUtil.blankToDefault(visionDesc, "（无视觉描述：无图片或口述模式）"))
                .replace("{{HISTORY}}", history)
                .replace("{{ASK_BUDGET}}", String.valueOf(askBudget))
                .replace("{{USER_CATEGORIES}}", JSON.toJSONString(categories))
                .replace("{{USER_TOPICS}}", topicsJson)
                .replace("{{MODE}}", express ? MODE_EXPRESS : mock ? MODE_MOCK : MODE_NORMAL)
                .replace("{{DRAFT_STATE}}", draftState)
                .replace("{{USER_TEXT}}", userText);

        if (sink != null) {
            sink.progress("parsing");
        }
        JSONObject decision = callStructured(systemPrompt, userText);
        String action = decision.getString("action");
        String reply = StrUtil.blankToDefault(decision.getString("reply"), "");
        JSONObject modelDraft = decision.getJSONObject("draft");
        JSONObject clarify = decision.getJSONObject("clarify");

        // intent 兜底路由：正则未命中但模型判定为查询（仅纯文本；同轮解析结果丢弃，查询管线独立解析；
        // mock 模拟建档跳过，防模型误判 query 抢走模拟建档）
        if (!mock && AiIntentRouter.routeToQuery(text, imgs, decision.getString("intent"), express)) {
            return AiIntentRouter.delegateQuery(queryService, uid, sessionId, text, images, sink);
        }

        // 限流：SSE 通道已由端点前置获取，此处仅旧一次性 POST 路径执行（防双重计数）
        if (sink == null) {
            String limitError = rateLimiter.tryAcquireChat(uid);
            if (limitError != null) {
                throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, limitError);
            }
        }

        // 1. 用户消息落库
        AiChatMessage userMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_USER,
                AiChatMessage.TYPE_TEXT, StrUtil.nullToEmpty(text), imgs, null, null, null, sink);

        List<AiChatMessage> assistantMsgs = new ArrayList<>();
        AiChatSendVO vo = new AiChatSendVO().setSessionId(sessionId);

        if ("clarify".equals(action) && clarify != null && askBudget > 0) {
            // S4 · 追问槽位：计数 +1，chips 随消息持久化
            String question = StrUtil.blankToDefault(clarify.getString("question"), "请补充一些信息？");
            JSONArray chips = clarify.getJSONArray("chips");
            if (chips == null) {
                chips = new JSONArray();
            }
            JSONObject chipBox = new JSONObject(true);
            chipBox.put("chips", chips);
            chipBox.put("slot", StrUtil.blankToDefault(clarify.getString("slot"), ""));
            JSONObject clarifyDraft = modelDraft == null ? null
                    : normalizeDraft(modelDraft, categories, imgs, userText);
            // 同草稿分支：真实图片建档时 icon 槽位直填首图（mock 不注入）
            if (!mock && clarifyDraft != null && !imgs.isEmpty()) {
                itemTools.injectCoverImage(clarifyDraft, imgs);
            }
            assistantMsgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_CLARIFY, question, null,
                    chipBox.toJSONString(),
                    clarifyDraft == null ? null : clarifyDraft.toJSONString(),
                    null, sink));
            if (clarifyDraft != null) {
                session.setDraftSnapshot(clarifyDraft.toJSONString());
            }
            session.setAskCount(nz(session.getAskCount()) + 1);
            session.setLastSummary(clip(question, 60));
            sessionService.touch(session);
            return vo.setAskRemaining(Math.max(0, MAX_ASKS - nz(session.getAskCount())))
                    .setSessionStatus(session.getStatus())
                    .setMessages(join(userMsg, assistantMsgs))
                    .setDraft(null);
        }

        // draft / chat（追问预算耗尽时模型返回 clarify 也降级为 draft/chat）
        if (modelDraft != null && !"chat".equals(action)) {
            JSONObject draft = normalizeDraft(modelDraft, categories, imgs, userText);
            // 真实图片建档：icon 槽位直填本轮首图作封面（App 卡片渲染照片而非示意图标）；
            // mock 模拟建档保持图标提名设计，不注入
            if (!mock && !imgs.isEmpty()) {
                itemTools.injectCoverImage(draft, imgs);
            }
            int overall = overallOf(draft);

            // express 直接建档：direct=true 且 overall≥85 → 免确认创建；失败/低置信落草稿降级。
            // manualFill 非空（restricted 拦截/敏感兜底命中）→ 不自动创建，降级草稿让用户补录后确认
            if (express && Boolean.TRUE.equals(draft.getBoolean("direct")) && overall >= CONFIDENCE_GATE
                    && draft.getJSONArray("manualFill") == null) {
                return expressCreate(uid, sessionId, session, userMsg, draft, overall, assistantMsgs, vo, sink);
            }

            AiChatMessage draftMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_DRAFT, reply, null, null, draft.toJSONString(), overall, sink);
            assistantMsgs.add(draftMsg);
            if ("新对话".equals(session.getTitle()) && StrUtil.isNotBlank(str(draft, "name"))) {
                session.setTitle(clip(str(draft, "name"), 20));
            }
            session.setDraftSnapshot(draft.toJSONString());
            session.setLastSummary(clip(reply.isEmpty() ? "草稿已生成：" + str(draft, "name") : reply, 60));
            sessionService.touch(session);
            return vo.setAskRemaining(Math.max(0, MAX_ASKS - nz(session.getAskCount())))
                    .setSessionStatus(session.getStatus())
                    .setMessages(join(userMsg, assistantMsgs))
                    .setDraft(draft)
                    .setOverallConfidence(overall);
        }

        // chat 闲聊/解释回合
        AiChatMessage chatMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_TEXT, reply, null, null, null, null, sink);
        assistantMsgs.add(chatMsg);
        session.setLastSummary(clip(reply, 60));
        sessionService.touch(session);
        return vo.setAskRemaining(Math.max(0, MAX_ASKS - nz(session.getAskCount())))
                .setSessionStatus(session.getStatus())
                .setMessages(join(userMsg, assistantMsgs));
    }

    // ================================ express 直接建档（方案 4.2） ================================

    /**
     * express 免确认自动创建：执行 proposed（分类/标签提名）→ ItemTools.createItems
     * → 过程条目 + 回执卡；失败自动降级草稿卡，不阻塞用户。
     */
    private AiChatSendVO expressCreate(String uid, String sessionId, AiChatSession session,
                                       AiChatMessage userMsg, JSONObject draft, int overall,
                                       List<AiChatMessage> assistantMsgs, AiChatSendVO vo, AiStreamSink sink) {
        // proposed 执行（失败回退不阻塞）
        applyProposed(uid, draft);
        int quantity = draftQuantity(draft);
        ItemTools.CreateOutcome outcome = itemTools.createItems(uid, draft, quantity);

        List<AiChatMessage> msgs = new ArrayList<>(assistantMsgs);
        JSONObject process = new JSONObject(true);
        process.put("step", "create");
        process.put("total", outcome.total());
        process.put("ok", outcome.itemIds().size());
        process.put("fail", outcome.total() - outcome.itemIds().size());
        process.put("itemIds", outcome.itemIds());
        process.put("express", true);
        msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_PROCESS, null, null, process.toJSONString(), null, null, sink));

        if (outcome.itemIds().isEmpty()) {
            // 降级：落草稿卡走手动确认
            String reply = "自动建档失败（" + StrUtil.blankToDefault(outcome.firstError(), "未知原因")
                    + "），已生成草稿，请确认后手动创建";
            AiChatMessage draftMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_DRAFT, reply, null, null, draft.toJSONString(), overall, sink);
            msgs.add(draftMsg);
            session.setDraftSnapshot(draft.toJSONString());
            session.setLastSummary(clip(reply, 60));
            sessionService.touch(session);
            return vo.setAskRemaining(0)
                    .setSessionStatus(session.getStatus())
                    .setMessages(join(userMsg, msgs))
                    .setDraft(draft)
                    .setOverallConfidence(overall)
                    .setDirect(false);
        }

        String receipt = "已创建（×" + outcome.quantity() + "）";
        JSONObject receiptBox = new JSONObject(true);
        receiptBox.put("count", outcome.quantity());
        receiptBox.put("itemIds", outcome.itemIds());
        receiptBox.put("express", true);
        msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_RECEIPT, receipt, null, receiptBox.toJSONString(), null, null, sink));
        if ("新对话".equals(session.getTitle()) && StrUtil.isNotBlank(str(draft, "name"))) {
            session.setTitle(clip(str(draft, "name"), 20));
        }
        session.setStatus(AiChatSession.STATUS_FILED);
        session.setDraftSnapshot(null);
        session.setAskCount(0);
        session.setLastSummary(receipt);
        sessionService.touch(session);
        return vo.setAskRemaining(0)
                .setSessionStatus(session.getStatus())
                .setMessages(join(userMsg, msgs))
                .setDraft(null)
                .setDirect(true)
                .setItemIds(outcome.itemIds())
                .setQuantity(outcome.quantity());
    }

    // ================================ S5 · 创建调度 ================================

    /**
     * 确认创建：数量 N（1–99 默认 1）→ proposed 执行（分类/标签提名，失败回退）
     * → ItemTools.createItems ×N → 过程条目 + 回执（×N 标注）。
     *
     * @param draftJson App 端就地编辑后的草稿 JSON（可选）；提供时归一化（类别映射/overall 重算）并回写会话快照
     */
    public AiChatCreateVO confirmCreate(String uid, String sessionId, Integer quantity, String draftJson) {
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        int n = quantity == null || quantity <= 0 ? 1 : Math.min(quantity, MAX_QTY);
        String limitError = rateLimiter.tryAcquireCreate(uid);
        if (limitError != null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, limitError);
        }
        JSONObject draft = StrUtil.isBlank(session.getDraftSnapshot()) ? null : JSON.parseObject(session.getDraftSnapshot());
        if (StrUtil.isNotBlank(draftJson)) {
            try {
                JSONObject edited = JSON.parseObject(draftJson);
                if (edited != null) {
                    List<AiCategoryRef> categories = taxonomyPort.listUnifiedCategories(uid);
                    draft = normalizeDraft(edited, categories, List.of(), null);
                    session.setDraftSnapshot(draft.toJSONString());
                }
            } catch (Exception e) {
                log.warn("[ai-agent] 编辑草稿解析失败，回退会话快照 uid={}", uid, e);
            }
        }
        // 置信度改为可选：不做门限阻断；confidence ≥85 → normal(status=1)，<85 → draft(status=0) 由创建链路落库
        if (draft == null || StrUtil.isBlank(str(draft, "name"))) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, "草稿未就绪：请先补充物品名称后再创建");
        }

        // proposed 执行（分类/标签提名，失败回退未分类/丢弃标签，不阻塞）
        applyProposed(uid, draft);

        ItemTools.CreateOutcome outcome = itemTools.createItems(uid, draft, n);
        List<String> itemIds = outcome.itemIds();
        String firstError = outcome.firstError();

        // 过程条目 + 回执（×N 标注；total/ok/fail 为记录口径，件数由回执 ×N 表达；itemIds 为云端 itemId）
        List<AiChatMessage> msgs = new ArrayList<>();
        JSONObject process = new JSONObject(true);
        process.put("step", "create");
        process.put("total", outcome.total());
        process.put("ok", itemIds.size());
        process.put("fail", outcome.total() - itemIds.size());
        process.put("itemIds", itemIds);
        msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_PROCESS, null, null, process.toJSONString(), null, null, null));
        AiChatCreateVO vo = new AiChatCreateVO().setSessionId(sessionId)
                .setTotal(n).setCount(itemIds.isEmpty() ? 0 : n).setItemIds(itemIds);
        if (itemIds.isEmpty()) {
            vo.setError(StrUtil.blankToDefault(firstError, "创建失败"));
            session.setLastSummary("创建失败");
        } else {
            String receipt = "已创建（×" + n + "）";
            JSONObject receiptBox = new JSONObject(true);
            receiptBox.put("count", n);
            receiptBox.put("itemIds", itemIds);
            msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_RECEIPT, receipt, null, receiptBox.toJSONString(), null, null, null));
            session.setStatus(AiChatSession.STATUS_FILED);
            session.setDraftSnapshot(null);
            session.setAskCount(0);
            session.setLastSummary(receipt);
        }
        sessionService.touch(session);
        return vo.setMessages(msgs);
    }

    // ================================ R4 · proposed 执行 ================================

    /**
     * 执行草稿 proposed 提名：分类/标签查重或创建，成功后重映射 draft；
     * ensure 失败回退「未分类」/丢弃该标签，自动分类永不阻塞建档主链路。
     */
    private void applyProposed(String uid, JSONObject draft) {
        JSONObject proposed = draft.getJSONObject("proposed");
        if (proposed == null) {
            return;
        }
        // 分类提名：仅当前 category 未解析（value=0 占位）时执行
        JSONObject category = draft.getJSONObject("category");
        boolean unresolved = category == null || "0".equals(StrUtil.nullToEmpty(category.getString("value")));
        JSONObject propCategory = proposed.getJSONObject("category");
        if (unresolved && propCategory != null && StrUtil.isNotBlank(propCategory.getString("name"))) {
            TaxonomyTools.EnsureResult ensured = taxonomyTools.ensureCategory(uid,
                    propCategory.getString("name"), propCategory.getString("icon"));
            if (!ensured.fallback() && category == null) {
                category = new JSONObject(true);
            }
            if (!ensured.fallback() && category != null) {
                category.put("value", ensured.id());
                category.put("valueName", ensured.name());
                category.put("confidence", Math.max(nz(category.getInteger("confidence")), 80));
                draft.put("category", category);
            }
        }
        // 标签提名：合并进 tags 槽位（去重，≤3 截断）
        JSONArray propTags = proposed.getJSONArray("tags");
        if (propTags != null && !propTags.isEmpty()) {
            Set<String> tagNames = new LinkedHashSet<>();
            JSONArray tags = draft.getJSONArray("tags");
            if (tags != null) {
                for (int i = 0; i < tags.size(); i++) {
                    JSONObject t = tags.getJSONObject(i);
                    if (t != null && StrUtil.isNotBlank(t.getString("value"))) {
                        tagNames.add(t.getString("value").trim());
                    }
                }
            }
            for (int i = 0; i < propTags.size() && tagNames.size() < MAX_TAGS; i++) {
                JSONObject t = propTags.getJSONObject(i);
                if (t == null || StrUtil.isBlank(t.getString("name"))) {
                    continue;
                }
                TaxonomyTools.EnsureResult ensured = taxonomyTools.ensureTag(uid, t.getString("name"));
                if (!ensured.fallback()) {
                    tagNames.add(ensured.name());
                }
            }
            JSONArray merged = new JSONArray();
            for (String name : tagNames) {
                merged.add(slot(name, 80));
            }
            draft.put("tags", merged);
        }
        // 提名已消费，清空防重复执行
        draft.remove("proposed");
    }

    // ================================ 私有：解析与组装 ================================

    /**
     * 视觉链：deepseek-flash 图像理解（vision）→ 失败/未配置时口述降级（用户话术即物品描述，tool_schema.json fallback=verbatim）
     */
    private String resolveVision(List<String> images, String text) {
        if (images.isEmpty()) {
            return null;
        }
        try {
            String v = modelAdapter.vision(images, text);
            return v != null ? v : text; // vision 未实现/无图 → 口述模式
        } catch (Exception e) {
            log.warn("AI vision 调用失败，降级口述模式: {}", e.getMessage());
            return text; // 口述模式
        }
    }

    /** 上下文窗口：最近消息条数 / 总字符预算 / 单条压缩上限（user/assistant/追问分开） */
    private static final int HISTORY_MAX_MESSAGES = 12;
    private static final int HISTORY_MAX_CHARS = 6000;
    private static final int HISTORY_USER_CLIP = 200;
    private static final int HISTORY_ASSISTANT_CLIP = 300;
    private static final int HISTORY_ITEM_CLIP = 160;

    /**
     * 会话上下文装配：窗口 → 压缩 → 预算丢弃。
     * 1. 窗口：只取最近 {@link #HISTORY_MAX_MESSAGES} 条消息
     * 2. 压缩：user/assistant 文本按上限截断；draft/receipt/clarify 等组件消息
     *    转简短摘要行（草稿快照/卡片 JSON 很长，全量拼会挤占预算）
     * 3. 预算：总字符超 {@link #HISTORY_MAX_CHARS} 时从最旧开始丢弃，
     *    超大上下文允许丢失部分历史，丢弃量以「更早 N 条已省略」标注给模型
     */
    private String recentHistory(String uid, String sessionId) {
        List<AiChatMessage> recent = messageService.listBySession(uid, sessionId);
        int windowStart = Math.max(0, recent.size() - HISTORY_MAX_MESSAGES);
        List<String> lines = new ArrayList<>();
        for (int i = windowStart; i < recent.size(); i++) {
            String line = historyLine(recent.get(i));
            if (line != null) {
                lines.add(line);
            }
        }
        int total = 0;
        for (String line : lines) {
            total += line.length();
        }
        int drop = 0;
        while (total > HISTORY_MAX_CHARS && drop < lines.size() - 1) {
            total -= lines.get(drop).length();
            drop++;
        }
        StringBuilder sb = new StringBuilder();
        if (drop > 0) {
            sb.append("（更早 ").append(drop).append(" 条消息已省略）\n");
        }
        for (int i = drop; i < lines.size(); i++) {
            sb.append(lines.get(i)).append('\n');
        }
        return sb.toString();
    }

    /** 单条消息 → 历史行；组件消息转摘要行，文本消息按角色截断。返回 null 表示跳过 */
    private String historyLine(AiChatMessage m) {
        boolean isUser = AiChatMessage.ROLE_USER.equals(m.getRole());
        String role = isUser ? "用户" : "AI";
        String type = StrUtil.blankToDefault(m.getMsgType(), AiChatMessage.TYPE_TEXT);
        switch (type) {
            case AiChatMessage.TYPE_DRAFT: {
                String name = draftNameOf(m.getDraftSnapshot());
                return role + ": [出示建档草稿" + (name == null ? "" : "：" + name) + "]";
            }
            case AiChatMessage.TYPE_RECEIPT: {
                return role + ": " + clip(StrUtil.blankToDefault(m.getContent(), "[已创建物品档案]"), 60);
            }
            case AiChatMessage.TYPE_CLARIFY: {
                return role + ": [追问] " + clip(StrUtil.blankToDefault(m.getContent(), ""), HISTORY_ITEM_CLIP);
            }
            case AiChatMessage.TYPE_PROCESS: {
                return null; // 解析过程条目对后续决策无价值，直接跳过
            }
            case AiChatMessage.TYPE_QUERY_RESULT: {
                return role + ": [查询结果] " + clip(StrUtil.blankToDefault(m.getContent(), ""), HISTORY_ASSISTANT_CLIP);
            }
            default: {
                String content = StrUtil.blankToDefault(m.getContent(), "");
                if (StrUtil.isNotBlank(m.getImages())) {
                    content = "[图片] " + content;
                }
                if (StrUtil.isBlank(content.replace("[图片]", "").trim())) {
                    return null; // 纯图片且无文字的过程消息：LLM 已有本轮视觉描述，历史中无增量价值
                }
                return role + ": " + clip(content, isUser ? HISTORY_USER_CLIP : HISTORY_ASSISTANT_CLIP);
            }
        }
    }

    /** 草稿快照中的物品名（name 槽位 value），解析失败返回 null */
    private String draftNameOf(String draftSnapshot) {
        if (StrUtil.isBlank(draftSnapshot)) {
            return null;
        }
        try {
            JSONObject slot = JSON.parseObject(draftSnapshot).getJSONObject("name");
            return slot == null ? null : StrUtil.trimToNull(slot.getString("value"));
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject callStructured(String systemPrompt, String userText) {
        String raw = modelAdapter.structured(systemPrompt, userText);
        JSONObject decision = tryParse(raw);
        if (decision == null) {
            // 重试 ≤1 次：强调只输出 JSON
            raw = modelAdapter.structured(systemPrompt,
                    userText + "\n\n（重要：你上一条输出不是合法 JSON。请只输出一个合法 JSON 对象，不要任何解释或 markdown。）");
            decision = tryParse(raw);
        }
        if (decision == null) {
            decision = new JSONObject(true);
            decision.put("action", "chat");
            decision.put("reply", "没听清，请再描述一下物品（是什么、有什么特征）");
        }
        return decision;
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

    /**
     * 草稿规范化 v2：顶层槽位沿用 v1（类别名 → id 映射、tags ≤3 截断），
     * fields[] 通用组件白名单校验（restricted → manualFill 手动补录提示），
     * proposed/manualFill 透传进快照；userText 敏感词兜底（红线第三重防线）。
     */
    private JSONObject normalizeDraft(JSONObject modelDraft, List<AiCategoryRef> categories, List<String> imgs,
                                      String userText) {
        JSONObject draft = mergeDraft(null, modelDraft, categories);
        // fields[] 白名单校验（v2 通用组件槽）
        sanitizeFields(draft);
        // 用户口述敏感信息但模型未给 manualFill（如 express 省略）→ 注入手动补录提示
        ensureSensitiveHint(draft, userText);
        // 附图 → icon/图片组件兜底值（vision 元数据）
        if (!imgs.isEmpty()) {
            JSONObject icon = draft.getJSONObject("icon");
            if (icon == null || StrUtil.isBlank(icon.getString("value"))) {
                draft.put("icon", slot(imgs.get(0), 90));
            }
            // fields element 12 图片：附图 URL 兜底（模型未给时）
            ensureImagesField(draft, imgs);
        }
        return draft;
    }

    /** 用户原文敏感词模式（restricted 类元素语义：证件号/有效期/密码/兑换码/账号/凭证等） */
    private static final Pattern SENSITIVE_HINT_PATTERN = Pattern.compile(
            "证件|身份证|护照|驾驶证|行驶证|密码|兑换码|卡密|卡号|账号标识|购买凭证|有效期");

    /**
     * 敏感信息兜底（红线第三重防线）：用户原文命中敏感词但草稿无 manualFill 时注入提示。
     * sanitizeFields 只能拦截「模型输出了 restricted field」的场景；模型整体遗漏时由此兜底。
     */
    private void ensureSensitiveHint(JSONObject draft, String userText) {
        if (draft.containsKey("manualFill") || StrUtil.isBlank(userText)
                || !SENSITIVE_HINT_PATTERN.matcher(userText).find()) {
            return;
        }
        JSONArray hint = new JSONArray();
        hint.add("证件号等敏感信息请手动补录");
        draft.put("manualFill", hint);
    }

    /**
     * fields[] 校验：已知 element + 非 restricted + 非顶层槽位 + valueType 宽松合法；
     * 违规项丢弃，restricted 候选转 manualFill「请手动补录」提示（红线第二重防线）。
     */
    private void sanitizeFields(JSONObject draft) {
        JSONArray fields = draft.getJSONArray("fields");
        JSONArray manual = draft.getJSONArray("manualFill");
        Set<String> manualSet = new LinkedHashSet<>();
        if (manual != null) {
            for (int i = 0; i < manual.size(); i++) {
                String s = manual.getString(i);
                if (StrUtil.isNotBlank(s)) {
                    manualSet.add(s.trim());
                }
            }
        }
        if (fields == null || fields.isEmpty()) {
            if (!manualSet.isEmpty()) {
                draft.put("manualFill", new JSONArray(new ArrayList<>(manualSet)));
            } else {
                draft.remove("manualFill");
            }
            return;
        }
        JSONArray kept = new JSONArray();
        for (int i = 0; i < fields.size(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (f == null) {
                continue;
            }
            Integer element = f.getInteger("element");
            if (element == null) {
                continue;
            }
            if (AiComponentCatalog.isRestricted(element)) {
                manualSet.add(AiComponentCatalog.get(element).name() + "请手动补录");
                continue;
            }
            if (!AiComponentCatalog.isKnown(element) || AiComponentCatalog.TOP_LEVEL_ELEMENTS.contains(element)) {
                continue;
            }
            Object value = sanitizeFieldValue(element, f.get("value"));
            if (value == null) {
                continue;
            }
            JSONObject keptField = new JSONObject(true);
            keptField.put("element", element);
            keptField.put("value", value);
            keptField.put("confidence", Math.max(0, Math.min(100, nz(f.getInteger("confidence")))));
            kept.add(keptField);
        }
        draft.put("fields", kept);
        if (!manualSet.isEmpty()) {
            draft.put("manualFill", new JSONArray(new ArrayList<>(manualSet)));
        } else {
            draft.remove("manualFill");
        }
    }

    /** fieldValue 宽松校验（valueType 遵循 AiComponentCatalog；非法返回 null 丢弃） */
    private Object sanitizeFieldValue(int element, Object value) {
        if (value == null) {
            return null;
        }
        String type = AiComponentCatalog.valueType(element);
        switch (type == null ? "" : type) {
            case "int": {
                if (value instanceof Number n) {
                    return n.intValue();
                }
                String s = String.valueOf(value).trim();
                return StrUtil.isNumeric(s) ? Integer.valueOf(s) : null;
            }
            case "number": {
                if (value instanceof Number n) {
                    return n;
                }
                try {
                    return new java.math.BigDecimal(String.valueOf(value).trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case "date": {
                String s = String.valueOf(value).trim();
                return s.length() >= 8 ? s : null;
            }
            case "daterange": {
                if (value instanceof JSONArray arr && arr.size() == 2) {
                    return arr;
                }
                return null;
            }
            case "arrstr": {
                return toStringArray(value, false);
            }
            case "arrurl": {
                return toStringArray(value, true);
            }
            case "url": {
                String s = String.valueOf(value).trim();
                return (s.startsWith("http://") || s.startsWith("https://")) ? s : null;
            }
            case "obj": {
                return value instanceof JSONObject ? value : null;
            }
            case "arrobj": {
                return value instanceof JSONArray ? value : null;
            }
            default: {
                // string / html：任意非空字符串
                String s = String.valueOf(value).trim();
                return s.isEmpty() ? null : s;
            }
        }
    }

    /** 字符串数组规整（requireUrl 时逐项校验 http/https 前缀） */
    private Object toStringArray(Object value, boolean requireUrl) {
        JSONArray out = new JSONArray();
        if (value instanceof JSONArray arr) {
            for (int i = 0; i < arr.size(); i++) {
                String s = StrUtil.trimToNull(arr.getString(i));
                if (s == null) {
                    continue;
                }
                if (requireUrl && !s.startsWith("http://") && !s.startsWith("https://")) {
                    continue;
                }
                out.add(s);
            }
        } else {
            String s = StrUtil.trimToNull(String.valueOf(value));
            if (s != null && (!requireUrl || s.startsWith("http://") || s.startsWith("https://"))) {
                out.add(s);
            }
        }
        return out.isEmpty() ? null : out;
    }

    /** 附图 URL 兜底进 fields element 12（图片组件，模型未给时使用 vision 通道附图） */
    private void ensureImagesField(JSONObject draft, List<String> imgs) {
        JSONArray fields = draft.getJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                if (f != null && Integer.valueOf(12).equals(f.getInteger("element"))) {
                    return;
                }
            }
        }
        if (fields == null) {
            fields = new JSONArray();
        }
        JSONObject imagesField = new JSONObject(true);
        imagesField.put("element", 12);
        imagesField.put("value", new JSONArray(imgs));
        imagesField.put("confidence", 90);
        fields.add(imagesField);
        draft.put("fields", fields);
    }

    /** 草稿合并：base（会话快照）+ patch（模型本轮产出），回答追问时由模型负责合并，此处做结构兜底 */
    private JSONObject mergeDraft(String baseJson, JSONObject patch, List<AiCategoryRef> categories) {
        JSONObject draft = new JSONObject(true);
        List<String> slotKeys = List.of("name", "category", "tags", "description", "icon", "quantity", "attributes");
        for (String key : slotKeys) {
            if (baseJson != null && !baseJson.equals("null")) {
                try {
                    JSONObject base = JSON.parseObject(baseJson);
                    if (base != null) {
                        Object baseSlot = base.get(key);
                        if (baseSlot instanceof JSONObject || baseSlot instanceof JSONArray) {
                            draft.put(key, baseSlot);
                        }
                    }
                } catch (Exception ignored) {
                    // 快照损坏按空处理
                }
            }
        }
        if (patch != null) {
            for (String key : slotKeys) {
                Object slotValue = patch.get(key);
                if (slotValue instanceof JSONObject || slotValue instanceof JSONArray) {
                    draft.put(key, slotValue);
                }
            }
            // v2 扩展槽位：fields / proposed / manualFill / direct 透传（fields 稍后 sanitize）
            for (String key : List.of("fields", "proposed", "manualFill")) {
                Object v = patch.get(key);
                if (v instanceof JSONObject || v instanceof JSONArray) {
                    draft.put(key, v);
                }
            }
            if (patch.getBoolean("direct") != null) {
                draft.put("direct", patch.getBoolean("direct"));
            }
            // mock 模拟草稿标志透传（App/E2E 识别模拟草稿用）
            if (patch.getBoolean("mock") != null) {
                draft.put("mock", patch.getBoolean("mock"));
            }
        }
        // 类别解析（必做）：模型给名称或回显历史快照中的 id 都要正确归一
        JSONObject category = draft.getJSONObject("category");
        if (category == null) {
            category = new JSONObject(true);
            category.put("value", "");
            category.put("confidence", 0);
        }
        String rawValue = StrUtil.trimToNull(category.getString("value"));
        String rawName = StrUtil.trimToNull(category.getString("valueName"));
        String candidate = rawValue != null ? rawValue : rawName;
        String categoryId = null;
        String categoryName = null;
        if (candidate != null) {
            // 1) 候选命中用户分类 id（模型从 DRAFT_STATE 回显）→ 视为已解析
            for (AiCategoryRef c : categories) {
                if (candidate.equals(c.getId())) {
                    categoryId = c.getId();
                    categoryName = c.getName();
                    break;
                }
            }
            // 2) 候选命中名称 → 名称映射为 id
            if (categoryId == null) {
                final String mapped = mapCategory(candidate, categories);
                if (mapped != null) {
                    categoryId = mapped;
                    for (AiCategoryRef c : categories) {
                        if (c.getId().equals(mapped)) {
                            categoryName = c.getName();
                            break;
                        }
                    }
                }
            }
        }
        if (categoryId != null) {
            category.put("value", categoryId);
            category.put("valueName", categoryName);
        } else {
            category.put("value", "0");
            if (candidate != null) {
                category.put("valueName", candidate);
            }
            category.put("confidence", Math.min(nz(category.getInteger("confidence")), 40));
        }
        draft.put("category", category);
        // tags 截断 ≤3
        JSONArray tags = draft.getJSONArray("tags");
        if (tags != null && tags.size() > MAX_TAGS) {
            draft.put("tags", new JSONArray(new ArrayList<>(tags.subList(0, MAX_TAGS))));
        }
        return draft;
    }

    /** 类别名 → 用户分类 id：精确匹配优先，其次包含匹配 */
    private String mapCategory(String name, List<AiCategoryRef> categories) {
        if (StrUtil.isBlank(name) || categories.isEmpty()) {
            return null;
        }
        for (AiCategoryRef c : categories) {
            if (name.equalsIgnoreCase(c.getName())) {
                return c.getId();
            }
        }
        for (AiCategoryRef c : categories) {
            if (c.getName() != null && (c.getName().contains(name) || name.contains(c.getName()))) {
                return c.getId();
            }
        }
        return null;
    }

    /** overall：name/category 置信度与 fields[] 置信度的最小值（fields 已 sanitize） */
    private int overallOf(JSONObject draft) {
        JSONObject overall = draft.getJSONObject("overall");
        if (overall != null) {
            return nz(overall.getInteger("confidence"));
        }
        int min = 100;
        for (String key : List.of("name", "category")) {
            JSONObject slot = draft.getJSONObject(key);
            min = Math.min(min, slot == null ? 0 : nz(slot.getInteger("confidence")));
        }
        JSONArray fields = draft.getJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                if (f != null) {
                    min = Math.min(min, nz(f.getInteger("confidence")));
                }
            }
        }
        return min;
    }

    /** 草稿 quantity → 创建件数（1–99，缺省 1） */
    private int draftQuantity(JSONObject draft) {
        JSONObject q = draft.getJSONObject("quantity");
        Integer v = q == null ? null : q.getInteger("value");
        return v == null || v <= 0 ? 1 : Math.min(v, MAX_QTY);
    }

    // ================================ 私有：通用 ================================

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

    private String prompt() {
        if (promptTemplate == null) {
            synchronized (this) {
                if (promptTemplate == null) {
                    try (InputStream in = getClass().getClassLoader()
                            .getResourceAsStream("skills/mustard-item-filing-v2/system_prompt.md")) {
                        if (in == null) {
                            throw new IllegalStateException("技能包缺失：skills/mustard-item-filing-v2/system_prompt.md");
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

    private String str(JSONObject draft, String key) {
        JSONObject slot = draft.getJSONObject(key);
        if (slot == null) {
            return null;
        }
        // 字符串槽位：LLM 误输出数字（如 name: 0）时 fastjson getString 会转成 "0"，先做类型判断
        Object value = slot.get("value");
        if (!(value instanceof String s)) {
            return null;
        }
        return StrUtil.trimToNull(s);
    }

    private JSONObject slot(String value, int confidence) {
        JSONObject s = new JSONObject(true);
        s.put("value", value);
        s.put("confidence", confidence);
        return s;
    }

    private List<AiChatMessage> join(AiChatMessage first, List<AiChatMessage> rest) {
        List<AiChatMessage> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    private String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
