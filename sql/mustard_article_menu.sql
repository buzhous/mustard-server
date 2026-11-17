-- =============================================================
-- 文章管理（栏目 + 文章）菜单 + 权限（MySQL，jeecg sys_permission）
-- 本库菜单按角色授权（sys_role_permission）加载，无超管旁路：
-- 菜单插入后需授权给角色（下方默认授权给 admin 管理员角色）才可见。
-- 列名按本库实际结构：is_leaf / is_route（非 leaf / route）
-- =============================================================

-- 一级目录：内容管理
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('article2026menu', NULL, '内容管理', '/content', 'layouts/default/index', NULL, 0, 'ant-design:read-outlined', 9.00, 0, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：栏目管理
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('article2026col', 'article2026menu', '栏目管理', '/content/articleColumn', 'article/column/index', 'article-column', 1, 'ant-design:appstore-outlined', 1.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 二级页面：文章管理（含富文本编辑）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time)
VALUES
    ('article2026art', 'article2026menu', '文章管理', '/content/article', 'article/article/index', 'article-admin', 1, 'ant-design:file-text-outlined', 2.00, 1, 1, 0, 0, 0, 0, 0, '1', 'admin', NOW());

-- 按钮权限（供角色授权配置，后端接口未强制校验权限标识）
INSERT IGNORE INTO sys_permission
    (id, parent_id, name, url, component, component_name, menu_type, icon, sort_no, is_leaf, is_route, always_show, hide_tab, keep_alive, internal_or_external, del_flag, status, create_by, create_time, perms)
VALUES
    ('article2026artadd', 'article2026art', '新增文章', NULL, NULL, NULL, 2, NULL, 1.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'article:admin:add'),
    ('article2026artedit', 'article2026art', '编辑文章', NULL, NULL, NULL, 2, NULL, 2.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'article:admin:edit'),
    ('article2026artdel', 'article2026art', '删除文章', NULL, NULL, NULL, 2, NULL, 3.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'article:admin:deleteBatch'),
    ('article2026coladd', 'article2026col', '新增栏目', NULL, NULL, NULL, 2, NULL, 1.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'article:admin:column:add'),
    ('article2026coledit', 'article2026col', '编辑栏目', NULL, NULL, NULL, 2, NULL, 2.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'article:admin:column:edit'),
    ('article2026coldel', 'article2026col', '删除栏目', NULL, NULL, NULL, 2, NULL, 3.00, 1, 0, 0, 0, 0, 0, 0, '1', 'admin', NOW(), 'article:admin:column:delete');

-- 角色授权：授予 admin 管理员角色（sys_role_permission）
INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT REPLACE(UUID(), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW()
FROM sys_permission p
WHERE p.id IN ('article2026menu', 'article2026col', 'article2026art',
               'article2026artadd', 'article2026artedit', 'article2026artdel',
               'article2026coladd', 'article2026coledit', 'article2026coldel')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND rp.permission_id = p.id);
