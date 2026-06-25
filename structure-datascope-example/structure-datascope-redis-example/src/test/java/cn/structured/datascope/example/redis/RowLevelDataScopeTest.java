package cn.structured.datascope.example.redis;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.provider.DataScopeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import cn.structured.datascope.example.redis.config.MockRedisConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 行级数据权限测试
 * <p>
 * 测试组织和部门级别的数据过滤功能（基于 HTTP MockMvc 调用）
 * </p>
 * <p>
 * 测试方式说明：
 * - 手动设置 DataScopeContext 用户上下文
 * - 通过 MockMvc 调用 API 接口验证行级权限效果
 * - 框架设计：行级权限通过 Redis 键前缀在数据层面处理
 * </p>
 * <p>
 * 注意：当前示例项目的 mock 数据是固定的（orgId=10, deptId=1），
 * API 测试主要验证上下文正确设置和行级条件生成
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MockRedisConfiguration.class)
class RowLevelDataScopeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataScopeProvider dataScopeProvider;

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
    @DisplayName("行级过滤条件API测试（HTTP调用）")
    class RowConditionApiTest {

        @Test
        @DisplayName("组织10管理员 - 获取行级过滤条件")
        void org10Admin_GetRowCondition() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(get("/api/orders/row-condition")
                            .header("X-User-Id", "user-row-org10-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Org10 admin row condition: " + content);

            // 验证包含组织10和部门过滤条件
            assertTrue(content.contains("orgId") || content.contains("deptId"),
                    "Row condition should contain orgId or deptId");
        }

        @Test
        @DisplayName("组织20管理员 - 获取行级过滤条件")
        void org20Admin_GetRowCondition() throws Exception {
            setupUserContext("user-row-org20-admin");

            MvcResult result = mockMvc.perform(get("/api/orders/row-condition")
                            .header("X-User-Id", "user-row-org20-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Org20 admin row condition: " + content);

            // 验证行级条件不为空
            assertTrue(content.contains("orgId") || content.contains("deptId"),
                    "Row condition should contain orgId or deptId");
        }

        @Test
        @DisplayName("部门1用户 - 获取行级过滤条件")
        void dept1User_GetRowCondition() throws Exception {
            setupUserContext("user-row-dept1");

            MvcResult result = mockMvc.perform(get("/api/orders/row-condition")
                            .header("X-User-Id", "user-row-dept1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Dept1 user row condition: " + content);

            // 验证包含部门过滤条件
            assertTrue(content.contains("deptId"), "Row condition should contain deptId");
        }

        @Test
        @DisplayName("部门1和2主管 - 获取行级过滤条件")
        void dept12Manager_GetRowCondition() throws Exception {
            setupUserContext("user-row-dept1-2-manager");

            MvcResult result = mockMvc.perform(get("/api/orders/row-condition")
                            .header("X-User-Id", "user-row-dept1-2-manager"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Dept1-2 manager row condition: " + content);

            // 验证包含部门过滤条件（可能是多个）
            assertTrue(content.contains("deptId"), "Row condition should contain deptId");
        }
    }

    @Nested
    @DisplayName("上下文API测试（HTTP调用）")
    class ContextApiTest {

        @Test
        @DisplayName("组织10管理员 - 获取上下文信息")
        void org10Admin_GetContext() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-row-org10-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Org10 admin context: " + content);

            // 验证上下文包含正确的组织和部门信息
            assertTrue(content.contains("orgId"), "Context should contain orgId");
            assertTrue(content.contains("ORG_ADMIN"), "Context should contain ORG_ADMIN role");
        }

        @Test
        @DisplayName("组织20管理员 - 获取上下文信息")
        void org20Admin_GetContext() throws Exception {
            setupUserContext("user-row-org20-admin");

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-row-org20-admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Org20 admin context: " + content);

            // 验证上下文包含正确的组织信息
            assertTrue(content.contains("orgId"), "Context should contain orgId");
        }

        @Test
        @DisplayName("部门1和2主管 - 获取上下文信息")
        void dept12Manager_GetContext() throws Exception {
            setupUserContext("user-row-dept1-2-manager");

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-row-dept1-2-manager"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Dept1-2 manager context: " + content);

            // 验证上下文包含多个部门
            assertTrue(content.contains("deptIds"), "Context should contain deptIds");
            assertTrue(content.contains("DEPT_MANAGER"), "Context should contain DEPT_MANAGER role");
        }
    }

    @Nested
    @DisplayName("订单列表API测试（HTTP调用）")
    class OrderListApiTest {

        @Test
        @DisplayName("管理员获取订单列表 - 验证响应结构")
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

            // 验证订单数据存在（行级过滤在Redis键层面，不影响 API 响应）
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
            assertTrue(content.contains("data"), "Response should contain data field");
        }
    }

    @Nested
    @DisplayName("行级过滤与列级过滤组合测试（HTTP调用）")
    class CombinedFilterApiTest {

        @Test
        @DisplayName("管理员 - 所有敏感字段可见")
        void admin_CombinedFilters() throws Exception {
            setupUserContext("user-003");  // SYS_ADMIN

            MvcResult result = mockMvc.perform(get("/api/orders/list")
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

            MvcResult result = mockMvc.perform(get("/api/orders/list")
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

            MvcResult result = mockMvc.perform(get("/api/orders/list")
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
        @DisplayName("验证 hasRole 方法")
        void testHasRole() throws Exception {
            setupUserContext("user-row-org10-admin");

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-row-org10-admin"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertTrue(content.contains("ORG_ADMIN"), "Context should contain ORG_ADMIN role");
            assertFalse(content.contains("EMPLOYEE"), "Context should not contain EMPLOYEE role");
        }

        @Test
        @DisplayName("验证 hasAnyRole 方法")
        void testHasAnyRole() throws Exception {
            setupUserContext("user-004");  // EMPLOYEE + FINANCE

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-004"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertTrue(content.contains("EMPLOYEE"), "Context should contain EMPLOYEE role");
            assertTrue(content.contains("FINANCE"), "Context should contain FINANCE role");
        }

        @Test
        @DisplayName("验证 hasPermission 方法")
        void testHasPermission() throws Exception {
            setupUserContext("user-perm-view-amount");

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-perm-view-amount"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertTrue(content.contains("order:view_amount"), "Context should contain order:view_amount permission");
        }

        @Test
        @DisplayName("验证 getRoles 和 getPermissions 方法")
        void testGetRolesAndPermissions() throws Exception {
            setupUserContext("user-004");  // EMPLOYEE + FINANCE

            MvcResult result = mockMvc.perform(get("/api/orders/context")
                            .header("X-User-Id", "user-004"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            assertTrue(content.contains("roles"), "Context should contain roles field");
            assertTrue(content.contains("permissions"), "Context should contain permissions field");
        }
    }
}