package org.jeecg.modules.app.aiport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.ai.port.ObservabilityPort;
import org.jeecg.modules.app.entity.AiFeedback;
import org.jeecg.modules.app.mapper.AiFeedbackMapper;
import org.jeecg.modules.app.service.StatsEventService;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ObservabilityPort 桥接实现：委托 StatsEventService 写 tbl_stats_event（不建观测表），
 * 并在 ai_search_trace 低命中（finalTotal=0 且有查询词）时自动入库样本池
 * （tbl_ai_feedback source_type=2，uid+ref_content 去重合并更新）。
 * fire-and-forget：任何异常吞掉，不影响主链路。
 */
@Slf4j
public class AiObservabilityPortImpl implements ObservabilityPort {

    /** 低命中样本来源类型（tbl_ai_feedback.source_type：2观测低命中） */
    private static final int SOURCE_TYPE_OBSERVE_MISS = 2;

    private final StatsEventService statsEventService;
    private final AiFeedbackMapper feedbackMapper;

    public AiObservabilityPortImpl(StatsEventService statsEventService, AiFeedbackMapper feedbackMapper) {
        this.statsEventService = statsEventService;
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    public void fire(String event, String uid, JSONObject data) {
        try {
            statsEventService.fire(event, uid, data);
        } catch (Exception ignore) {
            // 埋点失败静默：可观测数据不得阻断业务
        }
        if ("ai_search_trace".equals(event)) {
            recordZeroHitSample(uid, data);
        }
    }

    /**
     * 低命中样本自动入库（观测→样本池闭环）：finalTotal=0 且查询词非空时 upsert，
     * 同 uid+ref_content+source_type 已存在则合并更新 result_summary/create_time，不新增行
     */
    private void recordZeroHitSample(String uid, JSONObject data) {
        try {
            Integer finalTotal = data.getInteger("finalTotal");
            String queryText = data.getString("queryText");
            if (finalTotal == null || finalTotal != 0 || queryText == null || queryText.isBlank()) {
                return;
            }
            Date now = new Date();
            AiFeedback exist = feedbackMapper.selectOne(new LambdaQueryWrapper<AiFeedback>()
                    .eq(AiFeedback::getUid, uid)
                    .eq(AiFeedback::getSourceType, SOURCE_TYPE_OBSERVE_MISS)
                    .eq(AiFeedback::getRefContent, queryText)
                    .last("LIMIT 1"));
            if (exist != null) {
                AiFeedback update = new AiFeedback()
                        .setId(exist.getId())
                        .setResultSummary(buildMissSummary(data))
                        .setCreateTime(now);
                feedbackMapper.updateById(update);
            } else {
                AiFeedback row = new AiFeedback()
                        .setSourceType(SOURCE_TYPE_OBSERVE_MISS)
                        .setUid(uid)
                        .setRefType(1)
                        .setRefContent(queryText)
                        .setResultSummary(buildMissSummary(data))
                        .setSampleStatus(0)
                        .setCreateTime(now);
                feedbackMapper.insert(row);
            }
        } catch (Exception ignore) {
            // 样本入库失败静默：不得阻断 AI 链路
        }
    }

    /** 样本摘要：finalTotal=0; hitSource=…; stages=[…]; totalMs=…（截断至列宽 255 内） */
    private String buildMissSummary(JSONObject data) {
        StringBuilder sb = new StringBuilder("finalTotal=0");
        String hitSource = data.getString("hitSource");
        if (hitSource != null) {
            sb.append("; hitSource=").append(hitSource);
        }
        Object stages = data.get("stages");
        if (stages instanceof List<?> list && !list.isEmpty()) {
            String stageJson = JSON.toJSONString(list.stream()
                    .map(o -> o instanceof Map<?, ?> m ? m : o).toList());
            if (stageJson.length() > 120) {
                stageJson = stageJson.substring(0, 120);
            }
            sb.append("; stages=").append(stageJson);
        }
        Integer totalMs = data.getInteger("totalMs");
        if (totalMs != null) {
            sb.append("; totalMs=").append(totalMs);
        }
        if (sb.length() > 255) {
            sb.setLength(255);
        }
        return sb.toString();
    }
}
