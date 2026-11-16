-- =============================================================
-- 用户反馈管理 菜单 + 权限（MySQL，jeecg sys_permission）
-- 本库菜单按角色授权（sys_role_permission）加载，无超管旁路：
-- 菜单插入后需授权给角色（下方默认授权给 admin 管理员角色）才可见。
-- 列名按本库实际结构：is_leaf / is_route（非 leaf / route）
-- =============================================================

-- 一级目录：运营管理
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('feedback2026menu', NULL, '运营管理', '/operations', 'layouts/default/index', NULL, 0, 'ant-design:solution-outlined', 10.00, 0, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：反馈管理（列表/回复/删除）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('feedback2026admin', 'feedback2026menu', '反馈管理', '/operations/feedback', 'feedback/admin/index', 'feedback-admin', 1, 'ant-design:message-outlined', 1.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 按钮权限（供角色授权配置，后端接口未强制校验权限标识）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time, perms)
VALUES
    ('feedback2026reply', 'feedback2026admin', '回复反馈', NULL, NULL, NULL, 2, NULL, 1.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'feedback:admin:reply'),
    ('feedback2026del',   'feedback2026admin', '删除反馈', NULL, NULL, NULL, 2, NULL, 2.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'feedback:admin:deleteBatch');

-- 角色授权：授予 admin 管理员角色（sys_role_permission）
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT REPLACE(UUID(), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW()
FROM sys_permission p
WHERE p.id IN ('feedback2026menu', 'feedback2026admin', 'feedback2026reply', 'feedback2026del')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND rp.permission_id = p.id);
