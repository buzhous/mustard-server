-- ----------------------------
-- 芥子空间开放平台表结构（MySQL 8.0）
-- 说明：本期为独立账本与独立鉴权体系，不修改 App 主业务表
-- ----------------------------

SET NAMES utf8mb4;

-- 应用
CREATE TABLE IF NOT EXISTS `tbl_open_app` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `app_id` varchar(40) NOT NULL COMMENT '应用公开 ID',
  `owner_user_id` varchar(64) NOT NULL COMMENT '开发者用户 ID',
  `name` varchar(64) NOT NULL COMMENT '应用名称',
  `description` varchar(500) DEFAULT NULL COMMENT '应用简介',
  `icon` varchar(500) DEFAULT NULL COMMENT '应用图标',
  `homepage_url` varchar(500) DEFAULT NULL COMMENT '应用主页',
  `redirect_uris` text COMMENT '回调地址 JSON 数组',
  `scopes` text COMMENT '应用 Scope JSON 数组',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_app_app_id` (`app_id`),
  KEY `idx_open_app_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台应用';

-- 密钥
CREATE TABLE IF NOT EXISTS `tbl_open_api_key` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `app_id` varchar(40) NOT NULL COMMENT '应用 ID',
  `name` varchar(64) NOT NULL COMMENT '密钥名称',
  `key_id` varchar(32) NOT NULL COMMENT '公开 Key ID',
  `secret_hash` varchar(64) NOT NULL COMMENT 'Secret SHA-256',
  `secret_prefix` varchar(16) DEFAULT NULL COMMENT 'Secret 前缀，仅展示',
  `scopes` text COMMENT '密钥 Scope JSON 数组',
  `ip_whitelist` varchar(500) DEFAULT NULL COMMENT 'IP 白名单，逗号分隔',
  `rate_limit_per_min` int NOT NULL DEFAULT '60' COMMENT '每分钟限流',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 禁用',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `last_used_at` datetime DEFAULT NULL COMMENT '最近使用时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_key_key_id` (`key_id`),
  KEY `idx_open_key_app` (`app_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台密钥';

-- 用户授权
CREATE TABLE IF NOT EXISTS `tbl_open_grant` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `app_id` varchar(40) NOT NULL COMMENT '应用 ID',
  `user_id` varchar(64) NOT NULL COMMENT '授权用户 ID',
  `scopes` text COMMENT '授权 Scope JSON 数组',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 有效 0 已撤销',
  `authorized_at` datetime DEFAULT NULL COMMENT '授权时间',
  `revoked_at` datetime DEFAULT NULL COMMENT '撤销时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_grant_app_user` (`app_id`, `user_id`),
  KEY `idx_open_grant_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台用户授权';

-- 令牌
CREATE TABLE IF NOT EXISTS `tbl_open_token` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `grant_id` varchar(32) NOT NULL COMMENT '授权 ID',
  `app_id` varchar(40) NOT NULL COMMENT '应用 ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID',
  `token_type` varchar(16) NOT NULL COMMENT 'ACCESS / REFRESH',
  `token_hash` varchar(64) NOT NULL COMMENT '令牌 SHA-256',
  `scopes` text COMMENT '令牌 Scope JSON 数组',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 有效 0 失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_token_hash` (`token_hash`),
  KEY `idx_open_token_grant` (`grant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台令牌';

-- 授权码
CREATE TABLE IF NOT EXISTS `tbl_open_auth_code` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `code_hash` varchar(64) NOT NULL COMMENT '授权码 SHA-256',
  `app_id` varchar(40) NOT NULL COMMENT '应用 ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID',
  `scopes` text COMMENT '授权 Scope JSON 数组',
  `redirect_uri` varchar(500) NOT NULL COMMENT '回调地址',
  `state` varchar(200) DEFAULT NULL COMMENT '状态参数',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `used` tinyint NOT NULL DEFAULT '0' COMMENT '是否已使用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_code_hash` (`code_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台授权码';

-- 积分账户
CREATE TABLE IF NOT EXISTS `tbl_open_point_account` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID',
  `balance` int NOT NULL DEFAULT '0' COMMENT '可用积分',
  `total_earned` int NOT NULL DEFAULT '0' COMMENT '累计获取',
  `total_spent` int NOT NULL DEFAULT '0' COMMENT '累计消耗',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_point_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放积分账户';

-- 积分流水
CREATE TABLE IF NOT EXISTS `tbl_open_point_record` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID',
  `app_id` varchar(40) DEFAULT NULL COMMENT '来源应用 ID',
  `amount` int NOT NULL COMMENT '变动数量',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `biz_type` varchar(32) NOT NULL COMMENT 'API_EARN / ADMIN_ADJUST',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '业务幂等键',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_point_biz` (`app_id`, `biz_id`),
  KEY `idx_open_point_user_time` (`user_id`, `create_time`),
  KEY `idx_open_point_app_time` (`app_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放积分流水';

-- 调用审计
CREATE TABLE IF NOT EXISTS `tbl_open_request_log` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `request_id` varchar(64) NOT NULL COMMENT '请求 ID',
  `app_id` varchar(40) DEFAULT NULL COMMENT '应用 ID',
  `key_id` varchar(32) DEFAULT NULL COMMENT '密钥 ID',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户 ID',
  `method` varchar(16) DEFAULT NULL COMMENT 'HTTP 方法',
  `path` varchar(500) DEFAULT NULL COMMENT '请求路径',
  `status_code` int DEFAULT NULL COMMENT 'HTTP 状态码',
  `error_code` varchar(32) DEFAULT NULL COMMENT '业务错误码',
  `ip` varchar(64) DEFAULT NULL COMMENT '客户端 IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_open_log_app_time` (`app_id`, `create_time`),
  KEY `idx_open_log_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台调用审计';
