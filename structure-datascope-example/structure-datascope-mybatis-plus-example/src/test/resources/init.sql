-- 数据权限示例数据库初始化脚本
-- 用于 MyBatis-Plus 示例项目的测试数据初始化

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS example_db;
USE example_db;

-- 订单表
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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_org_id (org_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 初始化测试数据
-- 组织10的数据
INSERT INTO orders (order_no, amount, phone, email, remark, org_id, dept_id, create_by) VALUES
('ORD-2024-0001', 1999.00, '13800138001', 'customer1@example.com', '重要客户订单', 10, 1, 'admin'),
('ORD-2024-0002', 3999.00, '13800138002', 'customer2@example.com', '普通订单', 10, 1, 'admin'),
('ORD-2024-0003', 2999.00, '13800138003', 'customer3@example.com', '财务订单', 10, 2, 'admin'),
('ORD-2024-0004', 4999.00, '13800138004', 'customer4@example.com', '大额订单', 10, 2, 'admin'),
('ORD-2024-0005', 1599.00, '13800138005', 'customer5@example.com', '普通订单', 10, 3, 'admin'),
('ORD-2024-0006', 5999.00, '13800138006', 'customer6@example.com', 'VIP订单', 10, 3, 'admin'),
('ORD-2024-0007', 7999.00, '13800138007', 'customer7@example.com', '高额订单', 10, 4, 'admin'),
('ORD-2024-0008', 999.00, '13800138008', 'customer8@example.com', '小额订单', 10, 5, 'admin'),
('ORD-2024-0009', 1299.00, '13800138009', 'customer9@example.com', '测试订单', 10, 1, 'admin'),
('ORD-2024-0010', 6999.00, '13800138010', 'customer10@example.com', '紧急订单', 10, 2, 'admin');

-- 组织20的数据
INSERT INTO orders (order_no, amount, phone, email, remark, org_id, dept_id, create_by) VALUES
('ORD-2024-0011', 2999.00, '13800138011', 'customer11@example.com', '组织20订单', 20, 6, 'admin'),
('ORD-2024-0012', 3999.00, '13800138012', 'customer12@example.com', '组织20订单', 20, 7, 'admin'),
('ORD-2024-0013', 5999.00, '13800138013', 'customer13@example.com', '组织20订单', 20, 8, 'admin'),
('ORD-2024-0014', 1999.00, '13800138014', 'customer14@example.com', '组织20订单', 20, 9, 'admin'),
('ORD-2024-0015', 8999.00, '13800138015', 'customer15@example.com', '组织20订单', 20, 10, 'admin');
