-- ----------------------------
-- 添加物品主题管理 - tbl_topic 表扩展（子标题、背景图）
-- 主题字段配置 fields 规范：[{element(1-20), name, sort, isShow, attributes, content}]
-- 名称(1)/图标(2)/类别(3) 为必填三要素；主题类型(topic_type)由后端根据 fields 自动推导
-- 推导规则：3模型型(含20) > 2多媒体型(含12/13/14/15) > 4扩展型(含17/18/19) > 1基础型
-- ----------------------------
ALTER TABLE `tbl_topic` ADD COLUMN `topic_subtitle` varchar(200) DEFAULT NULL COMMENT '子标题' AFTER `topic_name`;
ALTER TABLE `tbl_topic` ADD COLUMN `bg_image` varchar(500) DEFAULT NULL COMMENT '背景图' AFTER `topic_icon`;

-- ----------------------------
-- 主题管理 - 后台菜单配置（sys_permission：一级菜单"主题管理"，二级菜单"添加物品主题"，按钮权限）
-- 固定 id + 先删后插，保证脚本幂等
-- ----------------------------
DELETE FROM `sys_permission` WHERE `id` IN ('1942000000000000001', '1942000000000000002', '1942000000000000003', '1942000000000000004', '1942000000000000005', '1942000000000000006');
-- 一级菜单：主题管理（menu_type 0）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000001', NULL, '主题管理', '/topic', 'layouts/default/index', 1, NULL, NULL, 0, NULL, '0', 10.00, 0, 'ant-design:appstore-outlined', 0, 0, 0, 0, '添加物品主题管理', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 二级菜单：添加物品主题（menu_type 1，组件 topic/admin/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000002', '1942000000000000001', '添加物品主题', '/topic/admin', 'topic/admin/index', 1, 'topic-admin', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:profile-outlined', 1, 0, 0, 0, '添加物品主题配置', 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
-- 按钮权限（menu_type 2）：新增 / 编辑 / 删除
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000003', '1942000000000000002', '新增主题', NULL, NULL, 0, NULL, NULL, 2, 'topic:admin:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000004', '1942000000000000002', '编辑主题', NULL, NULL, 0, NULL, NULL, 2, 'topic:admin:edit', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000005', '1942000000000000002', '删除主题', NULL, NULL, 0, NULL, NULL, 2, 'topic:admin:delete', '1', 3.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942000000000000006', '1942000000000000002', '批量删除主题', NULL, NULL, 0, NULL, NULL, 2, 'topic:admin:deleteBatch', '1', 4.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, NULL, 0);
