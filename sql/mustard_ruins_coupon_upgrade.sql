-- =============================================================
-- 殷墟拾取 + 领券闭环 升级脚本（MySQL）
-- 新增列：
--   1. tbl_coupon.code            优惠券固定业务编码（RUINS_PICKUP = 殷墟拾取券）
--   2. tbl_item_ruins.pickup_user 殷墟拾取者用户ID
--   3. tbl_item_ruins.pickup_time 殷墟拾取时间
-- =============================================================

ALTER TABLE `tbl_coupon`
    ADD COLUMN IF NOT EXISTS `code` VARCHAR(64) NULL DEFAULT NULL COMMENT '优惠券编码（业务固定编码，如RUINS_PICKUP殷墟拾取券）' AFTER `coupon_name`;

ALTER TABLE `tbl_coupon`
    ADD UNIQUE INDEX IF NOT EXISTS `uk_coupon_code` (`code`);

ALTER TABLE `tbl_item_ruins`
    ADD COLUMN IF NOT EXISTS `pickup_user` VARCHAR(64) NULL DEFAULT NULL COMMENT '拾取者用户Id' AFTER `user_id`;

ALTER TABLE `tbl_item_ruins`
    ADD COLUMN IF NOT EXISTS `pickup_time` DATETIME NULL DEFAULT NULL COMMENT '拾取时间' AFTER `pickup_user`;
