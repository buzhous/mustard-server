-- =============================================================
-- 运营统计 菜单（MySQL，jeecg sys_permission）
-- 本库菜单按角色授权（sys_role_permission）加载，无超管旁路：
-- 菜单插入后需授权给角色（下方默认授权给 admin 管理员角色）才可见。
-- 列名按本库实际结构：is_leaf / is_route（非 leaf / route）
-- 页面组件：src/views/stats/dashboard.vue（F1 运营统计看板）
-- =============================================================

-- 一级目录：运营统计
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('stats2026menu', NULL, '运营统计', '/stats', 'layouts/default/index', NULL, 0, 'ant-design:bar-chart-outlined', 11.00, 0, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：运营看板（KPI / 漏斗 / 趋势 / 留存 / 事件明细 / CSV 导出）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('stats2026dashboard', 'stats2026menu', '运营看板', '/stats/dashboard', 'stats/dashboard', 'stats-dashboard', 1, 'ant-design:dashboard-outlined', 1.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 角色授权：授予 admin 管理员角色（sys_role_permission）
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT REPLACE(UUID(), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW()
FROM sys_permission p
WHERE p.id IN ('stats2026menu', 'stats2026dashboard')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND rp.permission_id = p.id);
