package cn.structured.datascope.mybatis.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * MySQL 数据规则引擎实现
 * <p>
 * 实现行级数据访问条件的 SQL WHERE 子句构建，
 * 自动从 DataScopeContext 获取组织ID和部门ID等上下文信息。
 * </p>
 * <p>
 * 支持通过 DataScopeFieldConfig 配置字段名称，默认使用 orgId、deptId。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 方式1: 使用默认字段名配置
 * MySqlDataRuleEngine engine = new MySqlDataRuleEngine();
 * 
 * // 方式2: 使用自定义字段名配置
 * DataScopeFieldConfig fieldConfig = new DataScopeFieldConfig();
 * fieldConfig.setOrgIdField("organization_id");
 * fieldConfig.setDeptIdField("department_id");
 * MySqlDataRuleEngine engine = new MySqlDataRuleEngine(fieldConfig);
 * 
 * engine.registerRule(orderRule);
 *
 * // 获取行级过滤条件
 * String condition = engine.buildRowCondition("order");
 * // 输出: organization_id = ? AND department_id IN (?, ?)
 * </pre>
 *
 * @see DefaultDataRuleEngine
 * @see DataScopeFieldConfig
 */
@Slf4j
public class MySqlDataRuleEngine extends DefaultDataRuleEngine {

    /**
     * 列分隔符
     */
    private static final String COLUMN_SEPARATOR = ", ";

    /**
     * 占位符前缀
     */
    private static final String PLACEHOLDER = "?";

    /**
     * 默认构造函数，使用默认字段配置
     */
    public MySqlDataRuleEngine() {
        super();
    }

    /**
     * 使用自定义字段配置的构造函数
     *
     * @param fieldConfig 字段配置
     */
    public MySqlDataRuleEngine(DataScopeFieldConfig fieldConfig) {
        super(fieldConfig);
    }

    @Override
    public String buildRowCondition(String resource) {
        // 从上下文获取当前用户的数据范围
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        StringBuilder sb = new StringBuilder();

        // 添加组织级过滤条件（使用可配置的字段名）
        if (orgId != null && !orgId.isEmpty()) {
            appendCondition(sb, getOrgIdField(), "=", PLACEHOLDER);
        }

        // 添加部门级过滤条件（使用可配置的字段名）
        if (deptIds != null && !deptIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(getDeptIdField()).append(" IN (");
            for (int i = 0; i < deptIds.size(); i++) {
                if (i > 0) {
                    sb.append(COLUMN_SEPARATOR);
                }
                sb.append(PLACEHOLDER);
            }
            sb.append(")");
        }

        String condition = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("Built MySQL row condition for resource '{}': {}", resource, condition);
        }
        return condition;
    }

    /**
     * 添加条件到 SQL 构建器
     */
    private void appendCondition(StringBuilder sb, String field, String op, String value) {
        if (sb.length() > 0) {
            sb.append(" AND ");
        }
        sb.append(field).append(" ").append(op).append(" ").append(value);
    }

    /**
     * 构建包含原始值的 SQL 条件（用于日志或测试）
     */
    public String buildRowConditionWithValues(String resource) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        StringBuilder sb = new StringBuilder();

        if (orgId != null && !orgId.isEmpty()) {
            appendCondition(sb, getOrgIdField(), "=", wrapStringValue(orgId));
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(getDeptIdField()).append(" IN (");
            for (int i = 0; i < deptIds.size(); i++) {
                if (i > 0) {
                    sb.append(COLUMN_SEPARATOR);
                }
                sb.append(wrapStringValue(deptIds.get(i)));
            }
            sb.append(")");
        }

        return sb.toString();
    }

    private String wrapStringValue(String value) {
        return "'" + value + "'";
    }
}