-- RBAC权限管理表结构

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    status INT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    resource_type VARCHAR(20) COMMENT '资源类型: menu-菜单, button-按钮, api-接口',
    resource_path VARCHAR(200) COMMENT '资源路径',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 初始化默认角色
INSERT INTO sys_role (role_name, role_code, description, status) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有所有权限', 1),
('管理员', 'ADMIN', '拥有大部分管理权限', 1),
('普通用户', 'USER', '基本查看权限', 1),
('游客', 'GUEST', '只读权限', 1);

-- 初始化默认权限（菜单）
INSERT INTO sys_permission (permission_name, permission_code, resource_type, resource_path, parent_id, sort_order) VALUES
('仪表盘', 'dashboard', 'menu', '/dashboard', 0, 1),
('聊天记录', 'chat', 'menu', '/chat', 0, 2),
('知识库管理', 'knowledge', 'menu', '/knowledge', 0, 3),
('RAG文档', 'rag', 'menu', '/rag', 0, 4),
('LLM配置', 'llm', 'menu', '/llm', 0, 5),
('形象管理', 'avatar', 'menu', '/avatar', 0, 6),
('路线管理', 'route', 'menu', '/route', 0, 7),
('用户分析', 'report', 'menu', '/report', 0, 8),
('游客消费分析', 'tourist-analysis', 'menu', '/tourist-analysis', 0, 9),
('消费数据导入', 'tourist-import', 'menu', '/tourist-import', 0, 10),
('用户管理', 'user', 'menu', '/user', 0, 11),
('角色管理', 'role', 'menu', '/role', 0, 12),
('权限管理', 'permission', 'menu', '/permission', 0, 13);

-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 管理员拥有除权限管理外的所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code NOT IN ('permission');

-- 普通用户拥有查看权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE permission_code IN ('dashboard', 'chat', 'report', 'tourist-analysis');

-- 默认admin用户设为超级管理员
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
