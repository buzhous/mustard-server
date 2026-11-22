package org.jeecg.modules.app.openapi;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 开放平台限流与配额（设计方案 3.3 / E4）
 * <p>
 * RedisUtil INCR + EXPIRE 固定窗口（无 Redisson 依赖）：
 * <pre>
 * open:rl:m:{appKey}:{yyyyMMddHHmm}   总分钟窗口（60/min），TTL 120s
 * open:rl:w:{appKey}:{yyyyMMddHHmm}   写分钟窗口（10/min），TTL 120s
 * open:rl:d:{appKey}:{yyyyMMdd}       日配额（daily_quota 默认 5000），TTL 172800s
 * </pre>
 */
@Slf4j
@Component
public class OpenRateLimiter {

    /** 只读接口阈值：60 次/分钟 */
    private static final long MINUTE_LIMIT = 60;
    /** 写接口阈值：10 次/分钟 */
    private static final long WRITE_MINUTE_LIMIT = 10;
    /** 日配额默认值 */
    private static final long DEFAULT_DAILY_QUOTA = 5000;

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String KEY_MINUTE = "open:rl:m:";
    private static final String KEY_WRITE = "open:rl:w:";
    private static final String KEY_DAILY = "open:rl:d:";

    private final RedisUtil redisUtil;

    public OpenRateLimiter(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 配额校验并计数
     *
     * @param appKey     调用方 AppKey
     * @param write      是否写操作
     * @param dailyQuota 日配额覆盖（null=默认 5000）
     * @return null=通过；否则返回限流原因（用于 429 message）
     */
    public String tryAcquire(String appKey, boolean write, Integer dailyQuota) {
        String minuteKey = KEY_MINUTE + appKey + ":" + LocalDateTime.now().format(MINUTE_FMT);
        long minute = incrWithTtl(minuteKey, 120);
        if (minute > MINUTE_LIMIT) {
            return "rate limit exceeded：" + MINUTE_LIMIT + " 次/分钟";
        }
        if (write) {
            String writeKey = KEY_WRITE + appKey + ":" + LocalDateTime.now().format(MINUTE_FMT);
            long w = incrWithTtl(writeKey, 120);
            if (w > WRITE_MINUTE_LIMIT) {
                return "rate limit exceeded：写接口 " + WRITE_MINUTE_LIMIT + " 次/分钟";
            }
        }
        String dailyKey = KEY_DAILY + appKey + ":" + LocalDate.now().format(DAY_FMT);
        long quota = dailyQuota == null ? DEFAULT_DAILY_QUOTA : dailyQuota;
        long daily = incrWithTtl(dailyKey, 172800);
        if (daily > quota) {
            return "daily quota exceeded：" + quota + " 次/天";
        }
        return null;
    }

    /**
     * 429 响应头 X-RateLimit-Reset：当前分钟窗口结束的 epoch 秒
     */
    public long minuteResetEpochSecond() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.plusMinutes(1).withSecond(0).withNano(0);
        return next.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
    }

    private long incrWithTtl(String key, long ttlSeconds) {
        long v = redisUtil.incr(key, 1);
        if (v == 1) {
            redisUtil.expire(key, ttlSeconds);
        }
        return v;
    }
}
