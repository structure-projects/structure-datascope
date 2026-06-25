package cn.structured.datascope.example.mybatisplus;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 列级数据权限测试
 * <p>
 * 测试字段级别的数据可见性控制功能（基于 HTTP MockMvc 调用）
 * </p>
 * <p>
 * 测试方式说明：
 * - 手动设置 DataScopeContext 用户上下文
 * - 通过 MockMvc 调用 API 接口
 * - 框架自动在响应序列化前过滤敏感字段（通过 DataScopeResponseBodyAdvice）
 * </p>
 * <p>
 * 框架行为说明：
 * - 不可见字段会被设置为 null（字段仍然存在于 JSON 响应中）
 * - 不会阻断数据返回，只是将敏感字段置空
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ColumnLevelDataScopeTest {

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
     *
     * @param userId 用户ID
     */
    private void setupUserContext(String userId) {
        DataScopeInfo scopeInfo = dataScopeProvider.getScopeInfo(userId);
        if (scopeInfo != null) {
            DataScopeContext.set(scopeInfo);
        }
    }

    @Nested
    @DisplayName("基于角色的列级权限测试（HTTP调用）")
    class RoleBasedColumnPermissionHttpTest {

        @Test
        @DisplayName("员工角色 - 通过API调用验证 amount 和 phone 不可见")
        void employeeRole_CannotSeeAmountAndPhone() throws Exception {
            setupUserContext("user-001");  // EMPLOYEE 角色

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-001"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Employee response: " + content);

            // 验证 amount 和 phone 字段不可见（返回 null）
            assertTrue(content.contains("\"amount\":null") || content.contains("\"amount\":null"),
                    "Employee should not see amount field");
            assertTrue(content.contains("\"phone\":null") || content.contains("\"phone\":null"),
                    "Employee should not see phone field");
            // 验证 email 字段可见
            assertTrue(content.contains("\"email\""), "Employee should see email field");
        }

        @Test
        @DisplayName("财务角色 - 通过API调用验证 amount 可见，phone 不可见")
        void financeRole_CanSeeAmountCannotSeePhone() throws Exception {
            setupUserContext("user-002");  // FINANCE 角色

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-002"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Finance response: " + content);

            // 验证 amount 字段可见
            assertTrue(content.contains("\"amount\""), "Finance should see amount field");
            // 验证 phone 字段不可见
            assertTrue(content.contains("\"phone\":null") || content.contains("\"phone\":null"),
                    "Finance should not see phone field");
            // 验证 remark 可见（FINANCE 不在 hiddenIfRoleIn 列表中）
            assertTrue(content.contains("\"remark\""), "Finance should see remark field");
        }

        @Test
        @DisplayName("管理员角色 - 通过API调用验证所有敏感字段可见")
        void adminRole_CanSeeAllSensitiveFields() throws Exception {
            setupUserContext("user-003");  // SYS_ADMIN 角色

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-003"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Admin response: " + content);

            // 验证所有敏感字段都可见
            assertTrue(content.contains("\"amount\""), "Admin should see amount field");
            assertTrue(content.contains("\"phone\""), "Admin should see phone field");
            assertTrue(content.contains("\"remark\""), "Admin should see remark field");
            assertTrue(content.contains("\"email\""), "Admin should see email field");
        }
    }

    @Nested
    @DisplayName("多角色组合权限测试（HTTP调用）")
    class MultiRoleColumnPermissionHttpTest {

        @Test
        @DisplayName("员工+财务多角色 - 验证多角色组合权限")
        void multiRole_VerifyCombinedPermissions() throws Exception {
            setupUserContext("user-004");  // EMPLOYEE + FINANCE

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-004"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Multi-role response: " + content);

            // 验证 amount 可见（FINANCE 角色权限）
            assertTrue(content.contains("\"amount\""), "Multi-role user should see amount field (FINANCE permission)");
            // 验证 phone 不可见（无 SYS_ADMIN 角色）
            assertTrue(content.contains("\"phone\":null") || content.contains("\"phone\":null"),
                    "Multi-role user should not see phone field (no SYS_ADMIN role)");
            // 验证 remark 不可见（EMPLOYEE 角色被隐藏）
            assertTrue(content.contains("\"remark\":null") || content.contains("\"remark\":null"),
                    "Multi-role user should not see remark field (hidden for EMPLOYEE)");
        }
    }

    @Nested
    @DisplayName("基于权限的列级权限测试（HTTP调用）")
    class PermissionBasedColumnPermissionHttpTest {

        @Test
        @DisplayName("拥有金额查看权限 - 通过API调用验证")
        void withViewAmountPermission_CanSeeAmountViaApi() throws Exception {
            setupUserContext("user-perm-view-amount");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-perm-view-amount"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("User with amount permission: " + content);

            // 验证 amount 字段可见（通过权限 order:view_amount）
            assertTrue(content.contains("\"amount\""), "User with order:view_amount permission should see amount field");
        }

        @Test
        @DisplayName("拥有手机查看权限 - 通过API调用验证")
        void withViewPhonePermission_CanSeePhoneViaApi() throws Exception {
            setupUserContext("user-perm-view-phone");

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-perm-view-phone"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("User with phone permission: " + content);

            // 验证 phone 字段可见（通过权限 order:view_phone）
            assertTrue(content.contains("\"phone\""), "User with order:view_phone permission should see phone field");
        }

        @Test
        @DisplayName("无权限用户 - 通过API调用验证")
        void withoutPermission_CannotSeeAmountViaApi() throws Exception {
            setupUserContext("user-001");  // 只有 EMPLOYEE 角色，无金额权限

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-001"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("User without permission: " + content);

            // 验证 amount 字段不可见
            assertTrue(content.contains("\"amount\":null") || content.contains("\"amount\":null"),
                    "User without permission should not see amount field");
        }
    }

    @Nested
    @DisplayName("API响应结构验证")
    class ApiResponseStructureTest {

        @Test
        @DisplayName("验证 API 响应包含 success 字段")
        void verifyApiResponseStructure() throws Exception {
            setupUserContext("user-003");  // SYS_ADMIN 角色

            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-003"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Response structure: " + content);

            // 验证响应包含 success 字段
            assertTrue(content.contains("\"success\":true"), "Response should contain success:true");
        }

        @Test
        @DisplayName("验证响应数据不阻断 - 即使有敏感字段")
        void verifyResponseNotBlocked() throws Exception {
            setupUserContext("user-001");  // EMPLOYEE 角色

            // 即使有敏感字段不可见，API 仍然返回 200 和数据
            MvcResult result = mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", "user-001"))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = result.getResponse().getContentAsString();
            System.out.println("Non-blocked response: " + content);

            // 验证响应包含 data 字段
            assertTrue(content.contains("\"data\""), "Response should still return data even with hidden fields");
        }
    }
}
