package org.jeecg.modules.ai.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 建档限流（ai-integration-plan §5.8：聊天 10/min/用户；MCP 创建 20/h/用户）。
 * 复用 ai-chat.rate-limit 三级限流的窗口语义，dev 内置内存滑动窗口实现。
 * <p>
 * 限流值支持配置覆盖（默认值不变）：{@code ai.rate-limit.chat-per-minute} / {@code ai.rate-limit.create-per-hour}，
 * 供 dev 全量跑测调高（如 -Dai.rate-limit.chat-per-minute=120）。
 */
@Component
public class AiRateLimiter {

    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 3_600_000L;

    /** 聊天限流（次/分钟/用户） */
    @Value("${ai.rate-limit.chat-per-minute:10}")
    private int chatPerMinute;

    /** 创建限流（次/小时/用户） */
    @Value("${ai.rate-limit.create-per-hour:20}")
    private int createPerHour;

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    /** 聊天限流：默认 10 次/分钟/用户 */
    public synchronized String tryAcquireChat(String uid) {
        return tryAcquire("chat:" + uid, chatPerMinute, MINUTE_MS);
    }

    /** 创建限流：默认 20 次/小时/用户 */
    public synchronized String tryAcquireCreate(String uid) {
        return tryAcquire("create:" + uid, createPerHour, HOUR_MS);
    }

    private String tryAcquire(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!deque.isEmpty() && now - deque.peekFirst() > windowMs) {
            deque.pollFirst();
        }
        if (deque.size() >= limit) {
            return "请求过于频繁，请稍后再试";
        }
        deque.addLast(now);
        return null;
    }
}
