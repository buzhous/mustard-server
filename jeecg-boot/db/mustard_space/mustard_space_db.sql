/*
 Navicat Premium Dump SQL

 Source Server         : sf-prod-new
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46-0ubuntu0.22.04.4)
 Source Host           : 8.134.112.180:3306
 Source Schema         : buzhous

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46-0ubuntu0.22.04.4)
 File Encoding         : 65001

 Date: 09/10/2026 16:47:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tbl_category
-- ----------------------------
DROP TABLE IF EXISTS `tbl_category`;
CREATE TABLE `tbl_category` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父级ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '分类编码',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类图标',
  `type` int DEFAULT '0' COMMENT '分类类型',
  `desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类描述',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '详情介绍',
  `is_sys` tinyint NOT NULL DEFAULT '0' COMMENT '系统分类',
  `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物品分类';

-- ----------------------------
-- Records of tbl_category
-- ----------------------------
BEGIN;
INSERT INTO `tbl_category` (`id`, `create_time`, `update_time`, `pid`, `name`, `code`, `icon`, `type`, `desc`, `sort`, `detail`, `is_sys`, `is_del`) VALUES (1908085489030721523, '2025-04-03 10:21:45', '2026-04-29 10:32:51', 0, '道具', 'C03A03', 'icon-doc', NULL, NULL, 3, NULL, 1, 0);
INSERT INTO `tbl_category` (`id`, `create_time`, `update_time`, `pid`, `name`, `code`, `icon`, `type`, `desc`, `sort`, `detail`, `is_sys`, `is_del`) VALUES (1908085489030721524, '2025-04-03 11:42:57', '2026-04-29 10:32:51', 0, '文档', 'C03A01', 'icon-prop', NULL, NULL, 1, NULL, 1, 0);
INSERT INTO `tbl_category` (`id`, `create_time`, `update_time`, `pid`, `name`, `code`, `icon`, `type`, `desc`, `sort`, `detail`, `is_sys`, `is_del`) VALUES (1908085489030721525, '2025-04-04 16:24:46', '2026-04-29 10:32:51', 0, '证件', 'C03A04', 'icon-credential', NULL, NULL, 4, NULL, 1, 0);
INSERT INTO `tbl_category` (`id`, `create_time`, `update_time`, `pid`, `name`, `code`, `icon`, `type`, `desc`, `sort`, `detail`, `is_sys`, `is_del`) VALUES (1908085489030721526, '2025-04-03 10:16:51', '2026-04-29 10:32:51', 0, '卡片', 'C03A02', 'icon-card', NULL, NULL, 2, NULL, 1, 0);
COMMIT;

-- ----------------------------
-- Table structure for tbl_coupon
-- ----------------------------
DROP TABLE IF EXISTS `tbl_coupon`;
CREATE TABLE `tbl_coupon` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `coupon_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '优惠券名称',
  `coupon_type` int DEFAULT NULL COMMENT '优惠券类型: 1-满减券, 2-折扣券, 3-兑换券',
  `discount_value` decimal(10,2) DEFAULT NULL COMMENT '折扣金额或折扣率',
  `min_amount` decimal(10,2) DEFAULT NULL COMMENT '最低消费金额',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '优惠券描述',
  `valid_start_time` datetime DEFAULT NULL COMMENT '有效期开始时间',
  `valid_end_time` datetime DEFAULT NULL COMMENT '有效期结束时间',
  `total_count` int DEFAULT NULL COMMENT '发放总数量',
  `remain_count` int DEFAULT NULL COMMENT '剩余数量',
  `used_count` int DEFAULT NULL COMMENT '已使用数量',
  `per_limit` int DEFAULT NULL COMMENT '每人限领数量',
  `daily_limit` int DEFAULT NULL COMMENT '每日限领数量',
  `status` int DEFAULT '0' COMMENT '状态: 0-未发布, 1-已发布, 2-已下架',
  `sort` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- ----------------------------
-- Records of tbl_coupon
-- ----------------------------
BEGIN;
INSERT INTO `tbl_coupon` (`id`, `create_time`, `update_time`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `description`, `valid_start_time`, `valid_end_time`, `total_count`, `remain_count`, `used_count`, `per_limit`, `daily_limit`, `status`, `sort`, `remark`) VALUES (1, '2026-06-01 22:06:09', '2026-06-01 22:06:09', '新人专享满减券', 1, 10.00, 50.00, '新人专享，满50减10', '2026-06-01 22:06:09', '2026-07-01 22:06:09', 1000, 1000, 0, 1, -1, 1, 1, '测试优惠券');
INSERT INTO `tbl_coupon` (`id`, `create_time`, `update_time`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `description`, `valid_start_time`, `valid_end_time`, `total_count`, `remain_count`, `used_count`, `per_limit`, `daily_limit`, `status`, `sort`, `remark`) VALUES (2, '2026-06-01 22:06:09', '2026-06-01 22:06:09', '95折折扣券', 2, 0.95, 100.00, '满100元可享95折', '2026-06-01 22:06:09', '2026-06-16 22:06:09', 500, 500, 0, 2, -1, 1, 2, '测试折扣券');
INSERT INTO `tbl_coupon` (`id`, `create_time`, `update_time`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `description`, `valid_start_time`, `valid_end_time`, `total_count`, `remain_count`, `used_count`, `per_limit`, `daily_limit`, `status`, `sort`, `remark`) VALUES (3, '2026-06-01 22:06:14', '2026-06-01 22:44:35', '殷墟使用券', 3, 0.00, 0.00, '殷墟使用券殷墟使用券殷墟使用券殷墟使用券殷墟使用券殷墟使用券。', '2026-06-01 00:00:00', '2030-06-01 00:00:00', 999, 995, 0, 999, 1, 1, 0, 'tempor sunt in aliqua');
COMMIT;

-- ----------------------------
-- Table structure for tbl_factory_info
-- ----------------------------
DROP TABLE IF EXISTS `tbl_factory_info`;
CREATE TABLE `tbl_factory_info` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `factory_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工坊名称',
  `factory_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工坊编码',
  `factory_icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '工坊图标',
  `factory_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '工坊描述',
  `factory_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工坊详情介绍',
  `factory_owner_id` bigint DEFAULT NULL COMMENT '工坊拥有者',
  `factory_status` int DEFAULT NULL COMMENT '工坊状态',
  `check_time` datetime DEFAULT NULL COMMENT '审核时间',
  `check_id` bigint DEFAULT NULL COMMENT '审核人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工坊信息';

-- ----------------------------
-- Records of tbl_factory_info
-- ----------------------------
BEGIN;
INSERT INTO `tbl_factory_info` (`id`, `create_time`, `update_time`, `factory_name`, `factory_code`, `factory_icon`, `factory_desc`, `factory_text`, `factory_owner_id`, `factory_status`, `check_time`, `check_id`) VALUES (1908085489030721520, '2025-04-04 15:28:10', '2025-04-04 17:13:52', '系统工坊', '1908059000059244544', '0', NULL, NULL, NULL, NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_gift
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_gift`;
CREATE TABLE `tbl_item_gift` (
  `id` varchar(64) NOT NULL COMMENT 'ID',
  `gift_type` int DEFAULT NULL COMMENT '赠送类型：1-指定用户赠送，2-公开赠送',
  `status` int DEFAULT NULL COMMENT '赠送状态：0-待领取，1-部分领取，2-已领完，3-已撤回，4-已过期',
  `sender_user_id` varchar(64) DEFAULT NULL COMMENT '赠送者用户ID',
  `receiver_user_id` varchar(64) DEFAULT NULL COMMENT '接收者用户ID',
  `item_id` varchar(64) DEFAULT NULL COMMENT '物品ID',
  `ori_id` varchar(64) DEFAULT NULL COMMENT '物品原始ID',
  `inventory_id` varchar(64) DEFAULT NULL COMMENT '库存ID',
  `total_quantity` int DEFAULT NULL COMMENT '赠送总数量',
  `received_quantity` int DEFAULT NULL COMMENT '已领取数量',
  `gift_code` varchar(32) DEFAULT NULL COMMENT '兑换码',
  `qr_code` text COMMENT '二维码内容',
  `valid_start_time` datetime DEFAULT NULL COMMENT '领取有效期开始时间',
  `valid_end_time` datetime DEFAULT NULL COMMENT '领取有效期结束时间',
  `per_limit` int DEFAULT NULL COMMENT '每人限领数量',
  `remark` varchar(500) DEFAULT NULL COMMENT '赠送备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '撤回时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender_user_id` (`sender_user_id`),
  KEY `idx_receiver_user_id` (`receiver_user_id`),
  KEY `idx_gift_code` (`gift_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物品赠送记录';

-- ----------------------------
-- Records of tbl_item_gift
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062461572371668994', 1, 2, '1872249865963053057', '1950497348299567106', '2062434781934362625', '111', NULL, 27, 27, NULL, NULL, NULL, NULL, 1, 'sint veniam magna', '2026-06-04 17:08:52', '2026-06-04 17:33:22', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062468251133878273', 1, 2, '1872249865963053057', '1950497348299567106', '2062434781934362625', '111', NULL, 10, 10, NULL, NULL, '2026-06-04 17:35:25', '2029-02-27 17:35:25', NULL, NULL, '2026-06-04 17:35:25', '2026-06-04 17:35:56', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062471471075893249', 1, 2, '1872249865963053057', '1950497348299567106', '2062434781934362625', '111', NULL, 1, 1, NULL, NULL, '2026-06-04 17:48:12', '2029-02-27 17:48:12', NULL, NULL, '2026-06-04 17:48:12', '2026-06-04 17:48:40', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062475552507027458', 1, 2, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', NULL, 1, 1, NULL, NULL, '2026-06-04 18:04:25', '2029-02-27 18:04:25', NULL, NULL, '2026-06-04 18:04:25', '2026-06-04 18:04:32', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062475972268777474', 1, 2, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', NULL, 10, 10, NULL, NULL, '2026-06-04 18:06:05', '2029-02-27 18:06:05', NULL, NULL, '2026-06-04 18:06:05', '2026-06-04 18:06:26', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062477130056065026', 1, 0, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', NULL, 10, 0, NULL, NULL, '2026-06-04 18:10:41', '2029-02-27 18:10:41', NULL, NULL, '2026-06-04 18:10:41', '2026-06-04 18:10:41', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062485224899207169', 1, 0, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', NULL, 10, 0, NULL, NULL, '2026-06-04 18:42:51', '2029-02-27 18:42:51', NULL, NULL, '2026-06-04 18:42:51', '2026-06-04 18:42:51', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062485545104900098', 1, 0, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 0, NULL, NULL, '2026-06-04 18:44:08', '2029-02-27 18:44:08', NULL, NULL, '2026-06-04 18:44:08', '2026-06-04 18:44:08', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062485599391776770', 1, 0, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 0, NULL, NULL, '2026-06-04 18:44:21', '2029-02-27 18:44:21', NULL, NULL, '2026-06-04 18:44:21', '2026-06-04 18:44:21', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062485790572285953', 1, 2, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 10, NULL, NULL, '2026-06-04 18:45:06', '2029-02-27 18:45:06', NULL, NULL, '2026-06-04 18:45:06', '2026-06-04 18:45:27', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062486352629993474', 1, 3, '1872249865963053057', '1950497348299567106', '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 0, NULL, NULL, '2026-06-04 18:47:20', '2029-02-27 18:47:20', NULL, NULL, '2026-06-04 18:47:20', '2026-06-04 18:47:30', '2026-06-04 18:47:30');
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062549957562372097', 2, 2, '1872249865963053057', NULL, '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 10, 'UWD8W9YO', 'QR_CODE_DATA:UWD8W9YO', '2026-06-04 23:00:05', NULL, 1, NULL, '2026-06-04 23:00:05', '2026-06-04 23:03:52', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062551037545336833', 2, 1, '1872249865963053057', NULL, '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 1, 'QDIHWI2Z', 'QR_GIFT_CODE:QDIHWI2Z', '2026-06-04 23:04:22', NULL, 1, NULL, '2026-06-04 23:04:22', '2026-06-04 23:04:43', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062551364919152641', 2, 2, '1872249865963053057', NULL, '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 10, 'ZVEI7LFZ', 'QR_GIFT_CODE:ZVEI7LFZ', '2026-06-04 23:05:40', NULL, 1, NULL, '2026-06-04 23:05:40', '2026-06-04 23:05:57', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2062552194103681026', 2, 3, '1872249865963053057', NULL, '2062471932583550978', '2062434781934362625', '2062471932793266178', 10, 1, 'CMXAEOZO', 'QR_GIFT_CODE:CMXAEOZO', '2026-06-04 23:08:58', NULL, 1, NULL, '2026-06-04 23:08:58', '2026-06-04 23:11:13', '2026-06-04 23:11:13');
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2064910805857943553', 1, 0, '2048739717880901634', '2048739717880901639', '2064246952887218177', '322636753410035712', '2064246952966909954', 5, 0, NULL, NULL, '2026-06-11 11:21:15', '2029-03-06 11:21:15', NULL, '这是测试赠送', '2026-06-11 11:21:15', '2026-06-11 11:21:15', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2064978384626618369', 2, 0, '2048739717880901634', NULL, '2064633398768910337', '323023209940688896', '2064633398869573634', 2, 0, '6CSZAMLO', 'QR_GIFT_CODE:6CSZAMLO', '2026-06-11 15:49:47', '2026-09-04 00:00:00', 2, '测试赠送', '2026-06-11 15:49:47', '2026-06-11 15:49:47', NULL);
INSERT INTO `tbl_item_gift` (`id`, `gift_type`, `status`, `sender_user_id`, `receiver_user_id`, `item_id`, `ori_id`, `inventory_id`, `total_quantity`, `received_quantity`, `gift_code`, `qr_code`, `valid_start_time`, `valid_end_time`, `per_limit`, `remark`, `create_time`, `update_time`, `cancel_time`) VALUES ('2066682119962726401', 2, 0, '1872249865963053057', NULL, '2063976349802303489', '2062434781934362610', '2063976349894578177', 1, 0, 'DG88HQKI', 'QR_GIFT_CODE:DG88HQKI', '2026-06-16 08:39:49', '2026-06-16 00:00:00', 1, '1111', '2026-06-16 08:39:49', '2026-06-16 08:39:49', NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_gift_receive
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_gift_receive`;
CREATE TABLE `tbl_item_gift_receive` (
  `id` varchar(64) NOT NULL COMMENT 'ID',
  `gift_id` varchar(64) DEFAULT NULL COMMENT '赠送记录ID',
  `user_id` varchar(64) DEFAULT NULL COMMENT '领取者用户ID',
  `quantity` int DEFAULT NULL COMMENT '领取数量',
  `receive_time` datetime DEFAULT NULL COMMENT '领取时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_gift_id` (`gift_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物品赠送领取记录';

-- ----------------------------
-- Records of tbl_item_gift_receive
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062467737570717698', '2062461572371668994', '1950497348299567106', 27, '2026-06-04 17:33:22', '2026-06-04 17:33:22');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062468382440759297', '2062468251133878273', '1950497348299567106', 10, '2026-06-04 17:35:56', '2026-06-04 17:35:56');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062471587274891266', '2062471471075893249', '1950497348299567106', 1, '2026-06-04 17:48:40', '2026-06-04 17:48:40');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062475581720354818', '2062475552507027458', '1950497348299567106', 1, '2026-06-04 18:04:32', '2026-06-04 18:04:32');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062476058784686081', '2062475972268777474', '1950497348299567106', 10, '2026-06-04 18:06:26', '2026-06-04 18:06:26');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062485875771183106', '2062485790572285953', '1950497348299567106', 10, '2026-06-04 18:45:27', '2026-06-04 18:45:27');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062550911170957313', '2062549957562372097', '1950497348299567106', 10, '2026-06-04 23:03:52', '2026-06-04 23:03:52');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062551127781593090', '2062551037545336833', '1950497348299567106', 1, '2026-06-04 23:04:44', '2026-06-04 23:04:44');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062551436683694081', '2062551364919152641', '1950497348299567106', 10, '2026-06-04 23:05:58', '2026-06-04 23:05:58');
INSERT INTO `tbl_item_gift_receive` (`id`, `gift_id`, `user_id`, `quantity`, `receive_time`, `create_time`) VALUES ('2062552342108086274', '2062552194103681026', '1950497348299567106', 1, '2026-06-04 23:09:33', '2026-06-04 23:09:33');
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_info
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_info`;
CREATE TABLE `tbl_item_info` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户Id',
  `ori_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始Id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题名称',
  `icon` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题类型',
  `quantity` int unsigned NOT NULL DEFAULT '0' COMMENT '数量',
  `level` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '主题描述',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类Id',
  `price` decimal(12,2) unsigned NOT NULL DEFAULT '0.00' COMMENT '价格',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态',
  `attributes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '属性列表',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '标签列表',
  `fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组件列表',
  `extend_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '拓展数据',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物品同步表';

-- ----------------------------
-- Records of tbl_item_info
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2037206201555791874, '2026-03-27 00:33:03', '2026-03-27 00:33:02', '1872249865963053057', '86', NULL, NULL, 0, 0, '', 0.00, '0', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2039720043830321154, '2026-04-02 23:02:09', '2026-04-03 00:38:27', '1872249865963053057', '66', '雀国香', 'https://avatars.githubusercontent.com/u/10378468', 18, 1, '', 359.25, '话状委当。三广何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 0, '[]', '[]', '[{\"content\":{\"name\":\"121212\"},\"element\":\"mollit\",\"sort\":11}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2049058614644916225, '2026-04-28 17:30:18', '2026-04-28 17:30:18', '1872249865963053057', '1', NULL, NULL, 0, 0, '0', 0.00, '0', 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2049058665228222465, '2026-04-28 18:34:19', '2026-04-28 18:34:19', '1872249865963053057', '2', '雀国香1', 'https://avatars.githubusercontent.com/u/10378468', 12, 1, '0', 359.25, '话状委当。三广何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 1, '[]', '[]', '[{\"content\":{\"name\":\"121212\"},\"element\":1,\"sort\":11}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2049074766028787713, '2026-04-28 18:34:38', '2026-04-28 18:34:38', '1872249865963053057', '3', '雀国香1', 'https://avatars.githubusercontent.com/u/10378468', 12, 1, '0', 359.25, '话状委当。三广何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 1, '[]', '[]', '[{\"content\":{\"name\":\"121212\"},\"element\":1,\"sort\":11}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2049075088088420354, '2026-04-28 18:41:01', '2026-04-28 18:41:01', '1872249865963053057', '4', '雀国香1', 'https://avatars.githubusercontent.com/u/10378468', 12, 1, '0', 359.25, '话状委当。三广何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 1, '[]', '[]', NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062434409098485762, '2026-06-04 15:20:56', '2026-06-04 15:20:56', '1872249865963053057', '11', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062434781934362625, '2026-06-04 17:01:47', '2026-06-04 17:01:47', '1872249865963053057', '111', '测试物品12', 'https://avatars.githubusercontent.com/u/10378468', 121, 1, '0', 359.25, '何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 1, '[]', '[]', NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062471932583550978, '2026-06-08 19:56:08', '2026-06-08 19:56:08', '1872249865963053057', '2062434781934362625', '测试物品121', 'https://avatars.githubusercontent.com/u/10378468', 100, 1, '0', 359.25, '何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 1, '[]', '[]', '[{\"content\":{\"name\":\"121212\"},\"element\":1,\"sort\":11},{\"content\":{\"name\":\"121212\"},\"element\":2,\"sort\":11}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062819379108098050, '2026-06-05 16:50:40', '2026-06-05 16:50:40', '2048739717880901634', '321209191802327040', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062825492159377409, '2026-06-05 17:14:57', '2026-06-05 17:14:57', '2048739717880901634', '321215284874694656', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062829744604819458, '2026-06-05 17:31:51', '2026-06-05 17:31:51', '2048739717880901634', '321219549491724288', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062831222727880706, '2026-06-05 17:37:44', '2026-06-05 17:37:44', '2048739717880901634', '321221034929045504', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062838600558862338, '2026-06-05 18:07:03', '2026-06-05 18:07:03', '2048739717880901634', '321228412922953728', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062839884171075585, '2026-06-05 18:12:09', '2026-06-05 18:12:09', '2048739717880901634', '321229696597573632', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2062840981048680449, '2026-06-05 18:16:30', '2026-06-05 18:16:30', '2048739717880901634', '321230793479372800', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063865043824926722, '2026-06-08 14:05:46', '2026-06-08 14:05:46', '2048739717880901634', '322254332389486592', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063868986709762050, '2026-06-08 14:21:26', '2026-06-08 14:21:26', '2048739717880901634', '322258797136064512', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063871891500163073, '2026-06-08 14:32:58', '2026-06-08 14:32:58', '2048739717880901634', '322254332393680896', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063874279564894210, '2026-06-08 14:42:28', '2026-06-08 14:42:28', '2048739717880901634', '322264090777718784', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063877091522764802, '2026-06-08 14:53:38', '2026-06-08 14:53:38', '2048739717880901634', '322266904002289664', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063882079523299329, '2026-06-08 15:13:27', '2026-06-08 15:13:27', '2048739717880901634', '322271892142882816', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063883391317041154, '2026-06-08 15:18:40', '2026-06-08 15:18:40', '2048739717880901634', '322273202443780096', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063884940256731137, '2026-06-08 15:24:50', '2026-06-08 15:24:50', '2048739717880901634', '322274752439959552', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063887765489573889, '2026-06-08 15:36:03', '2026-06-08 15:36:03', '2048739717880901634', '322277499522871296', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063892434945302529, '2026-06-08 15:54:36', '2026-06-08 15:54:36', '2048739717880901634', '322254332385292288', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063894117733621761, '2026-06-08 16:01:18', '2026-06-08 16:01:18', '2048739717880901634', '322283918349402112', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063913957571682305, '2026-06-08 17:20:08', '2026-06-08 17:20:08', '2048739717880901634', '322303739501715456', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063922280169725953, '2026-06-08 17:53:12', '2026-06-08 17:53:12', '2048739717880901634', '322312078224801792', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063926546758361089, '2026-06-08 18:10:10', '2026-06-08 18:10:10', '2048739717880901634', '322316328453906432', '西游记', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780912247285_9c3362b5_1780913408298.jpg', 11, 3, '0', 108.00, '四大名著之西游记', 0, 1, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063928603875729409, '2026-06-08 18:18:20', '2026-06-08 18:18:20', '2048739717880901634', '322318408418824192', '水浒传', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780913839434_49abac3c_1780913898751.jpg', 20, 1, '0', 199.99, '长篇小说', 0, 1, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063953293465870338, '2026-06-08 21:26:24', '2026-06-08 21:26:24', '1872249865963053057', '2062434781934362626', '揭伟', 'https://avatars.githubusercontent.com/u/47542598', 1, 2, '0', 751.89, '部且现后。群省需维示要。包些了工。一须门况使和经史手。', 0, 1, '[]', '[]', '[{\"content\":{\"name\":\"121212\"},\"element\":1,\"sort\":11},{\"content\":{\"name\":\"121212\"},\"element\":2,\"sort\":11}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063960118584139777, '2026-06-08 20:23:57', '2026-06-08 20:23:57', '1872249865963053057', '2062434781934362621', '测试物品121', 'https://avatars.githubusercontent.com/u/10378468', 100, 1, '0', 359.25, '何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, 1, '[]', '[]', '[{\"content\":{\"name\":\"121212\"},\"element\":1,\"sort\":11},{\"content\":{\"name\":\"121212\"},\"element\":2,\"sort\":11}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063976045010620417, '2026-06-08 21:27:00', '2026-06-08 21:27:00', '1872249865963053057', '2062434781934362620', '揭伟', 'https://avatars.githubusercontent.com/u/47542598', 1, 2, '0', 751.89, '部且现后。群省需维示要。包些了工。一须门况使和经史手。', 0, 1, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2063976349802303489, '2026-06-08 21:56:41', '2026-06-08 21:56:41', '1872249865963053057', '2062434781934362610', '揭伟', 'https://avatars.githubusercontent.com/u/47542598', 1, 2, '0', 751.89, '部且现后。群省需维示要。包些了工。一须门况使和经史手。', 0, 1, '[{\"color\":\"olive\",\"id\":\"42\",\"name\":\"聊梓涵\",\"sort\":95}]', '[{\"color\":\"mint green\",\"id\":\"30\",\"name\":\"德文昊\",\"sort\":35},{\"color\":\"red\",\"id\":\"99\",\"name\":\"钟海燕\",\"sort\":87},{\"color\":\"yellow\",\"id\":\"18\",\"name\":\"机敬彪\",\"sort\":90}]', '[{\"attributes\":{\"color\":\"magenta\",\"type\":\"commodo esse quis tempor consectetur\"},\"content\":{\"address\":\"宋巷3616号\",\"attributes\":[{\"color\":\"gold\",\"id\":\"5\",\"name\":\"夏国良\",\"sort\":64},{\"color\":\"azure\",\"id\":\"37\",\"name\":\"银三锋\",\"sort\":62},{\"color\":\"gold\",\"id\":\"48\",\"name\":\"老万佳\",\"sort\":95}],\"author\":\"voluptate exercitation veniam\",\"color\":\"purple\",\"date\":\"2026-10-08\",\"id\":\"63\",\"latitude\":\"17.4013\",\"level\":\"in id qui consectetur\",\"longitude\":\"23.4593\",\"medias\":[{\"id\":\"20\",\"name\":\"官芳\",\"path\":\"/etc\",\"sort\":46,\"suffix\":\"PhD\",\"type\":67}],\"name\":\"母志明\",\"password\":\"lbvxRwtWTt6wwrk\",\"path\":\"/lib\",\"price\":479.69,\"rich\":\"sunt cillum\",\"shortName\":\"似超\",\"sort\":34,\"tags\":[{\"color\":\"orchid\",\"id\":\"77\",\"name\":\"湛思佳\",\"sort\":82},{\"color\":\"mint green\",\"id\":\"1\",\"name\":\"罕丹\",\"sort\":81},{\"color\":\"tan\",\"id\":\"51\",\"name\":\"明安琪\",\"sort\":44}],\"text\":\"如团记米北采市需则带。\",\"url\":\"https://negative-bathhouse.info/\",\"value\":\"ut in Excepteur cillum\"},\"element\":39,\"isShow\":49,\"name\":\"山哲新\",\"sort\":57}]', '{}');
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064186360289718273, '2026-06-09 11:22:34', '2026-06-09 11:22:34', '2048739717880901634', '322576166153691136', '西游记', '', 20, 3, '0', 199.99, '四大名著之西游记', 0, 1, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '[{\"content\":{\"value\":\"\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":3},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":4},{\"content\":{\"value\":\"199.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":5},{\"content\":{\"value\":\"四大名著之西游记\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":6}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064193074871300098, '2026-06-09 11:49:15', '2026-06-09 11:49:15', '2048739717880901634', '322582863905378304', '水浒传', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780976951816.jpg', 20, 3, '0', 199.99, '水壶', 0, 1, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780976951816.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"水浒传\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":3},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":4},{\"content\":{\"value\":\"199.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":5},{\"content\":{\"value\":\"水壶\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":6}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064239370642550785, '2026-06-09 14:53:13', '2026-06-09 14:53:13', '2048739717880901634', '322629142522265600', '西游记', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780987990661.jpg', 20, 3, '0', 199.23, '四大名著之西游记', 0, 1, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780987990661.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"199.23\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":6},{\"content\":{\"value\":\"四大名著之西游记\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":7}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064246952887218177, '2026-06-10 16:45:51', '2026-06-10 16:45:51', '2048739717880901634', '322636753410035712', '西游记续集11', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780988803670_baeb04ba_1780989798561.jpg', 20, 2, '0', 199.88, '四大名著之西游记4', 0, 1, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2}]', '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780988803670_baeb04ba_1780989798561.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记续集11\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"2\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-06-09\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"199.88\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"四大名著之西游记4\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064633398768910337, '2026-06-10 17:02:15', '2026-06-10 17:02:15', '2048739717880901634', '323023209940688896', '红楼梦', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781082102030_7a7bc2aa_1781082133874.jpg', 10, 3, '0', 200.99, '四大名著之红楼梦', 0, 1, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781082102030_7a7bc2aa_1781082133874.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"红楼梦\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"10\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-06-04\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"200.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"四大名著之红楼梦\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064641644623773698, '2026-06-10 17:31:42', '2026-06-10 17:31:42', '2048739717880901634', '323031450118836224', '水浒传', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083835104_668356b3_1781083901474.jpg', 10, 2, '0', 198.99, '四大名著之水浒传', 0, 1, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083835104_668356b3_1781083901474.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"水浒传\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"10\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"2\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-06-08\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"198.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"四大名著之水浒传\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2064642261442314241, '2026-06-10 17:34:09', '2026-06-10 17:34:09', '2048739717880901634', '323032067059011584', '遗失的美好', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083949957_6df371c7_1781084048669.jpg', 1, 2, '0', 20.00, '归还必重谢', 0, 1, NULL, '[{\"color\":\"#7631F5\",\"id\":\"2062743580002725890\",\"name\":\"身份证\",\"sort\":1}]', '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083949957_6df371c7_1781084048669.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"遗失的美好\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062460465698988033\",\"name\":\"身份证\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"1\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"2\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-01-15\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"20\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"归还必重谢\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#7631F5\",\"id\":\"2062743580002725890\",\"name\":\"身份证\",\"sort\":1}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL);
INSERT INTO `tbl_item_info` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `name`, `icon`, `quantity`, `level`, `category`, `price`, `description`, `sort`, `status`, `attributes`, `tags`, `fields`, `extend_data`) VALUES (2066682391854288898, '2026-06-16 08:40:54', '2026-06-16 08:40:54', '1872249865963053057', '325072207070699520', NULL, NULL, 0, 0, '0', 0.00, NULL, 0, 0, NULL, NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_ruins
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_ruins`;
CREATE TABLE `tbl_item_ruins` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `ori_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始Id',
  `item_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主题编码',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物品分类',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题名称',
  `icon` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题类型',
  `quantity` int unsigned NOT NULL DEFAULT '0' COMMENT '数量',
  `level` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '主题描述',
  `price` decimal(12,2) unsigned NOT NULL DEFAULT '0.00' COMMENT '价格',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '描述',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '排序',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户Id',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态',
  `fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组件列表',
  `extend_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '拓展数据',
  `attributes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '属性列表',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '标签列表',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='殷墟数据表';

-- ----------------------------
-- Records of tbl_item_ruins
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2037206201555791874, '2026-03-27 00:33:03', '2026-03-27 00:33:02', '86', '2037206201526431744', '', NULL, NULL, 0, 0, 0.00, '0', 0, '1872249865963053057', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2039720043830321154, '2026-04-02 23:02:09', '2026-04-03 00:38:27', '66', '2039720043817738240', '', '雀国香', 'https://avatars.githubusercontent.com/u/10378468', 18, 1, 359.25, '话状委当。三广何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, '1872249865963053057', 0, '[{\"content\":{\"name\":\"121212\"},\"element\":\"mollit\",\"sort\":11}]', '{}', '[]', '[]');
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2049075088516239361, '2026-04-28 18:35:46', '2026-04-28 18:46:42', '4', '2049075088088420354', '0', '雀国香1', 'https://avatars.githubusercontent.com/u/10378468', 12, 1, 359.25, '话状委当。三广何局问交小速认山。选律见声级式治在。回合下打工究消法。阶方收志现。易志正。规民提且整。意容却。', 0, '1872249865963053057', 0, '[FieldElementVO(element=1, name=null, sort=11, isShow=null, attributes=null, content=ElementContentVO(id=null, text=null, rich=null, path=null, url=null, level=null, color=null, date=null, price=null, tags=null, attributes=null, medias=null, author=null, name=121212, password=null, shortName=null, address=null, latitude=null, longitude=null, value=null, sort=null))]', 'ExtendDataVO()', '[]', '[]');
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063865043988504578, '2026-06-08 14:05:46', '2026-06-08 14:05:45', '322254332389486592', '2063865043824926722', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063868986823008258, '2026-06-08 14:21:26', '2026-06-08 14:21:25', '322258797136064512', '2063868986709762050', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063871891613409282, '2026-06-08 14:32:59', '2026-06-08 14:32:58', '322254332393680896', '2063871891500163073', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063874279673946113, '2026-06-08 14:42:28', '2026-06-08 14:42:27', '322264090777718784', '2063874279564894210', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063877091636011009, '2026-06-08 14:53:38', '2026-06-08 14:53:38', '322266904002289664', '2063877091522764802', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063882079636545537, '2026-06-08 15:13:28', '2026-06-08 15:13:27', '322271892142882816', '2063882079523299329', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063883391426093057, '2026-06-08 15:18:40', '2026-06-08 15:18:40', '322273202443780096', '2063883391317041154', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063884940420308993, '2026-06-08 15:24:50', '2026-06-08 15:24:49', '322274752439959552', '2063884940256731137', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063887765598625793, '2026-06-08 15:36:03', '2026-06-08 15:36:03', '322277499522871296', '2063887765489573889', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063892435104686082, '2026-06-08 15:54:36', '2026-06-08 15:54:36', '322254332385292288', '2063892434945302529', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063894117893005314, '2026-06-08 16:01:18', '2026-06-08 16:01:17', '322283918349402112', '2063894117733621761', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063913957680734209, '2026-06-08 17:20:08', '2026-06-08 17:20:07', '322303739501715456', '2063913957571682305', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2063922280278777857, '2026-06-08 17:53:12', '2026-06-08 17:53:12', '322312078224801792', '2063922280169725953', '0', NULL, NULL, 0, 0, 0.00, '0', 0, '2048739717880901634', 0, NULL, NULL, NULL, NULL);
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2064186360465879042, '2026-06-09 11:22:34', '2026-06-09 11:22:34', '322576166153691136', '2064186360289718273', '2062486537077415938', '西游记', '', 20, 3, 199.99, '四大名著之西游记', 0, '2048739717880901634', 0, '[{\"content\":{\"value\":\"\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":3},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":4},{\"content\":{\"value\":\"199.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":5},{\"content\":{\"value\":\"四大名著之西游记\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":6}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]');
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2064193075022295041, '2026-06-09 11:49:15', '2026-06-09 11:49:15', '322582863905378304', '2064193074871300098', '', '水浒传', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780976951816.jpg', 20, 3, 199.99, '水壶', 0, '2048739717880901634', 0, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780976951816.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"水浒传\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":3},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":4},{\"content\":{\"value\":\"199.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":5},{\"content\":{\"value\":\"水壶\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":6}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]');
INSERT INTO `tbl_item_ruins` (`id`, `create_time`, `update_time`, `ori_id`, `item_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `user_id`, `status`, `fields`, `extend_data`, `attributes`, `tags`) VALUES (2064239370789351425, '2026-06-09 14:53:12', '2026-06-09 14:53:13', '322629142522265600', '2064239370642550785', '2062486537077415938', '西游记', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780987990661.jpg', 20, 3, 199.23, '四大名著之西游记', 0, '2048739717880901634', 0, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780987990661.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"199.23\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":6},{\"content\":{\"value\":\"四大名著之西游记\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":7}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]');
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_sync
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_sync`;
CREATE TABLE `tbl_item_sync` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户Id',
  `ori_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原始Id',
  `item_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物品Id',
  `inventory_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '持有Id',
  `category_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类Id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题名称',
  `icon` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题类型',
  `quantity` int unsigned NOT NULL DEFAULT '0' COMMENT '数量',
  `level` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '主题描述',
  `price` decimal(12,2) unsigned NOT NULL DEFAULT '0.00' COMMENT '价格',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '描述',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态',
  `fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组件列表',
  `extend_data` text COLLATE utf8mb4_general_ci COMMENT '拓展数据',
  `attributes` text COLLATE utf8mb4_general_ci COMMENT '属性列表',
  `tags` text COLLATE utf8mb4_general_ci COMMENT '标签列表',
  `version` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本',
  `sync_status` tinyint NOT NULL DEFAULT '0' COMMENT '同步状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物品同步表';

-- ----------------------------
-- Records of tbl_item_sync
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2063976349949104130, '2026-06-08 21:28:03', '2026-06-16 08:39:49', '1872249865963053057', '2062434781934362610', '2063976349802303489', '2063976349894578177', '31', '揭伟', 'https://avatars.githubusercontent.com/u/47542598', 0, 2, 751.89, '部且现后。群省需维示要。包些了工。一须门况使和经史手。', 0, 3, '[{\"attributes\":{\"color\":\"magenta\",\"type\":\"commodo esse quis tempor consectetur\"},\"content\":{\"address\":\"宋巷3616号\",\"attributes\":[{\"color\":\"gold\",\"id\":\"5\",\"name\":\"夏国良\",\"sort\":64},{\"color\":\"azure\",\"id\":\"37\",\"name\":\"银三锋\",\"sort\":62},{\"color\":\"gold\",\"id\":\"48\",\"name\":\"老万佳\",\"sort\":95}],\"author\":\"voluptate exercitation veniam\",\"color\":\"purple\",\"date\":\"2026-10-08\",\"id\":\"63\",\"latitude\":\"17.4013\",\"level\":\"in id qui consectetur\",\"longitude\":\"23.4593\",\"medias\":[{\"id\":\"20\",\"name\":\"官芳\",\"path\":\"/etc\",\"sort\":46,\"suffix\":\"PhD\",\"type\":67}],\"name\":\"母志明\",\"password\":\"lbvxRwtWTt6wwrk\",\"path\":\"/lib\",\"price\":479.69,\"rich\":\"sunt cillum\",\"shortName\":\"似超\",\"sort\":34,\"tags\":[{\"color\":\"orchid\",\"id\":\"77\",\"name\":\"湛思佳\",\"sort\":82},{\"color\":\"mint green\",\"id\":\"1\",\"name\":\"罕丹\",\"sort\":81},{\"color\":\"tan\",\"id\":\"51\",\"name\":\"明安琪\",\"sort\":44}],\"text\":\"如团记米北采市需则带。\",\"url\":\"https://negative-bathhouse.info/\",\"value\":\"ut in Excepteur cillum\"},\"element\":39,\"isShow\":49,\"name\":\"山哲新\",\"sort\":57}]', '{}', '[{\"color\":\"olive\",\"id\":\"42\",\"name\":\"聊梓涵\",\"sort\":95}]', '[{\"color\":\"mint green\",\"id\":\"30\",\"name\":\"德文昊\",\"sort\":35},{\"color\":\"red\",\"id\":\"99\",\"name\":\"钟海燕\",\"sort\":87},{\"color\":\"yellow\",\"id\":\"18\",\"name\":\"机敬彪\",\"sort\":90}]', '9', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064186360465879042, '2026-06-09 11:22:34', '2026-06-09 15:54:55', '2048739717880901634', '322576166153691136', '2064186360289718273', '2064186360402964481', '2062486537077415938', '西游记', '', 20, 3, 199.99, '四大名著之西游记', 0, 3, '[{\"content\":{\"value\":\"\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":3},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":4},{\"content\":{\"value\":\"199.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":5},{\"content\":{\"value\":\"四大名著之西游记\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":6}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '2', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064193075022295041, '2026-06-09 11:49:15', '2026-06-09 15:54:56', '2048739717880901634', '322582863905378304', '2064193074871300098', '2064193074963574785', '', '水浒传', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780976951816.jpg', 20, 3, 199.99, '水壶', 0, 3, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780976951816.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"水浒传\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":3},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":4},{\"content\":{\"value\":\"199.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":5},{\"content\":{\"value\":\"水壶\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":6}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '2', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064239370789351425, '2026-06-09 14:53:12', '2026-06-09 15:54:56', '2048739717880901634', '322629142522265600', '2064239370642550785', '2064239370734825473', '2062486537077415938', '西游记', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780987990661.jpg', 20, 3, 199.23, '四大名著之西游记', 0, 3, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780976854292_d254bb60_1780987990661.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"value\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"199.23\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":6},{\"content\":{\"value\":\"四大名著之西游记\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":7}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '2', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064246953017241601, '2026-06-09 15:23:20', '2026-06-11 11:21:15', '2048739717880901634', '322636753410035712', '2064246952887218177', '2064246952966909954', '2062486537077415938', '西游记续集11', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780988803670_baeb04ba_1780989798561.jpg', 15, 2, 199.88, '四大名著之西游记4', 0, 0, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1780988803670_baeb04ba_1780989798561.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"西游记续集11\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"20\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"2\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-06-09\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"199.88\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"四大名著之西游记4\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2}]', '3', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064633398936682497, '2026-06-10 16:58:56', '2026-06-11 15:49:47', '2048739717880901634', '323023209940688896', '2064633398768910337', '2064633398869573634', '2062486537077415938', '红楼梦', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781082102030_7a7bc2aa_1781082133874.jpg', 8, 3, 200.99, '四大名著之红楼梦', 0, 0, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781082102030_7a7bc2aa_1781082133874.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"红楼梦\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"10\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"3\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-06-04\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"200.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"四大名著之红楼梦\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '3', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064641644787351554, '2026-06-10 17:31:42', '2026-06-10 17:51:00', '2048739717880901634', '323031450118836224', '2064641644623773698', '2064641644720242689', '2062486537077415938', '水浒传', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083835104_668356b3_1781083901474.jpg', 5, 2, 198.99, '四大名著之水浒传', 0, 0, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083835104_668356b3_1781083901474.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"水浒传\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062486537077415938\",\"name\":\"书籍\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"10\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"2\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-06-08\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"198.99\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"四大名著之水浒传\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL, NULL, '[{\"color\":\"#299A0F\",\"id\":\"2062825256372383746\",\"name\":\"小说\",\"sort\":2},{\"color\":\"#029292\",\"id\":\"2062838466773147649\",\"name\":\"四大名著\",\"sort\":3}]', '2', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2064642261639446529, '2026-06-10 17:34:09', '2026-06-10 17:34:09', '2048739717880901634', '323032067059011584', '2064642261442314241', '2064642261563949057', '2062460465698988033', '遗失的美好', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083949957_6df371c7_1781084048669.jpg', 1, 2, 20.00, '归还必重谢', 0, 0, '[{\"content\":{\"value\":\"https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/1781083949957_6df371c7_1781084048669.jpg\"},\"element\":2,\"isShow\":1,\"name\":\"icon\",\"sort\":1},{\"content\":{\"value\":\"遗失的美好\"},\"element\":1,\"isShow\":1,\"name\":\"name\",\"sort\":2},{\"content\":{\"id\":\"2062460465698988033\",\"name\":\"身份证\"},\"element\":3,\"isShow\":1,\"name\":\"category\",\"sort\":3},{\"content\":{\"value\":\"1\"},\"element\":7,\"isShow\":1,\"name\":\"quantity\",\"sort\":4},{\"content\":{\"level\":\"2\"},\"element\":4,\"isShow\":1,\"name\":\"level\",\"sort\":5},{\"content\":{\"value\":\"2026-01-15\"},\"element\":5,\"isShow\":1,\"name\":\"purchaseTime\",\"sort\":6},{\"content\":{\"value\":\"20\"},\"element\":6,\"isShow\":1,\"name\":\"price\",\"sort\":7},{\"content\":{\"value\":\"归还必重谢\"},\"element\":8,\"isShow\":1,\"name\":\"description\",\"sort\":8},{\"content\":{\"tags\":[{\"color\":\"#7631F5\",\"id\":\"2062743580002725890\",\"name\":\"身份证\",\"sort\":1}]},\"element\":10,\"isShow\":1,\"name\":\"tags\",\"sort\":9}]', NULL, NULL, '[{\"color\":\"#7631F5\",\"id\":\"2062743580002725890\",\"name\":\"身份证\",\"sort\":1}]', '2', 1);
INSERT INTO `tbl_item_sync` (`id`, `create_time`, `update_time`, `user_id`, `ori_id`, `item_id`, `inventory_id`, `category_id`, `name`, `icon`, `quantity`, `level`, `price`, `description`, `sort`, `status`, `fields`, `extend_data`, `attributes`, `tags`, `version`, `sync_status`) VALUES (2066682392017866753, '2026-06-16 08:40:54', '2026-06-16 08:40:54', '1872249865963053057', '325072207070699520', '2066682391854288898', '2066682391954952193', '0', NULL, NULL, 0, 0, 0.00, '0', 0, 0, NULL, NULL, NULL, NULL, '1', 0);
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_topic
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_topic`;
CREATE TABLE `tbl_item_topic` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `topic_id` bigint NOT NULL COMMENT '主题',
  `item_id` bigint NOT NULL COMMENT '物品',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='主题物品关联';

-- ----------------------------
-- Records of tbl_item_topic
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_topic` (`id`, `create_time`, `update_time`, `topic_id`, `item_id`, `sort`) VALUES (1912881355000406017, '2025-04-17 22:50:29', '2025-04-17 22:50:24', 1912535657079971842, 1908085489030721521, 0);
INSERT INTO `tbl_item_topic` (`id`, `create_time`, `update_time`, `topic_id`, `item_id`, `sort`) VALUES (1912883705538789377, '2025-04-17 22:59:50', '2025-04-17 22:59:45', 1912535657079971842, 1908085489030721522, 1212);
INSERT INTO `tbl_item_topic` (`id`, `create_time`, `update_time`, `topic_id`, `item_id`, `sort`) VALUES (1912883780738465794, '2025-04-17 23:00:08', '2025-04-17 23:00:03', 1912535510388383746, 1908085489030721521, 0);
INSERT INTO `tbl_item_topic` (`id`, `create_time`, `update_time`, `topic_id`, `item_id`, `sort`) VALUES (1912883850300997633, '2025-04-17 23:00:24', '2025-04-17 23:00:19', 1912535510388383746, 1908085489030721521, 0);
INSERT INTO `tbl_item_topic` (`id`, `create_time`, `update_time`, `topic_id`, `item_id`, `sort`) VALUES (1912883874523103234, '2025-04-17 23:00:30', '2025-04-17 23:00:25', 1912535657079971842, 1908085489030721522, 0);
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_usage_record
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_usage_record`;
CREATE TABLE `tbl_item_usage_record` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `item_id` bigint NOT NULL COMMENT '物品Id',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户Id',
  `type` int DEFAULT '0' COMMENT '使用类型',
  `usage_time` datetime DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户物品使用记录';

-- ----------------------------
-- Records of tbl_item_usage_record
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tbl_item_user_inventory
-- ----------------------------
DROP TABLE IF EXISTS `tbl_item_user_inventory`;
CREATE TABLE `tbl_item_user_inventory` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `get_time` datetime NOT NULL COMMENT '获取时间',
  `user_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '用户Id',
  `item_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物品Id',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '库存数量',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '物品状态',
  `is_sync` tinyint NOT NULL DEFAULT '0' COMMENT '是否同步',
  `is_drop` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '是否丢弃',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户物品库存';

-- ----------------------------
-- Records of tbl_item_user_inventory
-- ----------------------------
BEGIN;
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (1908086682737987586, '2025-04-04 17:18:10', '2025-09-03 11:27:19', '2025-04-04 17:18:10', '1872249865963053057', '1908085489030721521', 12, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (1908774658696871938, '2025-04-06 14:51:57', '2025-04-06 23:34:42', '2025-04-06 14:51:57', '1872249865963053057', '1908085489030721522', 2, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (1908775243619770369, '2025-04-06 14:54:16', '2025-09-03 11:27:17', '2025-04-06 14:54:16', '1872249865963053057', '1908085489030721521', 11, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2049058614833659906, '2026-04-28 17:30:18', '2026-04-28 17:30:18', '2026-04-28 17:30:18', '1872249865963053057', '2049058614644916225', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2049058665383411714, '2026-04-28 17:30:30', '2026-04-28 18:34:19', '2026-04-28 18:34:19', '1872249865963053057', '2049058665228222465', 12, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2049074766259474433, '2026-04-28 18:34:29', '2026-04-28 18:34:38', '2026-04-28 18:34:38', '1872249865963053057', '2049074766028787713', 12, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2049075088293941249, '2026-04-28 18:35:46', '2026-04-28 18:41:01', '2026-04-28 18:41:01', '1872249865963053057', '2049075088088420354', 12, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062434409333366785, '2026-06-04 15:20:56', '2026-06-04 15:20:56', '2026-06-04 15:20:56', '1872249865963053057', '2062434409098485762', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062434782173437953, '2026-06-04 15:22:25', '2026-06-04 17:48:12', '2026-06-04 17:01:47', '1872249865963053057', '2062434781934362625', 83, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062464289961893889, '2026-06-04 17:19:40', '2026-06-04 17:48:40', '2026-06-04 17:19:40', '1950497348299567106', '2062434781934362625', 56, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062471932793266178, '2026-06-04 17:50:02', '2026-06-08 19:56:08', '2026-06-08 19:56:08', '1872249865963053057', '2062471932583550978', 100, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062475581351256066, '2026-06-04 18:04:32', '2026-06-04 23:09:33', '2026-06-04 18:04:32', '1950497348299567106', '2062471932583550978', 43, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062819379217149953, '2026-06-05 16:50:40', '2026-06-05 16:50:40', '2026-06-05 16:50:40', '2048739717880901634', '2062819379108098050', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062825492264235010, '2026-06-05 17:14:57', '2026-06-05 17:14:57', '2026-06-05 17:14:57', '2048739717880901634', '2062825492159377409', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062829744701288449, '2026-06-05 17:31:51', '2026-06-05 17:31:51', '2026-06-05 17:31:51', '2048739717880901634', '2062829744604819458', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062831222828544002, '2026-06-05 17:37:44', '2026-06-05 17:37:43', '2026-06-05 17:37:44', '2048739717880901634', '2062831222727880706', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062838600680497154, '2026-06-05 18:07:03', '2026-06-05 18:07:02', '2026-06-05 18:07:03', '2048739717880901634', '2062838600558862338', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062839884271738882, '2026-06-05 18:12:09', '2026-06-05 18:12:08', '2026-06-05 18:12:09', '2048739717880901634', '2062839884171075585', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2062840981153538050, '2026-06-05 18:16:30', '2026-06-05 18:16:30', '2026-06-05 18:16:30', '2048739717880901634', '2062840981048680449', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063865043925590017, '2026-06-08 14:05:46', '2026-06-08 14:05:45', '2026-06-08 14:05:46', '2048739717880901634', '2063865043824926722', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063868986776870913, '2026-06-08 14:21:26', '2026-06-08 14:21:25', '2026-06-08 14:21:26', '2048739717880901634', '2063868986709762050', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063871891567271938, '2026-06-08 14:32:58', '2026-06-08 14:32:58', '2026-06-08 14:32:58', '2048739717880901634', '2063871891500163073', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063874279632003074, '2026-06-08 14:42:28', '2026-06-08 14:42:27', '2026-06-08 14:42:28', '2048739717880901634', '2063874279564894210', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063877091589873666, '2026-06-08 14:53:38', '2026-06-08 14:53:38', '2026-06-08 14:53:38', '2048739717880901634', '2063877091522764802', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063882079590408193, '2026-06-08 15:13:28', '2026-06-08 15:13:27', '2026-06-08 15:13:28', '2048739717880901634', '2063882079523299329', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063883391384150017, '2026-06-08 15:18:40', '2026-06-08 15:18:40', '2026-06-08 15:18:40', '2048739717880901634', '2063883391317041154', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063884940353200130, '2026-06-08 15:24:50', '2026-06-08 15:24:49', '2026-06-08 15:24:50', '2048739717880901634', '2063884940256731137', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063887765556682753, '2026-06-08 15:36:03', '2026-06-08 15:36:03', '2026-06-08 15:36:03', '2048739717880901634', '2063887765489573889', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063892435041771521, '2026-06-08 15:54:36', '2026-06-08 15:54:36', '2026-06-08 15:54:36', '2048739717880901634', '2063892434945302529', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063894117830090753, '2026-06-08 16:01:18', '2026-06-08 16:01:17', '2026-06-08 16:01:18', '2048739717880901634', '2063894117733621761', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063913957638791170, '2026-06-08 17:20:08', '2026-06-08 17:20:07', '2026-06-08 17:20:08', '2048739717880901634', '2063913957571682305', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063922280236834817, '2026-06-08 17:53:12', '2026-06-08 17:53:12', '2026-06-08 17:53:12', '2048739717880901634', '2063922280169725953', 0, 0, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063926546825469954, '2026-06-08 18:10:09', '2026-06-08 18:10:10', '2026-06-08 18:10:10', '2048739717880901634', '2063926546758361089', 11, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063928603942838274, '2026-06-08 18:18:20', '2026-06-08 18:18:20', '2026-06-08 18:18:20', '2048739717880901634', '2063928603875729409', 20, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063953293532979201, '2026-06-08 19:56:26', '2026-06-08 21:26:24', '2026-06-08 21:26:24', '1872249865963053057', '2063953293465870338', 1, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063960118680608769, '2026-06-08 20:23:33', '2026-06-08 20:23:57', '2026-06-08 20:23:57', '1872249865963053057', '2063960118584139777', 100, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063976045107089410, '2026-06-08 21:26:51', '2026-06-08 21:27:00', '2026-06-08 21:27:00', '1872249865963053057', '2063976045010620417', 1, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2063976349894578177, '2026-06-08 21:28:03', '2026-06-16 08:39:49', '2026-06-08 21:56:41', '1872249865963053057', '2063976349802303489', 0, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064186360402964481, '2026-06-09 11:22:34', '2026-06-09 11:22:34', '2026-06-09 11:22:34', '2048739717880901634', '2064186360289718273', 20, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064193074963574785, '2026-06-09 11:49:15', '2026-06-09 11:49:15', '2026-06-09 11:49:15', '2048739717880901634', '2064193074871300098', 20, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064239370734825473, '2026-06-09 14:53:12', '2026-06-09 14:53:13', '2026-06-09 14:53:13', '2048739717880901634', '2064239370642550785', 20, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064246952966909954, '2026-06-09 15:23:20', '2026-06-11 11:21:15', '2026-06-10 16:45:51', '2048739717880901634', '2064246952887218177', 15, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064633398869573634, '2026-06-10 16:58:56', '2026-06-11 15:49:47', '2026-06-10 17:02:15', '2048739717880901634', '2064633398768910337', 8, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064641644720242689, '2026-06-10 17:31:42', '2026-06-10 17:51:00', '2026-06-10 17:31:42', '2048739717880901634', '2064641644623773698', 5, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2064642261563949057, '2026-06-10 17:34:09', '2026-06-10 17:34:09', '2026-06-10 17:34:09', '2048739717880901634', '2064642261442314241', 1, 1, 0, 0);
INSERT INTO `tbl_item_user_inventory` (`id`, `create_time`, `update_time`, `get_time`, `user_id`, `item_id`, `quantity`, `status`, `is_sync`, `is_drop`) VALUES (2066682391954952193, '2026-06-16 08:40:54', '2026-06-16 08:40:54', '2026-06-16 08:40:54', '1872249865963053057', '2066682391854288898', 0, 0, 0, 0);
COMMIT;

-- ----------------------------
-- Table structure for tbl_oss_files
-- ----------------------------
DROP TABLE IF EXISTS `tbl_oss_files`;
CREATE TABLE `tbl_oss_files` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `token` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '临时验证Token',
  `file_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件名称',
  `url` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件地址',
  `file_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）',
  `store_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件上传类型(temp/本地上传(临时文件) manage/知识库)',
  `parent_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父级id',
  `tenant_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '租户id',
  `file_size` double(13,2) DEFAULT NULL COMMENT '文件大小（kb）',
  `iz_folder` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否文件夹(1：是  0：否)',
  `iz_root_folder` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否为1级文件夹，允许为空 (1：是 )',
  `iz_star` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否标星(1：是  0：否)',
  `down_count` int DEFAULT NULL COMMENT '下载次数',
  `read_count` int DEFAULT NULL COMMENT '阅读次数',
  `share_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '分享链接',
  `share_perms` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看)',
  `enable_down` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否允许下载(1：是  0：否)',
  `enable_updat` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否允许修改(1：是  0：否)',
  `del_flag` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '删除状态(0-正常,1-删除至回收站)',
  `usage_status` int DEFAULT NULL COMMENT '使用状态：0 未使用，1已使用',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `index_tenant_id` (`tenant_id`) USING BTREE,
  KEY `index_del_flag` (`del_flag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='OSS文件';

-- ----------------------------
-- Records of tbl_oss_files
-- ----------------------------
BEGIN;
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1580814573568143361', NULL, '11.jpg', 'comment/11_1665730539114.jpg', 'image', 'temp', NULL, NULL, 10956.00, '0', '0', '0', NULL, NULL, NULL, NULL, NULL, NULL, '0', NULL, 'admin', '2022-10-14 14:55:39', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1584493984691740674', NULL, 'jeecg-boot漏洞.pdf', 'comment/jeecg-boot漏洞_1666607779077.pdf', 'pdf', 'temp', NULL, NULL, 842789.00, '0', '0', '0', NULL, NULL, NULL, NULL, NULL, NULL, '0', NULL, 'admin', '2022-10-24 18:36:19', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1714455459928985601', NULL, '低代码平台.png', 'comment/低代码平台_1697593009343.png', 'image', 'temp', NULL, '1000', 1249631.00, '0', '0', '0', NULL, NULL, NULL, NULL, NULL, NULL, '0', NULL, 'admin', '2023-10-18 09:36:49', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957833644617768961', '1957833644504522752', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957833644215115776.jpg', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-19 23:54:50', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957833655841726465', '1957833655841726464', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957833655636205568.jpg', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-19 23:54:53', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957833878072745986', '1957833877963694080', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957833877657509888.jpg', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-19 23:55:46', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957834319208718338', '1957834319103860736', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957834318692818944.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-19 23:57:31', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957834323549822977', '1957834323549822976', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957834323394633728.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-19 23:57:32', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957834326586499074', '1957834326586499072', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957834326439698432.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-19 23:57:33', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957834329421848578', '1957834329421848576', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957834329283436544.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-19 23:57:34', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957834332148146177', '1957834332152340480', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957834332005539840.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-19 23:57:34', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957834335159656449', '1957834335155462144', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957834334983495680.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-19 23:57:35', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1957837310728622082', '1957837310545149952', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1957837310461263872.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-20 00:09:24', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1958199250458279938', '1958199250366005248', 'MNO管理平台.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/MNO管理平台_1958199250043043840.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-21 00:07:38', NULL, NULL);
INSERT INTO `tbl_oss_files` (`id`, `token`, `file_name`, `url`, `file_type`, `store_type`, `parent_id`, `tenant_id`, `file_size`, `iz_folder`, `iz_root_folder`, `iz_star`, `down_count`, `read_count`, `share_url`, `share_perms`, `enable_down`, `enable_updat`, `del_flag`, `usage_status`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES ('1958436112322002946', '1958436112030416896', 'WechatIMG827.jpg', 'https://sf-pic.oss-cn-guangzhou.aliyuncs.com/upload/oss/WechatIMG827_1958436111636152320.jpg', 'image', 'temp', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, '2025-08-21 15:48:50', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_queue_id
-- ----------------------------
DROP TABLE IF EXISTS `tbl_queue_id`;
CREATE TABLE `tbl_queue_id` (
  `id` bigint NOT NULL COMMENT 'id',
  `queue_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '队列Id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '同步状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='主题表';

-- ----------------------------
-- Records of tbl_queue_id
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tbl_send_sms_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_send_sms_log`;
CREATE TABLE `tbl_send_sms_log` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `sms_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '验证码',
  `send_status` int DEFAULT NULL COMMENT '发送状态',
  `verify_status` int DEFAULT NULL COMMENT '验证状态',
  `send_at` datetime DEFAULT NULL COMMENT '发送时间',
  `expired_at` datetime DEFAULT NULL COMMENT '有效期',
  `send_ip` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发送IP',
  `send_type` int DEFAULT NULL COMMENT '发送类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SMS发送记录';

-- ----------------------------
-- Records of tbl_send_sms_log
-- ----------------------------
BEGIN;
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014213259637149698, '2026-01-22 13:47:18', '2026-01-22 13:47:18', '13650962253', '702792', 1, NULL, '2026-01-22 13:46:57', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014213679822446593, '2026-01-22 13:48:58', '2026-01-22 13:48:58', '13650962253', '236837', 1, NULL, '2026-01-22 13:48:56', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014238246783774722, '2026-01-22 15:26:35', '2026-01-22 15:26:35', '13650962253', '368557', 1, NULL, '2026-01-22 15:26:35', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014239321171521537, '2026-01-22 15:30:52', '2026-01-22 15:30:51', '13650962253', '726640', 1, NULL, '2026-01-22 15:30:52', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014254503016869889, '2026-01-22 16:31:11', '2026-01-22 16:31:11', '13650962253', '942128', 1, NULL, '2026-01-22 16:31:11', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014263970408169474, '2026-01-22 17:08:48', '2026-01-22 17:08:48', '13650962253', '304659', 1, NULL, '2026-01-22 17:08:48', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014264918736199682, '2026-01-22 17:12:34', '2026-01-22 17:12:34', '13650962253', '028056', 1, NULL, '2026-01-22 17:12:34', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2014274747722731522, '2026-01-22 17:51:38', '2026-01-22 17:51:38', '13650962253', '077802', 1, NULL, '2026-01-22 17:51:38', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2031015233425891330, '2026-03-09 22:32:21', '2026-03-09 22:32:20', '13650962253', '178142', 1, NULL, '2026-03-09 22:32:21', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2031641656385044481, '2026-03-11 16:01:32', '2026-03-11 16:01:31', '13650962253', '875976', 1, NULL, '2026-03-11 16:01:32', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034803116394070017, '2026-03-20 09:24:02', '2026-03-20 09:24:02', '18723704657', '319180', 1, NULL, '2026-03-20 09:24:02', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034803117753024514, '2026-03-20 09:24:03', '2026-03-20 09:24:02', '18723704657', '307462', 0, NULL, '2026-03-20 09:24:03', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034808703014060034, '2026-03-20 09:46:14', '2026-03-20 09:46:14', '18723704657', '554939', 1, NULL, '2026-03-20 09:46:14', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034808715827658754, '2026-03-20 09:46:17', '2026-03-20 09:46:17', '18723704657', '326800', 0, NULL, '2026-03-20 09:46:17', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034808751781232641, '2026-03-20 09:46:26', '2026-03-20 09:46:26', '18723704657', '923798', 0, NULL, '2026-03-20 09:46:26', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034809225896968194, '2026-03-20 09:48:19', '2026-03-20 09:48:19', '18723704657', '579575', 1, NULL, '2026-03-20 09:48:19', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2034948095506563074, '2026-03-20 19:00:08', '2026-03-20 19:00:08', '18723704657', '310031', 1, NULL, '2026-03-20 19:00:08', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035562506407559169, '2026-03-22 11:41:35', '2026-03-22 11:41:35', '13650962253', '872252', 1, NULL, '2026-03-22 11:41:35', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035563973306662914, '2026-03-22 11:47:25', '2026-03-22 11:47:24', '13650962253', '749582', 1, NULL, '2026-03-22 11:47:25', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035801879816355841, '2026-03-23 03:32:46', '2026-03-23 03:32:46', '18723704657', '403478', 1, NULL, '2026-03-23 03:32:46', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035806374554157058, '2026-03-23 03:50:38', '2026-03-23 03:50:37', '18723704657', '827250', 1, NULL, '2026-03-23 03:50:38', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035808968953479170, '2026-03-23 04:00:56', '2026-03-23 04:00:56', '18723704657', '969495', 1, NULL, '2026-03-23 04:00:56', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035809506113798145, '2026-03-23 04:03:04', '2026-03-23 04:03:04', '18723704657', '498492', 1, NULL, '2026-03-23 04:03:04', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035810126472331265, '2026-03-23 04:05:32', '2026-03-23 04:05:32', '18723704657', '879900', 1, NULL, '2026-03-23 04:05:32', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035836409558253569, '2026-03-23 05:49:59', '2026-03-23 05:49:58', '18723704657', '800485', 1, NULL, '2026-03-23 05:49:59', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035837389796458498, '2026-03-23 05:53:52', '2026-03-23 05:53:52', '18723704657', '670335', 1, NULL, '2026-03-23 05:53:52', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2035838399096995842, '2026-03-23 05:57:53', '2026-03-23 05:57:53', '18723704657', '603617', 1, NULL, '2026-03-23 05:57:53', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2037192255054610434, '2026-03-26 23:37:37', '2026-03-26 23:37:37', '13650962253', '372786', 1, NULL, '2026-03-26 23:37:37', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2048732655759290370, '2026-04-27 19:55:03', '2026-04-27 19:55:03', '13550335215', '954394', 1, NULL, '2026-04-27 19:55:03', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2048732656690425858, '2026-04-27 19:55:04', '2026-04-27 19:55:03', '13550335215', '004328', 0, NULL, '2026-04-27 19:55:04', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2048739615997063170, '2026-04-27 20:22:43', '2026-04-27 20:22:42', '13550335215', '169457', 1, NULL, '2026-04-27 20:22:43', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2048742911558184961, '2026-04-27 20:35:49', '2026-04-27 20:35:48', '13550335215', '694669', 1, NULL, '2026-04-27 20:35:49', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2062457381136523266, '2026-06-04 16:52:13', '2026-06-04 16:52:12', '13550335215', '881054', 1, NULL, '2026-06-04 16:52:13', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2062469214606090241, '2026-06-04 17:39:14', '2026-06-04 17:39:14', '13550335215', '305357', 1, NULL, '2026-06-04 17:39:14', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064313478856187906, '2026-06-09 19:47:41', '2026-06-09 19:47:41', '13550335215', '536829', 0, NULL, '2026-06-09 19:47:41', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064313522200125442, '2026-06-09 19:47:51', '2026-06-09 19:47:51', '13550335215', '183503', 0, NULL, '2026-06-09 19:47:51', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064325415107174401, '2026-06-09 20:35:07', '2026-06-09 20:35:06', '13550335215', '836290', 0, NULL, '2026-06-09 20:35:07', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064326132891000833, '2026-06-09 20:37:58', '2026-06-09 20:37:58', '13550335215', '491571', 0, NULL, '2026-06-09 20:37:58', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064343818110902274, '2026-06-09 21:48:15', '2026-06-09 21:48:14', '13650962253', '731639', 0, NULL, '2026-06-09 21:48:15', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064343864180862977, '2026-06-09 21:48:26', '2026-06-09 21:48:25', '13650962253', '834006', 0, NULL, '2026-06-09 21:48:26', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064348463843262465, '2026-06-09 22:06:42', '2026-06-09 22:06:42', '13650962253', '351652', 1, NULL, '2026-06-09 22:06:42', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064628720001826817, '2026-06-10 16:40:20', '2026-06-10 16:40:20', '13550335215', '889975', 1, NULL, '2026-06-10 16:40:20', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064631040571817985, '2026-06-10 16:49:34', '2026-06-10 16:49:33', '13550335215', '742494', 1, NULL, '2026-06-10 16:49:34', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064639755345674241, '2026-06-10 17:24:11', '2026-06-10 17:24:11', '13550335215', '416256', 1, NULL, '2026-06-10 17:24:11', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2064687067417755649, '2026-06-10 20:32:12', '2026-06-10 20:32:11', '13550335215', '338181', 1, NULL, '2026-06-10 20:32:12', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2066538217532264449, '2026-06-15 23:08:00', '2026-06-15 23:08:00', '13650962253', '079792', 1, NULL, '2026-06-15 23:08:00', NULL, NULL, NULL);
INSERT INTO `tbl_send_sms_log` (`id`, `create_time`, `update_time`, `phone`, `sms_code`, `send_status`, `verify_status`, `send_at`, `expired_at`, `send_ip`, `send_type`) VALUES (2066681702197465089, '2026-06-16 08:38:10', '2026-06-16 08:38:09', '13650962253', '510041', 1, NULL, '2026-06-16 08:38:10', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_tag
-- ----------------------------
DROP TABLE IF EXISTS `tbl_tag`;
CREATE TABLE `tbl_tag` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `tag_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签编码',
  `tag_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名称',
  `tag_type` int NOT NULL DEFAULT '0' COMMENT '标签类型',
  `tag_icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签ICON',
  `tag_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签描述',
  `sort` int DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物品标签';

-- ----------------------------
-- Records of tbl_tag
-- ----------------------------
BEGIN;
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908085489030721527, '2025-04-03 11:59:20', '2025-04-04 22:58:23', '1907644057314451456', '美食卡', 0, 'icon-food', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908168619569745921, '2025-04-04 22:43:46', '2025-04-04 22:43:45', '1908168619549028352', '书籍', 0, 'icon-book', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908169460968103938, '2025-04-04 22:47:06', '2025-04-04 22:47:06', '1908169460955774976', '证件', 0, 'icon-card', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908169580568682498, '2025-04-04 22:47:35', '2025-04-04 22:47:41', '1908169580568936448', '时光信笺', 0, 'icon-paper', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908169743307677698, '2025-04-04 22:48:14', '2025-04-04 22:50:53', '1908169743312125952', '游戏武器', 0, 'icon-arm', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908169905543356418, '2025-04-04 22:48:52', '2025-04-04 22:55:30', '1908169905543610368', '游戏歌曲', 0, 'icon-music', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908170153418334209, '2025-04-04 22:49:51', '2025-04-04 22:50:47', '1908170153414393856', '游戏衣服', 0, 'icon-clothing', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908170359853588481, '2025-04-04 22:50:41', '2025-04-04 22:50:40', '1908170359845453824', '游戏鞋子', 0, 'icon-shoe', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908171624989892609, '2025-04-04 22:55:42', '2025-04-04 22:55:42', '1908171624990146560', '道具', 0, 'icon-tool', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908171753457229826, '2025-04-04 22:56:13', '2025-04-04 22:56:12', '1908171753457483776', '金币', 0, 'icon-coin', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908171830229770241, '2025-04-04 22:56:31', '2025-04-04 22:58:39', '1908171830230024192', '技能书', 0, 'icon-skill', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908171964258754562, '2025-04-04 22:57:03', '2025-04-04 22:57:03', '1908171964259008512', '宠物卡片', 0, 'icon-pet-card', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908172175098028033, '2025-04-04 22:57:53', '2025-04-04 23:00:10', '1908172175089893376', '时装', 0, 'icon-default', NULL, 0);
INSERT INTO `tbl_tag` (`id`, `create_time`, `update_time`, `tag_code`, `tag_name`, `tag_type`, `tag_icon`, `tag_desc`, `sort`) VALUES (1908172602141089793, '2025-04-04 22:59:35', '2025-04-04 23:00:03', '1908172602141343744', '代练券', 0, 'icon-default', NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for tbl_topic
-- ----------------------------
DROP TABLE IF EXISTS `tbl_topic`;
CREATE TABLE `tbl_topic` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `topic_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主题编码',
  `topic_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主题名称',
  `topic_type` int NOT NULL DEFAULT '0' COMMENT '主题类型',
  `topic_icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题ICON',
  `topic_cate` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题分类',
  `topic_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题描述',
  `topic_status` tinyint NOT NULL DEFAULT '0' COMMENT '状态',
  `item_cate` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '物品分类',
  `is_hot` int NOT NULL DEFAULT '0' COMMENT '是否热门',
  `fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组件',
  `is_charge` tinyint NOT NULL DEFAULT '0' COMMENT '是否付费',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '价格',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='主题表';

-- ----------------------------
-- Records of tbl_topic
-- ----------------------------
BEGIN;
INSERT INTO `tbl_topic` (`id`, `create_time`, `update_time`, `topic_code`, `topic_name`, `topic_type`, `topic_icon`, `topic_cate`, `topic_desc`, `topic_status`, `item_cate`, `is_hot`, `fields`, `is_charge`, `price`) VALUES (1912535510388383746, '2025-04-16 23:56:14', '2026-04-22 17:58:24', '1912535510363217920', 'NFT新系列', 0, NULL, '', '莫西沙2222222', 0, '', 1, NULL, 0, 0.00);
INSERT INTO `tbl_topic` (`id`, `create_time`, `update_time`, `topic_code`, `topic_name`, `topic_type`, `topic_icon`, `topic_cate`, `topic_desc`, `topic_status`, `item_cate`, `is_hot`, `fields`, `is_charge`, `price`) VALUES (1912535657079971842, '2025-04-16 23:56:49', '2026-04-22 17:58:24', '1912535657109331968', '武器装备', 0, NULL, '', NULL, 0, '', 1, NULL, 0, 0.00);
INSERT INTO `tbl_topic` (`id`, `create_time`, `update_time`, `topic_code`, `topic_name`, `topic_type`, `topic_icon`, `topic_cate`, `topic_desc`, `topic_status`, `item_cate`, `is_hot`, `fields`, `is_charge`, `price`) VALUES (1912890490672758786, '2025-04-17 23:26:47', '2026-04-22 17:58:24', '1912890490672758784', '游戏宠物', 0, NULL, '', NULL, 0, '', 1, NULL, 0, 0.00);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user`;
CREATE TABLE `tbl_user` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
  `realname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '姓名',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `gender` int DEFAULT '0' COMMENT '性别',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `thumb` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '封面',
  `id_card` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '身份证',
  `birthday` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生日',
  `language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '语言',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市',
  `country` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '县',
  `open_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'openid',
  `union_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'unionid',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `intro` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '介绍',
  `status` int NOT NULL COMMENT '状态',
  `is_lock` int unsigned NOT NULL DEFAULT '0' COMMENT '是否锁定',
  `is_del` int unsigned NOT NULL DEFAULT '0' COMMENT '注销客户',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `register_at` date DEFAULT NULL COMMENT '注册时间',
  `salt` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'salt',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息';

-- ----------------------------
-- Records of tbl_user
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (1872249865963053057, '2024-12-26 19:55:27', '2025-07-30 16:02:28', '13650962253', 'c9fe1d840479c0af64895e076d068d8a', '6jStSq5Teg', '杨东升', '13650962253', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2024-12-26', 'ArczZ1oM');
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (1950497348299567106, '2025-07-30 18:03:01', '2025-07-30 21:41:40', '13650962252', NULL, 'eOCgd0Qm4f', 'ces', '13650962252', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2025-07-30', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2034948136208089090, '2026-03-20 19:00:18', '2026-03-20 19:00:17', '18723704657', NULL, 'BG5Lmb5Nzr', NULL, '18723704657', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-03-20', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2048739717880901634, '2026-04-27 20:23:07', '2026-04-27 20:23:07', '13550335215', NULL, 'i6FwBvYcRO', NULL, '13550335215', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-04-27', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2048739717880901635, '2026-04-27 20:23:07', '2026-04-27 20:23:07', 'elinxer', NULL, 'i6FwBvYcRO', NULL, '13550335211', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-04-27', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2048739717880901636, '2026-04-27 20:23:07', '2026-04-27 20:23:07', 'elinxer1', NULL, 'i6FwBvYcRO', NULL, '13550335211', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-04-27', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2048739717880901637, '2026-04-27 20:23:07', '2026-04-27 20:23:07', 'elinxer12', NULL, 'i6FwBvYcRO', NULL, '13550335210', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-04-27', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2048739717880901638, '2026-04-27 20:23:07', '2026-04-27 20:23:07', 'elinxer123', NULL, 'i6FwBvYcRO', NULL, '13550335216', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-04-27', NULL);
INSERT INTO `tbl_user` (`id`, `create_time`, `update_time`, `username`, `password`, `nickname`, `realname`, `phone`, `gender`, `avatar`, `thumb`, `id_card`, `birthday`, `language`, `province`, `city`, `country`, `open_id`, `union_id`, `address`, `intro`, `status`, `is_lock`, `is_del`, `last_login_at`, `register_at`, `salt`) VALUES (2048739717880901639, '2026-04-27 20:23:07', '2026-04-27 20:23:07', 'elinxer1231', NULL, 'i6FwBvYcRO', NULL, '13550335218', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, NULL, '2026-04-27', NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_amount
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_amount`;
CREATE TABLE `tbl_user_amount` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` bigint NOT NULL COMMENT '用户Id',
  `fairy_stone_amount` int NOT NULL COMMENT '灵石金额',
  `gold_coin_amount` int NOT NULL DEFAULT '0' COMMENT '金币金额',
  `is_lock` tinyint unsigned DEFAULT '0' COMMENT '账户锁定',
  `lock_time` datetime DEFAULT NULL COMMENT '锁定时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资金账户信息';

-- ----------------------------
-- Records of tbl_user_amount
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user_amount` (`id`, `create_time`, `update_time`, `user_id`, `fairy_stone_amount`, `gold_coin_amount`, `is_lock`, `lock_time`) VALUES (1908527984000958466, '2025-04-05 22:31:45', '2025-04-05 22:33:23', 1872249865963053057, 9999, 1, 0, NULL);
INSERT INTO `tbl_user_amount` (`id`, `create_time`, `update_time`, `user_id`, `fairy_stone_amount`, `gold_coin_amount`, `is_lock`, `lock_time`) VALUES (1950497348391841794, '2025-07-30 18:03:01', '2025-07-30 18:03:30', 1950497348299567106, 999, 99999, 0, NULL);
INSERT INTO `tbl_user_amount` (`id`, `create_time`, `update_time`, `user_id`, `fairy_stone_amount`, `gold_coin_amount`, `is_lock`, `lock_time`) VALUES (2034948136484913153, '2026-03-20 19:00:18', '2026-03-20 19:00:17', 2034948136208089090, 0, 0, 0, NULL);
INSERT INTO `tbl_user_amount` (`id`, `create_time`, `update_time`, `user_id`, `fairy_stone_amount`, `gold_coin_amount`, `is_lock`, `lock_time`) VALUES (2048739718124171265, '2026-04-27 20:23:07', '2026-04-27 20:23:07', 2048739717880901634, 0, 0, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_category
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_category`;
CREATE TABLE `tbl_user_category` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '用户Id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_sys` tinyint NOT NULL DEFAULT '0' COMMENT '系统分类',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户物品库存';

-- ----------------------------
-- Records of tbl_user_category
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user_category` (`id`, `create_time`, `update_time`, `user_id`, `name`, `icon`, `sort`, `is_sys`) VALUES (2059797237300121601, '2026-05-28 08:41:45', '2026-05-28 08:42:51', '1872249865963053057', '东奕泽', 'https://avatars.githubusercontent.com/u/32590441', 61, 77);
INSERT INTO `tbl_user_category` (`id`, `create_time`, `update_time`, `user_id`, `name`, `icon`, `sort`, `is_sys`) VALUES (2059797587092492289, '2026-05-28 08:43:09', '2026-05-28 08:43:09', '1872249865963053057', '字美方', 'https://avatars.githubusercontent.com/u/18711282', 35, 43);
INSERT INTO `tbl_user_category` (`id`, `create_time`, `update_time`, `user_id`, `name`, `icon`, `sort`, `is_sys`) VALUES (2062460465698988033, '2026-06-04 17:04:28', '2026-06-05 16:49:16', '2048739717880901634', '身份证', 'card', 0, 0);
INSERT INTO `tbl_user_category` (`id`, `create_time`, `update_time`, `user_id`, `name`, `icon`, `sort`, `is_sys`) VALUES (2062486537077415938, '2026-06-04 18:48:04', '2026-06-05 16:49:35', '2048739717880901634', '书籍', 'manual', 0, 0);
INSERT INTO `tbl_user_category` (`id`, `create_time`, `update_time`, `user_id`, `name`, `icon`, `sort`, `is_sys`) VALUES (2066683003769688066, '2026-06-16 08:43:20', '2026-06-16 08:43:20', '1872249865963053057', '测试', 'bag', 3, 0);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_config
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_config`;
CREATE TABLE `tbl_user_config` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` bigint NOT NULL COMMENT '用户Id',
  `is_refresh_tag` tinyint NOT NULL DEFAULT '0' COMMENT '是否刷新标签',
  `is_sync_online_item` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要更新线上物品',
  `is_device_first` tinyint NOT NULL DEFAULT '0' COMMENT '是否设备首次登录',
  `device_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录设备Id',
  `configs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '系统设置项',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户配置';

-- ----------------------------
-- Records of tbl_user_config
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user_config` (`id`, `create_time`, `update_time`, `user_id`, `is_refresh_tag`, `is_sync_online_item`, `is_device_first`, `device_id`, `configs`) VALUES (1908548663744184321, '2025-04-05 23:53:55', '2025-04-07 00:08:17', 1872249865963053057, 0, 0, 1, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_coupon`;
CREATE TABLE `tbl_user_coupon` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `user_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户ID',
  `coupon_id` bigint DEFAULT NULL COMMENT '优惠券ID',
  `coupon_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '优惠券名称',
  `coupon_type` int DEFAULT NULL COMMENT '优惠券类型: 1-满减券, 2-折扣券, 3-兑换券',
  `discount_value` decimal(10,2) DEFAULT NULL COMMENT '折扣金额或折扣率',
  `min_amount` decimal(10,2) DEFAULT NULL COMMENT '最低消费金额',
  `valid_start_time` datetime DEFAULT NULL COMMENT '有效期开始时间',
  `valid_end_time` datetime DEFAULT NULL COMMENT '有效期结束时间',
  `obtain_type` int DEFAULT NULL COMMENT '获取方式: 1-主动领取, 2-系统发放, 3-活动奖励',
  `status` int DEFAULT '0' COMMENT '状态: 0-未使用, 1-已使用, 2-已过期, 3-已作废',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `order_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

-- ----------------------------
-- Records of tbl_user_coupon
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user_coupon` (`id`, `create_time`, `update_time`, `user_id`, `coupon_id`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `valid_start_time`, `valid_end_time`, `obtain_type`, `status`, `use_time`, `order_id`) VALUES (2061449708643143681, '2026-06-01 22:08:05', '2026-06-01 22:08:05', '1872249865963053057', 3, '殷墟使用券', 3, 0.00, 0.00, '2026-06-01 00:00:00', '2030-06-01 00:00:00', 1, 0, NULL, NULL);
INSERT INTO `tbl_user_coupon` (`id`, `create_time`, `update_time`, `user_id`, `coupon_id`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `valid_start_time`, `valid_end_time`, `obtain_type`, `status`, `use_time`, `order_id`) VALUES (2061455162597208066, '2026-06-01 22:29:45', '2026-06-01 22:29:45', '1872249865963053057', 3, '殷墟使用券', 3, 0.00, 0.00, '2026-06-01 00:00:00', '2030-06-01 00:00:00', 1, 0, NULL, NULL);
INSERT INTO `tbl_user_coupon` (`id`, `create_time`, `update_time`, `user_id`, `coupon_id`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `valid_start_time`, `valid_end_time`, `obtain_type`, `status`, `use_time`, `order_id`) VALUES (2061455250262355969, '2026-06-01 22:30:06', '2026-06-01 22:30:06', '1872249865963053057', 3, '殷墟使用券', 3, 0.00, 0.00, '2026-06-01 00:00:00', '2030-06-01 00:00:00', 1, 0, NULL, NULL);
INSERT INTO `tbl_user_coupon` (`id`, `create_time`, `update_time`, `user_id`, `coupon_id`, `coupon_name`, `coupon_type`, `discount_value`, `min_amount`, `valid_start_time`, `valid_end_time`, `obtain_type`, `status`, `use_time`, `order_id`) VALUES (2061458889932562433, '2026-06-01 22:44:34', '2026-06-01 22:44:34', '1872249865963053057', 3, '殷墟使用券', 3, 0.00, 0.00, '2026-06-01 00:00:00', '2030-06-01 00:00:00', 1, 0, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_give_record
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_give_record`;
CREATE TABLE `tbl_user_give_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `give_uid` bigint NOT NULL COMMENT '赠送者用户ID',
  `receive_uid` bigint NOT NULL COMMENT '接收者用户ID',
  `inventory_id` bigint NOT NULL COMMENT '拥有唯一ID',
  `item_id` bigint NOT NULL COMMENT '物品ID',
  `quantity` int DEFAULT NULL COMMENT '数量',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物品名称',
  `give_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '赠送时间',
  `receive_time` datetime DEFAULT NULL COMMENT '领取时间',
  `expired_time` datetime DEFAULT NULL COMMENT '领取过期时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '赠送状态',
  `get_expired_time` datetime DEFAULT NULL COMMENT '取回过期时间',
  `get_expired_status` tinyint DEFAULT '0' COMMENT '取回过期状态',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_a` (`give_uid`) USING BTREE COMMENT '索引：查询用户A的赠送记录',
  KEY `idx_user_b` (`receive_uid`) USING BTREE COMMENT '索引：查询用户B的接收记录',
  KEY `idx_gift_time` (`give_time`) USING BTREE COMMENT '索引：按赠送时间排序查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户物品赠送记录表';

-- ----------------------------
-- Records of tbl_user_give_record
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_login_device
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_login_device`;
CREATE TABLE `tbl_user_login_device` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` bigint NOT NULL COMMENT '用户Id',
  `device_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备Id',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  `device_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '设备类型',
  `system_version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统版本',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户登录设备';

-- ----------------------------
-- Records of tbl_user_login_device
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user_login_device` (`id`, `create_time`, `update_time`, `user_id`, `device_id`, `login_time`, `device_type`, `system_version`) VALUES (1908914666453868545, '2025-04-07 00:08:17', '2025-04-07 00:08:17', 1872249865963053057, 'i1743954750420', '2025-04-07 00:08:17', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tbl_user_tag
-- ----------------------------
DROP TABLE IF EXISTS `tbl_user_tag`;
CREATE TABLE `tbl_user_tag` (
  `id` bigint NOT NULL COMMENT 'id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '用户Id',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `sort` int DEFAULT NULL COMMENT '排序',
  `color` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户物品库存';

-- ----------------------------
-- Records of tbl_user_tag
-- ----------------------------
BEGIN;
INSERT INTO `tbl_user_tag` (`id`, `create_time`, `update_time`, `user_id`, `name`, `type`, `icon`, `sort`, `color`) VALUES (2059793953957003265, '2026-05-28 08:28:42', '2026-05-28 08:28:42', '1872249865963053057', '原子豪', 0, 'https://avatars.githubusercontent.com/u/5251348', 49, 'sky blue');
INSERT INTO `tbl_user_tag` (`id`, `create_time`, `update_time`, `user_id`, `name`, `type`, `icon`, `sort`, `color`) VALUES (2059794036341522433, '2026-05-28 08:29:02', '2026-05-28 08:29:02', '1872249865963053057', '宇开慧', 0, 'https://avatars.githubusercontent.com/u/19958976', 64, 'teal');
INSERT INTO `tbl_user_tag` (`id`, `create_time`, `update_time`, `user_id`, `name`, `type`, `icon`, `sort`, `color`) VALUES (2062743580002725890, '2026-06-05 11:49:28', '2026-06-05 11:49:28', '2048739717880901634', '身份证', 0, NULL, 1, '#7631F5');
INSERT INTO `tbl_user_tag` (`id`, `create_time`, `update_time`, `user_id`, `name`, `type`, `icon`, `sort`, `color`) VALUES (2062825256372383746, '2026-06-05 17:14:01', '2026-06-05 17:14:01', '2048739717880901634', '小说', 0, NULL, 2, '#299A0F');
INSERT INTO `tbl_user_tag` (`id`, `create_time`, `update_time`, `user_id`, `name`, `type`, `icon`, `sort`, `color`) VALUES (2062838466773147649, '2026-06-05 18:06:31', '2026-06-05 18:06:31', '2048739717880901634', '四大名著', 0, NULL, 3, '#029292');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
