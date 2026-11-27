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
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.catalog.AiComponentCatalog;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.jeecg.modules.ai.entity.AiChatSession;
import org.jeecg.modules.ai.port.ItemReadPort;
import org.jeecg.modules.ai.port.ItemWritePort;
import org.jeecg.modules.ai.port.TaxonomyPort;
import org.jeecg.modules.ai.service.impl.AiChatMessageServiceImpl;
import org.jeecg.modules.ai.service.impl.AiChatSessionServiceImpl;
import org.jeecg.modules.ai.stream.AiStreamSink;
import org.jeecg.modules.ai.tooling.ItemTools;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * C3 编辑编排器（P5 update 意图）：自然语言改已有物品。
 * <p>
 * 职责边界与 C1/C2 一致：AI 模型只产出结构化更新意图（target 定位 + patch 局部补丁），
 * 物品定位/白名单校验/对照卡产出/写库确认全部由本编排器执行（AI 无网络自由度）；
 * 提示词与输出契约来自技能包 skills/mustard-item-update-v1。
 * <p>
 * 两步确认流：send 产出 update_card（msgType=update_card，快照存对照卡 JSON），
 * 用户点确认后 {@link #confirmUpdate} 执行 ItemWritePort.updateItem（patch 合并在
 * app 模块写链路完成，restricted 原值保留 + patch restricted 元素丢弃）。
 * patch 全部为替换语义：对照卡 from→to 完整展示最终效果，用户可见误替换并可拒绝。
 */
@Slf4j
@Service
public class AiUpdateService {

    /** 标签上限（MIP 校验 E-TAGS：≤3） */
    private static final int MAX_TAGS = 3;
    /** 属性组上限 */
    private static final int MAX_ATTRIBUTES = 10;
    /** 名称长度上限（与建档契约一致） */
    private static final int MAX_NAME_LEN = 30;
    /** 对照卡变更行 from/to 展示截断 */
    private static final int MAX_CHANGE_LEN = 120;
    /** 默认兜底话术 */
    private static final String DEFAULT_REPLY = "想改哪件物品的什么信息？比如「把相机的价格改成 3000」";

    private final AiChatSessionServiceImpl sessionService;
    private final AiChatMessageServiceImpl messageService;
    private final TaxonomyPort taxonomyPort;
    private final ItemReadPort itemReadPort;
    private final ItemWritePort itemWritePort;
    private final AiModelAdapter modelAdapter;
    private final AiRateLimiter rateLimiter;
    private final AiGlobalSemaphore semaphore;
    private final org.jeecg.modules.ai.config.TuningConfigService tuningConfigService;
    /** 定位失败（0/多件）回退关键词检索卡（复用 searchCard 全兜底链与意图缓存） */
    private final AiQueryService queryService;
    /** 确认执行时 patch → attributes 组装（与建档同一组装规则） */
    private final ItemTools itemTools;

    private volatile String promptTemplate;

    /** L2 conv 槽位等待上限（毫秒）：超时抛 QUEUE_TIMEOUT */
    private static final long CONV_WAIT_MS = 3000;

    /** conv 槽位最大并发数（配置 ai.semaphore.conv-max，默认 300） */
    @org.springframework.beans.factory.annotation.Value("${ai.semaphore.conv-max:300}")
    private int convMax;

    public AiUpdateService(AiChatSessionServiceImpl sessionService,
                           AiChatMessageServiceImpl messageService,
                           TaxonomyPort taxonomyPort,
                           ItemReadPort itemReadPort,
                           ItemWritePort itemWritePort,
                           AiModelAdapter modelAdapter,
                           AiRateLimiter rateLimiter,
                           AiGlobalSemaphore semaphore,
                           org.jeecg.modules.ai.config.TuningConfigService tuningConfigService,
                           AiQueryService queryService,
                           ItemTools itemTools) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.taxonomyPort = taxonomyPort;
        this.itemReadPort = itemReadPort;
        this.itemWritePort = itemWritePort;
        this.modelAdapter = modelAdapter;
        this.rateLimiter = rateLimiter;
        this.semaphore = semaphore;
        this.tuningConfigService = tuningConfigService;
        this.queryService = queryService;
        this.itemTools = itemTools;
    }

    // ================================ 管线 ================================

    /**
     * 编辑会话：用户消息落库 → 第一段更新意图解析（target + patch）
     * → 定位物品（复用 get_item 口径）→ 更新确认卡落库。
     */
    public AiChatSendVO send(String uid, String sessionId, String text, AiStreamSink sink) {
        semaphore.beginRequest();
        boolean acquired = false;
        try {
            acquired = acquireConvSlot(sink);
            return doSend(uid, sessionId, text, sink);
        } finally {
            if (acquired) {
                semaphore.release(AiGlobalSemaphore.SEM_CONV);
            }
            semaphore.endRequest();
        }
    }

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

    private AiChatSendVO doSend(String uid, String sessionId, String text, AiStreamSink sink) {
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
        AiChatMessage userMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_USER,
                AiChatMessage.TYPE_TEXT, text, null, null, null, null, sink);

        if (sink != null) {
            sink.progress("parsing");
        }
        JSONObject intent = callStructured(updatePrompt()
                .replace("{{HISTORY}}", recentHistory(uid, sessionId))
                .replace("{{TODAY}}", DateUtil.today())
                .replace("{{USER_TEXT}}", text), text);
        String type = intent == null ? null : StrUtil.trimToNull(intent.getString("type"));
        log.info("[ai-update] stage1 intent={}", intent == null ? "null" : intent.toJSONString());

        // chat 兜底：非编辑意图（含解析失败），自然回应后结束
        if (!"update_item".equals(type)) {
            String reply = intent == null ? null : StrUtil.trimToNull(intent.getString("reply"));
            return finishText(uid, sessionId, session, userMsg,
                    StrUtil.blankToDefault(reply, DEFAULT_REPLY), sink);
        }

        // patch 白名单校验（restricted → manualFill 提示；违规丢弃）。
        // 模型畸形输出（如 tags 给成字符串）时 fastjson getJSON 访问会抛异常，整体降级 chat 兜底
        JSONObject patch;
        List<String> manualList = new ArrayList<>();
        boolean executable;
        try {
            patch = sanitizePatch(intent.getJSONObject("patch"));
            if (intent.getJSONArray("manualFill") != null) {
                for (String s : intent.getJSONArray("manualFill").toJavaList(String.class)) {
                    if (StrUtil.isNotBlank(s)) {
                        manualList.add(s.trim());
                    }
                }
            }
            executable = patch != null && (patch.get("fields") != null || patch.get("name") != null
                    || patch.get("description") != null || patch.get("tags") != null
                    || patch.get("attributes") != null);
            if (patch != null && patch.getJSONArray("manualFill") != null) {
                // 校验层产生的 manualFill（restricted 拦截）转出到对照卡层，patch 本体只保留可执行字段
                for (String s : patch.getJSONArray("manualFill").toJavaList(String.class)) {
                    if (StrUtil.isNotBlank(s) && !manualList.contains(s.trim())) {
                        manualList.add(s.trim());
                    }
                }
                patch.remove("manualFill");
            }
        } catch (Exception e) {
            log.warn("[ai-update] patch 解析异常，降级 chat 兜底: {}", e.getMessage());
            return finishText(uid, sessionId, session, userMsg,
                    StrUtil.blankToDefault(intent.getString("reply"), DEFAULT_REPLY), sink);
        }
        if (!executable) {
            if (!manualList.isEmpty()) {
                // 仅红线拦截：无实际改动，提示手动补录
                return finishText(uid, sessionId, session, userMsg,
                        "这类敏感信息不适合由 AI 修改，请在物品详情页手动补录", sink);
            }
            return finishText(uid, sessionId, session, userMsg,
                    StrUtil.blankToDefault(intent.getString("reply"), DEFAULT_REPLY), sink);
        }
        JSONArray manualFill = new JSONArray(manualList);

        // 定位（复用 get_item 口径：itemId 直查 / itemName 唯一命中 + category 消歧）
        JSONObject target;
        try {
            target = intent.getJSONObject("target");
        } catch (Exception e) {
            target = null;
        }
        AiItemRow row = locate(uid, target);
        if (row == null) {
            return fallbackSearchCard(uid, sessionId, session, userMsg, target, text, sink);
        }
        JSONObject detail = itemReadPort.itemDetail(uid, String.valueOf(row.getId()));
        if (detail == null) {
            return finishText(uid, sessionId, session, userMsg, "这件物品当前不可编辑，请稍后再试", sink);
        }

        JSONObject card = buildUpdateCard(row, detail, patch, manualFill);
        AiChatMessage cardMsg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_UPDATE_CARD,
                StrUtil.blankToDefault(intent.getString("reply"), "已生成更新对照，确认后执行"),
                null, null, card.toJSONString(), null, sink);
        touchSession(session, text, "待确认更新：" + StrUtil.blankToDefault(row.getName(), "物品"));
        return new AiChatSendVO().setSessionId(sessionId)
                .setSessionStatus(session.getStatus())
                .setMessages(List.of(userMsg, cardMsg));
    }

    // ================================ 确认执行 ================================

    /**
     * 确认更新：update_card 消息归属校验 → 对照卡 patch 组装 attributes
     * → ItemWritePort.updateItem（patch 合并 + 账本 update 事件）→ 过程 + 回执消息。
     */
    public AiChatSendVO confirmUpdate(String uid, String sessionId, String messageId) {
        AiChatSession session = sessionService.getOwned(uid, sessionId);
        if (session == null || StrUtil.isBlank(messageId)) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        AiChatMessage cardMsg = messageService.getById(messageId);
        if (cardMsg == null || !uid.equals(cardMsg.getUid()) || !sessionId.equals(cardMsg.getSessionId())
                || !AiChatMessage.TYPE_UPDATE_CARD.equals(cardMsg.getMsgType())
                || StrUtil.isBlank(cardMsg.getDraftSnapshot())) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR);
        }
        String limitError = rateLimiter.tryAcquireCreate(uid);
        if (limitError != null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, limitError);
        }
        JSONObject card;
        try {
            card = JSON.parseObject(cardMsg.getDraftSnapshot());
        } catch (Exception e) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, "更新对照卡已失效，请重新发起");
        }
        String itemId = card == null ? null : StrUtil.trimToNull(card.getString("itemId"));
        JSONObject patch = card == null ? null : card.getJSONObject("patch");
        if (itemId == null || patch == null) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, "更新对照卡已失效，请重新发起");
        }

        JSONArray attributes = new JSONArray();
        // 顶层槽位 → attributes（patch 替换语义：只带用户明确要改的项）
        appendSlotAttribute(attributes, 1, strValue(patch, "name"));
        appendSlotAttribute(attributes, 8, strValue(patch, "description"));
        JSONArray tags = patch.getJSONArray("tags");
        if (tags != null && !tags.isEmpty()) {
            JSONArray values = new JSONArray();
            for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
                String t = StrUtil.trimToNull(tags.getString(i));
                if (t != null) {
                    values.add(t);
                }
            }
            if (!values.isEmpty()) {
                appendSlotAttribute(attributes, 10, values);
            }
        }
        JSONArray attrs = patch.getJSONArray("attributes");
        if (attrs != null && !attrs.isEmpty()) {
            // 属性组与建档同规则：值并入名称（「攻击力：120」），详情属性卡直接展示完整信息
            JSONArray pairs = new JSONArray();
            for (int i = 0; i < Math.min(attrs.size(), MAX_ATTRIBUTES); i++) {
                JSONObject p = attrs.getJSONObject(i);
                if (p == null || StrUtil.isBlank(p.getString("name"))) {
                    continue;
                }
                String name = p.getString("name").trim();
                String value = p.get("value") == null ? "" : String.valueOf(p.get("value")).trim();
                JSONObject pair = new JSONObject(true);
                pair.put("name", value.isEmpty() ? name : name + "：" + value);
                pairs.add(pair);
            }
            if (!pairs.isEmpty()) {
                appendSlotAttribute(attributes, 11, pairs);
            }
        }
        // fields[] → attributes 直传（已过白名单校验）
        JSONArray fields = patch.getJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                if (f == null || f.getInteger("element") == null || f.get("value") == null) {
                    continue;
                }
                JSONObject a = new JSONObject(true);
                a.put("element", f.getIntValue("element"));
                a.put("value", f.get("value"));
                attributes.add(a);
            }
        }

        JSONObject body = new JSONObject(true);
        body.put("attributes", attributes);
        body.put("patch", true);
        body.put("source", "ai-assistant");
        List<AiChatMessage> msgs = new ArrayList<>();
        AiChatSendVO vo = new AiChatSendVO().setSessionId(sessionId)
                .setSessionStatus(session.getStatus());
        try {
            var result = itemWritePort.updateItem(uid, itemId, body);
            boolean ok = result != null && result.isSuccess();
            msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_PROCESS, null, null, processJson(ok, itemId,
                            ok ? null : StrUtil.blankToDefault(result == null ? null : result.getMessage(), "更新失败"))
                            .toJSONString(), null, null, null));
            if (ok) {
                String name = card.getString("itemName");
                JSONObject receiptBox = new JSONObject(true);
                receiptBox.put("count", 1);
                receiptBox.put("itemIds", List.of(itemId));
                receiptBox.put("update", true);
                msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                        AiChatMessage.TYPE_RECEIPT, "已更新", null, receiptBox.toJSONString(), null, null, null));
                touchSession(session, null, "已更新：" + StrUtil.blankToDefault(name, "物品"));
            } else {
                msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                        AiChatMessage.TYPE_TEXT,
                        "更新失败：" + StrUtil.blankToDefault(result == null ? null : result.getMessage(), "未知原因")
                                + "，物品信息未变更", null, null, null, null, null));
            }
        } catch (Exception e) {
            log.error("[ai-update] update_item 失败 uid={} itemId={}", uid, itemId, e);
            msgs.add(appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                    AiChatMessage.TYPE_TEXT, "更新失败：" + e.getMessage() + "，物品信息未变更",
                    null, null, null, null, null));
        }
        return vo.setMessages(msgs);
    }

    /** 过程条目 JSON：{step:"update", total:1, ok, fail, itemIds, error?} */
    private JSONObject processJson(boolean ok, String itemId, String error) {
        JSONObject process = new JSONObject(true);
        process.put("step", "update");
        process.put("total", 1);
        process.put("ok", ok ? 1 : 0);
        process.put("fail", ok ? 0 : 1);
        process.put("itemIds", List.of(itemId));
        if (error != null) {
            process.put("error", error);
        }
        return process;
    }

    // ================================ 私有：定位与回退 ================================

    /** 定位：itemId 直查优先；itemName 关键词唯一命中（total==1）才继续，category 消歧 */
    private AiItemRow locate(String uid, JSONObject target) {
        if (target == null) {
            return null;
        }
        String itemId = StrUtil.trimToNull(target.getString("itemId"));
        String itemName = StrUtil.trimToNull(target.getString("itemName"));
        String categoryHint = StrUtil.trimToNull(target.getString("category"));
        if (itemId != null) {
            AiItemRow row = itemReadPort.queryByItemId(uid, itemId);
            if (row != null) {
                return row;
            }
        }
        if (itemName == null) {
            return null;
        }
        AiItemQuery q = new AiItemQuery().setUserId(uid).setKeyword(itemName).setPageNo(1).setPageSize(2);
        Set<String> hint = resolveCategoryHint(uid, categoryHint);
        if (!hint.isEmpty()) {
            q.setCategoryList(new ArrayList<>(hint));
        }
        Page<AiItemRow> byName = itemReadPort.listByCondition(q);
        return byName.getTotal() == 1 ? byName.getRecords().get(0) : null;
    }

    /** category 消歧：用户分类 id/名称精确 + 双向包含 + 物品在用值匹配（get_item 同口径精简版） */
    private Set<String> resolveCategoryHint(String uid, String raw) {
        String name = StrUtil.trimToNull(raw);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (name == null) {
            return out;
        }
        List<AiCategoryRef> categories = taxonomyPort.listUnifiedCategories(uid);
        for (AiCategoryRef c : categories) {
            if (c.isUser() && (name.equals(String.valueOf(c.getId())) || name.equalsIgnoreCase(c.getName()))) {
                out.add(String.valueOf(c.getId()));
                if (c.getName() != null) {
                    out.add(c.getName());
                }
                return out;
            }
        }
        for (AiCategoryRef c : categories) {
            if (c.isUser() && c.getName() != null && c.getName().length() >= 2 && name.length() >= 2
                    && (name.contains(c.getName()) || c.getName().contains(name))) {
                out.add(String.valueOf(c.getId()));
                if (c.getName() != null) {
                    out.add(c.getName());
                }
            }
        }
        for (String v : itemReadPort.listItemCategories(uid)) {
            if (StrUtil.isBlank(v)) {
                continue;
            }
            if (v.equalsIgnoreCase(name) || (v.length() >= 2 && name.length() >= 2
                    && (v.contains(name) || name.contains(v)))) {
                out.add(v);
            }
        }
        return out;
    }

    /** 定位失败（0/多件）回退：关键词检索卡（fallbackFrom=update），引导用户说得更具体 */
    private AiChatSendVO fallbackSearchCard(String uid, String sessionId, AiChatSession session,
                                            AiChatMessage userMsg, JSONObject target, String text,
                                            AiStreamSink sink) {
        String keyword = target == null ? null
                : StrUtil.blankToDefault(target.getString("itemName"), target.getString("itemId"));
        if (StrUtil.isBlank(keyword)) {
            return finishText(uid, sessionId, session, userMsg, "想改哪件物品？说下它的名字我帮你定位", sink);
        }
        JSONObject card = queryService.keywordSearchCard(uid, keyword, target.getString("category"), text);
        String reply = card != null && card.getIntValue("total") > 0
                ? "找到 " + card.getIntValue("total") + " 件叫「" + keyword + "」的物品，请说得更具体些（比如它的分类）"
                : "没有找到这件物品，说下它的名字我帮你定位";
        if (card != null) {
            card.put("fallbackFrom", "update");
        }
        AiChatMessage msg = appendMessage(uid, sessionId, AiChatMessage.ROLE_ASSISTANT,
                AiChatMessage.TYPE_QUERY_RESULT, reply, null, null,
                card == null ? null : card.toJSONString(), null, sink);
        touchSession(session, null, reply);
        return new AiChatSendVO().setSessionId(sessionId)
                .setSessionStatus(session.getStatus())
                .setMessages(List.of(userMsg, msg));
    }

    // ================================ 私有：对照卡组装 ================================

    /**
     * 更新对照卡：{card:"update", type:"update_item", itemId, itemName, icon,
     * changes:[{element?,name,from,to}], manualFill, patch}。
     * from 取出域实例当前值（restricted 已剥离，不会出现敏感现值）；patch 随卡存储供确认执行。
     */
    private JSONObject buildUpdateCard(AiItemRow row, JSONObject detail, JSONObject patch, JSONArray manualFill) {
        JSONArray changes = new JSONArray();
        JSONObject identity = detail.getJSONObject("identity");
        String currentName = identity == null ? null : identity.getString("name");
        // attributes 当前值索引：element → value（出域实例已过 MipRedlineFilter）
        java.util.Map<Integer, Object> current = new java.util.LinkedHashMap<>();
        JSONArray detailAttrs = detail.getJSONArray("attributes");
        if (detailAttrs != null) {
            for (int i = 0; i < detailAttrs.size(); i++) {
                JSONObject a = detailAttrs.getJSONObject(i);
                if (a != null && a.getInteger("element") != null) {
                    current.put(a.getIntValue("element"), a.get("value"));
                }
            }
        }
        String name = strValue(patch, "name");
        if (name != null) {
            changes.add(change(null, "名称", currentName, name));
        }
        String desc = strValue(patch, "description");
        if (desc != null) {
            changes.add(change(8, "描述", current.get(8), desc));
        }
        JSONArray tags = patch.getJSONArray("tags");
        if (tags != null && !tags.isEmpty()) {
            JSONArray values = new JSONArray();
            for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
                String t = StrUtil.trimToNull(tags.getString(i));
                if (t != null) {
                    values.add(t);
                }
            }
            if (!values.isEmpty()) {
                changes.add(change(10, "标签", current.get(10), values));
            }
        }
        JSONArray attrs = patch.getJSONArray("attributes");
        if (attrs != null && !attrs.isEmpty()) {
            JSONArray pairs = new JSONArray();
            for (int i = 0; i < Math.min(attrs.size(), MAX_ATTRIBUTES); i++) {
                JSONObject p = attrs.getJSONObject(i);
                if (p == null || StrUtil.isBlank(p.getString("name"))) {
                    continue;
                }
                String pname = p.getString("name").trim();
                String value = p.get("value") == null ? "" : String.valueOf(p.get("value")).trim();
                JSONObject pair = new JSONObject(true);
                pair.put("name", value.isEmpty() ? pname : pname + "：" + value);
                pairs.add(pair);
            }
            if (!pairs.isEmpty()) {
                changes.add(change(11, "属性", current.get(11), pairs));
            }
        }
        JSONArray fields = patch.getJSONArray("fields");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                if (f == null || f.getInteger("element") == null || f.get("value") == null) {
                    continue;
                }
                int element = f.getIntValue("element");
                AiComponentCatalog.Component def = AiComponentCatalog.get(element);
                changes.add(change(element, def == null ? String.valueOf(element) : def.name(),
                        current.get(element), f.get("value")));
            }
        }

        JSONObject card = new JSONObject(true);
        card.put("card", "update");
        card.put("type", "update_item");
        card.put("itemId", String.valueOf(row.getId()));
        card.put("itemName", row.getName());
        card.put("icon", row.getIcon());
        card.put("changes", changes);
        if (manualFill != null && !manualFill.isEmpty()) {
            card.put("manualFill", manualFill);
        }
        card.put("patch", patch);
        return card;
    }

    /** 变更行：{element?, name, from, to}（值 stringify + 截断，from 空 → "（空）"） */
    private JSONObject change(Integer element, String name, Object from, Object to) {
        JSONObject c = new JSONObject(true);
        if (element != null) {
            c.put("element", element);
        }
        c.put("name", name);
        String fromStr = from == null || StrUtil.isEmpty(String.valueOf(from))
                ? "（空）" : clipDisplay(String.valueOf(from));
        c.put("from", fromStr);
        String toStr = to instanceof JSONArray || to instanceof JSONObject
                ? clipDisplay(JSON.toJSONString(to)) : clipDisplay(String.valueOf(to));
        c.put("to", toStr);
        return c;
    }

    private String clipDisplay(String s) {
        return s.length() <= MAX_CHANGE_LEN ? s : s.substring(0, MAX_CHANGE_LEN) + "…";
    }

    // ================================ 私有：patch 白名单校验 ================================

    /**
     * patch 白名单校验（编排器第二重防线）：
     * fields 逐项 known + 非 restricted + 非顶层槽位 + valueType 宽松合法（违规丢弃，
     * restricted 候选转 manualFill 提示）；顶层槽位 name/description/tags/attributes 规整。
     * 可执行字段全空时仍返回（仅含 manualFill），由调用方判定 executable 并兜底。
     */
    private JSONObject sanitizePatch(JSONObject modelPatch) {
        if (modelPatch == null) {
            return null;
        }
        JSONObject patch = new JSONObject(true);
        String name = strValue(modelPatch, "name");
        if (name != null) {
            patch.put("name", clip(name, MAX_NAME_LEN));
        }
        String desc = strValue(modelPatch, "description");
        if (desc != null) {
            patch.put("description", desc);
        }
        JSONArray tags = modelPatch.getJSONArray("tags");
        if (tags != null && !tags.isEmpty()) {
            Set<String> tagSet = new LinkedHashSet<>();
            for (int i = 0; i < tags.size() && tagSet.size() < MAX_TAGS; i++) {
                String t = StrUtil.trimToNull(tags.getString(i));
                if (t != null) {
                    tagSet.add(t);
                }
            }
            if (!tagSet.isEmpty()) {
                patch.put("tags", new JSONArray(new ArrayList<>(tagSet)));
            }
        }
        JSONArray attrs = modelPatch.getJSONArray("attributes");
        if (attrs != null && !attrs.isEmpty()) {
            JSONArray kept = new JSONArray();
            for (int i = 0; i < Math.min(attrs.size(), MAX_ATTRIBUTES); i++) {
                JSONObject p = attrs.getJSONObject(i);
                if (p == null || StrUtil.isBlank(p.getString("name"))) {
                    continue;
                }
                JSONObject pair = new JSONObject(true);
                pair.put("name", p.getString("name").trim());
                pair.put("value", p.get("value") == null ? "" : String.valueOf(p.get("value")).trim());
                kept.add(pair);
            }
            if (!kept.isEmpty()) {
                patch.put("attributes", kept);
            }
        }
        JSONArray fields = modelPatch.getJSONArray("fields");
        if (fields != null && !fields.isEmpty()) {
            List<String> manual = new ArrayList<>();
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
                    AiComponentCatalog.Component def = AiComponentCatalog.get(element);
                    manual.add((def == null ? "该字段" : def.name()) + "请手动补录");
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
                kept.add(keptField);
            }
            if (!kept.isEmpty()) {
                patch.put("fields", kept);
            }
            if (!manual.isEmpty()) {
                patch.put("manualFill", new JSONArray(manual));
            }
        }
        return patch;
    }

    /** fieldValue 宽松校验（与 AiAgentService.sanitizeFieldValue 同规则） */
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
                String s = String.valueOf(value).trim();
                return s.isEmpty() ? null : s;
            }
        }
    }

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

    /** 槽位字符串值：契约主形态为普通字符串（patch.name="相机"），兼容 {value} 槽位对象；
     * 数字/对象误输出时丢弃。注意不能先 getJSONObject——fastjson2 对普通字符串值会尝试
     * 当 JSON 解析并抛 JSONException（冒烟实测踩坑） */
    private String strValue(JSONObject obj, String key) {
        Object raw = obj == null ? null : obj.get(key);
        if (raw instanceof String s) {
            return StrUtil.trimToNull(s);
        }
        if (raw instanceof JSONObject slot) {
            Object value = slot.get("value");
            return value instanceof String s ? StrUtil.trimToNull(s) : null;
        }
        return null;
    }

    private void appendSlotAttribute(JSONArray attributes, int element, Object value) {
        if (value == null || (value instanceof String s && StrUtil.isBlank(s))) {
            return;
        }
        JSONObject a = new JSONObject(true);
        a.put("element", element);
        a.put("value", value);
        attributes.add(a);
    }

    // ================================ 私有：通用 ================================

    private String recentHistory(String uid, String sessionId) {
        List<AiChatMessage> recent = messageService.listBySession(uid, sessionId);
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, recent.size() - 6);
        for (int i = from; i < recent.size(); i++) {
            AiChatMessage m = recent.get(i);
            String content = StrUtil.blankToDefault(m.getContent(), "");
            if (AiChatMessage.TYPE_UPDATE_CARD.equals(m.getMsgType())) {
                content = "[出示更新对照卡]";
            } else if (AiChatMessage.TYPE_QUERY_RESULT.equals(m.getMsgType())) {
                content = "[查询结果] " + clip(content, 120);
            } else if (StrUtil.isBlank(content)) {
                content = "（结果卡/过程条目）";
            }
            sb.append(AiChatMessage.ROLE_USER.equals(m.getRole()) ? "用户" : "AI")
                    .append(": ").append(content).append('\n');
        }
        return sb.toString();
    }

    private JSONObject callStructured(String systemPrompt, String userText) {
        String raw = modelAdapter.structured(systemPrompt, userText);
        JSONObject out = tryParse(raw);
        if (out == null) {
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

    private String updatePrompt() {
        // P5：DB enabled 配置优先于内置技能包文件（无 DB 配置/异常时走内置，行为不变）
        return tuningConfigService.updatePrompt(builtinPrompt());
    }

    private String builtinPrompt() {
        if (promptTemplate == null) {
            synchronized (this) {
                if (promptTemplate == null) {
                    try (InputStream in = getClass().getClassLoader()
                            .getResourceAsStream("skills/mustard-item-update-v1/system_prompt.md")) {
                        if (in == null) {
                            throw new IllegalStateException("技能包缺失：skills/mustard-item-update-v1/system_prompt.md");
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
        if (sink != null && AiChatMessage.ROLE_ASSISTANT.equals(role)) {
            sink.message(saved);
        }
        return saved;
    }

    private String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
