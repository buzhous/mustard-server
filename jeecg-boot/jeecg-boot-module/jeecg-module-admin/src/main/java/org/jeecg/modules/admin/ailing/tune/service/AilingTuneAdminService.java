package org.jeecg.modules.admin.ailing.tune.service;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.admin.ailing.tune.mapper.AilingAliasAdminMapper;
import org.jeecg.modules.admin.ailing.tune.mapper.AilingTuneAdminMapper;
import org.jeecg.modules.admin.ailing.tune.mapper.AilingTuneAdminMapper.PageArgsHolder;
import org.jeecg.modules.ai.adapter.OpenAiCompatAdapter;
import org.jeecg.modules.ai.config.TuningConfig;
import org.jeecg.modules.ai.config.TuningConfigService;
import org.jeecg.modules.ai.mapper.TuningConfigMapper;
import org.jeecg.modules.ai.service.AiQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 芥灵调优中心服务（P4）：提示词在线编辑（DB enabled 配置 &gt; 内置技能包）/ 模型与检索参数下发 /
 * 兜底链观测聚合 / 样本池处置。调优配置表复用 ai 模块 TuningConfigMapper（admin→app→ai 依赖链）。
 * <p>
 * 版本模型：tbl_ai_tuning_config 同 key 多版本（active=1 生效，历史 active=0，恢复=追加新版本），
 * 任何写操作后 {@link TuningConfigService#bumpVersion()} 即刻全节点生效。
 */
@Slf4j
@Service
public class AilingTuneAdminService {

    /** 查询技能包两段契约分段标记（与 AiQueryService.STAGE_SPLIT_MARKER 同串，保存校验必需） */
    public static final String STAGE_SPLIT_MARKER = "<!-- ==== STAGE_SPLIT · 以下为第二段契约 ==== -->";

    /** filing 技能包必需占位符（与 AiAgentService 模板填充一致） */
    static final List<String> FILING_VARS = List.of("VISION_DESCRIPTION", "HISTORY", "ASK_BUDGET",
            "USER_CATEGORIES", "USER_TOPICS", "MODE", "DRAFT_STATE", "USER_TEXT");
    /** query 技能包必需占位符（与 AiQueryService 模板填充一致） */
    static final List<String> QUERY_VARS = List.of("USER_CATEGORIES", "USER_STATIONS", "HISTORY", "USER_TEXT");

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([A-Z_]+)\\}\\}");
    private static final String FILING_TEMPLATE_PATH = "skills/mustard-item-filing-v2/system_prompt.md";
    private static final String QUERY_TEMPLATE_PATH = "skills/mustard-item-query-v2/system_prompt.md";

    @Autowired
    private TuningConfigMapper configMapper;
    @Autowired
    private TuningConfigService tuningConfigService;
    @Autowired
    private OpenAiCompatAdapter modelAdapter;
    @Autowired
    private AiQueryService aiQueryService;
    @Autowired
    private AilingTuneAdminMapper tuneMapper;
    @Autowired
    private AilingAliasAdminMapper aliasMapper;
    @Autowired
    private RedisUtil redisUtil;

    // ================================ 配置全量视图 ================================

    /** 全量配置视图：技能包（DB 生效值+内置全文+必需占位符）/ 模型参数 / 检索参数 / 各 key 版本历史 */
    public Map<String, Object> configView() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("filing", skillView("filing", TuningConfigService.KEY_SKILL_FILING_PROMPT,
                FILING_TEMPLATE_PATH, FILING_VARS));
        out.put("query", skillView("query", TuningConfigService.KEY_SKILL_QUERY_PROMPT,
                QUERY_TEMPLATE_PATH, QUERY_VARS));

        boolean chatModelDb = dbActive(TuningConfigService.KEY_MODEL_CHAT_MODEL);
        boolean temperatureDb = dbActive(TuningConfigService.KEY_MODEL_TEMPERATURE);
        boolean maxAsksDb = dbActive(TuningConfigService.KEY_AGENT_MAX_ASKS);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("chatModel", paramStrView(tuningConfigService.strOpt(TuningConfigService.KEY_MODEL_CHAT_MODEL,
                modelAdapter.defaultChatModel()), chatModelDb, modelAdapter.defaultChatModel()));
        model.put("temperature", paramView(tuningConfigService.numOpt(TuningConfigService.KEY_MODEL_TEMPERATURE,
                modelAdapter.defaultTemperature()), temperatureDb, modelAdapter.defaultTemperature()));
        model.put("maxAsks", paramView((double) tuningConfigService.intOpt(TuningConfigService.KEY_AGENT_MAX_ASKS,
                TuningConfigService.DEFAULT_MAX_ASKS), maxAsksDb, (double) TuningConfigService.DEFAULT_MAX_ASKS));
        out.put("model", model);

        boolean aliasDb = dbActive(TuningConfigService.KEY_SEARCH_ALIAS_ENABLED);
        boolean indexDb = dbActive(TuningConfigService.KEY_SEARCH_INDEX_ENABLED);
        boolean cacheDb = dbActive(TuningConfigService.KEY_SEARCH_CACHE_ENABLED);
        boolean ttlDb = dbActive(TuningConfigService.KEY_SEARCH_CACHE_TTL_SECONDS);
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("aliasEnabled", boolView(tuningConfigService.boolOpt(TuningConfigService.KEY_SEARCH_ALIAS_ENABLED,
                aiQueryService.defaultAliasEnabled()), aliasDb, aiQueryService.defaultAliasEnabled()));
        search.put("indexEnabled", boolView(tuningConfigService.boolOpt(TuningConfigService.KEY_SEARCH_INDEX_ENABLED,
                aiQueryService.defaultIndexEnabled()), indexDb, aiQueryService.defaultIndexEnabled()));
        search.put("cacheEnabled", boolView(tuningConfigService.boolOpt(TuningConfigService.KEY_SEARCH_CACHE_ENABLED,
                TuningConfigService.DEFAULT_CACHE_ENABLED), cacheDb, TuningConfigService.DEFAULT_CACHE_ENABLED));
        search.put("cacheTtlSeconds", paramView((double) tuningConfigService.intOpt(
                TuningConfigService.KEY_SEARCH_CACHE_TTL_SECONDS, TuningConfigService.DEFAULT_CACHE_TTL_SECONDS),
                ttlDb, (double) TuningConfigService.DEFAULT_CACHE_TTL_SECONDS));
        out.put("search", search);

        out.put("history", historyView());
        return out;
    }

    private Map<String, Object> skillView(String skill, String key, String templatePath, List<String> requiredVars) {
        TuningConfig active = activeRow(key);
        boolean dbEnabled = active != null && active.getEnabled() != null && active.getEnabled() == 1;
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("skill", skill);
        view.put("source", dbEnabled ? "db" : "builtin");
        view.put("version", dbEnabled && active.getVersion() != null ? active.getVersion() : null);
        view.put("dbValue", dbEnabled ? active.getConfigValue() : null);
        view.put("builtin", readClasspathTemplate(templatePath));
        view.put("requiredVars", requiredVars);
        return view;
    }

    private Map<String, Object> paramView(double effective, boolean db, double builtin) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("effective", effective);
        v.put("source", db ? "db" : "builtin");
        v.put("builtin", builtin);
        return v;
    }

    private Map<String, Object> paramStrView(String effective, boolean db, String builtin) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("effective", effective);
        v.put("source", db ? "db" : "builtin");
        v.put("builtin", builtin);
        return v;
    }

    private Map<String, Object> boolView(boolean effective, boolean db, boolean builtin) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("effective", effective);
        v.put("source", db ? "db" : "builtin");
        v.put("builtin", builtin);
        return v;
    }

    /** 各 config_key 版本历史（全部行，update_time desc，上限 100） */
    private List<Map<String, Object>> historyView() {
        List<TuningConfig> rows = configMapper.selectList(new LambdaQueryWrapper<TuningConfig>()
                .orderByDesc(TuningConfig::getUpdateTime)
                .last("LIMIT 100"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (TuningConfig row : rows) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("key", row.getConfigKey());
            v.put("version", row.getVersion());
            v.put("enabled", row.getEnabled());
            v.put("active", row.getActive());
            v.put("valueType", row.getValueType());
            v.put("valueLength", row.getConfigValue() == null ? 0 : row.getConfigValue().length());
            v.put("remark", row.getRemark());
            v.put("updateBy", row.getUpdateBy());
            v.put("updateTime", row.getUpdateTime());
            out.add(v);
        }
        return out;
    }

    // ================================ 提示词在线编辑 ================================

    /** 提示词保存：占位符完整性校验（缺失阻断）→ 新版本写入 → 版本键 INCR */
    public Map<String, Object> promptSave(String skill, String content, String operator) {
        String key = requireSkillKey(skill);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("提示词内容不能为空");
        }
        Set<String> missing = new HashSet<>(filing(skill) ? FILING_VARS : QUERY_VARS);
        Matcher m = VAR_PATTERN.matcher(content);
        while (m.find()) {
            missing.remove(m.group(1));
        }
        if (!missing.isEmpty()) {
            List<String> tokens = missing.stream().map(v -> "{{" + v + "}}").toList();
            throw new IllegalArgumentException("保存失败，缺少占位符：" + String.join("、", tokens));
        }
        if (!filing(skill) && !content.contains(STAGE_SPLIT_MARKER)) {
            throw new IllegalArgumentException("保存失败，查询技能包需保留两段契约分段标记 " + STAGE_SPLIT_MARKER);
        }
        TuningConfig saved = upsertActive(key, content, "string", "在线编辑", operator);
        return savedView(saved);
    }

    /** 一键回滚内置：该 key 全部行 enabled=0（历史保留，行为回退内置文件） */
    public Map<String, Object> promptRollback(String skill, String operator) {
        String key = requireSkillKey(skill);
        int n = configMapper.update(null, new LambdaUpdateWrapper<TuningConfig>()
                .eq(TuningConfig::getConfigKey, key)
                .eq(TuningConfig::getEnabled, 1)
                .set(TuningConfig::getEnabled, 0)
                .set(TuningConfig::getUpdateBy, operator)
                .set(TuningConfig::getUpdateTime, new Date()));
        tuningConfigService.bumpVersion();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("skill", skill);
        out.put("disabledRows", n);
        out.put("source", "builtin");
        return out;
    }

    /** 恢复历史版本：不改旧行，追加新 active 版本（审计可追溯） */
    public Map<String, Object> promptRestore(String skill, Integer version, String operator) {
        String key = requireSkillKey(skill);
        if (version == null || version < 1) {
            throw new IllegalArgumentException("版本号无效");
        }
        TuningConfig row = configMapper.selectOne(new LambdaQueryWrapper<TuningConfig>()
                .eq(TuningConfig::getConfigKey, key)
                .eq(TuningConfig::getVersion, version));
        if (row == null || row.getConfigValue() == null) {
            throw new IllegalArgumentException("版本 v" + version + " 不存在");
        }
        TuningConfig saved = upsertActive(key, row.getConfigValue(),
                row.getValueType() == null ? "string" : row.getValueType(), "恢复自 v" + version, operator);
        return savedView(saved);
    }

    // ================================ 模型与检索参数下发 ================================

    /**
     * 参数下发：均可空=不改。校验 temperature 0–2 / maxAsks 0–5 / ttl 5–600 / bool 仅 0/1。
     */
    public Map<String, Object> paramsSave(JSONObject model, JSONObject search, String operator) {
        List<String> changed = new ArrayList<>();
        if (model != null) {
            String chatModel = model.getString("chatModel");
            if (chatModel != null && !chatModel.isBlank()) {
                upsertActive(TuningConfigService.KEY_MODEL_CHAT_MODEL, chatModel.trim(), "string", "参数下发", operator);
                changed.add("model.chat_model");
            }
            Object temperature = model.get("temperature");
            if (temperature != null) {
                double v = toDouble(temperature, "temperature");
                if (v < 0 || v > 2) {
                    throw new IllegalArgumentException("temperature 取值 0–2");
                }
                upsertActive(TuningConfigService.KEY_MODEL_TEMPERATURE, numberStr(v), "number", "参数下发", operator);
                changed.add("model.temperature");
            }
            Object maxAsks = model.get("maxAsks");
            if (maxAsks != null) {
                double v = toDouble(maxAsks, "maxAsks");
                if (v < 0 || v > 5 || v != Math.floor(v)) {
                    throw new IllegalArgumentException("maxAsks 取值 0–5 整数");
                }
                upsertActive(TuningConfigService.KEY_AGENT_MAX_ASKS, numberStr(v), "number", "参数下发", operator);
                changed.add("agent.max_asks");
            }
        }
        if (search != null) {
            changed.addAll(saveBool(search, "aliasEnabled", TuningConfigService.KEY_SEARCH_ALIAS_ENABLED, operator));
            changed.addAll(saveBool(search, "indexEnabled", TuningConfigService.KEY_SEARCH_INDEX_ENABLED, operator));
            changed.addAll(saveBool(search, "cacheEnabled", TuningConfigService.KEY_SEARCH_CACHE_ENABLED, operator));
            Object ttl = search.get("cacheTtlSeconds");
            if (ttl != null) {
                double v = toDouble(ttl, "cacheTtlSeconds");
                if (v < 5 || v > 600 || v != Math.floor(v)) {
                    throw new IllegalArgumentException("cacheTtlSeconds 取值 5–600 整数");
                }
                upsertActive(TuningConfigService.KEY_SEARCH_CACHE_TTL_SECONDS, numberStr(v), "number", "参数下发", operator);
                changed.add("search.cache_ttl_seconds");
            }
        }
        tuningConfigService.bumpVersion();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("changed", changed);
        return out;
    }

    private List<String> saveBool(JSONObject body, String field, String key, String operator) {
        Object v = body.get(field);
        if (v == null) {
            return List.of();
        }
        String s = String.valueOf(v);
        if (!"true".equalsIgnoreCase(s) && !"false".equalsIgnoreCase(s) && !"1".equals(s) && !"0".equals(s)) {
            throw new IllegalArgumentException(field + " 仅接受 true/false");
        }
        boolean value = "true".equalsIgnoreCase(s) || "1".equals(s);
        upsertActive(key, value ? "true" : "false", "bool", "参数下发", operator);
        return List.of(key);
    }

    // ================================ 兜底链观测 ================================

    /** 观测统计：总查询/缓存命中/各环节命中率+平均耗时/avg/p95/LLM 兜底占比 */
    public Map<String, Object> observeStats(int days) {
        int window = days <= 0 ? 7 : Math.min(days, 90);
        long since = System.currentTimeMillis() - window * 86_400_000L;
        // 取 props 原文 Java 聚合（JSON_TABLE 在 MySQL/MyBatis-Plus 双侧均不可用，见 mapper 注释）
        List<String> propsList = tuneMapper.selectTraceProps(since);
        long total = 0;
        long cached = 0;
        long llmCnt = 0;
        Map<String, long[]> stageAgg = new LinkedHashMap<>();   // stage → [cnt, hits, totalMs]
        List<Long> totalMsSamples = new ArrayList<>();
        for (String props : propsList) {
            total++;
            try {
                JSONObject p = com.alibaba.fastjson.JSON.parseObject(props);
                if (Boolean.TRUE.equals(p.getBoolean("cached"))) {
                    cached++;
                }
                Long totalMs = p.getLong("totalMs");
                if (totalMs != null) {
                    totalMsSamples.add(totalMs);
                }
                com.alibaba.fastjson.JSONArray stages = p.getJSONArray("stages");
                if (stages == null) {
                    continue;
                }
                boolean hasLlm = false;
                for (int i = 0; i < stages.size(); i++) {
                    JSONObject s = stages.getJSONObject(i);
                    if (s == null || s.getString("stage") == null) {
                        continue;
                    }
                    String stage = s.getString("stage");
                    if ("llm_pick".equals(stage)) {
                        hasLlm = true;
                    }
                    long[] agg = stageAgg.computeIfAbsent(stage, k -> new long[3]);
                    agg[0]++;
                    if (Boolean.TRUE.equals(s.getBoolean("hit"))) {
                        agg[1]++;
                    }
                    agg[2] += s.getLongValue("ms");
                }
                if (hasLlm) {
                    llmCnt++;
                }
            } catch (Exception ignore) {
                // 脏 props 跳过
            }
        }

        List<Map<String, Object>> stages = new ArrayList<>();
        for (Map.Entry<String, long[]> e : stageAgg.entrySet()) {
            long[] agg = e.getValue();
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("stage", e.getKey());
            v.put("cnt", agg[0]);
            v.put("hits", agg[1]);
            v.put("avgMs", agg[0] == 0 ? 0
                    : BigDecimal.valueOf((double) agg[2] / agg[0]).setScale(1, RoundingMode.HALF_UP).doubleValue());
            stages.add(v);
        }

        double avgMs = 0;
        double p95Ms = 0;
        if (!totalMsSamples.isEmpty()) {
            List<Long> sorted = totalMsSamples.stream().filter(java.util.Objects::nonNull).sorted().toList();
            if (!sorted.isEmpty()) {
                avgMs = BigDecimal.valueOf(sorted.stream().mapToLong(Long::longValue).average().orElse(0))
                        .setScale(1, RoundingMode.HALF_UP).doubleValue();
                p95Ms = sorted.get((int) Math.floor(0.95 * (sorted.size() - 1)));
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", window);
        out.put("totalQueries", total);
        out.put("cachedHits", cached);
        out.put("cacheHitRate", total == 0 ? 0 : BigDecimal.valueOf(cached * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue());
        out.put("avgTotalMs", avgMs);
        out.put("p95TotalMs", p95Ms);
        out.put("llmPickShare", total == 0 ? 0 : BigDecimal.valueOf(llmCnt * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue());
        out.put("stages", stages);
        return out;
    }

    /** 低命中样本列表（观测页）：source_type=2 最近 200 条 */
    public List<Map<String, Object>> observeMisses() {
        return tuneMapper.selectFeedbackPage(new PageArgsHolder(1, 200), 2, null, null, null, null);
    }

    // ================================ 样本池 ================================

    /** 样本池分页 */
    public Map<String, Object> feedbackList(long pageNo, long pageSize, Integer sourceType, Integer sampleStatus,
                                            String uid, String timeStart, String timeEnd) {
        PageArgsHolder page = new PageArgsHolder(pageNo, pageSize);
        List<Map<String, Object>> records = tuneMapper.selectFeedbackPage(page, sourceType, sampleStatus, uid, timeStart, timeEnd);
        Long total = tuneMapper.countFeedback(sourceType, sampleStatus, uid, timeStart, timeEnd);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", records);
        out.put("total", total == null ? 0 : total);
        return out;
    }

    /** 样本处置：alias=补词表 / ignore=忽略 / baseline=导出基准用例 JSON */
    public Map<String, Object> feedbackHandle(String id, String action, String standard, Integer aliasType, String operator) {
        Map<String, Object> row = tuneMapper.selectFeedbackById(id);
        if (row == null) {
            throw new IllegalArgumentException("样本不存在");
        }
        String refContent = row.get("ref_content") == null ? null : String.valueOf(row.get("ref_content"));
        Map<String, Object> out = new LinkedHashMap<>();
        if ("alias".equals(action)) {
            if (refContent == null || refContent.isBlank()) {
                throw new IllegalArgumentException("样本缺少查询词，无法补词表");
            }
            if (standard == null || standard.isBlank()) {
                throw new IllegalArgumentException("请填写标准词");
            }
            int type = aliasType == null ? 1 : aliasType;
            if (type != 1 && type != 2) {
                throw new IllegalArgumentException("aliasType 仅支持 1(同义词)/2(上下位)");
            }
            Long conflict = aliasMapper.countAliasConflict(refContent.trim(), standard.trim(), null);
            if (conflict != null && conflict > 0) {
                throw new IllegalArgumentException("词表已存在该别名+标准词组合");
            }
            aliasMapper.insertAliasFromSample(cn.hutool.core.util.IdUtil.getSnowflakeNextId(),
                    refContent.trim(), standard.trim(), type, operator);
            redisUtil.del("ail:alias:cache");
            tuneMapper.updateFeedbackStatus(id, 1);
            out.put("action", "alias");
        } else if ("ignore".equals(action)) {
            tuneMapper.updateFeedbackStatus(id, 2);
            out.put("action", "ignore");
        } else if ("baseline".equals(action)) {
            Map<String, Object> benchmarkCase = new LinkedHashMap<>();
            benchmarkCase.put("id", "OBS-" + id);
            benchmarkCase.put("group", "兜底");
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("mode", "normal");
            input.put("text", refContent);
            benchmarkCase.put("input", input);
            Map<String, Object> expect = new LinkedHashMap<>();
            expect.put("card", "search_items");
            expect.put("assert", "total>0（观测低命中固化 "
                    + (row.get("result_summary") == null ? "" : row.get("result_summary")) + "）");
            benchmarkCase.put("expect", expect);
            tuneMapper.updateFeedbackStatus(id, 1);
            out.put("action", "baseline");
            out.put("case", benchmarkCase);
            out.put("filename", "case-obs-" + id + ".json");
        } else {
            throw new IllegalArgumentException("action 仅支持 alias/ignore/baseline");
        }
        return out;
    }

    // ================================ 私有工具 ================================

    private String requireSkillKey(String skill) {
        if ("filing".equals(skill)) {
            return TuningConfigService.KEY_SKILL_FILING_PROMPT;
        }
        if ("query".equals(skill)) {
            return TuningConfigService.KEY_SKILL_QUERY_PROMPT;
        }
        throw new IllegalArgumentException("skill 仅支持 filing/query");
    }

    private boolean filing(String skill) {
        return "filing".equals(skill);
    }

    private TuningConfig activeRow(String key) {
        List<TuningConfig> rows = configMapper.selectList(new LambdaQueryWrapper<TuningConfig>()
                .eq(TuningConfig::getConfigKey, key)
                .eq(TuningConfig::getActive, 1));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean dbActive(String key) {
        TuningConfig row = activeRow(key);
        return row != null && row.getEnabled() != null && row.getEnabled() == 1;
    }

    /** 新版本写入：旧 active 翻 0 → 追加 version=max+1 的 active=1 enabled=1 行 → 版本键 INCR */
    private TuningConfig upsertActive(String key, String value, String valueType, String remark, String operator) {
        List<TuningConfig> actives = configMapper.selectList(new LambdaQueryWrapper<TuningConfig>()
                .eq(TuningConfig::getConfigKey, key)
                .eq(TuningConfig::getActive, 1));
        for (TuningConfig row : actives) {
            row.setActive(0);
            configMapper.updateById(row);
        }
        List<TuningConfig> latest = configMapper.selectList(new LambdaQueryWrapper<TuningConfig>()
                .eq(TuningConfig::getConfigKey, key)
                .orderByDesc(TuningConfig::getVersion)
                .last("LIMIT 1"));
        int nextVersion = (latest.isEmpty() || latest.get(0).getVersion() == null ? 0 : latest.get(0).getVersion()) + 1;
        TuningConfig row = new TuningConfig()
                .setConfigKey(key)
                .setConfigValue(value)
                .setValueType(valueType)
                .setVersion(nextVersion)
                .setEnabled(1)
                .setActive(1)
                .setRemark(remark)
                .setUpdateBy(operator)
                .setUpdateTime(new Date());
        configMapper.insert(row);
        tuningConfigService.bumpVersion();
        return row;
    }

    private Map<String, Object> savedView(TuningConfig saved) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", saved.getConfigKey());
        out.put("version", saved.getVersion());
        out.put("source", "db");
        return out;
    }

    private String readClasspathTemplate(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return "";
            }
            return IoUtil.read(in, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[tune] 内置模板读取失败 {}: {}", path, e.getMessage());
            return "";
        }
    }

    private double toDouble(Object v, String field) {
        try {
            return v instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            throw new IllegalArgumentException(field + " 数值无效");
        }
    }

    private String numberStr(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
