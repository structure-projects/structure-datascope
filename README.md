# structure-datascope

数据范围管理框架，提供统一的数据隔离能力，支持行级和列级的数据权限控制。

## 功能特性

- 支持 HTTP 请求的数据范围上下文传递
- 支持 XXL-Job 任务的数据范围上下文
- 支持消息队列（如 RabbitMQ、Kafka）的数据范围隔离
- 支持多种数据存储的范围过滤：
  - MySQL (MyBatis-Plus)
  - Redis
  - Elasticsearch
  - MongoDB
- **行级权限控制**：在 DAO 层自动添加 WHERE 条件
- **列级权限控制**：在序列化前过滤敏感字段

## 数据权限两层模型

### 第一层：行级权限（Row-Level）

控制：WHERE 条件

**SQL 示例**：
```sql
SELECT * 
FROM orders 
WHERE org_id = 10 
  AND dept_id IN (1,2,3)
```

**规则表达**：
```json
{
  "resource": "order",
  "row_rules": [
    { "field": "org_id", "op": "=", "value": 10 },
    { "field": "dept_id", "op": "IN", "value": [1,2,3] }
  ]
}
```

- 在 **DAO / Repository 层**生效

### 第二层：列级权限（Column-Level）

控制：SELECT 哪些字段 / 是否序列化

**规则表达**：
```json
{
  "resource": "user",
  "column_rules": [
    { "field": "phone", "visible_if_role_in": ["SYS_ADMIN"] },
    { "field": "email", "visible": true }
  ]
}
```

**Java / DTO 层示例**：
```java
if (!dataRuleEngine.canSeeField("user.phone")) {
    user.setPhone(null);
}
```

- 在 **序列化前**处理
- 防止敏感数据泄露

### 管理员 / 特殊角色处理

行级 + 列级一起使用：

```json
{
  "resource": "order",
  "row_rules": [
    { "field": "org_id", "op": "=", "value": 10 }
  ],
  "column_rules": [
    { "field": "amount", "visible_if_role_in": ["FINANCE"] }
  ]
}
```

- 普通员工：看不到 amount 字段
- 财务角色：能看到 amount 字段

### 推荐数据流转流程

```
DB
  ↓
DAO（行级过滤）
  ↓
Domain Object
  ↓
Data Rule Engine（列级过滤）
  ↓
DTO / JSON
```

> **最佳实践**：不要在 Controller 里手写 if 判断，统一在出口处处理

## 模块结构

```
structure-datascope/
├── structure-datascope-dependencies/    # 父模块，管理依赖和版本
├── structure-datascope-core/            # 核心模块：数据权限上下文、规则引擎
├── structure-datascope-starter/         # Spring Boot Starter
├── structure-datascope-mybatis-plus/    # MyBatis-Plus 行级权限拦截器
├── structure-datascope-redis/           # Redis 数据范围隔离
├── structure-datascope-elasticsearch/   # Elasticsearch 数据范围隔离
├── structure-datascope-mongodb/         # MongoDB 数据范围隔离
├── structure-datascope-message/         # 消息队列数据范围传递
├── structure-datascope-job/             # XXL-Job 数据范围支持
└── structure-datascope-example/         # 示例模块
    ├── structure-datascope-core-example/
    ├── structure-datascope-redis-example/
    ├── structure-datascope-mybatis-plus-example/
    ├── structure-datascope-message-example/
    ├── structure-datascope-elasticsearch-example/
    ├── structure-datascope-mongodb-example/
    └── structure-datascope-job-example/
```

## 快速开始

### 环境要求

- JDK 17+
- Spring Boot 3.2+
- Maven 3.6+

### 依赖引入

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>cn.structured</groupId>
    <artifactId>structure-datascope-starter</artifactId>
    <version>${structure-datascope.version}</version>
</dependency>
```

### 配置示例

```yaml
structure:
  data-scope:
    enabled: true
    header-name: X-DataScope-Id
```

## 使用方式

### 1. 数据范围上下文设置

```java
@RestController
public class DemoController {
    
    @GetMapping("/demo")
    public String demo(@RequestHeader("X-DataScope-Id") String dataScopeId) {
        // 数据范围上下文会自动从请求头获取
        String currentScope = DataScopeContext.getDataScopeId();
        return "Current datascope: " + currentScope;
    }
}
```

### 2. 定义数据权限规则

```java
@Data
@DataScopeRule(resource = "order")
public class Order {
    private Long id;
    
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN", "FINANCE"})
    private BigDecimal amount;
    
    private Long orgId;
    private Long deptId;
}
```

### 3. MyBatis-Plus 行级权限过滤

```java
@DataScopeRule(resource = "order")
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT * FROM orders")
    @DataScopeRow(fields = {"org_id", "dept_id"})
    List<Order> selectAll();
}
```

### 4. 列级权限过滤（序列化前）

```java
@Autowired
private DataRuleEngine dataRuleEngine;

public OrderDTO toDTO(Order order) {
    OrderDTO dto = new OrderDTO();
    BeanUtils.copyProperties(order, dto);
    
    // 应用列级权限规则
    dataRuleEngine.filter(dto, "order");
    
    return dto;
}
```

## 模块说明

| 模块 | 说明 |
|------|------|
| structure-datascope-dependencies | 父模块，定义依赖版本和基础配置 |
| structure-datascope-core | 核心模块：数据权限上下文、规则引擎、注解定义 |
| structure-datascope-starter | Spring Boot Starter，自动配置 |
| structure-datascope-mybatis-plus | MyBatis-Plus 分页插件，自动添加行级条件 |
| structure-datascope-redis | Redis 数据范围前缀处理 |
| structure-datascope-elasticsearch | ES 查询自动添加范围过滤 |
| structure-datascope-mongodb | MongoDB 查询自动添加范围过滤 |
| structure-datascope-message | 消息发送和消费时传递数据范围 |
| structure-datascope-job | XXL-Job 任务执行时的数据范围上下文 |

## 许可证

Apache License 2.0

## 开发团队

- chuck (15524415221@163.com)
- Chuanqiang Liu (361648887@qq.com)

## 项目地址

https://github.com/structure-projects/structure-boot
