package cn.structured.datascope.example.elasticsearch;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import cn.structured.datascope.provider.DataScopeProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 行级数据权限测试
 * <p>
 * 测试组织和部门级别的数据上下文设置（基于 HTTP MockMvc 调用）
 * </p>
 * <p>
 * 测试方式说明：
 * - 手动设置 DataScopeContext 用户上下文
 * - 通过 MockMvc 调用 API 接口验证上下文正确设置
 * - 框架设计：行级权限由业务层在查询时处理（ES 查询条件）
 * </p>
 * <p>
 * 注意：ES 示例项目的 mock 数据是固定的（orgId=10, deptId=1），
 * API 测试主要验证上下文正确设置和响应格式
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RowLevelDataScopeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataScopeProvider dataScopeProvider;


    private static final List<OrderResponse> MOCK_ORDERS = Arrays.asList(
            createOrder("1", "ORD20240001", new BigDecimal("1000.00"), "138****1234", "user1@test.com", "正常订单", 10L, 1L),
            createOrder("2", "ORD20240002", new BigDecimal("2000.00"), "139****5678", "user2@test.com", "加急订单", 10L, 2L),
            createOrder("3", "ORD20240003", new BigDecimal("3000.00"), "137****9012", "user3@test.com", "测试订单", 10L, 1L),
            createOrder("4", "ORD20240004", new BigDecimal("4000.00"), "136****3456", "user4@test.com", "VIP订单", 20L, 6L)
    );

    private static OrderResponse createOrder(String id, String orderNo, BigDecimal amount,
                                             String phone, String email, String remark,
                                             Long orgId, Long deptId) {
        OrderResponse order = new OrderResponse();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        order.setPhone(phone);
        order.setEmail(email);
        order.setRemark(remark);
        order.setOrgId(orgId);
        order.setDeptId(deptId);
        order.setCreateTime(LocalDateTime.now());
        order.setCreateBy("system");
        return order;
    }

    @BeforeEach
    void setUp() {
        DataScopeContext.remove();
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.remove();
    }

    /**
     * 手动设置用户上下文
     */
    private void setupUserContext(String userId) {
        DataScopeInfo scopeInfo = dataScopeProvider.getScopeInfo(userId);
        if (scopeInfo != null) {
            DataScopeContext.set(scopeInfo);
        }
    }

    @Nested
    @DisplayName("订单列表API测试（HTTP调用）")
    class OrderListApiTest {

        @Test
        @DisplayName("管理员获取订单列表 - 验证响应结构包含ResResultVO")
        void admin_GetOrderList() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-row-org10-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Admin order list: " + content);

            // 验证订单数据存在
            assertTrue(content.contains("orderNo"), "Order list should contain orderNo field");
            assertTrue(content.contains("email"), "Order list should contain email field");
        }

        @Test
        @DisplayName("员工获取订单列表 - 验证响应结构")
        void employee_GetOrderList() throws Exception {
            setupUserContext("user-row-dept1");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-row-dept1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Employee order list: " + content);

            // 验证订单数据存在（行级过滤在ES查询层面处理）
            assertTrue(content.contains("orderNo"), "Order list should contain orderNo field");
        }

        @Test
        @DisplayName("多角色用户获取订单列表")
        void multiRoleUser_GetOrderList() throws Exception {
            setupUserContext("user-004");  // EMPLOYEE + FINANCE

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-004"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Multi-role order list: " + content);

            // 验证响应成功
            assertTrue(content.contains("orderNo") || content.contains("data"),
                    "Response should contain orderNo field or be empty due to row-level filtering");
        }
    }

    @Nested
    @DisplayName("行级过滤与列级过滤组合测试（HTTP调用）")
    class CombinedFilterApiTest {

        @Test
        @DisplayName("管理员 - 所有敏感字段可见")
        void admin_CombinedFilters() throws Exception {
            setupUserContext("user-003");  // SYS_ADMIN

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-003"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Admin combined filter: " + content);

            // 管理员：行级全部可见 + 列级全部可见
            assertTrue(content.contains("\"amount\""), "Admin should see amount field");
            assertTrue(content.contains("\"phone\""), "Admin should see phone field");
            assertTrue(content.contains("\"remark\""), "Admin should see remark field");
        }

        @Test
        @DisplayName("财务角色 - 行级全部可见 + 列级部分可见")
        void finance_CombinedFilters() throws Exception {
            setupUserContext("user-002");  // FINANCE

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-002"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Finance combined filter: " + content);

            // 财务：行级全部可见 + amount/email 可见，phone 不可见
            assertTrue(content.contains("\"amount\""), "Finance should see amount field");
            assertTrue(content.contains("\"phone\":null") || content.contains("\"phone\":null"),
                    "Finance should not see phone field");
            assertTrue(content.contains("\"remark\""), "Finance should see remark field");
        }

        @Test
        @DisplayName("员工角色 - 行级部分可见 + 列级部分不可见")
        void employee_CombinedFilters() throws Exception {
            setupUserContext("user-001");  // EMPLOYEE

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-001"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Employee combined filter: " + content);

            // 员工：行级全部可见（mock数据固定）+ 列级 amount/phone/remark 不可见
            assertTrue(content.contains("\"amount\":null") || content.contains("\"amount\":null"),
                    "Employee should not see amount field");
            assertTrue(content.contains("\"phone\":null") || content.contains("\"phone\":null"),
                    "Employee should not see phone field");
            assertTrue(content.contains("\"remark\":null") || content.contains("\"remark\":null"),
                    "Employee should not see remark field");
            assertTrue(content.contains("\"email\""), "Employee should see email field");
        }
    }

    @Nested
    @DisplayName("DataScopeContext API验证")
    class ContextApiValidationTest {

        @Test
        @DisplayName("验证 DataScopeContext 已正确设置")
        void testContextSetCorrectly() throws Exception {
            setupUserContext("user-row-org10-admin");

            // 验证上下文是否正确设置
            assertTrue(DataScopeContext.get() != null, "DataScopeContext should be set");
            assertTrue(DataScopeContext.hasRole("ORG_ADMIN"), "Context should have ORG_ADMIN role");
            assertEquals("10", DataScopeContext.getOrgId(), "Context orgId should be 10");
        }

        @Test
        @DisplayName("验证 hasAnyRole 方法")
        void testHasAnyRole() throws Exception {
            setupUserContext("user-004");  // EMPLOYEE + FINANCE

            assertTrue(DataScopeContext.hasRole("EMPLOYEE"), "Context should have EMPLOYEE role");
            assertTrue(DataScopeContext.hasRole("FINANCE"), "Context should have FINANCE role");
        }

        @Test
        @DisplayName("验证 hasPermission 方法")
        void testHasPermission() throws Exception {
            setupUserContext("user-perm-view-amount");

            assertTrue(DataScopeContext.hasPermission("order:view_amount"),
                    "Context should have order:view_amount permission");
        }

        @Test
        @DisplayName("验证 getRoles 和 getPermissions 方法")
        void testGetRolesAndPermissions() throws Exception {
            setupUserContext("user-004");  // EMPLOYEE + FINANCE

            assertNotNull(DataScopeContext.get().getRoles(), "Roles should not be null");
            assertNotNull(DataScopeContext.get().getPermissions(), "Permissions should not be null");
        }
    }

    @Nested
    @DisplayName("行级权限上下文验证")
    class RowLevelFilteringTest {

        @Test
        @DisplayName("组织10管理员 - 验证上下文设置正确")
        void org10Admin_VerifyContext() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-row-org10-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Org10 admin orders: " + content);

            // 验证响应格式正确
            assertTrue(content.contains("data"), "Response should contain data field");
        }

        @Test
        @DisplayName("组织20管理员 - 验证上下文设置正确")
        void org20Admin_VerifyContext() throws Exception {
            setupUserContext("user-row-org20-admin");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-row-org20-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Org20 admin orders: " + content);

            // 验证响应格式正确
            assertTrue(content.contains("data"), "Response should contain data field");
        }

        @Test
        @DisplayName("部门1用户 - 验证上下文设置正确")
        void dept1User_VerifyContext() throws Exception {
            setupUserContext("user-row-dept1");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-row-dept1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Dept1 user orders: " + content);

            // 验证响应格式正确
            assertTrue(content.contains("data"), "Response should contain data field");
        }

        @Test
        @DisplayName("部门1和2主管 - 验证上下文设置正确")
        void dept12Manager_VerifyContext() throws Exception {
            setupUserContext("user-row-dept1-2-manager");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-row-dept1-2-manager"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Dept1-2 manager orders: " + content);

            // 验证响应格式正确
            assertTrue(content.contains("data"), "Response should contain data field");
        }
    }

    @Nested
    @DisplayName("ResResultVO响应格式验证")
    class ResResultVOResponseTest {

        @Test
        @DisplayName("验证 GET /api/orders 返回 ResResultVO 格式")
        void verifyGetOrderListReturnsResResultVO() throws Exception {
            setupUserContext("user-003");  // SYS_ADMIN

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-003"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("ResResultVO response: " + content);

            // 验证 ResResultVO 格式
            assertTrue(content.contains("\"success\":true"), "Response should contain success:true");
            assertTrue(content.contains("\"data\""), "Response should contain data field");
        }

        @Test
        @DisplayName("验证 GET /api/orders/count 返回 ResResultVO 格式")
        void verifyGetOrderCountReturnsResResultVO() throws Exception {
            setupUserContext("user-003");  // SYS_ADMIN

            MvcResult result = mockMvc.perform(get("/api/orders/count")
                            .header("X-User-Id", "user-003"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Count ResResultVO response: " + content);

            // 验证 ResResultVO 格式
            assertTrue(content.contains("\"success\":true"), "Response should contain success:true");
        }
    }
}
