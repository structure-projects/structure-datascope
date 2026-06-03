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
- **角色 + 权限双重控制**：支持角色和权限标识两种维度的字段可见性控制

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

支持 **角色** 和 **权限标识** 两种维度控制：

**规则表达（角色维度）**：
```json
{
  "resource": "user",
  "column_rules": [
    { "field": "phone", "visible_if_role_in": ["SYS_ADMIN"] },
    { "field": "email", "visible": true }
  ]
}
```

**规则表达（权限维度）**：
```json
{
  "resource": "user",
  "column_rules": [
    { "field": "phone", "visible_if_permission_in": ["user:view_phone"] },
    { "field": "salary", "hidden_if_permission_in": ["user:hidden_salary"] }
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
    { "field": "amount", "visible_if_role_in": ["FINANCE"] },
    { "field": "secret", "visible_if_permission_in": ["order:view_secret"] }
  ]
}
```

- 普通员工：看不到 amount 和 secret 字段
- 财务角色：能看到 amount 字段
- 拥有 `order:view_secret` 权限的用户：能看到 secret 字段

### 角色与权限的区别

| 维度 | 说明 | 示例 |
|------|------|------|
| 角色 | 粗粒度控制，基于用户角色 | `SYS_ADMIN`、`FINANCE`、`EMPLOYEE` |
| 权限标识 | 细粒度控制，基于具体权限 | `order:view_amount`、`user:view_phone` |

**推荐使用场景**：
- 角色：适用于简单的权限场景，如部门级、岗位级控制
- 权限标识：适用于复杂的权限场景，如功能级、数据级控制

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
    ├── structure-datascope-spring-boot-example/
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
    role-header-name: X-DataScope-Roles
    permission-header-name: X-DataScope-Permissions
    org-id-header-name: X-Org-Id
    dept-ids-header-name: X-Dept-Ids
    user-id-header-name: X-User-Id
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
        
        // 获取当前用户角色
        List<String> roles = DataScopeContext.getRoles();
        
        // 获取当前用户权限
        List<String> permissions = DataScopeContext.getPermissions();
        
        // 检查是否拥有指定角色
        boolean isAdmin = DataScopeContext.hasRole("SYS_ADMIN");
        
        // 检查是否拥有指定权限
        boolean canViewAmount = DataScopeContext.hasPermission("order:view_amount");
        
        return "Current datascope: " + currentScope;
    }
}
```

### 2. 定义数据权限规则（支持角色和权限）

```java
@Data
@DataScopeRule(resource = "order")
public class OrderDTO {
    private Long id;
    private String orderNo;
    
    // 角色控制：仅管理员和财务可见
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN", "FINANCE"})
    private BigDecimal amount;
    
    // 权限控制：拥有 order:view_phone 权限可见
    @DataScopeField(visibleIfPermissionIn = {"order:view_phone"})
    private String phone;
    
    // 组合控制：角色或权限任一满足即可见
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"}, 
                   visibleIfPermissionIn = {"order:view_secret"})
    private String secret;
    
    // 隐藏控制：员工角色不可见
    @DataScopeField(hiddenIfRoleIn = {"EMPLOYEE"})
    private String remark;
    
    // 隐藏控制：拥有 order:hidden_email 权限不可见
    @DataScopeField(hiddenIfPermissionIn = {"order:hidden_email"})
    private String email;
    
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
    
    // 应用列级权限规则（自动根据角色和权限过滤）
    dataRuleEngine.filter(dto, "order");
    
    return dto;
}

// 批量过滤
public List<OrderDTO> toDTOList(List<Order> orders) {
    return orders.stream()
        .map(order -> {
            OrderDTO dto = new OrderDTO();
            BeanUtils.copyProperties(order, dto);
            dataRuleEngine.filter(dto, "order");
            return dto;
        })
        .collect(Collectors.toList());
}
```

### 5. 手动检查字段可见性

```java
// 检查字段是否可见（基于角色和权限）
boolean canSeeAmount = dataRuleEngine.canSeeField("order", "amount");

// 检查用户是否拥有角色
boolean hasRole = DataScopeContext.hasRole("SYS_ADMIN");

// 检查用户是否拥有权限
boolean hasPermission = DataScopeContext.hasPermission("order:view_amount");

// 检查用户是否拥有任意角色
boolean hasAnyRole = DataScopeContext.hasAnyRole("SYS_ADMIN", "FINANCE");

// 检查用户是否拥有任意权限
boolean hasAnyPermission = DataScopeContext.hasAnyPermission("order:view_amount", "order:view_secret");
```

## HTTP 请求示例

### 基于角色的请求

```bash
# 员工访问（看不到 amount, phone, secret）
curl -H "X-DataScope-Id: scope-1" \
     -H "X-DataScope-Roles: EMPLOYEE" \
     -H "X-Org-Id: 10" \
     -H "X-User-Id: user-001" \
     http://localhost:8080/api/orders

# 财务访问（可以看到 amount）
curl -H "X-DataScope-Id: scope-2" \
     -H "X-DataScope-Roles: FINANCE" \
     -H "X-Org-Id: 10" \
     http://localhost:8080/api/orders

# 管理员访问（可以看到所有字段）
curl -H "X-DataScope-Id: scope-3" \
     -H "X-DataScope-Roles: SYS_ADMIN" \
     -H "X-Org-Id: 10" \
     http://localhost:8080/api/orders
```

### 基于权限的请求

```bash
# 拥有查看金额权限
curl -H "X-DataScope-Id: scope-4" \
     -H "X-DataScope-Roles: EMPLOYEE" \
     -H "X-DataScope-Permissions: order:view_amount" \
     -H "X-Org-Id: 10" \
     http://localhost:8080/api/orders

# 拥有多个权限
curl -H "X-DataScope-Id: scope-5" \
     -H "X-DataScope-Roles: EMPLOYEE" \
     -H "X-DataScope-Permissions: order:view_amount,order:view_phone,order:view_secret" \
     -H "X-Org-Id: 10" \
     http://localhost:8080/api/orders
```

## 模块说明

| 模块 | 说明 |
|------|------|
| structure-datascope-dependencies | 父模块，定义依赖版本和基础配置 |
| structure-datascope-core | 核心模块：数据权限上下文、规则引擎、注解定义（支持角色和权限） |
| structure-datascope-starter | Spring Boot Starter，自动配置（自动提取角色和权限请求头） |
| structure-datascope-mybatis-plus | MyBatis-Plus 分页插件，自动添加行级条件 |
| structure-datascope-redis | Redis 数据范围前缀处理 |
| structure-datascope-elasticsearch | ES 查询自动添加范围过滤 |
| structure-datascope-mongodb | MongoDB 查询自动添加范围过滤 |
| structure-datascope-message | 消息发送和消费时传递数据范围 |
| structure-datascope-job | XXL-Job 任务执行时的数据范围上下文 |

## 注解说明

### @DataScopeRule

类级注解，标记资源名称：

```java
@DataScopeRule(resource = "order")
public class OrderDTO {
    // ...
}
```

### @DataScopeField

字段级注解，控制字段可见性：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `name` | 字段名称 | 字段实际名称 |
| `visible` | 是否可见 | `true` |
| `visibleIfRoleIn` | 拥有指定角色时可见 | `{}` |
| `hiddenIfRoleIn` | 拥有指定角色时隐藏 | `{}` |
| `visibleIfPermissionIn` | 拥有指定权限时可见 | `{}` |
| `hiddenIfPermissionIn` | 拥有指定权限时隐藏 | `{}` |

**规则优先级**：
1. 隐藏规则优先于可见规则
2. 角色和权限规则可以组合使用
3. 如果同时配置了角色和权限，任一满足即可

## 许可证

Apache License 2.0

## 开发团队

- chuck (15524415221@163.com)
- Chuanqiang Liu (361648887@qq.com)

## 项目地址

https://github.com/structure-projects/structure-boot