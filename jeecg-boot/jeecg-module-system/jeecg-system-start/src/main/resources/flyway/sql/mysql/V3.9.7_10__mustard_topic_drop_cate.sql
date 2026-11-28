-- 主题管理移除「主题分类」字段：topic_cate 无任何业务消费
-- （服务端无查询/过滤使用，App 端 TopicSummary 不解析，管理端表单项同步移除）
-- 物品分类 item_cate 保留不动
-- 幂等：列不存在时跳过（MySQL 8.0 不支持 IF EXISTS，用存储过程兼容重复执行）
DELIMITER $$
DROP PROCEDURE IF EXISTS `mst_drop_topic_cate` $$
CREATE PROCEDURE `mst_drop_topic_cate`()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'tbl_topic'
               AND COLUMN_NAME = 'topic_cate') THEN
    ALTER TABLE `tbl_topic` DROP COLUMN `topic_cate`;
  END IF;
END $$
DELIMITER ;
CALL `mst_drop_topic_cate`();
DROP PROCEDURE IF EXISTS `mst_drop_topic_cate`;
