-- =============================================================
-- 殷墟物品管理 菜单 + 权限（MySQL，jeecg sys_permission）
-- 本库菜单按角色授权（sys_role_permission）加载，无超管旁路：
-- 菜单插入后需授权给角色（下方默认授权给 admin 管理员角色）才可见。
-- 列名按本库实际结构：is_leaf / is_route（非 leaf / route）
-- =============================================================

-- 一级目录：殷墟管理
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('ruins2026menu', NULL, '殷墟管理', '/ruins', 'layouts/default/index', NULL, 0, 'ant-design:bank-outlined', 9.00, 0, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：殷墟物品（批量删除/批量下架/批量指定用户拾取）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('ruins2026admin', 'ruins2026menu', '殷墟物品', '/ruins/admin', 'ruins/admin/index', 'ruins-admin', 1, 'ant-design:appstore-outlined', 1.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 按钮权限（供角色授权配置，后端接口未强制校验权限标识）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time, perms)
VALUES
    ('ruins2026del',  'ruins2026admin', '批量删除',     NULL, NULL, NULL, 2, NULL, 1.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'ruins:admin:deleteBatch'),
    ('ruins2026off',  'ruins2026admin', '批量下架',     NULL, NULL, NULL, 2, NULL, 2.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'ruins:admin:offShelfBatch'),
    ('ruins2026asg',  'ruins2026admin', '批量指定拾取', NULL, NULL, NULL, 2, NULL, 3.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'ruins:admin:assignPickup');

-- 角色授权：授予 admin 管理员角色（sys_role_permission）
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT REPLACE(UUID(), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW()
FROM sys_permission p
WHERE p.id IN ('ruins2026menu', 'ruins2026admin', 'ruins2026del', 'ruins2026off', 'ruins2026asg')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND rp.permission_id = p.id);
