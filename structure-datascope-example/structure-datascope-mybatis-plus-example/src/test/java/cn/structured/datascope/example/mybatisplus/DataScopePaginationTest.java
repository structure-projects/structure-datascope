package cn.structured.datascope.example.mybatisplus;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.mybatisplus.dto.OrderResponse;
import cn.structured.datascope.example.mybatisplus.service.OrderService;
import cn.structured.datascope.provider.DataScopeProvider;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分页查询与数据权限插件联合测试
 * <p>
 * 验证以下功能：
 * 1. 分页查询接口正常工作
 * 2. 分页插件与数据权限插件联合使用
 * 3. 多表JOIN场景下的分页与数据权限
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class DataScopePaginationTest {

    @Autowired
    private OrderService orderService;

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

    private void setupUserContext(String userId) {
        DataScopeInfo scopeInfo = dataScopeProvider.getScopeInfo(userId);
        if (scopeInfo != null) {
            DataScopeContext.set(scopeInfo);
        }
    }

    @Nested
    @DisplayName("分页查询测试")
    class PaginationTests {

        @Test
        @DisplayName("分页查询 - 无数据权限上下文")
        void pagination_NoDataScopeContext() {
            IPage<OrderResponse> page = orderService.getOrderPage(1, 5, null);
            
            assertNotNull(page);
            assertNotNull(page.getRecords());
            assertTrue(page.getTotal() >= 0);
            System.out.println("Total records: " + page.getTotal());
            System.out.println("Page size: " + page.getSize());
            System.out.println("Current page: " + page.getCurrent());
        }

        @Test
        @DisplayName("分页查询 - 部门1用户")
        void pagination_Dept1User() {
            setupUserContext("user-row-dept1");

            IPage<OrderResponse> page = orderService.getOrderPage(1, 5, null);

            assertNotNull(page);
            assertNotNull(page.getRecords());
            for (OrderResponse order : page.getRecords()) {
                assertEquals(1L, order.getDeptId(), "Order deptId should be 1");
            }
            System.out.println("Dept1 user page records: " + page.getRecords().size());
        }

        @Test
        @DisplayName("分页查询 - 多部门用户")
        void pagination_MultiDeptUser() {
            setupUserContext("user-row-dept1-2-manager");

            IPage<OrderResponse> page = orderService.getOrderPage(1, 10, null);

            assertNotNull(page);
            assertNotNull(page.getRecords());
            for (OrderResponse order : page.getRecords()) {
                assertTrue(order.getDeptId() == 1L || order.getDeptId() == 2L,
                        "Order deptId should be 1 or 2");
            }
            System.out.println("Multi-dept user page records: " + page.getRecords().size());
        }

        @Test
        @DisplayName("分页查询 - 自定义SQL分页")
        void pagination_CustomSqlPage() {
            setupUserContext("user-row-dept1");

            IPage<OrderResponse> page = orderService.getOrderPage(1, 5, "COMPLETED");

            assertNotNull(page);
            assertNotNull(page.getRecords());
            System.out.println("Custom SQL page records: " + page.getRecords().size());
        }

        @Test
        @DisplayName("分页查询 - Wrapper分页")
        void pagination_WrapperPage() {
            setupUserContext("user-row-dept1");

            IPage<OrderResponse> page = orderService.getOrderPageByWrapper(1, 5, null);

            assertNotNull(page);
            assertNotNull(page.getRecords());
            System.out.println("Wrapper page records: " + page.getRecords().size());
        }

        @Test
        @DisplayName("分页查询 - 多页测试")
        void pagination_MultiplePages() {
            setupUserContext("user-row-dept1");

            IPage<OrderResponse> page1 = orderService.getOrderPage(1, 2, null);
            IPage<OrderResponse> page2 = orderService.getOrderPage(2, 2, null);

            assertNotNull(page1);
            assertNotNull(page2);
            
            // 验证分页数据不重复（假设每页2条）
            if (page1.getRecords().size() > 0 && page2.getRecords().size() > 0) {
                assertNotEquals(page1.getRecords().get(0).getId(), 
                        page2.getRecords().get(0).getId());
            }
            
            System.out.println("Page 1 records: " + page1.getRecords().size());
            System.out.println("Page 2 records: " + page2.getRecords().size());
        }

        @Test
        @DisplayName("分页查询 - 超出范围页码")
        void pagination_OutOfRangePage() {
            setupUserContext("user-row-dept1");

            IPage<OrderResponse> page = orderService.getOrderPage(999, 10, null);

            assertNotNull(page);
            assertEquals(999, page.getCurrent());
            // 超出范围应该返回空记录或最后一页
        }
    }

    @Nested
    @DisplayName("插件联合使用测试")
    class PluginIntegrationTests {

        @Test
        @DisplayName("分页插件 + 数据权限插件 - 联合测试")
        void integration_PaginationAndDataScope() {
            setupUserContext("user-row-dept1");

            // 使用自定义SQL分页，测试数据权限拦截器与分页插件联合工作
            IPage<OrderResponse> page = orderService.getOrderPage(1, 5, null);

            assertNotNull(page);
            assertNotNull(page.getRecords());
            
            // 验证数据权限生效
            for (OrderResponse order : page.getRecords()) {
                assertEquals(1L, order.getDeptId());
            }
            
            // 验证分页信息正确
            assertTrue(page.getTotal() >= page.getRecords().size());
            assertEquals(1, page.getCurrent());
            assertEquals(5, page.getSize());
            
            System.out.println("Integration test - Total: " + page.getTotal() + ", Records: " + page.getRecords().size());
        }

        @Test
        @DisplayName("分页插件 + 数据权限插件 + 条件查询")
        void integration_WithCondition() {
            setupUserContext("user-row-dept1");

            // 添加状态条件
            IPage<OrderResponse> page = orderService.getOrderPage(1, 5, "COMPLETED");

            assertNotNull(page);
            
            System.out.println("Condition test - Status: COMPLETED, Records: " + page.getRecords().size());
        }

        @Test
        @DisplayName("分页插件 + 数据权限插件 - 管理员无限制")
        void integration_AdminUser() {
            setupUserContext("user-003");  // SYS_ADMIN

            IPage<OrderResponse> page = orderService.getOrderPage(1, 10, null);

            assertNotNull(page);
            // 管理员应该能看到所有数据
            System.out.println("Admin user page records: " + page.getRecords().size());
        }
    }

    @Nested
    @DisplayName("性能与边界测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("分页查询 - 空结果")
        void edgeCase_EmptyResult() {
            setupUserContext("user-row-dept1");

            // 查询不存在的状态
            IPage<OrderResponse> page = orderService.getOrderPage(1, 10, "NON_EXISTENT_STATUS");

            assertNotNull(page);
            assertEquals(0, page.getRecords().size());
            assertEquals(0, page.getTotal());
        }

        @Test
        @DisplayName("分页查询 - 大数据量")
        void edgeCase_LargePageSize() {
            setupUserContext("user-row-dept1");

            IPage<OrderResponse> page = orderService.getOrderPage(1, 100, null);

            assertNotNull(page);
            assertTrue(page.getRecords().size() <= 100);
        }

        @Test
        @DisplayName("分页查询 - 页码为0")
        void edgeCase_ZeroPageNum() {
            setupUserContext("user-row-dept1");

            // 页码为0应该返回第一页或报错
            try {
                IPage<OrderResponse> page = orderService.getOrderPage(0, 10, null);
                assertNotNull(page);
            } catch (Exception e) {
                // 允许抛出异常
            }
        }

        @Test
        @DisplayName("分页查询 - 负数页码")
        void edgeCase_NegativePageNum() {
            setupUserContext("user-row-dept1");

            try {
                IPage<OrderResponse> page = orderService.getOrderPage(-1, 10, null);
                assertNotNull(page);
            } catch (Exception e) {
                // 允许抛出异常
            }
        }
    }
}
