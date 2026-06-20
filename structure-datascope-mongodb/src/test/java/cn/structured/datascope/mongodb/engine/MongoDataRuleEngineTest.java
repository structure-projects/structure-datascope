package cn.structured.datascope.mongodb.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MongoDataRuleEngineTest {

    private MongoDataRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MongoDataRuleEngine();
        DataScopeContext.remove();
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.remove();
    }

    @Test
    void testBuildMongoFilter_WithOrgAndDept() {
        // 设置上下文
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2", "3"));
        DataScopeContext.setInfo(info);

        // 执行
        Document filter = engine.buildMongoFilter("order");

        // 验证
        assertNotNull(filter);
        assertEquals("10", filter.get("orgId"));
        assertNotNull(filter.get("deptId"));

        Document deptFilter = (Document) filter.get("deptId");
        assertNotNull(deptFilter.get("$in"));
        assertEquals(Arrays.asList("1", "2", "3"), deptFilter.get("$in"));
    }

    @Test
    void testBuildMongoFilter_WithOrgOnly() {
        // 设置上下文 - 只有组织ID
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        DataScopeContext.setInfo(info);

        // 执行
        Document filter = engine.buildMongoFilter("order");

        // 验证
        assertNotNull(filter);
        assertEquals("10", filter.get("orgId"));
        assertNull(filter.get("deptId"));
    }

    @Test
    void testBuildMongoFilter_WithDeptOnly() {
        // 设置上下文 - 只有部门ID
        DataScopeInfo info = new DataScopeInfo();
        info.setDeptIds(Arrays.asList("1", "2"));
        DataScopeContext.setInfo(info);

        // 执行
        Document filter = engine.buildMongoFilter("order");

        // 验证
        assertNotNull(filter);
        assertEquals(1, filter.size());
        assertNotNull(filter.get("deptId"));
    }

    @Test
    void testBuildMongoFilter_EmptyContext() {
        // 不设置上下文

        // 执行
        Document filter = engine.buildMongoFilter("order");

        // 验证 - 应该返回空Document
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
        Document existing = new Document("status", "active");

        // 执行
        Document merged = engine.mergeRowCondition("order", existing);

        // 验证
        assertNotNull(merged);
        assertNotNull(merged.get("$and"));
    }

    @Test
    void testMergeRowCondition_WithEmptyExisting() {
        // 设置上下文
        DataScopeInfo info = new DataScopeInfo();
        info.setOrgId("10");
        DataScopeContext.setInfo(info);

        // 执行
        Document merged = engine.mergeRowCondition("order", new Document());

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
}
