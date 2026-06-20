package cn.structured.datascope.elasticsearch.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ElasticDataRuleEngineTest {

    private ElasticDataRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ElasticDataRuleEngine();
        DataScopeContext.remove();
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.remove();
    }

    @Test
    void testBuildElasticFilter_WithOrgAndDept() {
        // 设置上下文
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2", "3"));
        DataScopeContext.setInfo(info);

        // 执行
        Map<String, Object> filter = engine.buildElasticFilter("order");

        // 验证
        assertNotNull(filter);
        assertEquals("10", filter.get("orgId"));
        assertNotNull(filter.get("deptId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> deptFilter = (Map<String, Object>) filter.get("deptId");
        assertNotNull(deptFilter.get("$in"));
    }

    @Test
    void testBuildElasticFilter_WithOrgOnly() {
        // 设置上下文 - 只有组织ID
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        DataScopeContext.setInfo(info);

        // 执行
        Map<String, Object> filter = engine.buildElasticFilter("order");

        // 验证
        assertNotNull(filter);
        assertEquals("10", filter.get("orgId"));
        assertNull(filter.get("deptId"));
    }

    @Test
    void testBuildElasticFilter_WithDeptOnly() {
        // 设置上下文 - 只有部门ID
        DataScopeInfo info = new DataScopeInfo();
        info.setDeptIds(Arrays.asList("1", "2"));
        DataScopeContext.setInfo(info);

        // 执行
        Map<String, Object> filter = engine.buildElasticFilter("order");

        // 验证
        assertNotNull(filter);
        assertEquals(1, filter.size());
        assertNotNull(filter.get("deptId"));
    }

    @Test
    void testBuildElasticFilter_EmptyContext() {
        // 不设置上下文

        // 执行
        Map<String, Object> filter = engine.buildElasticFilter("order");

        // 验证 - 应该返回空Map
        assertNotNull(filter);
        assertTrue(filter.isEmpty());
    }

    @Test
    void testBuildRowConditionAsString() {
        // 设置上下文
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2"));
        DataScopeContext.setInfo(info);

        // 执行
        String condition = engine.buildRowConditionAsString("order");

        // 验证
        assertNotNull(condition);
        assertTrue(condition.contains("orgId = '10'"));
        assertTrue(condition.contains("deptId IN (1, 2)"));
    }

    @Test
    void testMergeRowCondition() {
        // 设置上下文
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2"));
        DataScopeContext.setInfo(info);

        // 现有查询条件
        Map<String, Object> existing = Map.of("status", "active");

        // 执行
        Map<String, Object> merged = engine.mergeRowCondition("order", existing);

        // 验证
        assertNotNull(merged);
        assertNotNull(merged.get("bool"));
    }

    @Test
    void testMergeRowCondition_WithEmptyExisting() {
        // 设置上下文
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        DataScopeContext.setInfo(info);

        // 执行
        Map<String, Object> merged = engine.mergeRowCondition("order", Map.of());

        // 验证 - 应该只返回row filter
        assertNotNull(merged);
        assertEquals("10", merged.get("orgId"));
    }

    @Test
    void testBuildRowCondition_ReturnsString() {
        // 测试基类接口兼容性
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1"));
        DataScopeContext.setInfo(info);

        String condition = engine.buildRowCondition("order");

        // 验证返回String类型
        assertNotNull(condition);
        assertTrue(condition instanceof String);
        assertTrue(condition.contains("orgId"));
        assertTrue(condition.contains("deptId"));
    }

    @Test
    void testBuildDeptFilter() {
        // 执行
        Map<String, Object> filter = engine.buildDeptFilter(Arrays.asList("1", "2", "3"));

        // 验证
        assertNotNull(filter);
        assertNotNull(filter.get("deptId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> deptFilter = (Map<String, Object>) filter.get("deptId");
        assertEquals(Arrays.asList("1", "2", "3"), deptFilter.get("$in"));
    }

    @Test
    void testBuildOrgFilter() {
        // 执行
        Map<String, Object> filter = engine.buildOrgFilter("10");

        // 验证
        assertNotNull(filter);
        assertEquals("10", filter.get("orgId"));
    }
}
