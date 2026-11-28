-- 物品管理重构（v2.5）· 需求2前置：用户物品数据全量清空
-- 策略：不维护旧数据，直接清空替换（App 强制升级 + 本地库重置，重登自动恢复）
-- 执行前置说明：
--   1) 每张表清空前先建 *_bak_20261127 备份表（CREATE ... AS SELECT），可整体回滚；
--   2) 脚本幂等：备份表已存在则跳过备份，DELETE 重复执行为空操作；
--   3) 重登后由 AppLoginServiceImpl 自动重建：用户虚拟仓点 / 默认真实仓 / 资金账户均保留或自动补建。
-- 保留范围（不在本脚本）：tbl_user / tbl_user_amount / tbl_points_flow / tbl_user_category /
--   tbl_user_tag / tbl_system_category / tbl_field_element / tbl_coupon / tbl_user_coupon /
--   tbl_vip_order / tbl_recharge_order / tbl_warehouse_station（平台收寄点，平台统一运维）/
--   tbl_stats_* / tbl_ai_feedback / tbl_factory_info 等。

-- ==================== 备份（可回滚） ====================
CREATE TABLE IF NOT EXISTS `tbl_item_info_bak_20261127`            AS SELECT * FROM `tbl_item_info`;
CREATE TABLE IF NOT EXISTS `tbl_item_sync_bak_20261127`            AS SELECT * FROM `tbl_item_sync`;
CREATE TABLE IF NOT EXISTS `tbl_item_recycle_bak_20261127`         AS SELECT * FROM `tbl_item_recycle`;
CREATE TABLE IF NOT EXISTS `tbl_item_ruins_bak_20261127`           AS SELECT * FROM `tbl_item_ruins`;
CREATE TABLE IF NOT EXISTS `tbl_item_gift_bak_20261127`            AS SELECT * FROM `tbl_item_gift`;
CREATE TABLE IF NOT EXISTS `tbl_item_gift_receive_bak_20261127`    AS SELECT * FROM `tbl_item_gift_receive`;
CREATE TABLE IF NOT EXISTS `tbl_user_give_record_bak_20261127`     AS SELECT * FROM `tbl_user_give_record`;
CREATE TABLE IF NOT EXISTS `tbl_item_lineage_bak_20261127`         AS SELECT * FROM `tbl_item_lineage`;
CREATE TABLE IF NOT EXISTS `tbl_item_storage_in_bak_20261127`      AS SELECT * FROM `tbl_item_storage_in`;
CREATE TABLE IF NOT EXISTS `tbl_kuaidi_order_bak_20261127`         AS SELECT * FROM `tbl_kuaidi_order`;
CREATE TABLE IF NOT EXISTS `tbl_user_storage_bak_20261127`         AS SELECT * FROM `tbl_user_storage`;
CREATE TABLE IF NOT EXISTS `tbl_user_station_bind_bak_20261127`    AS SELECT * FROM `tbl_user_station_bind`;
CREATE TABLE IF NOT EXISTS `tbl_user_virtual_station_bak_20261127` AS SELECT * FROM `tbl_user_virtual_station`;
CREATE TABLE IF NOT EXISTS `tbl_item_search_index_bak_20261127`    AS SELECT * FROM `tbl_item_search_index`;
CREATE TABLE IF NOT EXISTS `tbl_item_user_inventory_bak_20261127`  AS SELECT * FROM `tbl_item_user_inventory`;
CREATE TABLE IF NOT EXISTS `tbl_ai_chat_session_bak_20261127`      AS SELECT * FROM `tbl_ai_chat_session`;
CREATE TABLE IF NOT EXISTS `tbl_ai_chat_message_bak_20261127`      AS SELECT * FROM `tbl_ai_chat_message`;
CREATE TABLE IF NOT EXISTS `tbl_item_topic_bak_20261127` AS SELECT * FROM `tbl_item_topic`; -- 补：物品主题关联（主题重建后旧关联失效）

-- ==================== 清空（物品本体与全部级联） ====================
-- 物品本体 + 同步队列
DELETE FROM `tbl_item_info`;
DELETE FROM `tbl_item_sync`;
-- 回收站 / 遗物（殷墟）
DELETE FROM `tbl_item_recycle`;
DELETE FROM `tbl_item_ruins`;
-- 赠予（赠予单 / 领取记录 / 赠予行为记录）
DELETE FROM `tbl_item_gift`;
DELETE FROM `tbl_item_gift_receive`;
DELETE FROM `tbl_user_give_record`;
-- 物品流转账本（MIP 5.1）
DELETE FROM `tbl_item_lineage`;
-- 仓储入库 / 寄件订单（快递单关联入库记录）
DELETE FROM `tbl_item_storage_in`;
DELETE FROM `tbl_kuaidi_order`;
-- 用户仓位（自建真实仓 / 仓位绑定 / 虚拟仓点；重登自动重建）
DELETE FROM `tbl_user_storage`;
DELETE FROM `tbl_user_station_bind`;
DELETE FROM `tbl_user_virtual_station`;
-- 检索索引（由物品落库链重建）
DELETE FROM `tbl_item_search_index`;
-- 物品库存
DELETE FROM `tbl_item_user_inventory`;
-- 芥灵AI 会话与消息（草稿引用已清空物品，历史会话不做迁移，失效直接重建）
DELETE FROM `tbl_ai_chat_message`;
DELETE FROM `tbl_ai_chat_session`;
-- 补：物品主题关联（主题重建后旧关联失效）
DELETE FROM `tbl_item_topic`;
