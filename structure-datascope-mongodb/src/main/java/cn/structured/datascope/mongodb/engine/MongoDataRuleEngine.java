package cn.structured.datascope.mongodb.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB 数据规则引擎实现
 * <p>
 * 实现行级数据访问的 MongoDB 查询过滤器构建，
 * 用于在 MongoDB 查询中自动添加数据范围隔离条件。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 方式1: 使用默认字段名配置
 * MongoDataRuleEngine engine = new MongoDataRuleEngine();
 * 
 * // 方式2: 使用自定义字段名配置
 * DataScopeFieldConfig fieldConfig = new DataScopeFieldConfig();
 * fieldConfig.setOrgIdField("organization_id");
 * fieldConfig.setDeptIdField("department_id");
 * MongoDataRuleEngine engine = new MongoDataRuleEngine(fieldConfig);
 * 
 * engine.registerRule(orderRule);
 * 
 * // 获取行级过滤条件
 * Document filter = engine.buildMongoFilter("order");
 * // 输出: Document{{organization_id=10, department_id=Document{{$in=[1, 2, 3]}}}
 * 
 * // 在查询中使用
 * MongoCollection&lt;Document&gt; collection = database.getCollection("orders");
 * FindIterable&lt;Document&gt; results = collection.find(filter);
 * </pre>
 *
 * @see DefaultDataRuleEngine
 * @see DataScopeFieldConfig
 */
@Slf4j
public class MongoDataRuleEngine extends DefaultDataRuleEngine {

    /**
     * 默认构造函数，使用默认字段配置
     */
    public MongoDataRuleEngine() {
        super();
    }

    /**
     * 使用自定义字段配置的构造函数
     *
     * @param fieldConfig 字段配置
     */
    public MongoDataRuleEngine(DataScopeFieldConfig fieldConfig) {
        super(fieldConfig);
    }

    /**
     * 构建行级数据访问条件
     * <p>
     * 返回字符串格式，用于兼容基类接口。
     * 实际MongoDB查询请使用 {@link #buildMongoFilter(String)} 方法。
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
     * 构建 MongoDB 查询过滤器
     *
     * @param resource 资源名称
     * @return MongoDB Document 形式的查询过滤器
     */
    public Document buildMongoFilter(String resource) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        Document filter = new Document();

        // 添加组织级过滤条件（使用父类可配置的字段名）
        if (orgId != null && !orgId.isEmpty()) {
            filter.append(getOrgIdField(), orgId);
        }

        // 添加部门级过滤条件（使用父类可配置的字段名）
        if (deptIds != null && !deptIds.isEmpty()) {
            filter.append(getDeptIdField(), new Document("$in", deptIds));
        }

        Document result = filter.isEmpty() ? new Document() : filter;

        if (log.isDebugEnabled()) {
            log.debug("Built MongoDB filter for resource '{}': {}", resource, result.toJson());
        }

        return result;
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
            log.debug("Built MongoDB filter string for resource '{}': {}", resource, result);
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
    public Document mergeRowCondition(String resource, Document existingFilter) {
        Document rowFilter = buildMongoFilter(resource);

        if (existingFilter == null || existingFilter.isEmpty()) {
            return rowFilter;
        }

        if (rowFilter.isEmpty()) {
            return existingFilter;
        }

        // 使用 $and 合并条件
        List<Document> andConditions = new ArrayList<>();
        andConditions.add(existingFilter);
        andConditions.add(rowFilter);

        Document merged = new Document("$and", andConditions);

        if (log.isDebugEnabled()) {
            log.debug("Merged MongoDB filter for resource '{}': {}", resource, merged.toJson());
        }

        return merged;
    }

    /**
     * 添加部门条件到查询
     *
     * @param filter    现有查询条件
     * @param fieldName 部门字段名
     * @param deptIds   部门ID列表
     * @return 合并后的查询条件
     */
    public Document addDeptCondition(Document filter, String fieldName, List<String> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return filter;
        }

        Document condition = new Document("$in", deptIds);
        Document deptFilter = new Document(fieldName, condition);

        if (filter == null || filter.isEmpty()) {
            return deptFilter;
        }

        List<Document> andConditions = new ArrayList<>();
        andConditions.add(filter);
        andConditions.add(deptFilter);

        return new Document("$and", andConditions);
    }
}
