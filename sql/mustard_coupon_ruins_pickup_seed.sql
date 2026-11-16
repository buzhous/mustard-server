-- =============================================================
-- 殷墟拾取券 兜底种子（MySQL，幂等：不存在时插入，已存在则跳过）
-- App 端领取时也会惰性创建同一条记录（code 唯一索引兜底并发），
-- 本脚本用于初始化环境或后台提前录入，保证后台可直接管理。
-- =============================================================

INSERT INTO tbl_coupon
    (id, create_time, update_time, code, coupon_name, coupon_type, discount_value, min_amount, description,
     valid_start_time, valid_end_time, total_count, remain_count, used_count, per_limit, daily_limit, status, sort)
SELECT 1930000000000000001, NOW(), NOW(), 'RUINS_PICKUP', '殷墟拾取券', 3, 0, 0,
       '每日可领取1张，用于废墟广场拾取物品，拾取一次消耗一张',
       NOW(), '2099-12-31 23:59:59', 99999999, 99999999, 0, 0, 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tbl_coupon WHERE code = 'RUINS_PICKUP');
