package org.jeecg.modules.app.service;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.app.entity.StatsDaily;
import org.jeecg.modules.app.entity.StatsEvent;
import org.jeecg.modules.app.mapper.StatsDailyMapper;
import org.jeecg.modules.app.mapper.StatsEventMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * F1 运营统计 · 每日聚合与归档任务（设计方案 2.1 / T4）
 * <p>
 * - 聚合：每日 02:00 聚合 T-1 原始事件 → tbl_stats_daily（先删后插，幂等可重跑）；
 * - 归档：每日 02:30 将 server_ts 早于 90 天的原始事件转储 CSV 后删除（daily 不受影响）；
 * - 手动重跑：StatsController#aggregate 可指定日期修复对账。
 */
@Slf4j
@Service
public class StatsAggregateJob {

    private static final int ARCHIVE_BATCH = 2000;
    private static final long ARCHIVE_RETENTION_MS = 90L * 24 * 3600 * 1000;

    private final StatsEventMapper eventMapper;
    private final StatsDailyMapper dailyMapper;

    public StatsAggregateJob(StatsEventMapper eventMapper, StatsDailyMapper dailyMapper) {
        this.eventMapper = eventMapper;
        this.dailyMapper = dailyMapper;
    }

    /** 每日 02:00 聚合昨日 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void aggregateDaily() {
        aggregate(LocalDate.now().minusDays(1));
    }

    /** 每日 02:30 归档 90 天前原始事件 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void archiveDaily() {
        archive(System.currentTimeMillis() - ARCHIVE_RETENTION_MS);
    }

    /**
     * 聚合指定日（幂等：先删后插）。date 传 null 聚合昨日。
     */
    public void aggregate(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now().minusDays(1) : date;
        Date day = Date.from(target.atStartOfDay(ZoneId.systemDefault()).toInstant());
        try {
            dailyMapper.deleteByDate(day);
            upsert(day, "dau", "", nz(eventMapper.countDau(day)));

            Long newUsers = eventMapper.countNewUsers(day);
            upsert(day, "new_users", "", nz(newUsers));

            Long launchDevices = eventMapper.countLaunchDevices(day);
            upsert(day, "event:app_launch", "", nz(launchDevices));

            for (Map<String, Object> row : eventMapper.countGroupByEvent(day)) {
                String event = String.valueOf(row.get("event"));
                long cnt = row.get("cnt") == null ? 0 : Long.parseLong(String.valueOf(row.get("cnt")));
                upsert(day, "event:" + event, "", cnt);
            }

            // 注册来源分布（dimension = source 值）
            for (Map<String, Object> row : eventMapper.registerSourceOfDay(day)) {
                String source = String.valueOf(row.get("props"));
                long cnt = row.get("cnt") == null ? 0 : Long.parseLong(String.valueOf(row.get("cnt")));
                upsert(day, "register_source", source.isEmpty() ? "unknown" : source, cnt);
            }
            log.info("[stats] 聚合完成 date={}", target);
        } catch (Exception e) {
            log.error("[stats] 聚合失败 date={}: {}", target, e.getMessage(), e);
        }
    }

    /** 归档：CSV 转储后删除（分批） */
    public synchronized void archive(long beforeTs) {
        long total = eventMapper.countArchive(beforeTs);
        if (total == 0) {
            return;
        }
        File dir = new File("logs/stats-archive");
        FileUtil.mkParentDirs(dir);
        File csv = new File(dir, "stats_archive_" + beforeTs + ".csv");
        StringBuilder sb = new StringBuilder("event_id,event,uid,device_id,platform,app_version,props,client_ts,server_ts\n");
        int offset = 0;
        try {
            while (true) {
                List<StatsEvent> batch = eventMapper.selectArchiveBatch(beforeTs, ARCHIVE_BATCH, offset);
                if (batch.isEmpty()) {
                    break;
                }
                for (StatsEvent e : batch) {
                    sb.append(csvCell(e.getEventId())).append(',')
                            .append(csvCell(e.getEvent())).append(',')
                            .append(csvCell(e.getUid())).append(',')
                            .append(csvCell(e.getDeviceId())).append(',')
                            .append(csvCell(e.getPlatform())).append(',')
                            .append(csvCell(e.getAppVersion())).append(',')
                            .append(csvCell(e.getProps())).append(',')
                            .append(e.getClientTs()).append(',')
                            .append(e.getServerTs()).append('\n');
                }
                offset += batch.size();
                if (batch.size() < ARCHIVE_BATCH) {
                    break;
                }
            }
            FileUtil.writeString(sb.toString(), csv, StandardCharsets.UTF_8);
            eventMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StatsEvent>()
                    .lt(StatsEvent::getServerTs, beforeTs));
            log.info("[stats] 归档完成 {} 条 → {}", offset, csv.getAbsolutePath());
        } catch (Exception e) {
            log.error("[stats] 归档失败（原始数据保留，下次重试）: {}", e.getMessage(), e);
        }
    }

    private void upsert(Date day, String metric, String dimension, long value) {
        StatsDaily d = new StatsDaily().setStatsDate(day).setMetric(metric)
                .setDimension(dimension == null ? "" : dimension).setValue(value);
        dailyMapper.insert(d);
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }

    private static String csvCell(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
