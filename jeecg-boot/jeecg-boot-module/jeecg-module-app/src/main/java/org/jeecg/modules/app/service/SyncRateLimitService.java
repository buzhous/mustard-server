package org.jeecg.modules.app.service;

import org.jeecg.modules.app.bean.enums.ExceptionEnum;
import org.jeecg.modules.app.bean.exception.AppException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 物品同步接口限流：Redis 固定窗口计数（按用户维度），
 * 防 2000 人并发下异常客户端风暴（死循环重试/高频轮询）拖垮同步链路。
 * Redis 不可用时降级放行——限流是保护性措施，不得成为同步链路的单点故障源。
 *
 * 阈值 120 次/分钟：客户端 6s tick（拉取 10 次/分钟）+ 队列上传 + 冲突即时拉取，
 * 正常使用余量充足；仅异常风暴会被拦截。
 */
@Service
public class SyncRateLimitService {

    /** 每用户每分钟同步请求上限 */
    private static final int LIMIT_PER_MINUTE = 120;

    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final RedisTemplate redisTemplate;

    public SyncRateLimitService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验同步接口调用频次，超限抛 10206（可重试，客户端按退避策略稍后重试）
     */
    public void checkLimit(String userId) {
        try {
            String minute = LocalDateTime.now().format(MINUTE_FORMAT);
            String key = "app:sync:rate:" + userId + ":" + minute;
            Long count = redisTemplate.opsForValue().increment(key, 1);
            if (count == null) {
                return;
            }
            if (count == 1) {
                redisTemplate.expire(key, 90, TimeUnit.SECONDS);
            }
            if (count > LIMIT_PER_MINUTE) {
                throw new AppException(ExceptionEnum.SYNC_RATE_LIMIT);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            // Redis 异常（连接失败/超时）：降级放行，不阻塞同步主链路
        }
    }

}
