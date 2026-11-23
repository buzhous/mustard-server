-- =====================================================================
-- F1 轻量自研采集统计 · 初始化 SQL（设计方案 2.1 / T1）
-- 兼容 PostgreSQL 与 KingbaseES；MySQL 8 需将 BIGSERIAL 换为 BIGINT AUTO_INCREMENT
-- 依赖：无（独立新表，不触碰既有表）
-- =====================================================================

-- 1) 原始事件表（append-only，eventId 幂等，90 天滚动归档）
CREATE TABLE tbl_stats_event (
  id           BIGSERIAL PRIMARY KEY,
  event_id     VARCHAR(64)  NOT NULL,
  event        VARCHAR(64)  NOT NULL,
  uid          VARCHAR(64)  DEFAULT '',
  device_id    VARCHAR(64)  DEFAULT '',
  platform     VARCHAR(16)  DEFAULT '',
  app_version  VARCHAR(16)  DEFAULT '',
  props        TEXT,
  client_ts    BIGINT,
  server_ts    BIGINT,
  stats_date   DATE         NOT NULL,
  CONSTRAINT uk_stats_event UNIQUE (event_id)
);
CREATE INDEX idx_stats_event_date ON tbl_stats_event (stats_date, event);
CREATE INDEX idx_stats_event_uid   ON tbl_stats_event (uid, stats_date);
CREATE INDEX idx_stats_event_arch  ON tbl_stats_event (server_ts);

-- 2) 日汇总表（StatsAggregateJob 每日 02:00 先删后插；看板只读本表，永久保留）
CREATE TABLE tbl_stats_daily (
  id         BIGSERIAL PRIMARY KEY,
  stats_date DATE         NOT NULL,
  metric     VARCHAR(64)  NOT NULL,
  dimension  VARCHAR(64)  DEFAULT '',
  value      BIGINT       DEFAULT 0,
  CONSTRAINT uk_stats_daily UNIQUE (stats_date, metric, dimension)
);
CREATE INDEX idx_stats_daily_metric ON tbl_stats_daily (metric, stats_date);

-- 3) 回归对账 SQL（R-11）
-- SELECT COUNT(*) FROM tbl_stats_event WHERE stats_date = CURRENT_DATE - INTERVAL '1 day';
-- SELECT COALESCE(SUM(value),0) FROM tbl_stats_daily
--  WHERE stats_date = CURRENT_DATE - INTERVAL '1 day' AND metric LIKE 'event:%';
-- 注：event:app_launch 为去重设备数，与原始条数不等属预期口径差异；其余事件应相等。
