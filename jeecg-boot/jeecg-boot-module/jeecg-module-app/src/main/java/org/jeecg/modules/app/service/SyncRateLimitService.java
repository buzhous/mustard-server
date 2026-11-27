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

    /** L4 增量拉取最小间隔（毫秒）：窗口内服务端无该用户数据更新则静默空增量 */
    private static final long PULL_MIN_INTERVAL_MS = 10_000;
    /** 拉取水位键 TTL（秒）：略大于最小间隔即可，过期自重建 */
    private static final long PULL_WATERMARK_TTL_SECONDS = 15;

    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final RedisTemplate redisTemplate;
    private final org.jeecg.modules.app.mapper.ItemSyncMapper itemSyncMapper;

    public SyncRateLimitService(RedisTemplate redisTemplate,
                                org.jeecg.modules.app.mapper.ItemSyncMapper itemSyncMapper) {
        this.redisTemplate = redisTemplate;
        this.itemSyncMapper = itemSyncMapper;
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

    /**
     * L4 增量拉取节流（P2-8）：距上次拉取 <10s 且服务端无该用户数据更新 → 返回 false（端点静默空增量）；
     * 有更新（tbl_item_sync.update_time > 上次拉取水位）或超时 → 放行并刷新水位。
     * Redis/DB 异常降级放行——节流是保护性措施，不得阻塞同步主链路。
     */
    public boolean checkPullInterval(String userId) {
        try {
            String key = "ail:sync:pull:" + userId;
            Object lastObj = redisTemplate.opsForValue().get(key);
            long now = System.currentTimeMillis();
            long last = 0;
            if (lastObj != null) {
                try {
                    last = Long.parseLong(String.valueOf(lastObj));
                } catch (NumberFormatException e) {
                    last = 0;
                }
            }
            if (lastObj == null || now - last >= PULL_MIN_INTERVAL_MS) {
                redisTemplate.opsForValue().set(key, String.valueOf(now), PULL_WATERMARK_TTL_SECONDS, TimeUnit.SECONDS);
                return true;
            }
            // 窗口内：有数据更新则放行（刷新水位），否则静默空增量
            Long changes = itemSyncMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<org.jeecg.modules.app.entity.ItemSync>()
                    .lambda()
                    .eq(org.jeecg.modules.app.entity.ItemSync::getUserId, userId)
                    .gt(org.jeecg.modules.app.entity.ItemSync::getUpdateTime, new java.util.Date(last)));
            if (changes != null && changes > 0) {
                redisTemplate.opsForValue().set(key, String.valueOf(now), PULL_WATERMARK_TTL_SECONDS, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            // Redis/DB 异常：降级放行
            return true;
        }
    }

}
