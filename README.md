# structure-datascope

数据范围管理框架，提供统一的数据隔离能力，支持行级和列级的数据权限控制。

## 功能特性

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
    # 提供器类型: remote / local
    provider-type: local
    
    # 远程模式配置
    remote:
      service-url: http://datascope-service:8080
      timeout: 5000
    
    # 是否自动注册规则
    auto-register-rules: true
```

### 2. 实现数据权限提供器

**核心设计**：通过 `userId` 获取数据权限，支持本地和远程两种模式。

#### 2.1 本地模式（从本地数据库获取）

```java
// 实现 DataScopeProvider 接口
@Service
public class MyLocalDataScopeProvider implements DataScopeProvider {
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private DataRuleMapper dataRuleMapper;
    
    @Override
    public DataScopeInfo getScopeInfo(String userId) {
        // 从本地数据库获取用户权限信息
        UserRole userRole = userRoleMapper.selectByUserId(userId);
        
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setRoles(userRole.getRoles());
        info.setPermissions(userRole.getPermissions());
        info.setOrgId(userRole.getOrgId());
        info.setDeptIds(userRole.getDeptIds());
        
        return info;
    }
    
    @Override
    public DataRule getRule(String resource, List<String> roles) {
        // 从本地数据库获取数据规则
        return dataRuleMapper.selectByResourceAndRoles(resource, roles);
    }
    
    @Override
    public Map<String, DataRule> getRules(List<String> resources, List<String> roles) {
        return dataRuleMapper.selectByResourcesAndRoles(resources, roles);
    }
    
    @Override
    public String getType() {
        return "local";
    }
}
```

#### 2.2 远程模式（调用权限服务）

```java
// 实现远程服务客户端
@Service
public class MyRemoteDataScopeClient implements RemoteDataScopeClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${structure.data-scope.remote.service-url}")
    private String serviceUrl;
    
    @Override
    public DataScopeInfo fetchScopeInfo(String userId) {
        return restTemplate.getForObject(
            serviceUrl + "/api/scope/{userId}", 
            DataScopeInfo.class, 
            userId
        );
    }
    
    @Override
    public DataRule fetchRule(String resource, List<String> roles) {
        return restTemplate.postForObject(
            serviceUrl + "/api/rule",
            new RuleRequest(resource, roles),
            DataRule.class
        );
    }
    
    @Override
    public Map<String, DataRule> fetchRules(List<String> resources, List<String> roles) {
        return restTemplate.postForObject(
            serviceUrl + "/api/rules/batch",
            new BatchRuleRequest(resources, roles),
            new ParameterizedTypeReference<Map<String, DataRule>>() {}
        );
    }
}

// 注册远程提供器
@Bean
public DataScopeProvider remoteDataScopeProvider(RemoteDataScopeClient client) {
    return new RemoteDataScopeProvider(client);
}
```

### 3. 使用 DataScopeRouter（推荐）

```java
@Service
public class DataScopeService {
    
    private final DataScopeRouter router;
    
    public DataScopeService(DataScopeRouter router) {
        this.router = router;
    }
    
    public void processRequest(String userId) {
        // 初始化用户权限上下文（自动从提供器获取权限信息）
        DataScopeInfo info = router.initScope(userId);
        
        // 使用权限规则
        boolean canSeeAmount = router.canSeeField("order", "amount");
        router.filter(orderDTO, "order");
        String condition = router.buildRowCondition("order");
    }
}
```

### 4. 直接操作上下文

```java
// 批量设置数据范围上下文
DataScopeInfo info = new DataScopeInfo();
info.setUserId("user-001");
info.setRoles(Arrays.asList("EMPLOYEE", "FINANCE"));
info.setPermissions(Arrays.asList("order:view_amount"));
info.setOrgId("org-10");
info.setDeptIds(Arrays.asList("dept-1", "dept-2"));
DataScopeContext.setInfo(info);

// 获取当前上下文
DataScopeInfo current = DataScopeContext.getInfo();

// 检查权限
boolean isAdmin = DataScopeContext.hasRole("SYS_ADMIN");
boolean canView = DataScopeContext.hasPermission("order:view_amount");

// 清理上下文
DataScopeContext.remove();
```

### 5. 定义数据权限规则（支持角色和权限）

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

### 6. MyBatis-Plus 行级权限过滤

```java
@DataScopeRule(resource = "order")
public interface OrderMapper extends BaseMapper<Order> {
    @Select("SELECT * FROM orders")
    @DataScopeRow(fields = {"org_id", "dept_id"})
    List<Order> selectAll();
}
```

### 7. 列级权限过滤（序列化前）

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

### 8. 手动检查字段可见性

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

## 模块说明

| 模块 | 说明 |
|------|------|
| structure-datascope-dependencies | 父模块，定义依赖版本和基础配置 |
| structure-datascope-core | 核心模块：数据权限上下文、规则引擎、注解定义（支持角色和权限） |
| structure-datascope-starter | Spring Boot Starter，自动配置（通过DataScopeProvider获取数据权限） |
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

## 新增模块使用说明

### 1. MongoDB 引擎使用

```java
// 创建MongoDB引擎
MongoDataRuleEngine engine = new MongoDataRuleEngine();

// 设置上下文
DataScopeInfo info = new DataScopeInfo();
info.setOrgId("10");
info.setDeptIds(Arrays.asList("1", "2", "3"));
DataScopeContext.setInfo(info);

// 构建MongoDB查询过滤器
Document filter = engine.buildMongoFilter("order");
// 输出: Document{{orgId=10, deptId=Document{{$in=[1, 2, 3]}}}

// 在查询中使用
MongoCollection<Document> collection = database.getCollection("orders");
FindIterable<Document> results = collection.find(filter);

// 与现有查询合并
Document existingFilter = new Document("status", "active");
Document merged = engine.mergeRowCondition("order", existingFilter);
```

### 2. Elasticsearch 引擎使用

```java
// 创建Elasticsearch引擎
ElasticDataRuleEngine engine = new ElasticDataRuleEngine();

// 设置上下文
DataScopeInfo info = new DataScopeInfo();
info.setOrgId("10");
info.setDeptIds(Arrays.asList("1", "2", "3"));
DataScopeContext.setInfo(info);

// 构建ES查询过滤器
Map<String, Object> filter = engine.buildElasticFilter("order");
// 输出: {orgId=10, deptId={$in=[1, 2, 3]}}

// 在查询中使用
SearchRequest searchRequest = SearchRequest.of(s -> s
    .index("orders")
    .query(q -> q.bool(b -> b.filter(filter)))
);
```

### 3. Message 消息传递使用

#### 发送消息时构建消息头

```java
// 从当前上下文构建消息头
Map<String, Object> headers = DataScopeMessageHeader.buildFromContext();

// 发送到RabbitMQ/Kafka
rabbitTemplate.convertAndSend("exchange", "routing.key", message, headers);
```

#### 消费消息时注入上下文

```java
// 在消息监听器中
@RabbitListener(queues = "queue")
public void handleMessage(Message message, @Header Map<String, Object> headers) {
    try {
        // 注入上下文到当前线程
        DataScopeMessageHeader.injectToContext(headers);

        // 业务处理 - 可以通过DataScopeContext获取权限信息
        DataScopeInfo info = DataScopeContext.getInfo();

    } finally {
        // 清理上下文
        DataScopeMessageHeader.clearContext();
    }
}
```

### 4. Job 任务上下文使用

#### 在Job执行前初始化上下文

```java
public class MyJob {
    public void execute() {
        // 方式1: 通过userId初始化（需要DataScopeProvider）
        DataScopeJobContext.initContext(jobUserId);

        try {
            // 业务处理
            DataScopeInfo info = DataScopeJobContext.getContext();

        } finally {
            // 清理上下文
            DataScopeJobContext.clearContext();
        }
    }
}
```

#### Spring Boot集成

Job模块会自动检测`DataScopeProvider` Bean并初始化：

```java
// 自动配置 - 无需额外代码
// 只需确保DataScopeProvider被正确注册
@Service
public class MyDataScopeProvider implements DataScopeProvider {
    // ...
}
```

### 5. DataScopeRouter 统一路由使用

```java
@Service
public class OrderService {

    private final DataScopeRouter router;

    public OrderService(DataScopeRouter router) {
        this.router = router;
    }

    public void processOrder(Long orderId) {
        // 初始化上下文
        router.initScope(currentUserId);

        // 构建行级条件
        String condition = router.buildRowCondition("order");

        // 过滤对象字段
        OrderDTO order = getOrder(orderId);
        router.filter(order, "order");

        // 检查字段可见性
        if (router.canSeeField("order", "amount")) {
            // 处理金额字段
        }
    }
}
```

## 开发团队

- chuck (15524415221@163.com)
- Chuanqiang Liu (361648887@qq.com)

## 项目地址

https://github.com/structure-projects/structure-boot