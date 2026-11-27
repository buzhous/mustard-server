package org.jeecg.modules.ai.service;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.bean.dto.AiItemQuery;
import org.jeecg.modules.ai.config.TuningConfigService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 查询意图缓存（P4）：search_items 结果卡 Redis TTL 缓存。
 * <p>
 * key：{@code ail:qcache:<uid>:<md5(AiItemQuery 全字段 JSON + userText 归一化 trim)>}；
 * TTL 与开关走调优配置（search.cache_ttl_seconds 默认 60 / search.cache_enabled 默认 true）。
 * 写失效：app 侧物品写路径成功后 {@link #evictUid(String)} 批量删该 uid 全部缓存。
 * 口径：仅缓存（Redis/解析）异常吞掉返回未命中，不影响查询主链路。
 */
@Slf4j
@Service
public class AiQueryCacheService {

    /** 缓存键前缀（uid 维度隔离） */
    public static final String KEY_PREFIX = "ail:qcache:";

    /** TTL 范围保护（与 admin 参数校验一致：5–600 秒） */
    private static final int MIN_TTL_SECONDS = 5;
    private static final int MAX_TTL_SECONDS = 600;

    private final StringRedisTemplate redisTemplate;
    private final TuningConfigService tuningConfigService;

    public AiQueryCacheService(StringRedisTemplate redisTemplate, TuningConfigService tuningConfigService) {
        this.redisTemplate = redisTemplate;
        this.tuningConfigService = tuningConfigService;
    }

    /** 意图缓存开关（search.cache_enabled，默认 true） */
    public boolean enabled() {
        return tuningConfigService.boolOpt(TuningConfigService.KEY_SEARCH_CACHE_ENABLED,
                TuningConfigService.DEFAULT_CACHE_ENABLED);
    }

    /** 缓存键：ail:qcache:<uid>:<md5(query JSON + "|" + userText.trim())> */
    public String cacheKey(String uid, AiItemQuery query, String userText) {
        String normalized = JSON.toJSONString(query) + "|" + (userText == null ? "" : userText.trim());
        return KEY_PREFIX + uid + ":" + SecureUtil.md5(normalized);
    }

    /** 读缓存：命中返回结果卡 JSON，未命中/异常返回 null */
    public JSONObject get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? null : JSON.parseObject(json);
        } catch (Exception e) {
            log.warn("[ai-qcache] 读取失败视为未命中: {}", e.getMessage());
            return null;
        }
    }

    /** 写缓存：TTL = search.cache_ttl_seconds（默认 60，clamp 5–600），异常吞掉 */
    public void put(String key, JSONObject card) {
        try {
            int ttl = tuningConfigService.intOpt(TuningConfigService.KEY_SEARCH_CACHE_TTL_SECONDS,
                    TuningConfigService.DEFAULT_CACHE_TTL_SECONDS);
            ttl = Math.max(MIN_TTL_SECONDS, Math.min(MAX_TTL_SECONDS, ttl));
            redisTemplate.opsForValue().set(key, card.toJSONString(), Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("[ai-qcache] 写入失败忽略: {}", e.getMessage());
        }
    }

    /** uid 维度批量失效（物品写路径成功后调用），异常吞掉不影响写主流程 */
    public void evictUid(String uid) {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + uid + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("[ai-qcache] 失效失败忽略: {}", e.getMessage());
        }
    }
}
