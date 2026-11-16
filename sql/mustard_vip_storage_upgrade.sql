-- =============================================================
-- VIP 会员 与 用户仓库 升级脚本（MySQL）
-- 1) 用户表新增 VIP 到期时间
-- 2) VIP 购买订单表
-- 3) 用户仓库表（真实个人仓；系统虚拟仓不落库，列表兜底返回 id=-1）
-- =============================================================

-- 1. 用户表新增 VIP 到期时间
ALTER TABLE `tbl_user`
    ADD COLUMN `vip_expire_time` datetime NULL COMMENT 'VIP到期时间';

-- 2. VIP 购买订单表
CREATE TABLE IF NOT EXISTS `tbl_vip_order` (
    `id`          varchar(32)    NOT NULL COMMENT '主键Id',
    `order_no`    varchar(64)    NOT NULL COMMENT '订单号',
    `user_id`     varchar(32)    NOT NULL COMMENT '用户Id',
    `amount`      decimal(10, 2) NOT NULL DEFAULT 99.00 COMMENT '支付金额(元)',
    `years`       int            NOT NULL DEFAULT 1 COMMENT '购买年数',
    `pay_channel` varchar(16)    NULL COMMENT '支付渠道:WECHAT|ALIPAY',
    `pay_status`  varchar(16)    NULL COMMENT '支付状态:PAID',
    `mock`        tinyint(1)     NOT NULL DEFAULT 1 COMMENT '是否模拟支付:0否,1是',
    `create_time` datetime       NULL COMMENT '创建时间',
    `pay_time`    datetime       NULL COMMENT '支付时间',
    PRIMARY KEY (`id`),
    KEY `idx_vip_order_user_id` (`user_id`),
    KEY `idx_vip_order_order_no` (`order_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'VIP购买订单表';

-- 3. 用户仓库表
CREATE TABLE IF NOT EXISTS `tbl_user_storage` (
    `id`           varchar(32)  NOT NULL COMMENT '主键Id',
    `user_id`      varchar(32)  NOT NULL COMMENT '用户Id',
    `name`         varchar(64)  NOT NULL COMMENT '仓库名称',
    `storage_type` varchar(16)  NOT NULL DEFAULT 'REAL' COMMENT '仓库类型:VIRTUAL|REAL',
    `is_default`   tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否默认仓:0否,1是',
    `deleted`      tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
    `create_time`  datetime     NULL COMMENT '创建时间',
    `update_time`  datetime     NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_storage_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户仓库表';

-- 说明：
-- 物品归属仓库沿用 tbl_item_info.station_id / station_type：
--   REAL 仓: station_id = tbl_user_storage.id, station_type = 2
--   虚拟仓: station_id = tbl_user_virtual_station.id, station_type = 1
