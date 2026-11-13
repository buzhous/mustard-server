-- ----------------------------
-- 实物云仓 - 用户虚拟仓库点表
-- 规则：普通用户固定拥有 1 个虚拟仓库点 + 1 个真实仓库点（槽位不随 VIP 扩容）
-- 虚拟仓库点仅包含编号与位置说明，位置说明默认为平台 OSS 位置
-- 新用户注册/首次登录时惰性初始化；物品新增未指定仓位时自动落入默认仓位
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbl_user_virtual_station` (
  `id`            varchar(36)  NOT NULL COMMENT '主键',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_time`   datetime     DEFAULT NULL COMMENT '修改时间',
  `user_id`       varchar(64)  NOT NULL COMMENT '用户Id',
  `station_no`    varchar(64)  DEFAULT NULL COMMENT '虚拟仓编号(如 VS10000001)',
  `location_desc` varchar(255) DEFAULT NULL COMMENT '位置说明(默认:芥子空间平台 OSS 仓库)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_virtual_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '实物云仓-用户虚拟仓库点';

-- ----------------------------
-- 物品表扩展：仓位归属（station_type 1=虚拟仓 2=真实站点仓）
-- 未指定仓位的新增物品由服务端自动落入用户默认仓位（优先主站点，其次虚拟仓）
-- ----------------------------
ALTER TABLE `tbl_item_info`
  ADD COLUMN `station_id` varchar(36) DEFAULT NULL COMMENT '仓位Id(虚拟仓Id或站点Id)' AFTER `user_id`,
  ADD COLUMN `station_type` tinyint DEFAULT NULL COMMENT '仓位类型:1虚拟仓,2真实站点仓' AFTER `station_id`,
  ADD KEY `idx_item_station` (`station_id`, `station_type`);
