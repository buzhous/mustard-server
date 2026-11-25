package org.jeecg.modules.ai.stream;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.service.AiAgentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * L1 SSE 阶段流服务：stream 端点的 emitter 生命周期管理（会话回执/心跳/异步任务提交）。
 * <p>
 * 职责边界：参数校验/鉴权/限流前置在 controller（servlet 线程内）；本服务只管
 * emitter 创建 → session 事件立即回执 → 心跳注册 → 任务提交（有界线程池，拒绝补 error 事件）。
 * 断连不回放：emitter 回调只置断连+取消心跳，任务继续执行并照常落库，客户端重连走会话消息拉取。
 */
@Slf4j
@Service
public class AiStreamService {

    /** 流式通道开关（关闭时端点直接回 Result 错误 JSON，App 自动落回一次性 POST） */
    @Value("${ai.stream.enabled:true}")
    private boolean enabled;
    /** emitter 超时（模型/查询管线最慢路径余量，心跳兜底代理层空闲断开） */
    @Value("${ai.stream.timeout-ms:120000}")
    private long timeoutMs;
    /** 心跳间隔（comment 帧保活） */
    @Value("${ai.stream.heartbeat-ms:30000}")
    private long heartbeatMs;

    private final AiAgentService agentService;
    private final ExecutorService aiStreamExecutor;
    private final ScheduledExecutorService aiStreamHeartbeat;

    public AiStreamService(AiAgentService agentService,
                           ExecutorService aiStreamExecutor,
                           ScheduledExecutorService aiStreamHeartbeat) {
        this.agentService = agentService;
        this.aiStreamExecutor = aiStreamExecutor;
        this.aiStreamHeartbeat = aiStreamHeartbeat;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 开启流式回合：建 emitter → session 事件立即回执 → 心跳 → 提交流式任务。
     * 调用前提：controller 已完成鉴权/参数校验/限流前置。
     */
    public SseEmitter open(String uid, String sessionId, String text, List<String> images, String mode) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        SseEmitterSink sink = new SseEmitterSink(emitter);

        // 断连/超时：仅置断连+取消心跳，任务照常执行落库（断连不回放）
        ScheduledFuture<?> heartbeat = scheduleHeartbeat(sink);
        emitter.onCompletion(() -> close(sink, heartbeat));
        emitter.onTimeout(() -> {
            close(sink, heartbeat);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 容器已完成，忽略
            }
        });
        emitter.onError(t -> close(sink, heartbeat));

        // session 立即回执（限流/校验已在 servlet 线程通过）
        sink.session(sessionId);

        try {
            aiStreamExecutor.submit(() -> {
                try {
                    AiChatSendVO vo = agentService.send(uid, sessionId, text, images, mode, sink);
                    sink.done(vo);
                } catch (AppException e) {
                    sink.error(e.getCode(), e.getMsg());
                } catch (Exception e) {
                    log.error("[ai-stream] 流式任务执行失败 uid={} sessionId={}", uid, sessionId, e);
                    sink.error(11002, "AI 服务暂不可用");
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[ai-stream] 流式任务提交被拒（线程池饱和）uid={}", uid);
            sink.error(11002, "AI 服务繁忙，请稍后再试");
        }
        return emitter;
    }

    private ScheduledFuture<?> scheduleHeartbeat(SseEmitterSink sink) {
        return aiStreamHeartbeat.scheduleAtFixedRate(() -> {
            if (!sink.sendComment()) {
                // 发送失败即断连；本次调度由 close() 取消（抛出以终止后续调度亦可）
                throw new IllegalStateException("sse disconnected");
            }
        }, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);
    }

    private void close(SseEmitterSink sink, ScheduledFuture<?> heartbeat) {
        heartbeat.cancel(false);
        sink.markDisconnected();
    }
}
