package org.jeecg.modules.ai.adapter;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 建档限流（ai-integration-plan §5.8：聊天 10/min/用户；MCP 创建 20/h/用户）。
 * 复用 ai-chat.rate-limit 三级限流的窗口语义，dev 内置内存滑动窗口实现。
 */
@Component
public class AiRateLimiter {

    private static final int CHAT_PER_MINUTE = 10;
    private static final int CREATE_PER_HOUR = 20;
    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 3_600_000L;

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    /** 聊天限流：10 次/分钟/用户 */
    public synchronized String tryAcquireChat(String uid) {
        return tryAcquire("chat:" + uid, CHAT_PER_MINUTE, MINUTE_MS);
    }

    /** 创建限流：20 次/小时/用户 */
    public synchronized String tryAcquireCreate(String uid) {
        return tryAcquire("create:" + uid, CREATE_PER_HOUR, HOUR_MS);
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
