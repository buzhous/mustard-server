-- =====================================================================
-- MIP v1 存量库升级 · 回滚脚本（与 mustard_mip_upgrade_v1.sql 配对）
-- 原则：停写窗口执行升级前先跑本文件第 1 节备份；升级失败时执行第 2 节回滚。
-- =====================================================================

-- ---------- 1. 升级前备份（升级脚本已含 IF NOT EXISTS，可重复执行） ----------
CREATE TABLE IF NOT EXISTS `tbl_topic_bak_mip_v1` AS SELECT * FROM `tbl_topic`;
CREATE TABLE IF NOT EXISTS `tbl_item_lineage_bak_mip_v1` AS SELECT * FROM `tbl_item_lineage` WHERE 1=0;

-- ---------- 2. 回滚（仅在升级失败需要退回时执行） ----------
-- 2.1 回滚 tbl_topic.mip_profile（列存在才删）
DROP PROCEDURE IF EXISTS mip_drop_column;
DELIMITER $$
CREATE PROCEDURE mip_drop_column()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tbl_topic' AND COLUMN_NAME = 'mip_profile'
    ) THEN
        ALTER TABLE `tbl_topic` DROP COLUMN `mip_profile`;
    END IF;
END$$
DELIMITER ;
CALL mip_drop_column();
DROP PROCEDURE IF EXISTS mip_drop_column;

-- 2.2 回滚账本表（仅当为空或确认废弃时删除；账本一旦启用禁止随意清空——链少记一天=永久缺失）
-- DROP TABLE IF EXISTS `tbl_item_lineage`;

-- 2.3 fields 数据回滚：tbl_topic_bak_mip_v1 恢复
-- UPDATE `tbl_topic` t JOIN `tbl_topic_bak_mip_v1` b ON t.id = b.id SET t.fields = b.fields;
