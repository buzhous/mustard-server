-- ----------------------------
-- 芥灵AI P2：物品工作台（只读）+ 检索提效基础 + 限流基建
-- 内容：检索索引表 / 别名词表 / AI反馈表 / 账本物品关联列 / 建档来源列 / 后台菜单 / 种子词表
-- 幂等性：新表 IF NOT EXISTS；加列含存在性说明（重复执行如报 Duplicate column 可忽略该条）；菜单固定 id 先删后插
-- ----------------------------

-- ----------------------------
-- 1. 物品检索索引表（ngram 全文检索快照，admin 可异步全量重建）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_item_search_index` (
  `item_id`      varchar(32)  NOT NULL COMMENT '物品Id(tbl_item_info.id)',
  `uid`          varchar(64)  NOT NULL COMMENT '所属用户',
  `norm_name`    varchar(255) DEFAULT NULL COMMENT '归一化名称(trim/全角转半角)',
  `category_norm` varchar(128) DEFAULT NULL COMMENT '归一化分类',
  `tag_norms`    varchar(255) DEFAULT NULL COMMENT '归一化标签(空格分隔)',
  `attr_text`    text         COMMENT '非受限组件属性文本',
  `search_blob`  text         COMMENT '全文检索拼接文本(含别名折叠)',
  `updated_time` datetime     DEFAULT NULL COMMENT '快照更新时间',
  PRIMARY KEY (`item_id`),
  FULLTEXT KEY `ft_search` (`search_blob`) WITH PARSER `ngram`,
  KEY `idx_search_uid` (`uid`),
  KEY `idx_search_category` (`category_norm`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物品检索索引(可重建快照)';

-- ----------------------------
-- 2. 检索别名词表（同义词/上下位，admin 维护走 P4；App 侧只读 + 60s Redis 缓存）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_keyword_alias` (
  `id`          bigint      NOT NULL COMMENT '主键(雪花)',
  `alias`       varchar(64) NOT NULL COMMENT '别名(用户输入词)',
  `standard`    varchar(64) NOT NULL COMMENT '标准词',
  `alias_type`  tinyint     DEFAULT 1 COMMENT '词型:1同义词,2上下位(alias为下位)',
  `enabled`     tinyint     DEFAULT 1 COMMENT '是否启用:1启用,0停用',
  `create_by`   varchar(50)  DEFAULT NULL COMMENT '创建人',
  `create_time` datetime    DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alias` (`alias`, `standard`),
  KEY `idx_alias_standard` (`standard`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检索别名词表';

-- ----------------------------
-- 3. AI 反馈表（用户反馈/观测低命中/人工标记样本，P4 观测页消费）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_ai_feedback` (
  `id`             bigint       NOT NULL COMMENT '主键(雪花)',
  `source_type`    tinyint      DEFAULT 1 COMMENT '来源:1用户反馈,2观测低命中,3人工标记',
  `session_id`     varchar(64)  DEFAULT NULL COMMENT '会话Id',
  `message_id`     varchar(64)  DEFAULT NULL COMMENT '消息Id',
  `uid`            varchar(64)  DEFAULT NULL COMMENT '用户',
  `ref_type`       tinyint      DEFAULT 1 COMMENT '关联对象:1查询,2草稿',
  `ref_content`    text         COMMENT '关联内容(查询词/草稿摘要)',
  `result_summary` varchar(255) DEFAULT NULL COMMENT '结果摘要(命中数/命中来源)',
  `reason`         varchar(255) DEFAULT NULL COMMENT '反馈原因',
  `sample_status`  tinyint      DEFAULT 0 COMMENT '处理状态:0待处理,1已入库,2已忽略',
  `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_uid` (`uid`),
  KEY `idx_feedback_session` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI反馈样本';

-- ----------------------------
-- 4. 账本表补写物品关联列（append-only 哈希链不动：item_id 不参与 eventBody 哈希）
-- ----------------------------
ALTER TABLE `tbl_item_lineage`
  ADD COLUMN `item_id` varchar(32) DEFAULT NULL COMMENT '物品Id(事件关联,不入哈希)',
  ADD KEY `idx_lineage_item_id` (`item_id`);

-- ----------------------------
-- 5. 物品表补建档来源列（manual/ai/mcp/import）
-- ----------------------------
ALTER TABLE `tbl_item_info`
  ADD COLUMN `source` varchar(16) DEFAULT 'manual' COMMENT '建档来源:manual/ai/mcp/import' AFTER `status`,
  ADD KEY `idx_item_source` (`source`);

-- ----------------------------
-- 6. 后台菜单（sys_permission：一级"芥灵AI"，二级"物品管理/物品统计"，按钮权限 ailing:rebuild）
-- 固定 id + 先删后插，保证脚本幂等；组件路径对应 jeecgboot-vue3 src/views/ailing/*
-- ----------------------------
DELETE FROM `sys_permission` WHERE `id` IN ('1942100000000000001', '1942100000000000002', '1942100000000000003', '1942100000000000004');
-- 一级菜单：芥灵AI（menu_type 0）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000001', NULL, '芥灵AI', '/ailing', 'layouts/default/index', 1, NULL, NULL, 0, NULL, '0', 9.00, 0, 'ant-design:robot-outlined', 0, 0, 0, 0, '芥灵AI物品工作台', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
-- 二级菜单：物品管理（menu_type 1，组件 ailing/items/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000002', '1942100000000000001', '物品管理', '/ailing/items', 'ailing/items/index', 1, 'ailing-items', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:appstore-outlined', 1, 0, 0, 0, '物品只读工作台(列表/详情/时间线)', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
-- 二级菜单：物品统计（menu_type 1，组件 ailing/stats/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000003', '1942100000000000001', '物品统计', '/ailing/stats', 'ailing/stats/index', 1, 'ailing-stats', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:bar-chart-outlined', 1, 0, 0, 0, '物品统计分析(分类/标签/等级/趋势/来源)', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
-- 按钮权限：检索索引重建（menu_type 2）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000004', '1942100000000000002', '重建检索索引', NULL, NULL, 0, NULL, NULL, 2, 'ailing:rebuild', '1', 1.00, 0, NULL, 1, 0, 0, 0, '触发物品检索索引异步全量重建', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- ----------------------------
-- 7. 种子词表（首批高频同义词/上下位，固定 id 便于重复执行幂等）
-- alias_type：1=同义词 2=上下位(alias 为下位，standard 为上位/泛化词)
-- ----------------------------
DELETE FROM `tbl_keyword_alias` WHERE `id` BETWEEN 1942100010000000001 AND 1942100010000000099;
INSERT INTO `tbl_keyword_alias` (`id`, `alias`, `standard`, `alias_type`, `enabled`, `create_by`, `create_time`) VALUES
-- 同义词（双向）
(1942100010000000001, '电话',   '手机',       1, 1, 'admin', NOW()),
(1942100010000000002, '手机',   '电话',       1, 1, 'admin', NOW()),
(1942100010000000003, '移动电话', '手机',      1, 1, 'admin', NOW()),
(1942100010000000004, '手提电话', '手机',      1, 1, 'admin', NOW()),
(1942100010000000005, '证件',   '身份证',     1, 1, 'admin', NOW()),
(1942100010000000006, '身份证',  '证件',      1, 1, 'admin', NOW()),
(1942100010000000007, '护照',   '证件',       1, 1, 'admin', NOW()),
(1942100010000000008, '驾照',   '驾驶证',     1, 1, 'admin', NOW()),
(1942100010000000009, '驾驶证',  '证件',      1, 1, 'admin', NOW()),
(1942100010000000010, '充电宝',  '移动电源',   1, 1, 'admin', NOW()),
(1942100010000000011, '移动电源', '充电宝',    1, 1, 'admin', NOW()),
(1942100010000000012, '笔记本',  '笔记本电脑',  1, 1, 'admin', NOW()),
(1942100010000000013, '手提电脑', '笔记本电脑',  1, 1, 'admin', NOW()),
(1942100010000000014, '平板',   '平板电脑',    1, 1, 'admin', NOW()),
(1942100010000000015, '口红',   '唇膏',       1, 1, 'admin', NOW()),
(1942100010000000016, '唇膏',   '口红',       1, 1, 'admin', NOW()),
(1942100010000000017, '粉底',   '粉底液',     1, 1, 'admin', NOW()),
(1942100010000000018, '香水',   '香氛',       1, 1, 'admin', NOW()),
(1942100010000000019, '雨伞',   '伞',        1, 1, 'admin', NOW()),
(1942100010000000020, '伞',     '雨伞',      1, 1, 'admin', NOW()),
(1942100010000000021, '手表',   '腕表',       1, 1, 'admin', NOW()),
(1942100010000000022, '腕表',   '手表',      1, 1, 'admin', NOW()),
(1942100010000000023, '相机',   '照相机',     1, 1, 'admin', NOW()),
(1942100010000000024, '照相机',  '相机',      1, 1, 'admin', NOW()),
(1942100010000000025, '电脑',   '计算机',     1, 1, 'admin', NOW()),
(1942100010000000026, '保温杯',  '保温水杯',   1, 1, 'admin', NOW()),
(1942100010000000027, '充电器',  '电源适配器',  1, 1, 'admin', NOW()),
(1942100010000000028, '钥匙串',  '钥匙扣',    1, 1, 'admin', NOW()),
-- 上下位（alias 为下位 → standard 为上位/泛化词）
(1942100010000000101, '口红',   '化妆品',     2, 1, 'admin', NOW()),
(1942100010000000102, '粉底',   '化妆品',     2, 1, 'admin', NOW()),
(1942100010000000103, '粉底液',  '化妆品',    2, 1, 'admin', NOW()),
(1942100010000000104, '香水',   '化妆品',     2, 1, 'admin', NOW()),
(1942100010000000105, '眼影',   '化妆品',     2, 1, 'admin', NOW()),
(1942100010000000106, '键盘',   '游戏装备',    2, 1, 'admin', NOW()),
(1942100010000000107, '鼠标',   '游戏装备',    2, 1, 'admin', NOW()),
(1942100010000000108, '手柄',   '游戏装备',    2, 1, 'admin', NOW()),
(1942100010000000109, '耳机',   '数码配件',    2, 1, 'admin', NOW()),
(1942100010000000110, '充电宝',  '数码配件',   2, 1, 'admin', NOW()),
(1942100010000000111, '充电器',  '数码配件',   2, 1, 'admin', NOW()),
(1942100010000000112, '数据线',  '数码配件',   2, 1, 'admin', NOW()),
(1942100010000000113, '手机壳',  '数码配件',   2, 1, 'admin', NOW()),
(1942100010000000114, '钢化膜',  '数码配件',   2, 1, 'admin', NOW()),
(1942100010000000115, '上衣',   '服饰',       2, 1, 'admin', NOW()),
(1942100010000000116, '裤子',   '服饰',       2, 1, 'admin', NOW()),
(1942100010000000117, '外套',   '服饰',       2, 1, 'admin', NOW()),
(1942100010000000118, '连衣裙',  '服饰',      2, 1, 'admin', NOW()),
(1942100010000000119, '毛衣',   '服饰',       2, 1, 'admin', NOW()),
(1942100010000000120, 'T恤',    '服饰',      2, 1, 'admin', NOW()),
(1942100010000000121, '运动鞋',  '鞋靴',      2, 1, 'admin', NOW()),
(1942100010000000122, '皮鞋',   '鞋靴',       2, 1, 'admin', NOW()),
(1942100010000000123, '凉鞋',   '鞋靴',       2, 1, 'admin', NOW()),
(1942100010000000124, '靴子',   '鞋靴',       2, 1, 'admin', NOW()),
(1942100010000000128, '社保卡',  '证件',      2, 1, 'admin', NOW()),
(1942100010000000129, '银行卡',  '证件',      2, 1, 'admin', NOW()),
(1942100010000000130, '遥控器',  '家电配件',   2, 1, 'admin', NOW()),
(1942100010000000131, '电池',   '家电配件',    2, 1, 'admin', NOW());

-- ----------------------------
-- 8. 后台菜单：芥灵过程（P3 会话可视化，sys_permission：二级"芥灵过程"，按钮权限 ailing:session:view）
-- 固定 id + 先删后插，保证脚本幂等；组件路径对应 jeecgboot-vue3 src/views/ailing/process/*
-- 角色分配：会话回放仅客服/运营角色可见——超级管理员天然全量可见；其他角色通过
-- 系统管理-角色管理勾选"芥灵过程"菜单与"会话回放查看"按钮授权（sys_role_permission），此处不写死角色
-- ----------------------------
DELETE FROM `sys_permission` WHERE `id` IN ('1942100000000000005', '1942100000000000006');
-- 二级菜单：芥灵过程（menu_type 1，组件 ailing/process/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000005', '1942100000000000001', '芥灵过程', '/ailing/process', 'ailing/process/index', 1, 'ailing-process', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:message-outlined', 1, 0, 0, 0, 'AI会话可视化(会话列表/回放/质量看板)', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
-- 按钮权限：会话回放查看（menu_type 2）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000006', '1942100000000000005', '会话回放查看', NULL, NULL, 0, NULL, NULL, 2, 'ailing:session:view', '1', 1.00, 0, NULL, 1, 0, 0, 0, '打开会话详情回放(仅客服/运营角色)', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- dev 环境角色映射（幂等）：admin 管理员角色绑定 /ailing 全菜单树（001 一级 + 002/003/005/007 二级 + 004/006/008 按钮）
-- 注意：动态菜单按 sys_role_permission 组装，一级/二级菜单必须同样映射否则整棵树不下发；
-- 服务端 @RequiresPermissions("ailing:session:view"/"ailing:tune:edit") 生效依赖此映射；映射变更后需清除
-- Redis Shiro 授权缓存（shiro:cache:org.jeecg.config.shiro.ShiroRealm.authorizationCache:<用户id>）后重新登录
DELETE FROM `sys_role_permission` WHERE `permission_id` IN ('1942100000000000001', '1942100000000000002', '1942100000000000003', '1942100000000000004', '1942100000000000005', '1942100000000000006', '1942100000000000007', '1942100000000000008') AND `role_id` = 'f6817f48af4fb3af11b9e8bf182f618b';
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`) VALUES
('1942100200000000001', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000001'),
('1942100200000000002', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000002'),
('1942100200000000003', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000003'),
('1942100200000000004', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000004'),
('1942100200000000005', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000005'),
('1942100200000000006', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000006'),
('1942100200000000007', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000007'),
('1942100200000000008', 'f6817f48af4fb3af11b9e8bf182f618b', '1942100000000000008');

-- ----------------------------
-- 9. P4 调优闭环：AI 调优配置表 + 后台菜单「调优中心」（sys_permission：二级"调优中心"，按钮权限 ailing:tune:edit）
-- 版本历史同表多版本：active=1 当前生效，active=0 历史版本；enabled=0 整 key 停用回退内置技能包/默认值
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_ai_tuning_config` (
  `id`           bigint       NOT NULL COMMENT '主键(雪花)',
  `config_key`   varchar(64)  NOT NULL COMMENT '配置键(skill.filing.prompt/skill.query.prompt/model.chat_model/model.temperature/agent.max_asks/search.alias_enabled/search.index_enabled/search.cache_enabled/search.cache_ttl_seconds)',
  `config_value` text         COMMENT '配置值',
  `value_type`   varchar(16)  DEFAULT 'string' COMMENT '值类型:string/number/bool',
  `version`      int          NOT NULL DEFAULT 1 COMMENT '版本号(同 key 递增)',
  `enabled`      tinyint      DEFAULT 1 COMMENT 'DB配置是否生效:1生效,0停用(回退内置)',
  `active`       tinyint      DEFAULT 1 COMMENT '是否当前生效版本:1是,0历史',
  `remark`       varchar(255) DEFAULT NULL COMMENT '备注(操作说明/审计)',
  `update_by`    varchar(50)  DEFAULT NULL COMMENT '更新人',
  `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_version` (`config_key`, `version`),
  KEY `idx_key_active` (`config_key`, `active`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI调优配置(提示词在线编辑/模型参数/检索开关,版本历史同表)';

-- 后台菜单（幂等）：二级"调优中心" + 按钮权限 ailing:tune:edit
DELETE FROM `sys_permission` WHERE `id` IN ('1942100000000000007', '1942100000000000008');
-- 二级菜单：调优中心（menu_type 1，组件 ailing/tune/index）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000007', '1942100000000000001', '调优中心', '/ailing/tune', 'ailing/tune/index', 1, 'ailing-tune', NULL, 1, NULL, '0', 4.00, 0, 'ant-design:tool-outlined', 1, 0, 0, 0, '调优闭环(提示词在线编辑/兜底链观测/样本固化/词表维护/参数下发)', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
-- 按钮权限：调优编辑（menu_type 2）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('1942100000000000008', '1942100000000000007', '调优编辑', NULL, NULL, 0, NULL, NULL, 2, 'ailing:tune:edit', '1', 1.00, 0, NULL, 1, 0, 0, 0, '提示词保存/回滚/参数下发/词表维护/样本处置(限管理员角色)', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
