# structure-datascope

数据范围管理框架，提供统一的数据隔离能力，支持行级和列级的数据权限控制。

## 功能特性

- 支持多种数据存储的范围过滤：
  - MySQL (MyBatis-Plus)
  - Redis
  - Elasticsearch
  - MongoDB
  - Spring Cloud Stream (消息传递)
- **行级权限控制**：在 DAO 层自动添加 WHERE 条件
- **列级权限控制**：在序列化前过滤敏感字段
- **角色 + 权限双重控制**：支持角色和权限标识两种维度的字段可见性控制
- **多表关联查询支持**：自动识别 SQL 中的多表并正确注入数据权限条件
- **租户隔离集成**：与 MyBatis-Plus 租户插件无缝集成
- **缓存数据隔离**：自动为缓存键添加数据权限前缀

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
├── structure-datascope-elasticsearch/  # Elasticsearch 数据范围隔离
├── structure-datascope-mongodb/        # MongoDB 数据范围隔离
├── structure-datascope-cache/           # 缓存数据范围隔离（支持 Caffeine/Ehcache/Redis）
├── structure-datascope-message/        # Spring Cloud Stream 消息数据权限透传
└── structure-datascope-example/         # 示例模块
    ├── structure-datascope-core-example/
    ├── structure-datascope-spring-boot-example/
    ├── structure-datascope-redis-example/
    ├── structure-datascope-mybatis-plus-example/
    ├── structure-datascope-elasticsearch-example/
    ├── structure-datascope-mongodb-example/
    ├── structure-datascope-cache-example/
    ├── structure-datascope-message-example/
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

    # 扫描 DTO 类的包路径，支持列级权限注解
    scan-packages:
      - cn.structured.datascope.example.dto

    # 响应体自动过滤（启用后自动过滤带 @DataScopeRule 注解的 DTO 敏感字段）
    auto-filter-response: true

    # 响应包装类名（用于处理包装类内层 data 字段的过滤）
    result-wrapper-class: cn.structure.common.entity.ResResultVO
    result-wrapper-data-method: getData
```

### 2. 实现数据权限提供器

实现 `DataScopeProvider` 接口，从本地数据源获取用户权限信息：

```java
@Service
public class MyDataScopeProvider implements DataScopeProvider {
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
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
}
```

或继承 `DefaultDataScopeProvider` 抽象类：

```java
@Service
public class MyDataScopeProvider extends DefaultDataScopeProvider {
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Override
    protected DataScopeInfo doGetScopeInfo(String userId) {
        UserRole userRole = userRoleMapper.selectByUserId(userId);
        
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setRoles(userRole.getRoles());
        info.setPermissions(userRole.getPermissions());
        info.setOrgId(userRole.getOrgId());
        info.setDeptIds(userRole.getDeptIds());
        
        return info;
    }
}
```


### 3. 直接操作上下文

```java
// 批量设置数据范围上下文
DataScopeInfo info = new DataScopeInfo();
info.setUserId("user-001");
info.setRoles(Arrays.asList("EMPLOYEE", "FINANCE"));
info.setPermissions(Arrays.asList("order:view_amount"));
info.setOrgId("org-10");
info.setDeptIds(Arrays.asList("dept-1", "dept-2"));
DataScopeContext.set(info);

// 获取当前上下文
DataScopeInfo current = DataScopeContext.get();

// 检查权限
boolean isAdmin = DataScopeContext.hasRole("SYS_ADMIN");
boolean canView = DataScopeContext.hasPermission("order:view_amount");

// 检查用户是否拥有任意角色
boolean hasAnyRole = DataScopeContext.hasAnyRole("SYS_ADMIN", "FINANCE");

// 检查用户是否拥有任意权限
boolean hasAnyPermission = DataScopeContext.hasAnyPermission("order:view_amount", "order:view_secret");

// 清理上下文
DataScopeContext.remove();
```

### 4. 定义数据权限规则（支持角色和权限）

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

### 5. MyBatis-Plus 行级权限过滤

```java
public interface OrderMapper extends BaseMapper<Order> {
    // 无需任何额外注解，DataScopeInterceptor 会自动拦截所有 SELECT 语句
    // 并根据 DataScopeContext 中的 orgId 和 deptIds 自动添加 WHERE 条件
    List<Order> selectAll();
}
```

**自动添加的 WHERE 条件示例**：
```sql
-- 如果当前用户属于 orgId=10, deptIds=[1,2,3]
SELECT * FROM orders WHERE (orders.dept_id IN ('1','2','3'))
```

### 5.1 MyBatis-Plus 完整配置示例

```yaml
structure:
  data-scope:
    enabled: true
    enable-tenant: true          # 启用租户拦截器
    enable-pagination: true      # 启用分页拦截器
    enable-data-scope: true      # 启用数据权限
    auto-register-rules: true
    auto-filter-response: true
    scan-packages:
      - cn.structured.datascope.example.dto
    tenant-id-column: org_id     # 租户字段名
    default-tenant-id: 10        # 默认租户ID
```

**拦截器执行顺序**：
1. `TenantLineInnerInterceptor` - 先添加租户隔离条件
2. `DataScopeInterceptor` - 添加数据权限条件（部门级）
3. `PaginationInnerInterceptor` - 最后执行分页，生成COUNT查询时会包含前面的条件

### 5.2 多表关联查询支持

框架自动识别 SQL 中的多表关联查询，只对主表添加数据权限条件：

```java
// Mapper 接口
public interface OrderUserMapper {

    // 多表关联查询
    @Select("SELECT o.*, u.name FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.status = #{status}")
    List<OrderUserVO> selectOrderWithUser(@Param("status") Integer status);
}
```

自动转换为：
```sql
-- 自动只对主表(orders)添加数据权限条件
SELECT o.*, u.name FROM orders o 
LEFT JOIN users u ON o.user_id = u.id 
WHERE (o.dept_id IN ('1','2','3')) AND o.status = 1
```

### 5.3 自定义多表数据权限处理器

```java
@Service
public class CustomMultiTableDataScopeHandler implements MultiTableDataScopeHandler {

    @Override
    public String handleMultiTable(String sql, List<TableInfo> tableInfos, 
                                   DataScopeFieldConfig fieldConfig) {
        // 自定义多表处理逻辑
        // tableInfos 包含所有表的别名和位置信息
        return sql;
    }
}

// 配置
@Bean
public DataScopeInterceptor dataScopeInterceptor(DataScopeFieldConfig fieldConfig) {
    DataScopeInterceptor interceptor = new DataScopeInterceptor(fieldConfig);
    interceptor.setMultiTableDataScopeHandler(new CustomMultiTableDataScopeHandler());
    return interceptor;
}
```

### 6. 列级权限过滤（序列化前）

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

### 7. 手动检查字段可见性

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
| structure-datascope-mybatis-plus | MyBatis-Plus 分页插件，自动添加行级条件，支持多表关联查询 |
| structure-datascope-redis | Redis 数据范围前缀处理 |
| structure-datascope-elasticsearch | ES 查询自动添加范围过滤 |
| structure-datascope-mongodb | MongoDB 查询自动添加范围过滤 |
| structure-datascope-cache | 缓存数据范围隔离，支持 Caffeine/Ehcache/Redis 等多种缓存 |
| structure-datascope-message | Spring Cloud Stream 消息数据权限透传 |

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