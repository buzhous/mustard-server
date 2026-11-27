package org.jeecg.modules.ai.adapter;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.enums.AiErrorEnum;
import org.jeecg.modules.ai.bean.exception.AppException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 全局并发信号量（P2-8 限流基建）：
 * Redis incr/decr 计数 + key TTL 60s 防泄漏；Redis 故障 fail-open（限流是保护性措施，
 * 不得成为 AI 链路的单点故障源）。
 * <p>
 * 槽位粒度：
 * - L2 conv：一次 send 处理占一个槽（编排轮次间空闲不占槽），超时抛 QUEUE_TIMEOUT；
 * - L3 llm：单次模型调用占一个槽（调用结束立即释放）。
 * <p>
 * 单请求模型调用上限：ThreadLocal 计数（入口 beginRequest / finally endRequest），
 * 超限抛 11002「AI 服务繁忙」——防编排 bug 无限重试烧模型。
 */
@Slf4j
@Component
public class AiGlobalSemaphore {

    public static final String SEM_CONV = "conv";
    public static final String SEM_LLM = "llm";

    private static final String KEY_PREFIX = "ail:sem:";
    /** 槽位键 TTL：进程崩溃泄漏的槽位 60s 内自动回收 */
    private static final long KEY_TTL_SECONDS = 60;
    private static final long POLL_INTERVAL_MS = 50;

    /** 单请求模型调用上限 */
    private static final int MAX_MODEL_CALLS_PER_REQUEST = 20;

    private final StringRedisTemplate redisTemplate;

    /** 单请求模型调用计数（同请求线程内：begin → 多次 countModelCall → end） */
    private static final ThreadLocal<Integer> REQUEST_MODEL_CALLS = ThreadLocal.withInitial(() -> 0);

    public AiGlobalSemaphore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 请求入口：重置单请求模型调用计数 */
    public void beginRequest() {
        REQUEST_MODEL_CALLS.set(0);
    }

    /** 请求出口：清理 ThreadLocal（防线程复用串计数） */
    public void endRequest() {
        REQUEST_MODEL_CALLS.remove();
    }

    /**
     * 单请求模型调用计数 +1；超上限抛 11002「AI 服务繁忙」
     */
    public void countModelCall() {
        int calls = REQUEST_MODEL_CALLS.get();
        if (calls >= MAX_MODEL_CALLS_PER_REQUEST) {
            throw new AppException(AiErrorEnum.REQUEST_PARAM_ERROR, "AI 服务繁忙");
        }
        REQUEST_MODEL_CALLS.set(calls + 1);
    }

    /**
     * 获取全局并发槽（Redis incr；TTL 60s 防泄漏；Redis 故障 fail-open）
     *
     * @param key    槽位名（SEM_CONV / SEM_LLM）
     * @param max    最大并发数
     * @param waitMs 等待时长（毫秒，0=只尝试一次）
     * @return 是否获得槽位；等待超时返回 false
     */
    public boolean tryAcquire(String key, int max, long waitMs) {
        long deadline = System.currentTimeMillis() + Math.max(0, waitMs);
        while (true) {
            if (acquireOnce(key, max)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 释放槽位（decr；计数归零即删键）
     */
    public void release(String key) {
        try {
            String k = KEY_PREFIX + key;
            Long v = redisTemplate.opsForValue().decrement(k);
            if (v != null && v <= 0) {
                redisTemplate.delete(k);
            }
        } catch (Exception e) {
            // fail-open：释放失败最多泄漏一个槽位（TTL 60s 兜底回收）
        }
    }

    private boolean acquireOnce(String key, int max) {
        try {
            String k = KEY_PREFIX + key;
            Long count = redisTemplate.opsForValue().increment(k);
            if (count == null) {
                // Redis 异常响应 null：fail-open 放行
                return true;
            }
            if (count == 1) {
                redisTemplate.expire(k, KEY_TTL_SECONDS, TimeUnit.SECONDS);
            }
            if (count <= max) {
                return true;
            }
            // 超限：回退本次增量，不占槽
            redisTemplate.opsForValue().decrement(k);
            return false;
        } catch (Exception e) {
            log.warn("全局信号量 Redis 异常 fail-open: key={}", key, e);
            return true;
        }
    }
}
