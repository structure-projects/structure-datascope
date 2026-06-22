-- 数据权限示例数据库初始化脚本
-- 用于 MyBatis-Plus 示例项目的测试数据初始化

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS example_db;
USE example_db;

-- ========== 部门表 ==========
DROP TABLE IF EXISTS departments;
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    parent_id BIGINT COMMENT '上级部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_id (org_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ========== 用户表 ==========
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) COMMENT '邮箱',
    org_id BIGINT NOT NULL COMMENT '组织ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_org_id (org_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========== 订单表 ==========
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单编号',
    amount DECIMAL(10, 2) COMMENT '订单金额',
    phone VARCHAR(20) COMMENT '客户手机号',
    email VARCHAR(100) COMMENT '客户邮箱',
    remark VARCHAR(500) COMMENT '内部备注',
    org_id BIGINT NOT NULL COMMENT '组织ID（数据权限隔离字段）',
    dept_id BIGINT NOT NULL COMMENT '部门ID（数据权限隔离字段）',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '订单状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_org_id (org_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========== 插入测试数据 ==========

-- ------------------------------
-- 部门数据
-- ------------------------------
INSERT INTO departments (id, name, org_id, parent_id) VALUES
(1, '研发一部', 10, NULL),
(2, '研发二部', 10, NULL),
(3, '财务部', 10, NULL),
(4, '市场部', 10, NULL),
(5, '运营部', 10, NULL),
(6, '销售一部', 20, NULL),
(7, '销售二部', 20, NULL),
(8, '客服部', 20, NULL),
(9, '技术部', 20, NULL),
(10, '管理部', 20, NULL);

-- ------------------------------
-- 用户数据
-- ------------------------------
INSERT INTO users (id, username, email, org_id, dept_id) VALUES
(1, 'admin', 'admin@example.com', 10, 1),
(2, 'user-dept1', 'user-dept1@example.com', 10, 1),
(3, 'user-dept2', 'user-dept2@example.com', 10, 2),
(4, 'user-dept3', 'user-dept3@example.com', 10, 3),
(5, 'user-dept4', 'user-dept4@example.com', 10, 4),
(6, 'user-dept5', 'user-dept5@example.com', 10, 5),
(7, 'org20-admin', 'org20-admin@example.com', 20, 6),
(8, 'user-org20-dept6', 'user-org20-dept6@example.com', 20, 6),
(9, 'user-org20-dept7', 'user-org20-dept7@example.com', 20, 7),
(10, 'user-org20-dept8', 'user-org20-dept8@example.com', 20, 8);

-- ------------------------------
-- 订单数据 - 组织10
-- ------------------------------
INSERT INTO orders (order_no, amount, phone, email, remark, org_id, dept_id, status, create_by) VALUES
-- 部门1的数据
('ORD-2024-0001', 1999.00, '13800138001', 'customer1@example.com', '重要客户订单', 10, 1, 'COMPLETED', 'admin'),
('ORD-2024-0002', 3999.00, '13800138002', 'customer2@example.com', '普通订单', 10, 1, 'PENDING', 'user-dept1'),
('ORD-2024-0009', 1299.00, '13800138009', 'customer9@example.com', '测试订单', 10, 1, 'COMPLETED', 'admin'),
-- 部门2的数据
('ORD-2024-0003', 2999.00, '13800138003', 'customer3@example.com', '财务订单', 10, 2, 'COMPLETED', 'admin'),
('ORD-2024-0004', 4999.00, '13800138004', 'customer4@example.com', '大额订单', 10, 2, 'PENDING', 'user-dept2'),
('ORD-2024-0010', 6999.00, '13800138010', 'customer10@example.com', '紧急订单', 10, 2, 'COMPLETED', 'admin'),
-- 部门3的数据
('ORD-2024-0005', 1599.00, '13800138005', 'customer5@example.com', '普通订单', 10, 3, 'PENDING', 'user-dept3'),
('ORD-2024-0006', 5999.00, '13800138006', 'customer6@example.com', 'VIP订单', 10, 3, 'COMPLETED', 'admin'),
-- 部门4的数据
('ORD-2024-0007', 7999.00, '13800138007', 'customer7@example.com', '高额订单', 10, 4, 'COMPLETED', 'user-dept4'),
-- 部门5的数据
('ORD-2024-0008', 999.00, '13800138008', 'customer8@example.com', '小额订单', 10, 5, 'PENDING', 'user-dept5');

-- ------------------------------
-- 订单数据 - 组织20
-- ------------------------------
INSERT INTO orders (order_no, amount, phone, email, remark, org_id, dept_id, status, create_by) VALUES
('ORD-2024-0011', 2999.00, '13800138011', 'customer11@example.com', '组织20订单', 20, 6, 'COMPLETED', 'org20-admin'),
('ORD-2024-0012', 3999.00, '13800138012', 'customer12@example.com', '组织20订单', 20, 7, 'PENDING', 'user-org20-dept6'),
('ORD-2024-0013', 5999.00, '13800138013', 'customer13@example.com', '组织20订单', 20, 8, 'COMPLETED', 'user-org20-dept7'),
('ORD-2024-0014', 1999.00, '13800138014', 'customer14@example.com', '组织20订单', 20, 9, 'PENDING', 'user-org20-dept8'),
('ORD-2024-0015', 8999.00, '13800138015', 'customer15@example.com', '组织20订单', 20, 10, 'COMPLETED', 'org20-admin');

-- ------------------------------
-- 租户测试数据（组织1和2）
-- ------------------------------
INSERT INTO departments (id, name, org_id, parent_id) VALUES
(11, '租户1-研发部', 1, NULL),
(12, '租户1-财务部', 1, NULL),
(13, '租户2-销售部', 2, NULL),
(14, '租户2-技术部', 2, NULL);

INSERT INTO users (id, username, email, org_id, dept_id) VALUES
(11, 'tenant1-admin', 'tenant1-admin@example.com', 1, 11),
(12, 'tenant1-user', 'tenant1-user@example.com', 1, 11),
(13, 'tenant2-admin', 'tenant2-admin@example.com', 2, 13),
(14, 'tenant2-user', 'tenant2-user@example.com', 2, 14);

INSERT INTO orders (order_no, amount, phone, email, remark, org_id, dept_id, status, create_by) VALUES
('ORD-TENANT-001', 1000.00, '13900139001', 'tenant1@example.com', '租户1订单', 1, 11, 'COMPLETED', 'tenant1-admin'),
('ORD-TENANT-002', 2000.00, '13900139002', 'tenant1-user@example.com', '租户1订单', 1, 11, 'PENDING', 'tenant1-user'),
('ORD-TENANT-003', 3000.00, '13900139003', 'tenant1-user@example.com', '租户1订单', 1, 12, 'COMPLETED', 'tenant1-admin'),
('ORD-TENANT-004', 4000.00, '13900139004', 'tenant2@example.com', '租户2订单', 2, 13, 'PENDING', 'tenant2-admin'),
('ORD-TENANT-005', 5000.00, '13900139005', 'tenant2-user@example.com', '租户2订单', 2, 14, 'COMPLETED', 'tenant2-user');

-- 查看数据统计
SELECT '部门数量' as type, COUNT(*) as count FROM departments
UNION ALL
SELECT '用户数量' as type, COUNT(*) as count FROM users
UNION ALL
SELECT '订单数量' as type, COUNT(*) as count FROM orders
UNION ALL
SELECT '组织10订单' as type, COUNT(*) as count FROM orders WHERE org_id = 10
UNION ALL
SELECT '组织20订单' as type, COUNT(*) as count FROM orders WHERE org_id = 20
UNION ALL
SELECT '租户1订单' as type, COUNT(*) as count FROM orders WHERE org_id = 1
UNION ALL
SELECT '租户2订单' as type, COUNT(*) as count FROM orders WHERE org_id = 2;