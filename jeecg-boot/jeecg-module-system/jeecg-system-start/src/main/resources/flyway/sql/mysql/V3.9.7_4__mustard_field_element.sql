-- 物品管理重构（v2.5）· 需求1：字段字典基础模块
-- tbl_field_element：App / 服务端 / 后台三端共用的唯一字段数据源
-- 内置种子 34 条（与现有 App/主题 fields 规范完全对齐），运营可扩展新增（35 起任意未占用正整数）
-- 必填三要素：element 1 图标 / 2 名称 / 3 类别
-- 端实现标记：android_flag / ios_flag —— 勾选 = 该端已实现渲染，未勾选端 App 初始化不加载该字段
-- 已录入字段一律不可删除，仅支持停用/启用

CREATE TABLE IF NOT EXISTS `tbl_field_element` (
    `id`           varchar(36)  NOT NULL COMMENT '主键（种子 fld_el_1..fld_el_34，运营新增 ASSIGN_ID）',
    `element`      int          NOT NULL COMMENT '组件编号，全局唯一；内置 1-34，扩展 35+ 任意未占用正整数',
    `name`         varchar(64)  NOT NULL COMMENT '字段名称（中文）',
    `field_key`    varchar(64)  NOT NULL COMMENT '字段标识（英文驼峰，与物品 fields JSON 的 key 对应）',
    `input_type`   varchar(32)  NOT NULL COMMENT '输入类型：内置 11 种（文本/数字/日期/日期区间/选择/媒体/文件/组/富文本/定位/模型），可扩展；未知类型 App 按文本兜底渲染',
    `is_required`  tinyint      NOT NULL DEFAULT 0 COMMENT '是否必填：1是 0否（名称/图标/类别固定必填）',
    `sort`         int          NOT NULL DEFAULT 0 COMMENT '默认排序（App 表单/详情渲染顺序，扩展字段排末尾）',
    `android_flag` tinyint      NOT NULL DEFAULT 1 COMMENT '安卓端实现标记：1已实现 0未实现（未勾选端 App 初始化不加载）',
    `ios_flag`     tinyint      NOT NULL DEFAULT 1 COMMENT 'iOS 端实现标记：同上',
    `status`       tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用（不提供删除）',
    `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_field_element_element` (`element`),
    UNIQUE KEY `uk_field_element_field_key` (`field_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字段字典（三端共用唯一字段数据源，独立基础服务缓存下发）';

-- 34 条字典种子（幂等：element 唯一键，重复执行忽略；sort 按既定展示顺序 1-34）
INSERT IGNORE INTO `tbl_field_element`
(`id`, `element`, `name`, `field_key`, `input_type`, `is_required`, `sort`, `android_flag`, `ios_flag`, `status`, `remark`) VALUES
('fld_el_1',  1,  '图标',       'icon',                '媒体',   1, 1,  1, 1, 1, '必填三要素；物品主图'),
('fld_el_2',  2,  '名称',       'name',                '文本',   1, 2,  1, 1, 1, '必填三要素；物品名称'),
('fld_el_3',  3,  '类别',       'category',            '选择',   1, 3,  1, 1, 1, '必填三要素；所属分类'),
('fld_el_4',  4,  '等级',       'level',               '选择',   0, 4,  1, 1, 1, NULL),
('fld_el_5',  10, '标签组',     'tags',                '组',     0, 5,  1, 1, 1, NULL),
('fld_el_6',  5,  '购入时间',   'purchaseTime',        '日期',   0, 6,  1, 1, 1, NULL),
('fld_el_7',  6,  '价格',       'price',               '数字',   0, 7,  1, 1, 1, NULL),
('fld_el_8',  7,  '数量',       'quantity',            '数字',   0, 8,  1, 1, 1, NULL),
('fld_el_9',  18, '定位',       'location',            '定位',   0, 9,  1, 1, 1, NULL),
('fld_el_10', 17, '密码',       'password',            '文本',   0, 10, 1, 1, 1, NULL),
('fld_el_11', 21, '证件号',     'certNo',              '文本',   0, 11, 1, 1, 1, NULL),
('fld_el_12', 22, '有效期范围', 'validDate',           '日期区间', 0, 12, 1, 1, 1, NULL),
('fld_el_13', 11, '属性组',     'attributeGroup',      '组',     0, 13, 1, 1, 1, NULL),
('fld_el_14', 9,  '特殊描述',   'specialDesc',         '文本',   0, 14, 1, 1, 1, NULL),
('fld_el_15', 8,  '描述',       'description',         '文本',   0, 15, 1, 1, 1, NULL),
('fld_el_16', 19, '富文本',     'richText',            '富文本', 0, 16, 1, 1, 1, NULL),
('fld_el_17', 12, '图片',       'images',              '媒体',   0, 17, 1, 1, 1, NULL),
('fld_el_18', 13, '视频',       'videos',              '媒体',   0, 18, 1, 1, 1, NULL),
('fld_el_19', 14, '文图视附件', 'attachments',         '媒体',   0, 19, 1, 1, 1, NULL),
('fld_el_20', 15, '附件',       'fileAttachments',     '文件',   0, 20, 1, 1, 1, NULL),
('fld_el_21', 16, '电子书',     'ebooks',              '文件',   0, 21, 1, 1, 1, NULL),
('fld_el_22', 20, '3D模型',     'threeDModel',         '模型',   0, 22, 1, 1, 1, NULL),
('fld_el_23', 23, '发行方平台', 'issuerPlatform',      '文本',   0, 23, 1, 1, 1, NULL),
('fld_el_24', 24, '权益有效期', 'benefitValidDate',    '日期区间', 0, 24, 1, 1, 1, NULL),
('fld_el_25', 25, '账号标识',   'accountIdentifier',   '文本',   0, 25, 1, 1, 1, NULL),
('fld_el_26', 26, '兑换码',     'redemptionCode',      '文本',   0, 26, 1, 1, 1, NULL),
('fld_el_27', 27, '链上凭证',   'chainCredential',     '文本',   0, 27, 1, 1, 1, NULL),
('fld_el_28', 28, '云端存放位置', 'cloudStorageLocation', '文本', 0, 28, 1, 1, 1, NULL),
('fld_el_29', 29, '品牌型号',   'brandModel',          '文本',   0, 29, 1, 1, 1, NULL),
('fld_el_30', 30, '序列号SN',   'serialNumber',        '文本',   0, 30, 1, 1, 1, NULL),
('fld_el_31', 31, '成色评级',   'conditionRating',     '选择',   0, 31, 1, 1, 1, NULL),
('fld_el_32', 32, '保修信息',   'warrantyInfo',        '文本',   0, 32, 1, 1, 1, NULL),
('fld_el_33', 33, '购买凭证',   'purchaseProof',       '媒体',   0, 33, 1, 1, 1, NULL),
('fld_el_34', 34, '维护记录',   'maintenanceRecord',   '文本',   0, 34, 1, 1, 1, NULL);

-- ----------------------------
-- 后台菜单：基础配置 > 字段管理 + 系统分类（sys_permission：固定 id + 先删后插，保证脚本幂等）
-- ----------------------------
DELETE FROM `sys_permission` WHERE `id` IN (
    '1942000000000000101', '1942000000000000102', '1942000000000000103',
    '1942000000000000104', '1942000000000000105', '1942000000000000106',
    '1942000000000000107', '1942000000000000108', '1942000000000000109');
-- 一级菜单：基础配置（menu_type 0）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000101', NULL, '基础配置', '/base', 'layouts/default/index', 1, NULL, NULL, 0, NULL, '0', 9.00, 0, 'ant-design:setting-outlined', 0, 0, 0, 0, '字段字典与系统分类等基础数据管理', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 二级菜单：字段管理（menu_type 1，组件 base/field/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000102', '1942000000000000101', '字段管理', '/base/field', 'base/field/index', 1, 'base-field', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:table-outlined', 1, 0, 0, 0, '字段字典管理（内置34+扩展，按端下发，不可删除仅停用）', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 二级菜单：系统分类（menu_type 1，组件 base/category/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000103', '1942000000000000101', '系统分类', '/base/category', 'base/category/index', 1, 'base-category', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:appstore-outlined', 1, 0, 0, 0, '系统物品分类治理（C3 分类治理）', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 字段管理按钮权限（menu_type 2）：新增 / 编辑 / 启停（无删除）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000104', '1942000000000000102', '新增字段', NULL, NULL, 0, NULL, NULL, 2, 'base:fieldElement:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000105', '1942000000000000102', '编辑字段', NULL, NULL, 0, NULL, NULL, 2, 'base:fieldElement:edit', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000106', '1942000000000000102', '启停字段', NULL, NULL, 0, NULL, NULL, 2, 'base:fieldElement:status', '1', 3.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 系统分类按钮权限（menu_type 2）：新增 / 编辑 / 启停（无删除）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000107', '1942000000000000103', '新增系统分类', NULL, NULL, 0, NULL, NULL, 2, 'base:systemCategory:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000108', '1942000000000000103', '编辑系统分类', NULL, NULL, 0, NULL, NULL, 2, 'base:systemCategory:edit', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000109', '1942000000000000103', '启停系统分类', NULL, NULL, 0, NULL, NULL, 2, 'base:systemCategory:status', '1', 3.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
