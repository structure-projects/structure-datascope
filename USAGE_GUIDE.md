# Structure DataScope 数据范围管理 - 完整使用指南

## 一、项目模块结构

```
structure-datascope-example/
├── structure-datascope-spring-boot-example    # Spring Boot Starter 示例
├── structure-datascope-mybatis-plus-example   # MyBatis-Plus 集成示例
├── structure-datascope-redis-example          # Redis 数据权限示例
├── structure-datascope-cache-example           # 缓存数据权限示例
├── structure-datascope-mongodb-example         # MongoDB 数据权限示例
├── structure-datascope-elasticsearch-example   # Elasticsearch 数据权限示例
├── structure-datascope-message-example         # 消息透传示例
└── structure-datascope-job-example             # 定时任务集成示例
```

---

## 二、核心注解说明

### 1. @DataScopeRule - 资源规则注解

标记需要数据权限控制的 DTO/实体类：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@DataScopeRule(resource = "order")  // 定义资源名称
public class OrderResponse {
    private Long id;
    private String orderNo;
    private BigDecimal amount;   // 敏感字段
    private String phone;       // 敏感字段
    private String email;
    private String remark;      // 敏感字段
    private Long orgId;
    private Long deptId;
}
```

### 2. @DataScopeField - 列级权限字段注解

```java
@Data
public class OrderDTO {
    @DataScopeField(name = "amount", visibleIfRoleIn = {"FINANCE", "SYS_ADMIN"})
    private BigDecimal amount;

    @DataScopeField(name = "phone", hiddenIfRoleIn = {"EMPLOYEE"})
    private String phone;

    @DataScopeField(name = "salary", visibleIfPermissionIn = {"order:view_salary"})
    private BigDecimal salary;
}
```

### 3. @DataScopeRow - 行级权限规则注解

```java
@DataScopeRule(resource = "order")
@DataScopeRow(fields = {"org_id", "dept_id"}, ops = {"=", "IN"})
public class Order {
    // org_id 使用 "=" 操作符，值来自 DataScopeContext.getOrgId()
    // dept_id 使用 "IN" 操作符，值来自 DataScopeContext.getDeptIds()
}
```

---

## 三、DataScopeContext 上下文管理

### 核心 API

```java
// 设置/获取上下文
DataScopeContext.set(DataScopeInfo info);
DataScopeContext.get();

// 用户信息
DataScopeContext.getUserId();
DataScopeContext.getOrgId();
DataScopeContext.getDeptIds();
DataScopeContext.getRoles();
DataScopeContext.getPermissions();

// 角色检查
DataScopeContext.hasRole(String role);
DataScopeContext.hasAnyRole(String... roles);

// 权限检查
DataScopeContext.hasPermission(String permission);
DataScopeContext.hasAnyPermission(String... permissions);

// 清理（重要！必须请求结束时调用）
DataScopeContext.remove();
```

---

## 四、DataScopeProvider 接口实现

### 接口定义

```java
public interface DataScopeProvider {
    DataScopeInfo getScopeInfo(String userId);
}
```

### Mock 实现示例

```java
@Component
public class MockDataScopeProvider implements DataScopeProvider {

    private static final Map<String, DataScopeInfo> USER_INFO_MAP = new HashMap<>();

    static {
        // 员工用户 - 基本角色，隐藏 amount, phone, remark
        DataScopeInfo employeeInfo = new DataScopeInfo();
        employeeInfo.setUserId("user-001");
        employeeInfo.setRoles(Arrays.asList("EMPLOYEE"));
        employeeInfo.setOrgId("10");
        employeeInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        employeeInfo.getHiddenFields().put("order", Arrays.asList("amount", "phone", "remark"));
        USER_INFO_MAP.put("user-001", employeeInfo);

        // 财务用户 - 可见订单金额，隐藏 phone
        DataScopeInfo financeInfo = new DataScopeInfo();
        financeInfo.setUserId("user-002");
        financeInfo.setRoles(Arrays.asList("FINANCE"));
        financeInfo.setOrgId("10");
        financeInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        financeInfo.getHiddenFields().put("order", Arrays.asList("phone"));
        USER_INFO_MAP.put("user-002", financeInfo);

        // 管理员用户 - 可见所有敏感字段
        DataScopeInfo adminInfo = new DataScopeInfo();
        adminInfo.setUserId("user-003");
        adminInfo.setRoles(Arrays.asList("SYS_ADMIN"));
        adminInfo.setOrgId("10");
        adminInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-003", adminInfo);

        // 多角色用户
        DataScopeInfo multiInfo = new DataScopeInfo();
        multiInfo.setUserId("user-004");
        multiInfo.setRoles(Arrays.asList("EMPLOYEE", "FINANCE"));
        multiInfo.setOrgId("10");
        multiInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        multiInfo.getHiddenFields().put("order", Arrays.asList("remark", "phone"));
        USER_INFO_MAP.put("user-004", multiInfo);

        // 组织维度用户
        DataScopeInfo org10AdminInfo = new DataScopeInfo();
        org10AdminInfo.setUserId("user-row-org10-admin");
        org10AdminInfo.setRoles(Arrays.asList("ORG_ADMIN"));
        org10AdminInfo.setOrgId("10");
        org10AdminInfo.setDeptIds(Arrays.asList("1", "2", "3", "4", "5"));
        USER_INFO_MAP.put("user-row-org10-admin", org10AdminInfo);

        // 部门维度用户
        DataScopeInfo dept1Info = new DataScopeInfo();
        dept1Info.setUserId("user-row-dept1");
        dept1Info.setRoles(Arrays.asList("EMPLOYEE"));
        dept1Info.setOrgId("10");
        dept1Info.setDeptIds(Arrays.asList("1"));
        USER_INFO_MAP.put("user-row-dept1", dept1Info);

        // 权限维度用户
        DataScopeInfo viewAmountPermInfo = new DataScopeInfo();
        viewAmountPermInfo.setUserId("user-perm-view-amount");
        viewAmountPermInfo.setRoles(Arrays.asList("EMPLOYEE"));
        viewAmountPermInfo.setPermissions(Arrays.asList("order:view_amount"));
        viewAmountPermInfo.setOrgId("10");
        viewAmountPermInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-perm-view-amount", viewAmountPermInfo);
    }

    @Override
    public DataScopeInfo getScopeInfo(String userId) {
        return USER_INFO_MAP.getOrDefault(userId, createDefaultInfo(userId));
    }

    private DataScopeInfo createDefaultInfo(String userId) {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setRoles(Arrays.asList("EMPLOYEE"));
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2", "3"));
        return info;
    }
}
```

---

## 五、Spring Boot 配置示例

### application.yml

```yaml
# DataScope Spring Boot Starter 配置示例
server:
  port: 8081

spring:
  application:
    name: data-scope-example

# 数据范围管理配置
structure:
  data-scope:
    enabled: true
    auto-register-rules: true
    auto-filter-response: true
    scan-packages:
      - cn.structured.datascope.example.dto

logging:
  level:
    cn.structured.datascope: DEBUG
    root: INFO
```

### MyBatis-Plus 配置示例

```yaml
# DataScope MyBatis-Plus 示例配置
server:
  port: 8083

spring:
  datasource:
    url: jdbc:mysql://172.24.20.15:3306/example_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: cn.structured.datascope.example.mybatisplus.entity

# 数据范围管理配置
structure:
  data-scope:
    enabled: true
    enable-tenant: true          # 启用租户拦截器
    enable-pagination: true      # 启用分页拦截器
    enable-data-scope: true      # 启用数据权限
    auto-register-rules: true
    auto-filter-response: true
    scan-packages:
      - cn.structured.datascope.example.mybatisplus.dto
    tenant-id-column: org_id
    default-tenant-id: 10
```

---

## 六、Service 层使用示例

### Spring Boot Starter 服务示例

```java
@Slf4j
@Service
public class OrderService {

    private final DataRuleEngine ruleEngine;

    public OrderService(DataRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 获取订单列表
     * 注意：字段过滤由框架自动完成（通过 DataScopeResponseBodyAdvice）
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list...");
        return createMockOrders();
    }

    /**
     * 获取当前用户的行级过滤条件
     * 用于在 DAO/Mapper 层构建 WHERE 条件
     */
    public String getRowCondition() {
        String condition = ruleEngine.buildRowCondition("order");
        log.info("Generated row condition: {}", condition);
        return condition;
    }
}
```

### MyBatis-Plus 服务示例

```java
@Slf4j
@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 获取订单列表
     * 无需显式处理数据权限，SQL拦截器自动注入权限条件
     */
    public List<OrderResponse> getOrderList() {
        List<OrderEntity> entities = orderMapper.selectList(null);
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建订单
     * 数据权限字段自动从上下文获取并填充
     */
    public OrderResponse createOrder(OrderResponse request) {
        OrderEntity entity = OrderEntity.builder()
                .orderNo(request.getOrderNo())
                .amount(request.getAmount())
                // 数据权限字段自动从上下文获取
                .orgId(Long.parseLong(DataScopeContext.getOrgId()))
                .deptId(Long.parseLong(DataScopeContext.getDeptIds().get(0)))
                .createBy(DataScopeContext.getUserId())
                .build();

        orderMapper.insert(entity);
        return convertToResponse(entity);
    }

    /**
     * 分页查询订单
     * 数据权限通过SQL拦截器自动注入，分页插件自动处理
     */
    public IPage<OrderResponse> getOrderPage(int pageNum, int pageSize, String status) {
        Page<OrderEntity> page = new Page<>(pageNum, pageSize);
        IPage<OrderEntity> entityPage = orderMapper.selectOrderPage(page, status);
        return entityPage.convert(this::convertToResponse);
    }
}
```

---

## 七、Controller 层使用示例

```java
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResResultVO<List<OrderResponse>> getOrderList() {
        // 获取当前数据范围上下文信息
        List<String> roles = DataScopeContext.getRoles();
        log.info("Current request -, roles: {}", roles);

        List<OrderResponse> orders = orderService.getOrderList();
        return ResultUtilSimpleImpl.success(orders);
    }

    @GetMapping("/list")
    public List<OrderResponse> getOrderListDirect() {
        // 直接返回数据，框架自动过滤敏感字段
        return orderService.getOrderList();
    }

    @GetMapping("/row-condition")
    public ResResultVO<String> getRowCondition() {
        String condition = orderService.getRowCondition();
        return ResultUtilSimpleImpl.success(condition);
    }

    @GetMapping("/context")
    public ResResultVO<Object> getContext() {
        return ResultUtilSimpleImpl.success(DataScopeContext.get());
    }
}
```

---

## 八、测试用例使用模式

### 设置用户上下文

```java
@Autowired
private DataScopeProvider dataScopeProvider;

@BeforeEach
void setUp() {
    DataScopeContext.remove();
}

private void setupUserContext(String userId) {
    DataScopeInfo scopeInfo = dataScopeProvider.getScopeInfo(userId);
    if (scopeInfo != null) {
        DataScopeContext.set(scopeInfo);
    }
}

@AfterEach
void tearDown() {
    DataScopeContext.remove();
}
```

### 行级权限测试

```java
@Test
@DisplayName("部门1用户 - 验证部门数据隔离")
void dept1User_QueryOrders() {
    setupUserContext("user-row-dept1");

    List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());

    assertNotNull(orders);
    for (OrderEntity order : orders) {
        assertEquals(1L, order.getDeptId(), "Order deptId should be 1 for dept1 user");
    }
}
```

### 列级权限测试

```java
@Test
@DisplayName("员工角色 - amount 和 phone 不可见")
void employeeRole_CannotSeeAmountAndPhone() throws Exception {
    setupUserContext("user-001");  // EMPLOYEE 角色

    MvcResult result = mockMvc.perform(get("/api/orders/list")
                    .header("X-User-Id", "user-001"))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();

    // 验证 amount 和 phone 字段不可见（返回 null）
    assertTrue(content.contains("\"amount\":null"));
    assertTrue(content.contains("\"phone\":null"));
    // 验证 email 字段可见
    assertTrue(content.contains("\"email\""));
}
```

### 分页与数据权限联合测试

```java
@Test
@DisplayName("分页查询 - 部门1用户")
void pagination_Dept1User() {
    setupUserContext("user-row-dept1");

    IPage<OrderResponse> page = orderService.getOrderPage(1, 5, null);

    assertNotNull(page);
    for (OrderResponse order : page.getRecords()) {
        assertEquals(1L, order.getDeptId());
    }
}
```

---

## 九、Job 定时任务集成示例

### TaskHandler 设置上下文

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncTask {

    private final DataScopeProvider dataScopeProvider;
    private final TaskService taskService;

    @XxlJob("dataSyncTask")
    public void dataSyncTask() {
        String userId = XxlJobHelper.getJobParam();
        if (userId == null || userId.isEmpty()) {
            log.warn("No userId provided in job parameter");
            XxlJobHelper.handleFail("No userId provided in job parameter");
            return;
        }

        // 初始化数据权限上下文
        DataScopeInfo info = initDataScopeContext(userId);
        if (info == null) {
            XxlJobHelper.handleFail("Failed to initialize data scope context");
            return;
        }

        try {
            taskService.executeDataSync();
            XxlJobHelper.handleSuccess("Task executed successfully");
        } catch (Exception e) {
            log.error("Task execution failed", e);
            XxlJobHelper.handleFail("Task execution failed: " + e.getMessage());
        } finally {
            // 必须清理上下文，避免内存泄漏
            DataScopeContext.remove();
        }
    }

    private DataScopeInfo initDataScopeContext(String userId) {
        DataScopeInfo info = dataScopeProvider.getScopeInfo(userId);
        if (info == null) {
            info = new DataScopeInfo();
            info.setUserId(userId);
        }
        DataScopeContext.setInfo(info);
        return info;
    }
}
```

### TaskService 中使用上下文

```java
public void executeDataSync() {
    String orgId = DataScopeContext.getOrgId();
    List<String> deptIds = DataScopeContext.getDeptIds();

    log.debug("Processing data with scope filter: orgId={}, deptIds={}", orgId, deptIds);

    List<TaskResponse> tasks = getTaskList();
    for (TaskResponse task : tasks) {
        processTask(task);
    }
}

public List<TaskResponse> getTaskList() {
    String orgId = DataScopeContext.getOrgId();
    List<String> deptIds = DataScopeContext.getDeptIds();

    return taskStorage.values().stream()
            .filter(task -> isInScope(task, orgId, deptIds))
            .collect(Collectors.toList());
}
```

---

## 十、消息中间件数据权限透传

### 生产者注入

```java
@Autowired
private DataScopeProvider dataScopeProvider;

public void sendOrderEvent(OrderEvent event) {
    // 设置当前用户上下文
    DataScopeInfo info = dataScopeProvider.getScopeInfo(currentUserId);
    DataScopeContext.setInfo(info);

    try {
        // 发送消息，DataScopeMessageUtils 会自动将权限信息注入消息头
        DataScopeMessageUtils.injectDataScopeIntoMessage(message);
        streamBridge.send("order-events-out-0", message);
    } finally {
        DataScopeContext.remove();
    }
}
```

### 消费者提取

```java
@Bean
public Function<OrderEvent, OrderEvent> orderEventProcessor() {
    return event -> {
        // 从消息头提取数据权限信息并设置到上下文
        // 注意：需要通过 Message<?> 获取原始消息
        return event;
    };
}
```

---

## 十一、MyBatis-Plus 拦截器配置

```java
@Configuration
@EnableConfigurationProperties(DataScopeMybatisProperties.class)
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataScopeMyBatisPlusAutoConfiguration {

    @Bean
    public DataScopeInterceptor dataScopeInterceptor(DataScopeFieldConfig fieldConfig) {
        return new DataScopeInterceptor(fieldConfig);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            DataScopeInterceptor dataScopeInterceptor,
            DataScopeMybatisProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 先添加租户拦截器
        if (Boolean.TRUE.equals(properties.getEnableTenant())) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                    new StructureTenantLineHandler(properties)
            ));
        }

        // 2. 添加数据权限拦截器
        interceptor.addInnerInterceptor(dataScopeInterceptor);

        // 3. 最后添加分页拦截器
        if (Boolean.TRUE.equals(properties.getEnablePagination())) {
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(properties.getDbType()));
        }

        return interceptor;
    }
}
```

---

## 十二、DataScopeInfo 数据结构

```java
@Data
public class DataScopeInfo {
    /**
     * 用户角色列表
     */
    private List<String> roles = new ArrayList<>();

    /**
     * 用户权限列表
     */
    private List<String> permissions = new ArrayList<>();

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 部门ID列表
     */
    private List<String> deptIds = new ArrayList<>();

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 列级字段可见性配置
     * key: 资源名称（如 "order"）
     * value: 该资源下隐藏的字段列表
     */
    private Map<String, List<String>> hiddenFields = new HashMap<>();
}
```

---

## 总结

1. **行级权限**：通过 `DataScopeInterceptor` 在 SQL 层面自动注入 `WHERE` 条件
2. **列级权限**：通过 `DataScopeResponseBodyAdvice` 在响应序列化前自动过滤敏感字段
3. **上下文管理**：使用 `DataScopeContext` 通过 ThreadLocal 管理当前线程的数据权限信息
4. **数据权限获取**：实现 `DataScopeProvider` 接口自定义获取数据权限的逻辑
5. **多表支持**：`MultiTableDataScopeHandler` 接口支持复杂 JOIN 场景的数据权限处理
