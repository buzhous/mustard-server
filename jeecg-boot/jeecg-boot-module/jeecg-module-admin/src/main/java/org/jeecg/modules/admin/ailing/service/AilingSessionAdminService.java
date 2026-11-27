package org.jeecg.modules.admin.ailing.service;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.admin.ailing.mapper.AilingItemAdminMapper;
import org.jeecg.modules.admin.ailing.mapper.AilingSessionAdminMapper;
import org.jeecg.modules.app.service.StatsEventService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 芥灵过程 · AI 会话可视化（P3-2）：会话列表 / 会话回放（读取即审计）/ 质量看板 / 审计查询。
 * 全部只读；审计经 StatsEventService 落 tbl_stats_event（fire 自吞异常，不影响主流程）。
 */
@Slf4j
@Service
public class AilingSessionAdminService {

    private final AilingSessionAdminMapper sessionMapper;
    private final StatsEventService statsEventService;

    public AilingSessionAdminService(AilingSessionAdminMapper sessionMapper,
                                     StatsEventService statsEventService) {
        this.sessionMapper = sessionMapper;
        this.statsEventService = statsEventService;
    }

    // ==================== 会话列表 ====================

    /**
     * 会话分页：每行附 messageCount/mode/lastConfidence（SQL 装配），LEFT JOIN username
     */
    public Map<String, Object> list(String sessionId, String uid, String username, String title,
                                    String status, String mode,
                                    Date createTimeStart, Date createTimeEnd,
                                    long pageNo, long pageSize) {
        Map<String, Object> out = new LinkedHashMap<>();
        AilingItemAdminMapper.PageArgs page = new AilingItemAdminMapper.PageArgs(pageNo, pageSize);
        List<Map<String, Object>> rows = sessionMapper.selectSessionPage(page, sessionId, uid, username,
                title, status, mode, createTimeStart, createTimeEnd);
        out.put("records", rows);
        out.put("pageNo", pageNo);
        out.put("pageSize", page.getSize());
        return out;
    }

    // ==================== 会话详情（回放） ====================

    /**
     * 会话详情回放：session 元信息 + messages（create_time ASC ≤200）。
     * 读取行为审计：ai_session_viewed {sessionId} 落 tbl_stats_event。
     */
    public Map<String, Object> detail(String sessionId, String viewer) {
        Map<String, Object> session = sessionMapper.selectSessionDetail(sessionId);
        if (session == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session", session);
        out.put("messages", sessionMapper.selectMessages(sessionId));
        // 审计留痕（fire 内部吞异常，不阻塞回放）
        JSONObject props = new JSONObject(true);
        props.put("sessionId", sessionId);
        props.put("viewer", viewer);
        statsEventService.fire("ai_session_viewed", viewer, props);
        return out;
    }

    // ==================== 质量看板 ====================

    /**
     * 质量看板：建档成功率 / express 直达率 / 平均置信度 / 追问轮次分布
     * + 会话量趋势 30d / 模式分布（补充图）
     */
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        Long total = sessionMapper.countSessions();
        Long createSessions = sessionMapper.countCreateSessions();
        Long receiptSessions = sessionMapper.countReceiptSessions();
        Long expressSessions = sessionMapper.countExpressSessions();
        Long expressSuccess = sessionMapper.countExpressSuccessSessions();
        Double avgConfidence = sessionMapper.avgConfidence();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalSessions", total);
        summary.put("createSessions", createSessions);
        summary.put("filedSessions", receiptSessions);
        summary.put("filedRate", rate(receiptSessions, createSessions));
        summary.put("expressSessions", expressSessions);
        summary.put("expressSuccessSessions", expressSuccess);
        summary.put("expressDirectRate", rate(expressSuccess, expressSessions));
        summary.put("avgConfidence", avgConfidence == null ? null
                : BigDecimal.valueOf(avgConfidence).setScale(1, RoundingMode.HALF_UP));
        out.put("summary", summary);

        // 追问轮次分布：clarify 条数分桶 0/1/2/3+（0 桶 = 会话总数 - 有追问会话数）
        List<Map<String, Object>> hist = sessionMapper.clarifyHist();
        long clarifiedSessions = 0;
        for (Map<String, Object> row : hist) {
            clarifiedSessions += ((Number) row.get("c")).longValue();
        }
        List<Map<String, Object>> askDist = new ArrayList<>();
        Map<String, Object> zero = new LinkedHashMap<>();
        zero.put("k", "0");
        zero.put("c", Math.max(0, (total == null ? 0 : total) - clarifiedSessions));
        askDist.add(zero);
        askDist.addAll(hist);
        out.put("askDist", askDist);

        Date thirtyDaysAgo = Date.from(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
        out.put("sessionTrend", sessionMapper.sessionTrend(thirtyDaysAgo));
        out.put("modeDist", sessionMapper.modeDist());
        return out;
    }

    /** 比率（%，1 位小数）；分母为 0 或空 → 0 */
    private BigDecimal rate(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    // ==================== 审计查询 ====================

    /** 会话回放查看审计（tbl_stats_event 最近 200 条） */
    public List<Map<String, Object>> auditList() {
        return sessionMapper.selectAuditList();
    }
}
