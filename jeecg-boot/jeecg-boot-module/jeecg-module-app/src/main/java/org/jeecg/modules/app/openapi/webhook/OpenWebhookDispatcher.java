package org.jeecg.modules.app.openapi.webhook;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.OpenAppCredential;
import org.jeecg.modules.app.entity.OpenWebhookEvent;
import org.jeecg.modules.app.mapper.OpenAppCredentialMapper;
import org.jeecg.modules.app.mapper.OpenWebhookEventMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * B4 Webhook 事件订阅 · 投递器
 * <p>
 * 每秒扫描到期 PENDING 事件，POST 回调订阅地址：
 * <ul>
 *   <li>签名：请求头 {@code X-Mustard-Signature} = HMAC-SHA256(secret, timestamp + "." + deliveryId + "." + body)，
 *       另附 {@code X-Mustard-Event} / {@code X-Mustard-Timestamp} / {@code X-Mustard-Delivery}（事件行 ID，供接收方幂等去重）；</li>
 *   <li>成功判定：HTTP 2xx → DELIVERED；否则指数退避重试 5 次（1s/2s/4s/8s/16s）后 DEAD 死信保留审计；</li>
 *   <li>凭据删除/停用/取消订阅 → 直接死信（订阅已失效）；</li>
 *   <li>fixedDelay 串行扫描（默认单线程调度器），投递语义为 at-least-once，接收方按 deliveryId 幂等。</li>
 * </ul>
 */
@Slf4j
@Component
public class OpenWebhookDispatcher {

    /** 最大重试次数（初投 + 重试 5 次 = 最多 6 次尝试，之后死信） */
    private static final int MAX_RETRY = 5;
    /** 单次回调超时（毫秒） */
    private static final int TIMEOUT_MS = 5000;
    /** 每轮最多投递条数 */
    private static final int BATCH_SIZE = 20;

    private final OpenWebhookEventMapper eventMapper;
    private final OpenAppCredentialMapper credentialMapper;

    public OpenWebhookDispatcher(OpenWebhookEventMapper eventMapper, OpenAppCredentialMapper credentialMapper) {
        this.eventMapper = eventMapper;
        this.credentialMapper = credentialMapper;
    }

    /**
     * 到期事件投递扫描（fixedDelay 保证同一方法不并发执行）
     */
    @Scheduled(fixedDelay = 1000)
    public void sweep() {
        List<OpenWebhookEvent> dueList = eventMapper.selectList(new LambdaQueryWrapper<OpenWebhookEvent>()
                .eq(OpenWebhookEvent::getStatus, OpenWebhookEvent.STATUS_PENDING)
                .le(OpenWebhookEvent::getNextRetryAt, new Date())
                .orderByAsc(OpenWebhookEvent::getId)
                .last("limit " + BATCH_SIZE));
        for (OpenWebhookEvent event : dueList) {
            try {
                deliver(event);
            } catch (Exception e) {
                log.warn("webhook 投递异常（保留 PENDING 下轮重试）: id={}, {}",
                        event.getId(), e.getMessage());
            }
        }
    }

    private void deliver(OpenWebhookEvent event) {
        OpenAppCredential credential = credentialMapper.selectOne(new LambdaQueryWrapper<OpenAppCredential>()
                .eq(OpenAppCredential::getAppKey, event.getAppKey())
                .last("limit 1"));
        if (credential == null || credential.getStatus() == null || credential.getStatus() != 1
                || StrUtil.hasEmpty(credential.getWebhookUrl(), credential.getWebhookSecret())) {
            markDead(event, "订阅已失效（凭据删除/停用/未配置）");
            return;
        }
        String body = event.getPayload();
        long timestamp = System.currentTimeMillis();
        String signature = new HMac(HmacAlgorithm.HmacSHA256,
                credential.getWebhookSecret().getBytes(StandardCharsets.UTF_8))
                .digestHex(timestamp + "." + event.getId() + "." + body);
        boolean ok = false;
        int code = 0;
        String error = null;
        try (HttpResponse response = HttpRequest.post(credential.getWebhookUrl())
                .header("X-Mustard-Event", event.getEvent())
                .header("X-Mustard-Timestamp", String.valueOf(timestamp))
                .header("X-Mustard-Delivery", event.getId())
                .header("X-Mustard-Signature", signature)
                .timeout(TIMEOUT_MS)
                .body(body, "application/json")
                .execute()) {
            code = response.getStatus();
            ok = response.isOk();
            if (!ok) {
                error = truncate("HTTP " + code);
            }
        } catch (Exception e) {
            error = truncate(e.getMessage());
        }
        Date now = new Date();
        event.setLastCode(code);
        event.setLastError(ok ? "" : (error == null ? "HTTP " + code : error));
        event.setUpdateTime(now);
        if (ok) {
            event.setStatus(OpenWebhookEvent.STATUS_DELIVERED);
            event.setDeliveredAt(now);
            eventMapper.updateById(event);
            return;
        }
        int retryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        event.setRetryCount(retryCount);
        if (retryCount > MAX_RETRY) {
            // 初投 + 5 次重试均失败 → 死信（保留审计，可人工排查）
            event.setStatus(OpenWebhookEvent.STATUS_DEAD);
            event.setNextRetryAt(null);
        } else {
            // 指数退避：重试 1..5 → 1s/2s/4s/8s/16s
            event.setNextRetryAt(new Date(now.getTime() + (1L << (retryCount - 1)) * 1000L));
        }
        eventMapper.updateById(event);
    }

    private void markDead(OpenWebhookEvent event, String reason) {
        event.setStatus(OpenWebhookEvent.STATUS_DEAD);
        event.setLastError(reason);
        event.setNextRetryAt(null);
        event.setUpdateTime(new Date());
        eventMapper.updateById(event);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
