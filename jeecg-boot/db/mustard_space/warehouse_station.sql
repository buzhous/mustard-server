-- ----------------------------
-- 实物云仓 - 仓储站点表（平台收寄点，平台统一运维，App 端只读）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_warehouse_station` (
  `id`                 varchar(36)    NOT NULL COMMENT '主键',
  `create_time`        datetime       DEFAULT NULL COMMENT '创建时间',
  `update_time`        datetime       DEFAULT NULL COMMENT '修改时间',
  `station_code`       varchar(64)    DEFAULT NULL COMMENT '站点编码',
  `station_name`       varchar(128)   DEFAULT NULL COMMENT '站点名称',
  `province`           varchar(64)    DEFAULT NULL COMMENT '省份(行政区域编码,如410000)',
  `city`               varchar(64)    DEFAULT NULL COMMENT '城市(行政区域编码,如410500)',
  `district`           varchar(64)    DEFAULT NULL COMMENT '区县(行政区域编码,如410506)',
  `address`            varchar(255)   DEFAULT NULL COMMENT '详细地址',
  `longitude`          decimal(10, 6) DEFAULT NULL COMMENT '经度',
  `latitude`           decimal(10, 6) DEFAULT NULL COMMENT '纬度',
  `cover_area`         varchar(255)   DEFAULT NULL COMMENT '覆盖城市/区域，逗号分隔',
  `storage_categories` varchar(512)   DEFAULT NULL COMMENT '可存储品类，逗号分隔',
  `warehouse_standard` varchar(512)   DEFAULT NULL COMMENT '仓储标准说明',
  `fee_standard`       text           DEFAULT NULL COMMENT '收费标准 JSON（按时长/按体积）',
  `delivery_scope`     varchar(255)   DEFAULT NULL COMMENT '提货配送覆盖范围',
  `pickup_scope`       varchar(255)   DEFAULT NULL COMMENT '上门取件覆盖范围',
  `estimate_days`      int            DEFAULT NULL COMMENT '预计时效（天）',
  `total_capacity`     int            DEFAULT 0 COMMENT '库存总容量（件）',
  `used_capacity`      int            DEFAULT 0 COMMENT '已用容量（件）',
  `business_hours`     varchar(128)   DEFAULT NULL COMMENT '营业时间',
  `contact_phone`      varchar(32)    DEFAULT NULL COMMENT '联系电话',
  `station_image`      varchar(512)   DEFAULT NULL COMMENT '站点图片',
  `status`             tinyint        DEFAULT 1 COMMENT '状态:0停用,1开放',
  `sort`               int            DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_station_code` (`station_code`),
  KEY `idx_station_city` (`city`),
  KEY `idx_station_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '实物云仓-仓储站点(平台收寄点)';

-- ----------------------------
-- 实物云仓 - 用户站点绑定表（普通用户最多绑定1个，VIP最多3个）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_user_station_bind` (
  `id`          varchar(36)  NOT NULL COMMENT '主键',
  `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime     DEFAULT NULL COMMENT '修改时间',
  `user_id`     varchar(64)  NOT NULL COMMENT '用户Id',
  `station_id`  varchar(36)  NOT NULL COMMENT '站点Id',
  `is_default`  tinyint      DEFAULT 0 COMMENT '是否主站点:0否,1是',
  `status`      tinyint      DEFAULT 1 COMMENT '状态:0已解绑,1已绑定',
  `bind_time`   datetime     DEFAULT NULL COMMENT '绑定时间',
  `unbind_time` datetime     DEFAULT NULL COMMENT '解绑时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_station` (`user_id`, `station_id`),
  KEY `idx_user_bind` (`user_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '实物云仓-用户站点绑定';

-- ----------------------------
-- 初始化示例站点数据（平台运维侧后续通过管理端维护）
-- ----------------------------
INSERT INTO `tbl_warehouse_station`
(`id`, `create_time`, `update_time`, `station_code`, `station_name`, `province`, `city`, `district`, `address`,
 `longitude`, `latitude`, `cover_area`, `storage_categories`, `warehouse_standard`, `fee_standard`,
 `delivery_scope`, `pickup_scope`, `estimate_days`, `total_capacity`, `used_capacity`, `business_hours`,
 `contact_phone`, `station_image`, `status`, `sort`)
VALUES ('1940000000000000001', NOW(), NOW(), 'STN-DEMO-001', '殷墟示范仓（安阳）', '410000', '410500', '410505',
        '示范路1号芥子空间仓储园A区', 114.305250, 36.128600, '安阳市全域', '居家用品,服饰鞋帽,数码家电,书籍文具,礼品藏品',
        '恒温恒湿，独立防尘柜位，贵重物品专属保险区',
        '{"month":{"小件":20,"中件":45,"大件":90,"整箱":120},"quarter":{"小件":50,"中件":120,"大件":240,"整箱":320},"year":{"小件":180,"中件":420,"大件":840,"整箱":1100}}',
        '安阳市区三环内免费配送', '安阳市区三环内免费上门取件', 2, 5000, 0, '09:00-18:00', '400-000-0000', NULL, 1, 1)
ON DUPLICATE KEY UPDATE `update_time` = NOW();

-- ----------------------------
-- 实物云仓 - 后台菜单配置（sys_permission：一级菜单"实物云仓"，二级菜单"仓储站点"，按钮权限）
-- 固定 id + 先删后插，保证脚本幂等
-- ----------------------------
DELETE FROM `sys_permission` WHERE `id` IN ('1941000000000000001', '1941000000000000002', '1941000000000000003', '1941000000000000004', '1941000000000000005');
-- 一级菜单：实物云仓（menu_type 0）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1941000000000000001', NULL, '实物云仓', '/warehouse', 'layouts/default/index', 1, NULL, NULL, 0, NULL, '0', 9.00, 0, 'ant-design:database-outlined', 0, 0, 0, 0, '实物云仓管理', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 二级菜单：仓储站点（menu_type 1，组件 warehouse/station/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1941000000000000002', '1941000000000000001', '仓储站点', '/warehouse/station', 'warehouse/station/index', 1, 'warehouse-station', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:home-outlined', 1, 0, 0, 0, '仓储站点管理', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 按钮权限（menu_type 2）：新增 / 编辑 / 删除
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1941000000000000003', '1941000000000000002', '新增站点', NULL, NULL, 0, NULL, NULL, 2, 'warehouse:station:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1941000000000000004', '1941000000000000002', '编辑站点', NULL, NULL, 0, NULL, NULL, 2, 'warehouse:station:edit', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1941000000000000005', '1941000000000000002', '删除站点', NULL, NULL, 0, NULL, NULL, 2, 'warehouse:station:delete', '1', 3.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
