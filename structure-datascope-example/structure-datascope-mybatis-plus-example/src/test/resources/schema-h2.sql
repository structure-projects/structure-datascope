-- H2 compatible schema for testing
-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    amount DECIMAL(10,2),
    phone VARCHAR(20),
    email VARCHAR(100),
    remark VARCHAR(500),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    org_id BIGINT,
    dept_id BIGINT,
    create_time TIMESTAMP,
    create_by VARCHAR(64),
    update_time TIMESTAMP,
    update_by VARCHAR(64)
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(100),
    org_id BIGINT,
    dept_id BIGINT,
    create_time TIMESTAMP
);

-- Departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    org_id BIGINT,
    parent_id BIGINT
);

-- Insert test orders data
INSERT INTO orders (order_no, amount, phone, email, remark, status, org_id, dept_id, create_time, create_by) VALUES
('ORD-2024-001', 9999.99, '13800138001', 'customer1@example.com', '这是内部机密备注 1', 'COMPLETED', 10, 1, CURRENT_TIMESTAMP, 'admin'),
('ORD-2024-002', 8888.88, '13800138002', 'customer2@example.com', '这是内部机密备注 2', 'COMPLETED', 10, 2, CURRENT_TIMESTAMP, 'admin'),
('ORD-2024-003', 7777.77, '13800138003', 'customer3@example.com', '这是内部机密备注 3', 'PENDING', 10, 3, CURRENT_TIMESTAMP, 'admin'),
('ORD-2024-004', 6666.66, '13800138004', 'customer4@example.com', '这是内部机密备注 4', 'COMPLETED', 20, 6, CURRENT_TIMESTAMP, 'admin'),
('ORD-2024-005', 5555.55, '13800138005', 'customer5@example.com', '这是内部机密备注 5', 'PENDING', 20, 7, CURRENT_TIMESTAMP, 'admin');

-- Insert test users data
INSERT INTO users (username, email, org_id, dept_id, create_time) VALUES
('admin', 'admin@example.com', 10, 1, CURRENT_TIMESTAMP),
('user1', 'user1@example.com', 10, 2, CURRENT_TIMESTAMP),
('user2', 'user2@example.com', 20, 6, CURRENT_TIMESTAMP);

-- Insert test departments data
INSERT INTO departments (name, org_id, parent_id) VALUES
('技术部', 10, NULL),
('市场部', 10, NULL),
('财务部', 10, NULL),
('研发组', 10, 1),
('测试组', 10, 1),
('销售部', 20, NULL),
('运营部', 20, NULL);
