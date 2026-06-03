package cn.structured.datascope.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据权限集成测试
 * <p>
 * 测试 Starter 的自动配置和 Filter 功能
 * </p>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class DataScopeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void testDataScopeFilter_Employee() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("X-DataScope-Id", "scope-employee")
                        .header("X-DataScope-Roles", "EMPLOYEE")
                        .header("X-Org-Id", "10")
                        .header("X-Dept-Ids", "1,2,3")
                        .header("X-User-Id", "user-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.dataScopeId").value("scope-employee"))
                .andExpect(jsonPath("$.data[0].amount").doesNotExist())
                .andExpect(jsonPath("$.data[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data[0].remark").doesNotExist())
                .andExpect(jsonPath("$.data[0].email").exists())
                .andDo(result -> System.out.println("Employee response: " + result.getResponse().getContentAsString()));
    }

    @Test
    void testDataScopeFilter_Finance() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("X-DataScope-Id", "scope-finance")
                        .header("X-DataScope-Roles", "FINANCE")
                        .header("X-Org-Id", "10")
                        .header("X-Dept-Ids", "1,2,3")
                        .header("X-User-Id", "user-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.dataScopeId").value("scope-finance"))
                .andExpect(jsonPath("$.data[0].amount").exists())
                .andExpect(jsonPath("$.data[0].phone").doesNotExist())
                .andExpect(jsonPath("$.data[0].remark").doesNotExist())
                .andDo(result -> System.out.println("Finance response: " + result.getResponse().getContentAsString()));
    }

    @Test
    void testDataScopeFilter_Admin() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("X-DataScope-Id", "scope-admin")
                        .header("X-DataScope-Roles", "SYS_ADMIN")
                        .header("X-Org-Id", "10")
                        .header("X-Dept-Ids", "1,2,3")
                        .header("X-User-Id", "user-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.dataScopeId").value("scope-admin"))
                .andExpect(jsonPath("$.data[0].amount").exists())
                .andExpect(jsonPath("$.data[0].phone").exists())
                .andExpect(jsonPath("$.data[0].remark").exists())
                .andDo(result -> System.out.println("Admin response: " + result.getResponse().getContentAsString()));
    }

    @Test
    void testDataScopeFilter_MultiRoles() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("X-DataScope-Id", "scope-multi")
                        .header("X-DataScope-Roles", "EMPLOYEE,FINANCE")
                        .header("X-Org-Id", "10")
                        .header("X-Dept-Ids", "1,2,3")
                        .header("X-User-Id", "user-004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andDo(result -> System.out.println("Multi-roles response: " + result.getResponse().getContentAsString()));
    }

    @Test
    void testGetRowCondition() throws Exception {
        mockMvc.perform(get("/api/orders/row-condition")
                        .header("X-DataScope-Id", "scope-test")
                        .header("X-DataScope-Roles", "SYS_ADMIN")
                        .header("X-Org-Id", "10")
                        .header("X-Dept-Ids", "1,2,3")
                        .header("X-User-Id", "user-005"))
                .andExpect(status().isOk())
                .andDo(result -> System.out.println("Row condition: " + result.getResponse().getContentAsString()));
    }

    @Test
    void testGetContext() throws Exception {
        mockMvc.perform(get("/api/orders/context")
                        .header("X-DataScope-Id", "scope-test")
                        .header("X-DataScope-Roles", "ADMIN")
                        .header("X-Org-Id", "10")
                        .header("X-User-Id", "user-006"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andDo(result -> System.out.println("Context response: " + result.getResponse().getContentAsString()));
    }
}