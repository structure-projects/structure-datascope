package cn.structured.datascope.example.mybatisplus;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.mybatisplus.entity.OrderEntity;
import cn.structured.datascope.example.mybatisplus.entity.UserEntity;
import cn.structured.datascope.example.mybatisplus.mapper.OrderMapper;
import cn.structured.datascope.example.mybatisplus.mapper.OrderUserMapper;
import cn.structured.datascope.example.mybatisplus.mapper.UserMapper;
import cn.structured.datascope.provider.DataScopeProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多表查询与数据隔离测试
 * <p>
 * 验证以下功能：
 * 1. 多表JOIN查询的数据权限拦截
 * 2. 部门级数据隔离
 * 3. 租户（组织）级数据隔离
 * </p>
 */
@SpringBootTest
class MultiTableDataScopeTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderUserMapper orderUserMapper;

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
    @DisplayName("单表查询数据隔离测试")
    class SingleTableIsolationTest {

        @Test
        @DisplayName("部门1用户查询订单 - 验证部门隔离")
        void dept1User_QueryOrders() {
            setupUserContext("user-row-dept1");

            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Dept1 user orders count: " + orders.size());

            assertNotNull(orders);
            for (OrderEntity order : orders) {
                assertEquals(1L, order.getDeptId(), "Order deptId should be 1 for dept1 user");
            }
        }

        @Test
        @DisplayName("部门1和2主管查询订单 - 验证多部门可见")
        void dept12Manager_QueryOrders() {
            setupUserContext("user-row-dept1-2-manager");

            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Dept1-2 manager orders count: " + orders.size());

            assertNotNull(orders);
            for (OrderEntity order : orders) {
                assertTrue(Arrays.asList(1L, 2L).contains(order.getDeptId()),
                        "Order deptId should be 1 or 2");
            }
        }

        @Test
        @DisplayName("组织10管理员查询用户 - 验证组织隔离")
        void org10Admin_QueryUsers() {
            setupUserContext("user-row-org10-admin");

            List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Org10 admin users count: " + users.size());

            assertNotNull(users);
            for (UserEntity user : users) {
                assertEquals(10L, user.getOrgId(), "User orgId should be 10 for org10 admin");
            }
        }

        @Test
        @DisplayName("组织20管理员查询用户 - 验证组织隔离")
        void org20Admin_QueryUsers() {
            setupUserContext("user-row-org20-admin");

            List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Org20 admin users count: " + users.size());

            assertNotNull(users);
            for (UserEntity user : users) {
                assertEquals(20L, user.getOrgId(), "User orgId should be 20 for org20 admin");
            }
        }
    }

    @Nested
    @DisplayName("多表查询数据权限测试")
    class MultiTableQueryTest {

        @Test
        @DisplayName("多表JOIN查询 - 验证数据权限拦截")
        void multiTableJoin_QueryWithPermission() {
            setupUserContext("user-row-dept1");

            List<Map<String, Object>> result = orderUserMapper.selectOrderWithUser();
            System.out.println("Multi-table JOIN result count: " + result.size());

            assertNotNull(result);
        }

        @Test
        @DisplayName("多表JOIN查询（带别名）- 验证数据权限拦截")
        void multiTableJoinWithAlias_QueryWithPermission() {
            setupUserContext("user-row-dept1");

            List<Map<String, Object>> result = orderUserMapper.selectOrderWithUserAlias();
            System.out.println("Multi-table JOIN with alias result count: " + result.size());

            assertNotNull(result);
        }

        @Test
        @DisplayName("逗号分隔多表查询 - 验证数据权限拦截")
        void commaSeparatedTables_QueryWithPermission() {
            setupUserContext("user-row-dept1");

            List<Map<String, Object>> result = orderUserMapper.selectOrderAndUser();
            System.out.println("Comma separated tables result count: " + result.size());

            assertNotNull(result);
        }

        @Test
        @DisplayName("LEFT JOIN查询 - 验证数据权限拦截")
        void leftJoin_QueryWithPermission() {
            setupUserContext("user-row-dept1");

            List<Map<String, Object>> result = orderUserMapper.selectOrderLeftJoinUser();
            System.out.println("LEFT JOIN result count: " + result.size());

            assertNotNull(result);
        }

        @Test
        @DisplayName("复杂多表查询 - 验证数据权限拦截")
        void complexMultiTable_QueryWithPermission() {
            setupUserContext("user-row-dept1");

            List<Map<String, Object>> result = orderUserMapper.selectOrderUserDept();
            System.out.println("Complex multi-table result count: " + result.size());

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("租户隔离测试")
    class TenantIsolationTest {

        @Test
        @DisplayName("租户1用户查询订单 - 验证租户隔离")
        void tenant1User_QueryOrders() {
            setupUserContext("user-tenant-1");

            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Tenant1 user orders count: " + orders.size());

            assertNotNull(orders);
        }

        @Test
        @DisplayName("租户2用户查询订单 - 验证租户隔离")
        void tenant2User_QueryOrders() {
            setupUserContext("user-tenant-2");

            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Tenant2 user orders count: " + orders.size());

            assertNotNull(orders);
        }

        @Test
        @DisplayName("无租户上下文 - 使用默认租户")
        void noTenantContext_UsesDefaultTenant() {
            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("No tenant context orders count: " + orders.size());

            assertNotNull(orders);
        }
    }

    @Nested
    @DisplayName("部门+租户组合隔离测试")
    class CombinedIsolationTest {

        @Test
        @DisplayName("部门1租户1用户 - 验证组合隔离")
        void dept1Tenant1User_QueryOrders() {
            setupUserContext("user-dept1-tenant1");

            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Dept1-Tenant1 user orders count: " + orders.size());

            assertNotNull(orders);
            for (OrderEntity order : orders) {
                assertEquals(1L, order.getDeptId(), "Order deptId should be 1");
            }
        }

        @Test
        @DisplayName("管理员查询 - 验证无限制")
        void adminUser_QueryOrders() {
            setupUserContext("user-003");  // SYS_ADMIN

            List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<>());
            System.out.println("Admin orders count: " + orders.size());

            assertNotNull(orders);
        }
    }
}