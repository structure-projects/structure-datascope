package cn.structured.datascope.elasticsearch.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 数据规则引擎实现
 * <p>
 * 实现行级数据访问的 Elasticsearch 查询 DSL 构建，
 * 用于在 ES 查询中自动添加数据范围隔离条件。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 方式1: 使用默认字段名配置
 * ElasticDataRuleEngine engine = new ElasticDataRuleEngine();
 * 
 * // 方式2: 使用自定义字段名配置
 * DataScopeFieldConfig fieldConfig = new DataScopeFieldConfig();
 * fieldConfig.setOrgIdField("organization_id");
 * fieldConfig.setDeptIdField("department_id");
 * ElasticDataRuleEngine engine = new ElasticDataRuleEngine(fieldConfig);
 * 
 * engine.registerRule(orderRule);
 * 
 * // 获取行级过滤条件
 * Map&lt;String, Object&gt; filter = engine.buildElasticFilter("order");
 * // 输出: {organization_id: "10", department_id: {$in: ["1", "2", "3"]}}
 * 
 * // 在查询中使用
 * SearchRequest searchRequest = SearchRequest.of(s -&gt; s
 *     .index("orders")
 *     .query(q -&gt; q.bool(b -&gt; b.filter(filter)))
 * );
 * </pre>
 *
 * @see DefaultDataRuleEngine
 * @see DataScopeFieldConfig
 */
@Slf4j
public class ElasticDataRuleEngine extends DefaultDataRuleEngine {

    /**
     * 默认构造函数，使用默认字段配置
     */
    public ElasticDataRuleEngine() {
        super();
    }

    /**
     * 使用自定义字段配置的构造函数
     *
     * @param fieldConfig 字段配置
     */
    public ElasticDataRuleEngine(DataScopeFieldConfig fieldConfig) {
        super(fieldConfig);
    }

    /**
     * 构建行级数据访问条件
     * <p>
     * 返回字符串格式，用于兼容基类接口。
     * 实际ES查询请使用 {@link #buildElasticFilter(String)} 方法。
     * </p>
     *
     * @param resource 资源名称
     * @return 字符串格式的条件
     */
    @Override
    public String buildRowCondition(String resource) {
        return buildRowConditionAsString(resource);
    }

    /**
     * 构建 Elasticsearch 查询过滤器
     *
     * @param resource 资源名称
     * @return ES 查询条件 Map
     */
    public Map<String, Object> buildElasticFilter(String resource) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        Map<String, Object> filter = new HashMap<>();

        // 添加组织级过滤条件（使用父类可配置的字段名）
        if (orgId != null && !orgId.isEmpty()) {
            filter.put(getOrgIdField(), orgId);
        }

        // 添加部门级过滤条件（使用父类可配置的字段名）
        if (deptIds != null && !deptIds.isEmpty()) {
            Map<String, Object> inClause = new HashMap<>();
            inClause.put("$in", deptIds);
            filter.put(getDeptIdField(), inClause);
        }

        if (log.isDebugEnabled()) {
            log.debug("Built Elasticsearch filter for resource '{}': {}", resource, filter);
        }

        return filter;
    }

    /**
     * 构建带原始值的查询条件字符串（用于日志）
     *
     * @param resource 资源名称
     * @return 条件字符串
     */
    public String buildRowConditionAsString(String resource) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        List<String> conditions = new ArrayList<>();

        if (orgId != null && !orgId.isEmpty()) {
            conditions.add(getOrgIdField() + " = '" + orgId + "'");
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            conditions.add(getDeptIdField() + " IN (" + String.join(", ", deptIds) + ")");
        }

        String result = String.join(" AND ", conditions);
        if (log.isDebugEnabled()) {
            log.debug("Built Elasticsearch filter string for resource '{}': {}", resource, result);
        }

        return result;
    }

    /**
     * 将行级条件与现有查询合并
     *
     * @param resource       资源名称
     * @param existingFilter 现有查询条件
     * @return 合并后的查询条件
     */
    public Map<String, Object> mergeRowCondition(String resource, Map<String, Object> existingFilter) {
        Map<String, Object> rowFilter = buildElasticFilter(resource);

        if (existingFilter == null || existingFilter.isEmpty()) {
            return rowFilter;
        }

        if (rowFilter.isEmpty()) {
            return existingFilter;
        }

        // 使用 bool filter 合并条件
        Map<String, Object> must = new HashMap<>();
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(existingFilter);
        filters.add(rowFilter);
        must.put("filter", filters);

        Map<String, Object> bool = new HashMap<>();
        bool.put("bool", must);

        if (log.isDebugEnabled()) {
            log.debug("Merged Elasticsearch filter for resource '{}': {}", resource, bool);
        }

        return bool;
    }

    /**
     * 构建部门级别的过滤条件
     *
     * @param deptIds 部门ID列表
     * @return ES 查询条件
     */
    public Map<String, Object> buildDeptFilter(List<String> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> inClause = new HashMap<>();
        inClause.put("$in", deptIds);

        Map<String, Object> filter = new HashMap<>();
        filter.put(getDeptIdField(), inClause);

        return filter;
    }

    /**
     * 构建组织级别的过滤条件
     *
     * @param orgId 组织ID
     * @return ES 查询条件
     */
    public Map<String, Object> buildOrgFilter(String orgId) {
        if (orgId == null || orgId.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> filter = new HashMap<>();
        filter.put(getOrgIdField(), orgId);

        return filter;
    }
}
