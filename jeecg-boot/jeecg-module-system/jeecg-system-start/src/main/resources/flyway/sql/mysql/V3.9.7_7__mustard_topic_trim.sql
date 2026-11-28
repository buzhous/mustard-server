-- 物品添加入口（主题列表）调整：移除「未分类」「全量字段」两个主题
-- 未分类主题：App 无对应建档场景，兜底分类仍由系统分类 sys_cat_0（tbl_system_category）承担，不受影响
-- 全量字段主题：改为 App 端本地固定卡（AI 建档入口右侧），不再由接口下发
-- 幂等：按 topic_code 删除，重复执行结果一致
DELETE FROM `tbl_topic` WHERE `topic_code` IN ('cat_0', 'full_fields');
