package org.jeecg.modules.app.openapi.webhook;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.ItemLineage;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.entity.OpenWebhookEvent;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.mapper.OpenWebhookEventMapper;
import org.jeecg.modules.app.mip.LineageService;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * B4 Webhook 事件订阅 · 入队钩子
 * <p>
 * 账本 {@link LineageService} append 成功后调用（与业务同事务）：
 * 命中订阅者（凭据绑定用户的哈希 = 事件 fromUid/toUid）则插入投递事件行，
 * 由 {@link OpenWebhookDispatcher} 异步签名投递。
 * <ul>
 *   <li>事件映射：mint/update/recycle/destroy/spawn → item.changed；
 *       gift_out/gift_in/ruins_drop/ruins_pick/transfer → item.flowed；import 为存量锚点不推送；</li>
 *   <li>报文为出域最小集：无 uid（账本已哈希）、无属性值（payload 仅为摘要）；</li>
 *   <li>无订阅者时仅一次小表查询、零写入；任何异常不阻断业务与记账。</li>
 * </ul>
 */
@Slf4j
@Component
public class OpenWebhookHook {

    public static final String EVENT_ITEM_CHANGED = "item.changed";
    public static final String EVENT_ITEM_FLOWED = "item.flowed";

    /** 账本事件 → Webhook 事件映射 */
    private static final Map<String, String> EVENT_MAPPING = Map.ofEntries(
            Map.entry("mint", EVENT_ITEM_CHANGED),
            Map.entry("update", EVENT_ITEM_CHANGED),
            Map.entry("recycle", EVENT_ITEM_CHANGED),
            Map.entry("destroy", EVENT_ITEM_CHANGED),
            Map.entry("spawn", EVENT_ITEM_CHANGED),
            Map.entry("gift_out", EVENT_ITEM_FLOWED),
            Map.entry("gift_in", EVENT_ITEM_FLOWED),
            Map.entry("flow_out", EVENT_ITEM_FLOWED),
            Map.entry("flow_in", EVENT_ITEM_FLOWED),
            Map.entry("ruins_drop", EVENT_ITEM_FLOWED),
            Map.entry("ruins_pick", EVENT_ITEM_FLOWED),
            Map.entry("transfer", EVENT_ITEM_FLOWED));

    private final OpenAppCredentialMapper credentialMapper;
    private final OpenWebhookEventMapper eventMapper;

    public OpenWebhookHook(OpenAppCredentialMapper credentialMapper, OpenWebhookEventMapper eventMapper) {
        this.credentialMapper = credentialMapper;
        this.eventMapper = eventMapper;
    }

    /**
     * 账本事件追加后入队（调用方处于业务事务内，入队与业务同事务原子提交）
     */
    public void onAppended(ItemLineage lineage) {
        try {
            if (lineage == null || StrUtil.isEmpty(lineage.getOriId())) {
                return;
            }
            String webhookEvent = EVENT_MAPPING.get(lineage.getEventType());
            if (webhookEvent == null) {
                return;
            }
            List<OpenAppCredential> subscribers = credentialMapper.selectList(
                    new LambdaQueryWrapper<OpenAppCredential>()
                            .eq(OpenAppCredential::getStatus, 1)
                            .isNotNull(OpenAppCredential::getWebhookUrl)
                            .ne(OpenAppCredential::getWebhookUrl, ""));
            if (subscribers.isEmpty()) {
                return;
            }
            JSONObject payload = buildPayload(webhookEvent, lineage);
            Date now = new Date();
            for (OpenAppCredential subscriber : subscribers) {
                if (!matches(subscriber, lineage)) {
                    continue;
                }
                eventMapper.insert(new OpenWebhookEvent()
                        .setAppKey(subscriber.getAppKey())
                        .setEvent(webhookEvent)
                        .setOpType(lineage.getEventType())
                        .setOriId(lineage.getOriId())
                        .setPayload(payload.toJSONString())
                        .setStatus(OpenWebhookEvent.STATUS_PENDING)
                        .setRetryCount(0)
                        .setNextRetryAt(now)
                        .setCreateTime(now));
            }
        } catch (Exception e) {
            log.warn("webhook 事件入队失败（不影响业务与账本）: oriId={}, {}",
                    lineage == null ? null : lineage.getOriId(), e.getMessage());
        }
    }

    /**
     * 订阅匹配：凭据绑定用户的哈希命中事件的 fromUid/toUid（账本不落明文 uid）
     */
    private boolean matches(OpenAppCredential subscriber, ItemLineage lineage) {
        String ownerHash = LineageService.hashUid(subscriber.getBoundUid());
        return Objects.equals(ownerHash, lineage.getFromUid())
                || Objects.equals(ownerHash, lineage.getToUid());
    }

    /**
     * 投递报文：出域最小集（事件/操作/档案ID/操作者/数量/时间/账本摘要），无 uid 与属性值
     */
    private JSONObject buildPayload(String webhookEvent, ItemLineage lineage) {
        JSONObject body = new JSONObject(true);
        body.put("event", webhookEvent);
        body.put("op", lineage.getEventType());
        body.put("oriId", lineage.getOriId());
        body.put("operator", lineage.getOperator());
        body.put("qty", lineage.getQty());
        body.put("occurredAt", lineage.getOccurredAt() == null ? null : lineage.getOccurredAt().getTime());
        body.put("data", JSON.parse(StrUtil.isEmpty(lineage.getPayload()) ? "{}" : lineage.getPayload()));
        return body;
    }
}
