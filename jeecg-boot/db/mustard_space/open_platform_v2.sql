-- =====================================================================
-- 芥子开放平台 v2 升级脚本（MCP 服务与 OpenAPI 接口设计方案 v1.0 · 3.2/3.4）
-- 目标库：mustard_space
-- 内容：
--   1. open_app_credentials 增加 scope / daily_quota / scope_updated_at（E2 scope 分级）
--   2. 新建 tbl_open_audit_log 开放调用审计表（E3 审计日志）
-- =====================================================================

-- 1. scope 权限分级（E2）
ALTER TABLE open_app_credentials
  ADD COLUMN scope            VARCHAR(16) NOT NULL DEFAULT 'readonly' COMMENT '权限分级：readonly/readwrite',
  ADD COLUMN daily_quota      INT         NULL     COMMENT '日调用配额，NULL=默认值（默认 5000）',
  ADD COLUMN scope_updated_at DATETIME    NULL     COMMENT 'scope 最近变更时间';

-- =====================================================================
-- 2. 开放调用审计表（E3）
-- 异步落库、append-only 使用习惯；App 授权页"调用记录"视图数据源
-- =====================================================================
CREATE TABLE tbl_open_audit_log (
  id             BIGINT       NOT NULL COMMENT '雪花ID',
  app_key        VARCHAR(64)  NOT NULL COMMENT '调用方 AppKey',
  bound_uid      VARCHAR(64)  NOT NULL COMMENT '数据归属用户',
  channel        VARCHAR(8)   NOT NULL COMMENT '出口：rest / mcp',
  path           VARCHAR(128) NOT NULL COMMENT '接口路径或 MCP tool 名',
  action         VARCHAR(16)  NOT NULL COMMENT 'read / write',
  params_digest  VARCHAR(512) NULL COMMENT '入参摘要（截断，敏感值脱敏）',
  result_code    INT          NULL COMMENT 'HTTP/业务返回码',
  data_count     INT          NULL COMMENT '返回条数（list/search）',
  cost_ms        INT          NULL COMMENT '耗时毫秒',
  client_ip      VARCHAR(64)  NULL COMMENT '调用方IP',
  create_time    DATETIME     NOT NULL COMMENT '调用时间',
  PRIMARY KEY (id),
  KEY idx_aud_appkey_time (app_key, create_time),
  KEY idx_aud_uid_time (bound_uid, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放调用审计';

-- 回滚脚本
-- ALTER TABLE open_app_credentials
--   DROP COLUMN scope, DROP COLUMN daily_quota, DROP COLUMN scope_updated_at;
-- DROP TABLE tbl_open_audit_log;
