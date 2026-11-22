-- =====================================================================
-- 芥子开放平台 v3 升级脚本（B4 Webhook 事件订阅 · 2026-11-21）
-- 目标库：mustard_space
-- 内容：
--   1. open_app_credentials 增加 webhook_url / webhook_secret（每凭据一条回调订阅）
--   2. 新建 tbl_open_webhook_event 事件投递表（PENDING → DELIVERED / DEAD）
-- 投递策略：HMAC-SHA256 签名，指数退避重试 5 次（1s/2s/4s/8s/16s）后死信保留审计
-- =====================================================================

-- 1. 凭据表：Webhook 订阅配置（一个应用一个密钥，即一条订阅）
ALTER TABLE open_app_credentials
  ADD COLUMN webhook_url    VARCHAR(255) NULL COMMENT 'Webhook 回调地址（空为未订阅）',
  ADD COLUMN webhook_secret VARCHAR(64)  NULL COMMENT 'Webhook 签名密钥（HMAC-SHA256，配置时生成）';

-- =====================================================================
-- 2. Webhook 事件投递表
-- 账本 append 钩子命中订阅者时入队（与业务同事务），投递器异步 POST 回调
-- =====================================================================
CREATE TABLE tbl_open_webhook_event (
  id            BIGINT        NOT NULL COMMENT '雪花ID（接口字符串出参）',
  app_key       VARCHAR(64)   NOT NULL COMMENT '订阅方 AppKey',
  event         VARCHAR(32)   NOT NULL COMMENT '事件：item.changed / item.flowed',
  op_type       VARCHAR(24)   NOT NULL COMMENT '账本操作：mint/update/recycle/gift_out/...',
  ori_id        VARCHAR(64)   NOT NULL COMMENT '物品档案ID',
  payload       VARCHAR(2048) NULL COMMENT '投递报文（出域最小集，无 uid 与属性值）',
  status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DELIVERED/DEAD',
  retry_count   INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_at DATETIME      NULL COMMENT '下次投递时间',
  last_code     INT           NULL COMMENT '最近响应码',
  last_error    VARCHAR(255)  NULL COMMENT '最近失败摘要',
  delivered_at  DATETIME      NULL COMMENT '最终投递成功时间',
  create_time   DATETIME      NOT NULL COMMENT '事件时间',
  update_time   DATETIME      NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_wh_sweep (status, next_retry_at),
  KEY idx_wh_appkey_time (app_key, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开放平台 Webhook 投递事件';

-- 回滚脚本
-- ALTER TABLE open_app_credentials
--   DROP COLUMN webhook_url, DROP COLUMN webhook_secret;
-- DROP TABLE tbl_open_webhook_event;
