package cn.structured.datascope.example;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import cn.structured.datascope.rule.ColumnRule;
import cn.structured.datascope.rule.DataRule;
import cn.structured.datascope.rule.RowRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 数据范围管理核心功能示例
 * <p>
 * 演示以下功能：
 * <ul>
 *     <li>数据规则注册与使用</li>
 *     <li>列级权限控制（字段可见性）</li>
 *     <li>行级权限控制（SQL 条件构建）</li>
 *     <li>数据范围上下文管理</li>
 * </ul>
 * </p>
 */
@Slf4j
@SpringBootApplication
public class DataScopeCoreExampleApplication {

    public static void main(String[] args) {
        log.info("Starting DataScope Core Example Application...");
        SpringApplication.run(DataScopeCoreExampleApplication.class, args);
        log.info("DataScope Core Example Application started successfully");
    }

    @Bean
    public DataRuleEngine dataRuleEngine() {
        log.info("Registering DataRuleEngine...");
        return new DefaultDataRuleEngine();
    }

    /**
     * 示例运行器
     */
    @Bean
    CommandLineRunner run(DataRuleEngine ruleEngine) {
        return args -> {
            log.info("========== 开始数据权限规则引擎演示 ==========");

            // 1. 注册数据规则
            log.info("\n--- 步骤 1: 注册数据权限规则 ---");
            registerOrderRules(ruleEngine);
            registerUserRules(ruleEngine);

            // 2. 演示不同角色访问订单数据
            log.info("\n--- 步骤 2: 演示不同角色访问订单数据 ---");
            demonstrateOrderAccess(ruleEngine);

            // 3. 演示不同角色访问用户数据
            log.info("\n--- 步骤 3: 演示不同角色访问用户数据 ---");
            demonstrateUserAccess(ruleEngine);

            // 4. 演示行级 SQL 条件构建
            log.info("\n--- 步骤 4: 演示行级 SQL 条件构建 ---");
            demonstrateRowCondition(ruleEngine);

            log.info("\n========== 数据权限规则引擎演示完成 ==========");
            DataScopeContext.remove();
        };
    }

    /**
     * 注册订单相关的数据规则
     */
    private void registerOrderRules(DataRuleEngine ruleEngine) {
        log.info("Registering order data rules...");

        DataRule orderRule = new DataRule();
        orderRule.setResource("order");

        // 行级规则：只能看到 org_id = 10 且 dept_id IN (1, 2, 3) 的订单
        RowRule orgRule = new RowRule();
        orgRule.setField("org_id");
        orgRule.setOp("=");
        orgRule.setValue(10);

        RowRule deptRule = new RowRule();
        deptRule.setField("dept_id");
        deptRule.setOp("IN");
        deptRule.setValue(Arrays.asList(1, 2, 3));

        orderRule.setRowRules(Arrays.asList(orgRule, deptRule));

        // 列级规则
        ColumnRule amountRule = new ColumnRule();
        amountRule.setField("amount");
        amountRule.setVisibleIfRoleIn(Arrays.asList("SYS_ADMIN", "FINANCE"));

        ColumnRule phoneRule = new ColumnRule();
        phoneRule.setField("phone");
        phoneRule.setVisibleIfRoleIn(Arrays.asList("SYS_ADMIN"));

        orderRule.setColumnRules(Arrays.asList(amountRule, phoneRule));

        ruleEngine.registerRule(orderRule);
        log.info("Order data rules registered successfully");
    }

    /**
     * 注册用户相关的数据规则
     */
    private void registerUserRules(DataRuleEngine ruleEngine) {
        log.info("Registering user data rules...");

        DataRule userRule = new DataRule();
        userRule.setResource("user");

        // 列级规则：员工看不到 secret 字段
        ColumnRule secretRule = new ColumnRule();
        secretRule.setField("secret");
        secretRule.setHiddenIfRoleIn(Arrays.asList("EMPLOYEE"));

        userRule.setColumnRules(Arrays.asList(secretRule));

        ruleEngine.registerRule(userRule);
        log.info("User data rules registered successfully");
    }

    /**
     * 演示不同角色访问订单数据
     */
    private void demonstrateOrderAccess(DataRuleEngine ruleEngine) {
        // 创建测试订单
        OrderDTO order = createTestOrder();
        log.info("Original order data: {}", order);

        // 场景 1: 普通员工访问
        log.info("\n>> 场景 1: 普通员工 (EMPLOYEE) 访问 <<");
        setupDataScopeContext(Arrays.asList("EMPLOYEE"), "10", "1", "user_001");
        OrderDTO employeeOrder = createTestOrder();
        ruleEngine.filter(employeeOrder, "order");
        log.info("After filtering (EMPLOYEE): {}", employeeOrder);

        // 场景 2: 财务人员访问
        log.info("\n>> 场景 2: 财务人员 (FINANCE) 访问 <<");
        setupDataScopeContext(Arrays.asList("FINANCE"), "10", "2", "user_002");
        OrderDTO financeOrder = createTestOrder();
        ruleEngine.filter(financeOrder, "order");
        log.info("After filtering (FINANCE): {}", financeOrder);

        // 场景 3: 系统管理员访问
        log.info("\n>> 场景 3: 系统管理员 (SYS_ADMIN) 访问 <<");
        setupDataScopeContext(Arrays.asList("SYS_ADMIN"), "10", "3", "user_003");
        OrderDTO adminOrder = createTestOrder();
        ruleEngine.filter(adminOrder, "order");
        log.info("After filtering (SYS_ADMIN): {}", adminOrder);
    }

    /**
     * 演示不同角色访问用户数据
     */
    private void demonstrateUserAccess(DataRuleEngine ruleEngine) {
        log.info("Checking field visibility for user resource...");

        // 员工无法看到 secret 字段
        setupDataScopeContext(Arrays.asList("EMPLOYEE"), "10", "1", "user_001");
        boolean canEmployeeSeeSecret = ruleEngine.canSeeField("user", "secret");
        log.info("EMPLOYEE can see 'secret' field: {}", canEmployeeSeeSecret);

        // 管理员可以看到 secret 字段
        setupDataScopeContext(Arrays.asList("SYS_ADMIN"), "10", "3", "user_003");
        boolean canAdminSeeSecret = ruleEngine.canSeeField("user", "secret");
        log.info("SYS_ADMIN can see 'secret' field: {}", canAdminSeeSecret);
    }

    /**
     * 演示行级 SQL 条件构建
     */
    private void demonstrateRowCondition(DataRuleEngine ruleEngine) {
        log.info("Building row condition for order resource...");

        // 构建 SQL WHERE 条件
        String condition = ruleEngine.buildRowCondition("order");
        log.info("Generated SQL condition: WHERE {}", condition);

        // 示例 SQL 查询
        String sql = "SELECT * FROM orders WHERE " + condition;
        log.info("Complete SQL query: {}", sql);
    }

    /**
     * 设置数据范围上下文
     */
    private void setupDataScopeContext(List<String> roles, String orgId, String deptId, String userId) {
        DataScopeInfo info = new DataScopeInfo();
        info.setRoles(roles);
        info.setOrgId(orgId);
        info.setDeptIds(Arrays.asList(deptId));
        info.setUserId(userId);
        DataScopeContext.set(info);
        log.info("DataScope context setup: roles={}, orgId={}, userId={}",
                roles, orgId, userId);
    }

    /**
     * 创建测试订单对象
     */
    private OrderDTO createTestOrder() {
        OrderDTO order = new OrderDTO();
        order.setId(1L);
        order.setOrderNo("ORD-2024-001");
        order.setAmount(new BigDecimal("9999.99"));
        order.setPhone("13800138000");
        order.setEmail("customer@example.com");
        order.setRemark("这是一条机密备注");
        order.setOrgId(10L);
        return order;
    }
}