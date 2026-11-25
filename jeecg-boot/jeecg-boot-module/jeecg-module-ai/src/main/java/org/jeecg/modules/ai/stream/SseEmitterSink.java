package org.jeecg.modules.ai.stream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.vo.ai.AiChatSendVO;
import org.jeecg.modules.ai.entity.AiChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 SseEmitter 的流式事件汇实现。
 * <p>
 * 断连容错：任何发送异常（IO/已完成）置 disconnected 后所有事件静默 no-op，
 * 不向上抛——保证编排任务继续执行并照常落库（断连不回放基线）；
 * done/error 为终端事件（二选一，只发一次），发送后 complete 连接。
 */
@Slf4j
public class SseEmitterSink implements AiStreamSink {

    private final SseEmitter emitter;
    /** 断连/完成标志：置位后所有事件静默丢弃 */
    private volatile boolean disconnected;
    /** 终端事件已发标志：done/error 二选一只发一次 */
    private final AtomicBoolean terminalSent = new AtomicBoolean(false);

    public SseEmitterSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    /** 标记断连/完成（emitter 回调与心跳失败时调用），此后事件全部 no-op */
    public void markDisconnected() {
        this.disconnected = true;
    }

    @Override
    public void session(String sessionId) {
        JSONObject data = new JSONObject(true);
        data.put("sessionId", sessionId);
        send("session", data.toJSONString());
    }

    @Override
    public void progress(String stage) {
        JSONObject data = new JSONObject(true);
        data.put("stage", stage);
        send("progress", data.toJSONString());
    }

    @Override
    public void message(AiChatMessage msg) {
        send("message", JSON.toJSONString(msg));
    }

    @Override
    public void done(AiChatSendVO vo) {
        if (terminalSent.compareAndSet(false, true)) {
            send("done", JSON.toJSONString(vo));
            complete();
        }
    }

    @Override
    public void error(int code, String message) {
        if (terminalSent.compareAndSet(false, true)) {
            JSONObject data = new JSONObject(true);
            data.put("code", code);
            data.put("message", message);
            send("error", data.toJSONString());
            complete();
        }
    }

    /** 心跳 comment 帧（`: ping`），供代理层保活；失败返回 false 供调用方取消心跳任务 */
    public boolean sendComment() {
        if (disconnected) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().comment("ping"));
            return true;
        } catch (IOException | IllegalStateException e) {
            disconnected = true;
            return false;
        }
    }

    private void send(String name, String data) {
        if (disconnected) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("[ai-stream] 事件发送失败（客户端断连），后续事件 no-op: {}", e.getMessage());
            disconnected = true;
        }
    }

    private void complete() {
        if (disconnected) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("[ai-stream] emitter complete 失败（已断连）: {}", e.getMessage());
            disconnected = true;
        }
    }
}
