-- 芥子开放平台应用凭据表
-- 第三方应用凭 AK/SK 调用 /app/open/** 数据接口，数据范围限定为 bound_uid 对应用户
CREATE TABLE IF NOT EXISTS `open_app_credentials` (
    `id`          varchar(36)  NOT NULL COMMENT '主键',
    `app_key`     varchar(64)  NOT NULL COMMENT '应用AppKey',
    `app_secret`  varchar(64)  NOT NULL COMMENT '应用密钥AppSecret',
    `app_name`    varchar(100) DEFAULT NULL COMMENT '应用名称',
    `bound_uid`   varchar(36)  DEFAULT NULL COMMENT '数据范围绑定的App用户ID(app_user.id)',
    `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
    `white_list`  varchar(500) DEFAULT NULL COMMENT 'IP白名单，逗号分隔，空为不限制',
    `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_open_app_key` (`app_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '芥子开放平台应用凭据';
