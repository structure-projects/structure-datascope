package cn.structured.datascope;

import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.rule.ColumnRule;
import cn.structured.datascope.rule.DataRule;
import cn.structured.datascope.rule.RowRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DataRuleEngineTest {

    private DataRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DataRuleEngine();
        DataScopeContext.remove();
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.remove();
    }

    @Test
    void testRegisterAndGetRule() {
        DataRule rule = new DataRule();
        rule.setResource("test");
        
        engine.registerRule(rule);
        
        assertNotNull(engine.getRule("test"));
        assertNull(engine.getRule("not-exists"));
    }

    @Test
    void testCanSeeField_WithVisibleRule() {
        DataRule rule = new DataRule();
        rule.setResource("order");
        
        ColumnRule columnRule = new ColumnRule();
        columnRule.setField("amount");
        columnRule.setVisible(true);
        rule.setColumnRules(Collections.singletonList(columnRule));
        
        engine.registerRule(rule);
        
        assertTrue(engine.canSeeField("order", "amount"));
        assertTrue(engine.canSeeField("order", "other"));
    }

    @Test
    void testCanSeeField_WithVisibleIfRoleIn() {
        DataRule rule = new DataRule();
        rule.setResource("order");
        
        ColumnRule columnRule = new ColumnRule();
        columnRule.setField("amount");
        columnRule.setVisibleIfRoleIn(Arrays.asList("FINANCE", "ADMIN"));
        rule.setColumnRules(Collections.singletonList(columnRule));
        
        engine.registerRule(rule);
        
        DataScopeContext.setRoles(Arrays.asList("FINANCE"));
        assertTrue(engine.canSeeField("order", "amount"));
        
        DataScopeContext.setRoles(Arrays.asList("ADMIN"));
        assertTrue(engine.canSeeField("order", "amount"));
        
        DataScopeContext.setRoles(Arrays.asList("USER"));
        assertFalse(engine.canSeeField("order", "amount"));
        
        DataScopeContext.setRoles(Collections.emptyList());
        assertFalse(engine.canSeeField("order", "amount"));
    }

    @Test
    void testCanSeeField_WithHiddenIfRoleIn() {
        DataRule rule = new DataRule();
        rule.setResource("user");
        
        ColumnRule columnRule = new ColumnRule();
        columnRule.setField("secret");
        columnRule.setHiddenIfRoleIn(Collections.singletonList("EMPLOYEE"));
        rule.setColumnRules(Collections.singletonList(columnRule));
        
        engine.registerRule(rule);
        
        DataScopeContext.setRoles(Arrays.asList("EMPLOYEE"));
        assertFalse(engine.canSeeField("user", "secret"));
        
        DataScopeContext.setRoles(Arrays.asList("ADMIN"));
        assertTrue(engine.canSeeField("user", "secret"));
    }

    @Test
    void testFilter_AnnotationBased() {
        Order order = new Order();
        order.setId(1L);
        order.setAmount(new BigDecimal("100.00"));
        order.setPhone("13800138000");
        order.setEmail("test@example.com");
        order.setSecretField("secret");
        
        DataScopeContext.setRoles(Collections.emptyList());
        engine.filter(order, "order");
        
        assertNotNull(order.getId());
        assertNull(order.getAmount());
        assertNull(order.getPhone());
        assertNotNull(order.getEmail());
        assertNotNull(order.getSecretField());
        
        DataScopeContext.setRoles(Arrays.asList("FINANCE"));
        order.setAmount(new BigDecimal("200.00"));
        engine.filter(order, "order");
        
        assertNotNull(order.getAmount());
        assertNull(order.getPhone());
        assertNotNull(order.getEmail());
        
        DataScopeContext.setRoles(Arrays.asList("SYS_ADMIN"));
        order.setPhone("13900139000");
        engine.filter(order, "order");
        
        assertNotNull(order.getAmount());
        assertNotNull(order.getPhone());
        
        DataScopeContext.setRoles(Arrays.asList("EMPLOYEE"));
        order.setSecretField("secret2");
        engine.filter(order, "order");
        
        assertNull(order.getSecretField());
    }

    @Test
    void testBuildRowCondition() {
        DataRule rule = new DataRule();
        rule.setResource("order");
        
        RowRule rule1 = new RowRule();
        rule1.setField("org_id");
        rule1.setOp("=");
        rule1.setValue(10);
        
        RowRule rule2 = new RowRule();
        rule2.setField("dept_id");
        rule2.setOp("IN");
        rule2.setValue(Arrays.asList(1, 2, 3));
        
        rule.setRowRules(Arrays.asList(rule1, rule2));
        engine.registerRule(rule);
        
        String condition = engine.buildRowCondition("order");
        assertNotNull(condition);
        assertTrue(condition.contains("org_id"));
        assertTrue(condition.contains("dept_id"));
        assertTrue(condition.contains("IN"));
    }

    @Test
    void testBuildRowCondition_NoRule() {
        String condition = engine.buildRowCondition("not-exists");
        assertEquals("", condition);
    }
}