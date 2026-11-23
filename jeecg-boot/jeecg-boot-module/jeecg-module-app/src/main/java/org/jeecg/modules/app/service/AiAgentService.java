package org.jeecg.modules.app.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.ai.AiModelAdapter;
import org.jeecg.modules.app.ai.AiRateLimiter;
import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.jeecg.modules.app.entity.AiChatMessage;
import org.jeecg.modules.app.entity.AiChatSession;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.entity.setting.UserCategory;
import org.jeecg.modules.app.service.impl.AiChatMessageServiceImpl;
import org.jeecg.modules.app.service.impl.AiChatSessionServiceImpl;
import org.jeecg.modules.app.service.impl.OpenAppCredentialServiceImpl;
import org.jeecg.modules.app.bean.vo.ai.AiChatCreateVO;
import org.jeecg.modules.app.bean.vo.ai.AiChatSendVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 建档编排器（ai-integration-plan §5.5 解析管线 / §5.6 创建调度 / §5.7 技能化三层 · S3+S4+S5）
 * <p>
 * 职责边界：AI 模型只产出结构化意图（单轮决策），创建/查重/持久化/限流全部由本编排器执行，
 * 模型无网络自由度；技能提示词/输出契约/状态机来自技能包 skills/mustard-item-filing-v1（声明式资源）。
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
    /** 数量上限 */
    private static final int MAX_QTY = 99;
    /** 标签上限（MIP 校验 E-TAGS：≤3） */
    private static final int MAX_TAGS = 3;
    /** AI 专属凭据标记 */
    private static final String AI_CREDENTIAL_MARK = "ai_assistant";

    private final AiChatSessionServiceImpl sessionService;
    private final AiChatMessageServiceImpl messageService;
    private final OpenAppCredentialServiceImpl credentialService;
    private final IUserCategoryService userCategoryService;
    private final AppOpenService appOpenService;
    private final AiModelAdapter modelAdapter;
    private final AiRateLimiter rateLimiter;

    @Value("${jeecg.ai-chat.default-item-icon:https://mustard.example.com/icon.png}")
    private String defaultIcon;

    private volatile String promptTemplate;

    public AiAgentService(AiChatSessionServiceImpl sessionService,
                          AiChatMessageServiceImpl messageService,
                          OpenAppCredentialServiceImpl credentialService,
                          IUserCategoryService userCategoryService,
                          AppOpenService appOpenService,
                          AiModelAdapter modelAdapter,
                          AiRateLimiter rateLimiter) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.credentialService = credentialService;
        this.userCategoryService = userCategoryService;
        this.appOpenService = appOpenService;
        this.modelAdapter = modelAdapter;
        this.rateLimiter = rateLimiter;
    }

    // ================================ S3/S4 · 发送与解析 ================================

    /**
     * 聊天发送：用户消息落库 → 解析管线（vision 降级口述 / 结构化 JSON / 置信度）→ 追问槽位 → 草稿快照。
     */
    public AiChatSendVO send(String uid, String sessionId, String text, List<String> images) {
        if (StrUtil.isBlank(text) && (images == null || images.isEmpty())) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        String limitError = rateLimiter.tryAcquireChat(uid);
        if (limitError != null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR, limitError);
        }
        List<String> imgs = images == null ? List.of() : images.stream().limit(MAX_IMAGES).toList();

        // 1. 用户消息落库
        AiChatMessage userMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_USER,
                AiChatMessage.TYPE_TEXT, StrUtil.nullToEmpty(text), imgs, null, null, null);

        // 2. 解析管线
        String visionDesc = resolveVision(imgs, text);
        String userText = StrUtil.blankToDefault(text, "（仅图片，无文字）");
        List<UserCategory> categories = userCategoryService.list(new LambdaQueryWrapper<UserCategory>()
                .eq(UserCategory::getUserId, uid));
        String draftState = StrUtil.blankToDefault(session.getDraftSnapshot(), "null");
        int askBudget = Math.max(0, MAX_ASKS - nz(session.getAskCount()));
        String history = recentHistory(uid, sessionId);
        String systemPrompt = prompt()
                .replace("{{VISION_DESCRIPTION}}", StrUtil.blankToDefault(visionDesc, "（无视觉描述：无图片或口述模式）"))
                .replace("{{HISTORY}}", history)
                .replace("{{ASK_BUDGET}}", String.valueOf(askBudget))
                .replace("{{USER_CATEGORIES}}", JSON.toJSONString(categories.stream().map(c -> c.getName()).toList()))
                .replace("{{DRAFT_STATE}}", draftState)
                .replace("{{USER_TEXT}}", userText);

        JSONObject decision = callStructured(systemPrompt, userText);
        String action = decision.getString("action");
        String reply = StrUtil.blankToDefault(decision.getString("reply"), "");
        JSONObject modelDraft = decision.getJSONObject("draft");
        JSONObject clarify = decision.getJSONObject("clarify");

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
                    : normalizeDraft(modelDraft, categories, imgs);
            assistantMsgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_CLARIFY, question, null,
                    chipBox.toJSONString(),
                    clarifyDraft == null ? null : clarifyDraft.toJSONString(),
                    null));
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
            JSONObject draft = normalizeDraft(modelDraft, categories, imgs);
            int overall = overallOf(draft);
            AiChatMessage draftMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_DRAFT, reply, null, null, draft.toJSONString(), overall);
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
                AiChatMessage.TYPE_TEXT, reply, null, null, null, null);
        assistantMsgs.add(chatMsg);
        session.setLastSummary(clip(reply, 60));
        sessionService.touch(session);
        return vo.setAskRemaining(Math.max(0, MAX_ASKS - nz(session.getAskCount())))
                .setSessionStatus(session.getStatus())
                .setMessages(join(userMsg, assistantMsgs));
    }

    // ================================ S5 · 创建调度 ================================

    /**
     * 确认创建：数量 N（1–99 默认 1）→ 用户 AI 凭据（无则自动创建 readwrite + ai_assistant 标记）
     * → 内部直调 add_item ×N（N 件 = N 个 oriId，相同字段信息）→ 过程条目 + 回执（×N 标注）。
     *
     * @param draftJson App 端就地编辑后的草稿 JSON（可选）；提供时归一化（类别映射/overall 重算）并回写会话快照
     */
    public AiChatCreateVO confirmCreate(String uid, String sessionId, Integer quantity, String draftJson) {
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR);
        }
        int n = quantity == null || quantity <= 0 ? 1 : Math.min(quantity, MAX_QTY);
        String limitError = rateLimiter.tryAcquireCreate(uid);
        if (limitError != null) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR, limitError);
        }
        JSONObject draft = StrUtil.isBlank(session.getDraftSnapshot()) ? null : JSON.parseObject(session.getDraftSnapshot());
        if (StrUtil.isNotBlank(draftJson)) {
            try {
                JSONObject edited = JSON.parseObject(draftJson);
                if (edited != null) {
                    List<UserCategory> categories = userCategoryService.list(new LambdaQueryWrapper<UserCategory>()
                            .eq(UserCategory::getUserId, uid));
                    draft = normalizeDraft(edited, categories, List.of());
                    session.setDraftSnapshot(draft.toJSONString());
                }
            } catch (Exception e) {
                log.warn("[ai-agent] 编辑草稿解析失败，回退会话快照 uid={}", uid, e);
            }
        }
        // 置信度改为可选：不做门限阻断；confidence ≥85 → normal(status=1)，<85 → draft(status=0) 由创建链路落库
        if (draft == null || StrUtil.isBlank(str(draft, "name"))) {
            throw new AppException(ExceptionEnum.REQUEST_PARAM_ERROR, "草稿未就绪：请先补充物品名称后再创建");
        }

        // 凭据：readwrite 且标记 ai_assistant 优先，其次任意启用 readwrite，否则自动创建
        OpenAppCredential credential = resolveCredential(uid);
        String attributesJson = buildAttributes(draft);
        int confidence = overallOf(draft);

        List<String> oriIds = new ArrayList<>();
        String firstError = null;
        for (int i = 0; i < n; i++) {
            JSONObject body = new JSONObject(true);
            body.put("attributes", JSON.parseArray(attributesJson));
            body.put("confidence", confidence);
            body.put("source", AI_CREDENTIAL_MARK.replace('_', '-'));
            try {
                var result = appOpenService.addItem(credential, body, IdUtil.fastSimpleUUID());
                if (result.isSuccess() && result.getResult() != null) {
                    oriIds.add(oriIdOf(result.getResult()));
                } else if (firstError == null) {
                    firstError = result.getMessage();
                }
            } catch (Exception e) {
                log.error("[ai-agent] add_item 失败 uid={} n={}", uid, n, e);
                if (firstError == null) {
                    firstError = e.getMessage();
                }
            }
        }

        // 过程条目 + 回执（×N 标注）
        List<AiChatMessage> msgs = new ArrayList<>();
        JSONObject process = new JSONObject(true);
        process.put("step", "create");
        process.put("total", n);
        process.put("ok", oriIds.size());
        process.put("fail", n - oriIds.size());
        process.put("itemIds", oriIds);
        msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_PROCESS, null, null, process.toJSONString(), null, null));
        AiChatCreateVO vo = new AiChatCreateVO().setSessionId(sessionId)
                .setTotal(n).setCount(oriIds.size()).setItemIds(oriIds);
        if (oriIds.isEmpty()) {
            vo.setError(StrUtil.blankToDefault(firstError, "创建失败"));
            session.setLastSummary("创建失败");
        } else {
            String receipt = "已创建（×" + oriIds.size() + "）";
            JSONObject receiptBox = new JSONObject(true);
            receiptBox.put("count", oriIds.size());
            receiptBox.put("itemIds", oriIds);
            msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_RECEIPT, receipt, null, receiptBox.toJSONString(), null, null));
            session.setStatus(AiChatSession.STATUS_FILED);
            session.setDraftSnapshot(null);
            session.setAskCount(0);
            session.setLastSummary(receipt);
        }
        sessionService.touch(session);
        return vo.setMessages(msgs);
    }

    // ================================ 私有：解析与组装 ================================

    /**
     * 视觉链：vision provider 缺失 → 口述降级（用户话术即物品描述，tool_schema.json fallback=verbatim）
     */
    private String resolveVision(List<String> images, String text) {
        if (images.isEmpty()) {
            return null;
        }
        String v = modelAdapter.vision(images, text);
        return v != null ? v : text; // 口述模式
    }

    private String recentHistory(String uid, String sessionId) {
        List<AiChatMessage> recent = messageService.listBySession(uid, sessionId);
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, recent.size() - 6);
        for (int i = from; i < recent.size(); i++) {
            AiChatMessage m = recent.get(i);
            sb.append(AiChatMessage.ROLE_USER.equals(m.getRole()) ? "用户" : "AI")
                    .append(": ").append(StrUtil.blankToDefault(m.getContent(), "（草稿/过程条目）")).append('\n');
        }
        return sb.toString();
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
     * 草稿规范化：类别名 → 用户分类 id 映射（未命中 value="0" 置信度压到 40 触发追问），
     * tags 截断 ≤3，overall = min(name, category, 模型 overall)。
     */
    private JSONObject normalizeDraft(JSONObject modelDraft, List<UserCategory> categories, List<String> imgs) {
        JSONObject draft = mergeDraft(null, modelDraft, categories);
        // 附图 → icon/图片组件兜底值（vision 元数据）
        if (!imgs.isEmpty()) {
            JSONObject icon = draft.getJSONObject("icon");
            if (icon == null || StrUtil.isBlank(icon.getString("value"))) {
                draft.put("icon", slot(imgs.get(0), 90));
            }
        }
        return draft;
    }

    /** 草稿合并：base（会话快照）+ patch（模型本轮产出），回答追问时由模型负责合并，此处做结构兜底 */
    private JSONObject mergeDraft(String baseJson, JSONObject patch, List<UserCategory> categories) {
        JSONObject draft = new JSONObject(true);
        for (String key : List.of("name", "category", "tags", "description", "icon", "quantity")) {
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
            for (String key : List.of("name", "category", "tags", "description", "icon", "quantity")) {
                Object slotValue = patch.get(key);
                if (slotValue instanceof JSONObject || slotValue instanceof JSONArray) {
                    draft.put(key, slotValue);
                }
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
            for (UserCategory c : categories) {
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
                    for (UserCategory c : categories) {
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
    private String mapCategory(String name, List<UserCategory> categories) {
        if (StrUtil.isBlank(name) || categories.isEmpty()) {
            return null;
        }
        for (UserCategory c : categories) {
            if (name.equalsIgnoreCase(c.getName())) {
                return c.getId();
            }
        }
        for (UserCategory c : categories) {
            if (c.getName() != null && (c.getName().contains(name) || name.contains(c.getName()))) {
                return c.getId();
            }
        }
        return null;
    }

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
        return min;
    }

    /**
     * 创建 attributes（tool_schema.json elements_map 白名单）：
     * 1 名称 / 2 图标 / 3 类别 / 8 描述 / 10 标签组 / 12 图片；每件数量默认 1（N 件=N 行）
     */
    private String buildAttributes(JSONObject draft) {
        JSONArray attributes = new JSONArray();
        attributes.add(attr(1, str(draft, "name")));
        JSONObject icon = draft.getJSONObject("icon");
        String iconUrl = icon == null ? null : StrUtil.trimToNull(icon.getString("value"));
        if (iconUrl == null || !(iconUrl.startsWith("http://") || iconUrl.startsWith("https://"))) {
            iconUrl = defaultIcon;
        }
        attributes.add(attr(2, iconUrl));
        attributes.add(attr(3, str(draft, "category")));
        String desc = str(draft, "description");
        if (StrUtil.isNotBlank(desc)) {
            attributes.add(attr(8, desc));
        }
        JSONArray tags = draft.getJSONArray("tags");
        if (tags != null && !tags.isEmpty()) {
            JSONArray tagValues = new JSONArray();
            for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
                JSONObject t = tags.getJSONObject(i);
                if (t != null && StrUtil.isNotBlank(t.getString("value"))) {
                    tagValues.add(t.getString("value").trim());
                }
            }
            if (!tagValues.isEmpty()) {
                attributes.add(attr(10, tagValues));
            }
        }
        return attributes.toJSONString();
    }

    private JSONObject attr(int element, Object value) {
        JSONObject a = new JSONObject(true);
        a.put("element", element);
        a.put("value", value);
        return a;
    }

    /**
     * 凭据解析：readwrite 优先（ai_assistant 标记 > 任意启用），无则自动创建 AI 专属凭据
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
        // 自动创建：appName=AI 建档助手 + scope=readwrite + remark 标记（§5.6 密钥自动带入）
        OpenAppCredential created = credentialService.addByUser(uid, "AI 建档助手");
        created.setScope("readwrite");
        created.setRemark("source:" + AI_CREDENTIAL_MARK);
        credentialService.updateById(created);
        return created;
    }

    // ================================ 私有：通用 ================================

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

    private String prompt() {
        if (promptTemplate == null) {
            synchronized (this) {
                if (promptTemplate == null) {
                    try (InputStream in = getClass().getClassLoader()
                            .getResourceAsStream("skills/mustard-item-filing-v1/system_prompt.md")) {
                        if (in == null) {
                            throw new IllegalStateException("技能包缺失：skills/mustard-item-filing-v1/system_prompt.md");
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

    private String oriIdOf(JSONObject instance) {
        JSONObject identity = instance.getJSONObject("identity");
        if (identity != null && StrUtil.isNotBlank(identity.getString("ori_id"))) {
            return identity.getString("ori_id");
        }
        return StrUtil.blankToDefault(instance.getString("ori_id"), "unknown");
    }

    private String str(JSONObject draft, String key) {
        JSONObject slot = draft.getJSONObject(key);
        return slot == null ? null : StrUtil.trimToNull(slot.getString("value"));
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
