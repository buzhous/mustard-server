-- =====================================================================
-- MIP v1 芥子物品档案协议 · 存量库升级脚本（协议 10.4 T3/T5，2026-11-20）
-- 执行窗口：凌晨 02:00-05:00 停写窗口（升级顺序铁律见协议 10.1）
-- 执行前：先运行 mustard_mip_upgrade_v1_rollback.sql 中的备份语句
-- 幂等性：ALTER 带 IF 判断的用存储过程包裹，可重复执行
-- 说明：fields 组件 sensitivity 补级与等级列对齐由 Java 迁移接口执行
--       （POST /app/mip/admin/upgradeItemFields，可重复调用），
--       content_hash 不落库（出域序列化时由 MipHashUtil 实时计算）。
-- =====================================================================

-- ---------- 1. 备份（回滚依据，先于一切执行） ----------
CREATE TABLE IF NOT EXISTS `tbl_topic_bak_mip_v1` AS SELECT * FROM `tbl_topic`;
CREATE TABLE IF NOT EXISTS `tbl_item_info_bak_mip_v1` AS SELECT * FROM `tbl_item_info` WHERE 1=0;  -- 结构备份：fields 升级幂等可重算，不复制数据
CREATE TABLE IF NOT EXISTS `tbl_item_sync_bak_mip_v1` AS SELECT * FROM `tbl_item_sync` WHERE 1=0;

-- ---------- 2. tbl_topic 增加 mip_profile 列（幂等） ----------
DROP PROCEDURE IF EXISTS mip_add_column;
DELIMITER $$
CREATE PROCEDURE mip_add_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tbl_topic' AND COLUMN_NAME = 'mip_profile'
    ) THEN
        ALTER TABLE `tbl_topic`
            ADD COLUMN `mip_profile` TEXT NULL COMMENT 'MIP v1 Profile文档JSON(保存时自动生成)' AFTER `fields`;
    END IF;
END$$
DELIMITER ;
CALL mip_add_column();
DROP PROCEDURE IF EXISTS mip_add_column;

-- ---------- 3. 物品流转账本 tbl_item_lineage（协议 5.1 + 10.3 缺陷①②） ----------
CREATE TABLE IF NOT EXISTS `tbl_item_lineage` (
  `id`              BIGINT       NOT NULL COMMENT '主键(雪花)',
  `ori_id`          VARCHAR(64)  NOT NULL COMMENT '物品全局唯一标识',
  `event_type`      VARCHAR(20)  NOT NULL COMMENT 'mint/gift_out/gift_in/ruins_drop/ruins_pick/transfer/spawn/destroy/recycle/import',
  `from_uid`        VARCHAR(64)  DEFAULT NULL COMMENT '流出方用户哈希',
  `to_uid`          VARCHAR(64)  DEFAULT NULL COMMENT '流入方用户哈希',
  `from_station`    VARCHAR(64)  DEFAULT NULL COMMENT '流出方仓位',
  `to_station`      VARCHAR(64)  DEFAULT NULL COMMENT '流入方仓位',
  `operator`        VARCHAR(30)  NOT NULL COMMENT 'user/system/agent:{appKey尾4位}',
  `payload`         JSON         DEFAULT NULL COMMENT '事件摘要,禁止fields内容',
  `qty`             INT          DEFAULT NULL COMMENT '事件涉及数量(数量拆分场景)',
  `parent_ori`      VARCHAR(64)  DEFAULT NULL COMMENT '拆分派生时的父物品oriId(spawn事件)',
  `idempotency_key` VARCHAR(128) DEFAULT NULL COMMENT '幂等键:ori_id+event_type+业务单号',
  `occurred_at`     DATETIME     NOT NULL COMMENT '事件发生时间',
  `prev_hash`       CHAR(64)     NOT NULL COMMENT '前一事件哈希',
  `hash`            CHAR(64)     NOT NULL COMMENT '本事件哈希',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idem` (`idempotency_key`),
  KEY `idx_ori` (`ori_id`, `occurred_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'MIP v1 物品流转账本(append-only:只INSERT,禁止UPDATE/DELETE)';
-- 排序规则注意：必须与既有业务表(utf8mb4_general_ci)一致，否则跨表字符串 JOIN 报 Illegal mix of collations；
-- 若已按默认排序规则建表，执行：ALTER TABLE tbl_item_lineage CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ---------- 4. 执行结果自检 ----------
SELECT 'tbl_topic.mip_profile' AS step,
       COUNT(*) AS has_column
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tbl_topic' AND COLUMN_NAME = 'mip_profile';

SHOW TABLES LIKE 'tbl_item_lineage';
