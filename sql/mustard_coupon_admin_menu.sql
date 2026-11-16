-- =============================================================
-- 优惠券管理 菜单 + 权限（MySQL，jeecg sys_permission）
-- 本库菜单按角色授权（sys_role_permission）加载，无超管旁路：
-- 菜单插入后需授权给角色（下方默认授权给 admin 管理员角色）才可见。
-- 列名按本库实际结构：is_leaf / is_route（非 leaf / route）
-- =============================================================

-- 一级目录：优惠券管理（平台级券统一管理入口）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('coupon2026menu', NULL, '优惠券管理', '/coupon', 'layouts/default/index', NULL, 0, 'ant-design:gift-outlined', 10.00, 0, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：优惠券模板（新增/编辑/发布/下架/删除/库存调整）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('coupon2026template', 'coupon2026menu', '优惠券模板', '/coupon/template', 'coupon/template/index', 'coupon-template', 1, 'ant-design:tags-outlined', 1.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：用户持券（查询/发放/作废）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('coupon2026user', 'coupon2026menu', '用户持券', '/coupon/user', 'coupon/user/index', 'coupon-user', 1, 'ant-design:wallet-outlined', 2.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 按钮权限（供角色授权配置，后端接口未强制校验权限标识）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time, perms)
VALUES
    ('coupon2026add',     'coupon2026template', '新增',     NULL, NULL, NULL, 2, NULL, 1.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:add'),
    ('coupon2026edit',    'coupon2026template', '编辑',     NULL, NULL, NULL, 2, NULL, 2.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:edit'),
    ('coupon2026del',     'coupon2026template', '删除',     NULL, NULL, NULL, 2, NULL, 3.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:delete'),
    ('coupon2026pub',     'coupon2026template', '发布/下架', NULL, NULL, NULL, 2, NULL, 4.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:publish'),
    ('coupon2026stock',   'coupon2026template', '库存调整', NULL, NULL, NULL, 2, NULL, 5.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:adjustStock'),
    ('coupon2026grant',   'coupon2026user',     '发放券',   NULL, NULL, NULL, 2, NULL, 1.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:grant'),
    ('coupon2026invalid', 'coupon2026user',     '作废券',   NULL, NULL, NULL, 2, NULL, 2.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'coupon:admin:invalidate');

-- 角色授权：授予 admin 管理员角色（sys_role_permission）
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT REPLACE(UUID(), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW()
FROM sys_permission p
WHERE p.id IN ('coupon2026menu', 'coupon2026template', 'coupon2026user', 'coupon2026add', 'coupon2026edit',
               'coupon2026del', 'coupon2026pub', 'coupon2026stock', 'coupon2026grant', 'coupon2026invalid')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND rp.permission_id = p.id);
