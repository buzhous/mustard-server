-- ============================================================
-- App 版本管理（版本管理策略 §4.1）建表 + 后台菜单
-- 主版本：MySQL（生产 mustard_space@8.134.112.180）；PG/Kingbase 变体要点见文末注释
-- ============================================================

CREATE TABLE IF NOT EXISTS tbl_app_version (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键（MyBatis-Plus ASSIGN_ID 雪花，显式插入不受自增影响）',
    platform      VARCHAR(10)  NOT NULL COMMENT 'ios / android',
    version       VARCHAR(32)  NOT NULL COMMENT '版本字符串（双平台统一）',
    build_no      INT          NOT NULL COMMENT '构建号（分平台独立递增，latest 判定基准）',
    min_build     INT          NULL COMMENT '强更门禁线（空 = 不设门禁）',
    force_upgrade INT          NOT NULL DEFAULT 0 COMMENT '1=门禁以下强更 0=仅提醒',
    rollout_pct   INT          NOT NULL DEFAULT 100 COMMENT '灰度百分比 0-100（deviceId hash 分桶）',
    status        INT          NOT NULL DEFAULT 0 COMMENT '0 草稿 / 1 灰度中 / 2 全量 / 3 下线',
    upgrade_url   VARCHAR(255) NULL COMMENT '商店页 / APK 直链',
    apk_md5       VARCHAR(32)  NULL COMMENT 'Android 直链完整性校验',
    apk_size      BIGINT       NULL COMMENT 'Android 直链下载预检与进度总量',
    changelog     TEXT         NULL COMMENT '更新说明',
    publish_time  DATETIME     NULL COMMENT '发布时间（latest 取数基准）',
    create_by     VARCHAR(64)  NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_app_version_platform (platform, status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App 版本发布记录';

-- 父目录（menu_type=0）：component 用 RouteView 容器，url 与子路由不能同名（否则 jeecg routeHelper 生成同名 route 整站崩溃）
INSERT INTO sys_permission (id, parent_id, name, url, component, menu_type, sort_no, icon, del_flag, create_time)
SELECT '1988000000000000100', p.id, 'App 版本管理', '/appversion', 'layouts/RouteView', 0, 5.0, 'ant-design:appstore-outlined', 0, now()
FROM sys_permission p
WHERE p.name = '系统管理' AND p.menu_type = 0
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '1988000000000000100');

INSERT INTO sys_permission (id, parent_id, name, url, component, menu_type, sort_no, del_flag, create_time)
VALUES ('1988000000000000101', '1988000000000000100', '版本列表', '/appversion/list', 'appversion/list', 1, 1.0, 0, now());

-- admin 角色授权（role_code = admin）
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT '1988000000000000201', r.id, '1988000000000000100' FROM sys_role r WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = r.id AND permission_id = '1988000000000000100');
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT '1988000000000000202', r.id, '1988000000000000101' FROM sys_role r WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = r.id AND permission_id = '1988000000000000101');

-- ============================================================
-- PG / Kingbase 变体：BIGINT AUTO_INCREMENT → BIGSERIAL；
-- PRIMARY KEY 内联 INDEX → 独立 CREATE INDEX idx_app_version_platform ON tbl_app_version(platform, status, publish_time)；
-- DATETIME → TIMESTAMP；ENGINE/CHARSET/COMMENT 子句去掉
-- ============================================================
