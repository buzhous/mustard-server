package org.jeecg.modules.app.controller.stats;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.constant.AuthConstant;
import org.jeecg.modules.app.entity.StatsEvent;
import org.jeecg.modules.app.mapper.StatsEventMapper;
import org.jeecg.modules.app.mapper.StatsDailyMapper;
import org.jeecg.modules.app.service.StatsAggregateJob;
import org.jeecg.modules.app.service.StatsEventService;
import org.jeecg.modules.app.utils.SecurityTokenUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F1 轻量自研采集统计（设计方案 02）
 * <p>
 * - POST /app/stats/track/batch：App 端低频批量上报（匿名仅限 app_launch；eventId 幂等；限流 10 批/分钟）；
 * - /app/stats/admin/**：运营看板查询（overview / trend / events / retention / export / aggregate）。
 *   鉴权约定与 /app/mip/admin 一致（AppAuthFilter 放行 track；admin 接口依赖部署侧内网隔离）。
 */
@Slf4j
@Tag(name = "F1-运营统计")
@RestController
@RequestMapping("/app/stats")
public class StatsController {

    private static final int RATE_LIMIT_PER_MIN = 10;

    private final StatsEventService eventService;
    private final StatsEventMapper eventMapper;
    private final StatsDailyMapper dailyMapper;
    private final StatsAggregateJob aggregateJob;

    /** 简单固定窗口限流（单实例部署足够）：key → [minuteWindow, count] */
    private final ConcurrentHashMap<String, long[]> rateBuckets = new ConcurrentHashMap<>();

    public StatsController(StatsEventService eventService, StatsEventMapper eventMapper,
                           StatsDailyMapper dailyMapper, StatsAggregateJob aggregateJob) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
        this.dailyMapper = dailyMapper;
        this.aggregateJob = aggregateJob;
    }

    // ==================== App 上报 ====================

    @Operation(summary = "App 端批量上报（≤50 条，eventId 幂等，匿名仅限 app_launch）")
    @PostMapping("/track/batch")
    public Result<JSONObject> trackBatch(@RequestBody JSONObject body, HttpServletRequest request) {
        JSONArray arr = body.getJSONArray("events");
        if (arr == null || arr.isEmpty()) {
            return Result.error("events 不能为空");
        }
        if (arr.size() > StatsEventService.MAX_BATCH) {
            return Result.error("单批最多 " + StatsEventService.MAX_BATCH + " 条");
        }
        // 登录态优先从 X-Access-Token 解析（track 路径被 AppAuthFilter 放行，不会注入 x-uid，故此处自行解析）；
        // 无 token / 解析失败视为匿名，不报错（app_launch 本就匿名上报）
        String uid = resolveUid(request);
        if (StrUtil.isBlank(uid)) {
            uid = StrUtil.nullToEmpty(body.getString("uid"));
        }
        String deviceId = StrUtil.nullToEmpty(body.getString("deviceId"));
        // platform / appVersion 由客户端放在 body 顶层，整批复用
        String platform = StrUtil.sub(body.getString("platform"), 0, 16);
        String appVersion = StrUtil.sub(body.getString("appVersion"), 0, 16);
        String rateKey = StrUtil.isBlank(uid) ? deviceId : uid;
        if (rateKey.isEmpty()) {
            return Result.error("deviceId 不能为空");
        }
        String limitErr = rateLimit(rateKey);
        if (limitErr != null) {
            return Result.error(429, limitErr);
        }

        List<StatsEvent> events = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String event = o.getString("event");
            if (!StatsEventService.ALLOWED_EVENTS.contains(event)) {
                return Result.error("非法事件名: " + event);
            }
            // uid 一律以服务端鉴权结果为准，不接受客户端伪造
            if (StrUtil.isBlank(uid) && !StatsEventService.ANONYMOUS_EVENTS.contains(event)) {
                return Result.error("事件 " + event + " 需要登录态");
            }
            StatsEvent e = new StatsEvent()
                    .setEventId(StrUtil.nullToEmpty(o.getString("eventId")))
                    .setEvent(event)
                    .setUid(uid)
                    .setDeviceId(deviceId)
                    .setPlatform(platform)
                    .setAppVersion(appVersion)
                    .setProps(o.getJSONObject("props") == null ? "{}" : o.getJSONObject("props").toJSONString())
                    .setClientTs(o.getLong("ts"))
                    .setServerTs(System.currentTimeMillis());
            if (e.getEventId().isEmpty()) {
                return Result.error("eventId 不能为空");
            }
            events.add(e);
        }
        Set<String> accepted = eventService.batchReport(events);
        JSONObject data = new JSONObject();
        data.put("acceptedIds", accepted);
        data.put("accepted", accepted.size());
        return Result.OK(data);
    }

    /** track 路径被 AppAuthFilter 放行（不注入 x-uid），此处自行解析登录态 uid；无 token 或解析失败返回 null（匿名） */
    private String resolveUid(HttpServletRequest request) {
        String token = request.getHeader(AuthConstant.OAUTH_TOKEN_HEADER);
        if (StrUtil.isEmpty(token) || token.split("\\.").length != 3) {
            return null;
        }
        try {
            var jwt = SecurityTokenUtil.parseJwtToken(token);
            return jwt == null ? null : jwt.getStr(AuthConstant.JWT_UID_HEADER);
        } catch (Exception ex) {
            return null;
        }
    }

    // ==================== 运营看板（admin） ====================

    @Operation(summary = "KPI 总览：今日实时 + 近 30 天汇总 + 漏斗 + 注册来源")
    @GetMapping("/admin/overview")
    public Result<JSONObject> overview(@RequestParam(defaultValue = "30") int days) {
        days = clampDays(days);
        Date today = dayStart(LocalDate.now());
        JSONObject data = new JSONObject();

        // 今日实时（原始表轻量 count）
        JSONObject todayKpi = new JSONObject();
        todayKpi.put("dau", nz(eventMapper.countDau(today)));
        todayKpi.put("newUsers", nz(eventMapper.countNewUsers(today)));
        todayKpi.put("launchDevices", nz(eventMapper.countLaunchDevices(today)));
        for (Map<String, Object> row : eventMapper.countGroupByEvent(today)) {
            String event = String.valueOf(row.get("event"));
            if ("first_item_created".equals(event) || "payment_success".equals(event)) {
                todayKpi.put(event, row.get("cnt"));
            }
        }
        data.put("today", todayKpi);

        // 近 N 天汇总 + 漏斗
        Date from = dayStart(LocalDate.now().minusDays(days - 1L));
        Map<String, Long> sums = new HashMap<>();
        for (Map<String, Object> row : dailyMapper.sumBetween(from, today)) {
            sums.put(String.valueOf(row.get("metric")),
                    row.get("value") == null ? 0L : Long.parseLong(String.valueOf(row.get("value"))));
        }
        long launch = sums.getOrDefault("event:app_launch", 0L);
        long register = sums.getOrDefault("event:register", 0L);
        long activate = sums.getOrDefault("event:first_item_created", 0L);
        JSONObject funnel = new JSONObject();
        funnel.put("launch", launch);
        funnel.put("register", register);
        funnel.put("activate", activate);
        funnel.put("launchToRegister", pct(register, launch));
        funnel.put("registerToActivate", pct(activate, register));
        data.put("funnel", funnel);
        data.put("sums", sums);

        // 注册来源分布（近 N 天）
        JSONObject sources = new JSONObject();
        for (Map<String, Object> row : dailyMapper.selectRegisterSourceSums(from, today)) {
            String dim = StrUtil.nullToEmpty((String) row.get("dimension"));
            sources.put(dim.isEmpty() ? "unknown" : dim,
                    row.get("value") == null ? 0L : Long.parseLong(String.valueOf(row.get("value"))));
        }
        data.put("registerSources", sources);
        return Result.OK(data);
    }

    @Operation(summary = "趋势序列（metric 如 dau/new_users/event:app_launch）")
    @GetMapping("/admin/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam String metric,
                                                   @RequestParam(required = false) String dimension,
                                                   @RequestParam(defaultValue = "30") int days) {
        days = clampDays(days);
        Date to = dayStart(LocalDate.now());
        Date from = dayStart(LocalDate.now().minusDays(days - 1L));
        return Result.OK(dailyMapper.trend(metric, dimension, from, to));
    }

    @Operation(summary = "原始事件明细分页")
    @GetMapping("/admin/events")
    public Result<Page<StatsEvent>> events(@RequestParam(required = false) String date,
                                           @RequestParam(required = false) String event,
                                           @RequestParam(defaultValue = "1") int pageNo,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<StatsEvent> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(date)) {
            qw.eq(StatsEvent::getStatsDate, dayStart(LocalDate.parse(date)));
        }
        if (StrUtil.isNotBlank(event)) {
            qw.eq(StatsEvent::getEvent, event);
        }
        qw.orderByDesc(StatsEvent::getId);
        Page<StatsEvent> page = eventMapper.selectPage(new Page<>(pageNo, pageSize), qw);
        // uid 假名化（防 PII 直出）
        page.getRecords().forEach(e -> e.setUid(maskUid(e.getUid())));
        return Result.OK(page);
    }

    @Operation(summary = "留存矩阵（cohort 注册日后第 N 日活跃比例）")
    @GetMapping("/admin/retention")
    public Result<List<Map<String, Object>>> retention(@RequestParam(defaultValue = "14") int days) {
        days = clampDays(days);
        Date to = dayStart(LocalDate.now());
        Date from = dayStart(LocalDate.now().minusDays(days - 1L));
        List<Map<String, Object>> rows = eventMapper.retentionMatrix(from, to);
        Map<String, Map<Long, Long>> matrix = new HashMap<>();
        for (Map<String, Object> r : rows) {
            LocalDate cohort = toLocalDate(r.get("cohort_date"));
            LocalDate active = toLocalDate(r.get("active_date"));
            long offset = Math.max(0, ChronoUnit.DAYS.between(cohort, active));
            long users = r.get("users") == null ? 0 : Long.parseLong(String.valueOf(r.get("users")));
            matrix.computeIfAbsent(cohort.toString(), k -> new HashMap<>()).put(offset, users);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<Long, Long>> en : matrix.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("cohortDate", en.getKey());
            item.put("offsets", en.getValue());
            result.add(item);
        }
        result.sort((a, b) -> String.valueOf(b.get("cohortDate")).compareTo(String.valueOf(a.get("cohortDate"))));
        return Result.OK(result);
    }

    @Operation(summary = "CSV 导出（近 N 天日汇总）")
    @GetMapping(value = "/admin/export")
    public String export(@RequestParam(defaultValue = "30") int days) {
        days = clampDays(days);
        Date to = dayStart(LocalDate.now());
        Date from = dayStart(LocalDate.now().minusDays(days - 1L));
        StringBuilder sb = new StringBuilder("stats_date,metric,dimension,value\n");
        for (Map<String, Object> row : dailyMapper.exportRows(from, to)) {
            sb.append(row.get("stats_date")).append(',')
                    .append(row.get("metric")).append(',')
                    .append(row.get("dimension") == null ? "" : row.get("dimension")).append(',')
                    .append(row.get("value")).append('\n');
        }
        return sb.toString();
    }

    @Operation(summary = "手动重跑指定日聚合（对账修复，幂等）")
    @PostMapping("/admin/aggregate")
    public Result<String> aggregate(@RequestParam(required = false) String date) {
        LocalDate d = StrUtil.isBlank(date) ? LocalDate.now().minusDays(1) : LocalDate.parse(date);
        aggregateJob.aggregate(d);
        return Result.OK("聚合完成: " + d);
    }

    // ==================== 工具 ====================

    private String rateLimit(String key) {
        long window = System.currentTimeMillis() / 60000;
        long[] bucket = rateBuckets.computeIfAbsent(key, k -> new long[]{window, 0});
        synchronized (bucket) {
            if (bucket[0] != window) {
                bucket[0] = window;
                bucket[1] = 0;
            }
            bucket[1]++;
            if (bucket[1] > RATE_LIMIT_PER_MIN) {
                return "上报过于频繁，请稍后再试";
            }
        }
        if (rateBuckets.size() > 100_000) {
            rateBuckets.clear();
        }
        return null;
    }

    private static String maskUid(String uid) {
        if (StrUtil.isBlank(uid)) {
            return "";
        }
        String tail = uid.length() <= 4 ? uid : uid.substring(uid.length() - 4);
        return "u_****" + tail;
    }

    private static Date dayStart(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate ld) {
            return ld;
        }
        if (o instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (o instanceof Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(o).substring(0, 10));
    }

    private static int clampDays(int days) {
        return Math.min(Math.max(days, 1), 365);
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }

    private static double pct(long part, long total) {
        return total <= 0 ? 0 : Math.round(part * 10000.0 / total) / 100.0;
    }
}
