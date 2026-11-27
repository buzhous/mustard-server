package org.jeecg.modules.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.mapper.TuningConfigMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 调优配置读路径（P4）：DB enabled 配置优先于内置技能包/默认值。
 * <p>
 * 缓存策略：本地内存缓存（TTL 60s 兜底）+ Redis 版本键 {@code ail:tuning:ver}——
 * admin 保存后 INCR 版本键，全节点下一次读取即失效重载（兼容未来多实例）。
 * <p>
 * 降级口径：DB/Redis 任何异常均回退内置默认值并 warn（调优配置不可用不得阻断 AI 链路）。
 */
@Slf4j
@Service
public class TuningConfigService {

    /** 调优配置版本键：admin 写操作后 INCR，读方比对不一致即重载 */
    public static final String VER_KEY = "ail:tuning:ver";

    public static final String KEY_SKILL_FILING_PROMPT = "skill.filing.prompt";
    public static final String KEY_SKILL_QUERY_PROMPT = "skill.query.prompt";
    public static final String KEY_SKILL_UPDATE_PROMPT = "skill.update.prompt";
    public static final String KEY_SKILL_RECEIPT_PROMPT = "skill.receipt.prompt";
    public static final String KEY_MODEL_CHAT_MODEL = "model.chat_model";
    public static final String KEY_MODEL_TEMPERATURE = "model.temperature";
    public static final String KEY_AGENT_MAX_ASKS = "agent.max_asks";
    public static final String KEY_SEARCH_ALIAS_ENABLED = "search.alias_enabled";
    public static final String KEY_SEARCH_INDEX_ENABLED = "search.index_enabled";
    public static final String KEY_SEARCH_CACHE_ENABLED = "search.cache_enabled";
    public static final String KEY_SEARCH_CACHE_TTL_SECONDS = "search.cache_ttl_seconds";
    public static final String KEY_UPDATE_ENABLED = "update.enabled";

    /** 本地缓存兜底 TTL（版本键未变时的最长复用时间） */
    private static final long LOCAL_TTL_MS = 60_000L;

    /** 内置默认值（无 DB 配置/停用时回退）：追问上限、意图缓存开关与 TTL（yml 默认项走各接入点 @Value） */
    public static final int DEFAULT_MAX_ASKS = 3;
    public static final boolean DEFAULT_CACHE_ENABLED = true;
    public static final int DEFAULT_CACHE_TTL_SECONDS = 60;

    private final TuningConfigMapper configMapper;
    private final StringRedisTemplate redisTemplate;

    /** config_key → config_value（仅 active=1 且 enabled=1 行） */
    private volatile Map<String, String> cache = Map.of();
    private volatile long localVer = -1;
    private volatile long lastLoadMs = 0;

    public TuningConfigService(TuningConfigMapper configMapper, StringRedisTemplate redisTemplate) {
        this.configMapper = configMapper;
        this.redisTemplate = redisTemplate;
    }

    /** DB 配置优先：active+enabled 行非空值覆盖，否则回退内置默认 */
    public String strOpt(String key, String builtinDefault) {
        String v = raw(key);
        return v == null || v.isBlank() ? builtinDefault : v;
    }

    /** bool 配置：true/1 → true，false/0 → false，其余回退内置默认 */
    public boolean boolOpt(String key, boolean builtinDefault) {
        String v = raw(key);
        if (v == null || v.isBlank()) {
            return builtinDefault;
        }
        String t = v.trim();
        if ("true".equalsIgnoreCase(t) || "1".equals(t)) {
            return true;
        }
        if ("false".equalsIgnoreCase(t) || "0".equals(t)) {
            return false;
        }
        return builtinDefault;
    }

    public double numOpt(String key, double builtinDefault) {
        String v = raw(key);
        if (v == null || v.isBlank()) {
            return builtinDefault;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return builtinDefault;
        }
    }

    public int intOpt(String key, int builtinDefault) {
        return (int) Math.round(numOpt(key, builtinDefault));
    }

    /** 建档技能包提示词：DB enabled 配置优先于内置文件 */
    public String filingPrompt(String builtinTemplate) {
        return strOpt(KEY_SKILL_FILING_PROMPT, builtinTemplate);
    }

    /** 查询技能包提示词：DB enabled 配置优先于内置文件 */
    public String queryPrompt(String builtinTemplate) {
        return strOpt(KEY_SKILL_QUERY_PROMPT, builtinTemplate);
    }

    /** 编辑（update）技能包提示词：DB enabled 配置优先于内置文件（P5，调优页 UI 入口后续接入） */
    public String updatePrompt(String builtinTemplate) {
        return strOpt(KEY_SKILL_UPDATE_PROMPT, builtinTemplate);
    }

    /** 票据预填技能包提示词：DB enabled 配置优先于内置文件（P7，调优页 UI 入口后续接入） */
    public String receiptPrompt(String builtinTemplate) {
        return strOpt(KEY_SKILL_RECEIPT_PROMPT, builtinTemplate);
    }

    /** admin 写操作后调用：版本键 +1（全节点缓存失效），本节点立即失效 */
    public void bumpVersion() {
        try {
            redisTemplate.opsForValue().increment(VER_KEY);
        } catch (Exception e) {
            log.warn("[tuning] 版本键自增失败（本地缓存将于 TTL 后刷新）: {}", e.getMessage());
        }
        localVer = -1;
    }

    private String raw(String key) {
        loadIfNeeded();
        return cache.get(key);
    }

    private void loadIfNeeded() {
        long remoteVer;
        try {
            String v = redisTemplate.opsForValue().get(VER_KEY);
            remoteVer = v == null ? 0 : Long.parseLong(v);
        } catch (Exception e) {
            // Redis 不可用：走本地 TTL 兜底；无本地缓存时回退内置
            if (localVer < 0 || System.currentTimeMillis() - lastLoadMs > LOCAL_TTL_MS) {
                if (localVer < 0) {
                    cache = Map.of();
                }
                lastLoadMs = System.currentTimeMillis();
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (localVer >= 0 && remoteVer == localVer && now - lastLoadMs < LOCAL_TTL_MS) {
            return;
        }
        try {
            List<TuningConfig> rows = configMapper.selectList(new LambdaQueryWrapper<TuningConfig>()
                    .eq(TuningConfig::getActive, 1)
                    .eq(TuningConfig::getEnabled, 1));
            Map<String, String> next = new HashMap<>();
            for (TuningConfig row : rows) {
                if (row.getConfigKey() != null && row.getConfigValue() != null) {
                    next.put(row.getConfigKey(), row.getConfigValue());
                }
            }
            cache = next;
            localVer = remoteVer;
            lastLoadMs = now;
        } catch (Exception e) {
            // DB 异常：清空 DB 覆盖（全部回退内置默认），不阻断 AI 链路
            log.warn("[tuning] 配置加载失败，回退内置默认: {}", e.getMessage());
            cache = Map.of();
            localVer = remoteVer;
            lastLoadMs = now;
        }
    }
}
