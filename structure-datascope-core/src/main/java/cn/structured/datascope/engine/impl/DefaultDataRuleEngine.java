package cn.structured.datascope.engine.impl;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import cn.structured.datascope.cache.DataScopeClassCache;
import cn.structured.datascope.cache.DataScopeResourceMeta;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.rule.ColumnRule;
import cn.structured.datascope.rule.DataRule;
import cn.structured.datascope.rule.RowRule;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认数据规则引擎实现
 * <p>
 * 提供标准的列级和行级权限处理逻辑，适用于通用场景。
 * 不同数据源可继承此类并重写特定方法，或实现 DataRuleEngine 接口自定义实现。
 * </p>
 * <p>
 * 支持字段名称配置，可通过 DataScopeFieldConfig 配置 orgId、deptId 等字段名。
 * </p>
 *
 * @see DataRuleEngine
 * @see DataScopeFieldConfig
 */
@Slf4j
public class DefaultDataRuleEngine implements DataRuleEngine {

    /**
     * 数据规则缓存，key 为资源名称
     */
    private final Map<String, DataRule> ruleCache = new HashMap<>();

    /**
     * 字段名称配置
     */
    private DataScopeFieldConfig fieldConfig;

    /**
     * 默认构造函数，使用默认字段配置
     */
    public DefaultDataRuleEngine() {
        this.fieldConfig = new DataScopeFieldConfig();
        log.debug("DefaultDataRuleEngine initialized with default field config");
    }

    /**
     * 使用自定义字段配置的构造函数
     *
     * @param fieldConfig 字段配置
     */
    public DefaultDataRuleEngine(DataScopeFieldConfig fieldConfig) {
        this.fieldConfig = fieldConfig != null ? fieldConfig : new DataScopeFieldConfig();
        if (log.isDebugEnabled()) {
            log.debug("DefaultDataRuleEngine initialized with field config: orgIdField={}, deptIdField={}, userIdField={}",
                    this.fieldConfig.getOrgIdField(),
                    this.fieldConfig.getDeptIdField(),
                    this.fieldConfig.getUserIdField());
        }
    }

    @Override
    public void registerRule(DataRule rule) {
        ruleCache.put(rule.getResource(), rule);
        log.info("Registered data rule for resource: {}", rule.getResource());
    }

    @Override
    public DataRule getRule(String resource) {
        return ruleCache.get(resource);
    }

    @Override
    public boolean canSeeField(String resource, String fieldName) {
        DataRule rule = getRule(resource);
        if (rule == null) {
            log.debug("No rule found for resource: {}, field '{}' visible by default", resource, fieldName);
            return true;
        }

        List<ColumnRule> columnRules = rule.getColumnRules();
        if (columnRules == null || columnRules.isEmpty()) {
            log.debug("No column rules for resource: {}, field '{}' visible by default", resource, fieldName);
            return true;
        }

        for (ColumnRule columnRule : columnRules) {
            if (columnRule.getField().equals(fieldName)) {
                boolean visible = evaluateColumnRule(columnRule);
                log.debug("Field '{}' visibility check: {}", fieldName, visible);
                return visible;
            }
        }

        log.debug("No column rule for field '{}', visible by default", fieldName);
        return true;
    }

    /**
     * 评估列级权限规则（支持角色和权限两种维度）
     *
     * @param columnRule 列级规则
     * @return true 表示字段可见
     */
    protected boolean evaluateColumnRule(ColumnRule columnRule) {
        List<String> userRoles = DataScopeContext.getRoles();
        List<String> userPermissions = DataScopeContext.getPermissions();

        if (log.isTraceEnabled()) {
            log.trace("Evaluating column rule: {}, user roles: {}, user permissions: {}",
                    columnRule, userRoles, userPermissions);
        }

        // 优先检查隐藏规则（角色维度）
        if (!columnRule.getHiddenIfRoleIn().isEmpty()) {
            for (String role : columnRule.getHiddenIfRoleIn()) {
                if (userRoles.contains(role)) {
                    log.debug("Field hidden because user has role: {}", role);
                    return false;
                }
            }
        }

        // 优先检查隐藏规则（权限维度）
        if (!columnRule.getHiddenIfPermissionIn().isEmpty()) {
            for (String permission : columnRule.getHiddenIfPermissionIn()) {
                if (userPermissions.contains(permission)) {
                    log.debug("Field hidden because user has permission: {}", permission);
                    return false;
                }
            }
        }

        // 检查可见规则（角色维度）
        if (!columnRule.getVisibleIfRoleIn().isEmpty()) {
            for (String role : columnRule.getVisibleIfRoleIn()) {
                if (userRoles.contains(role)) {
                    log.debug("Field visible because user has role: {}", role);
                    return true;
                }
            }
            // 如果配置了角色但用户没有任何所需角色，则继续检查权限
            if (columnRule.getVisibleIfPermissionIn().isEmpty()) {
                log.debug("Field hidden because user does not have any of the required roles");
                return false;
            }
        }

        // 检查可见规则（权限维度）
        if (!columnRule.getVisibleIfPermissionIn().isEmpty()) {
            for (String permission : columnRule.getVisibleIfPermissionIn()) {
                if (userPermissions.contains(permission)) {
                    log.debug("Field visible because user has permission: {}", permission);
                    return true;
                }
            }
            // 如果配置了权限但用户没有任何所需权限，则字段不可见
            log.debug("Field hidden because user does not have any of the required permissions");
            return false;
        }

        // 默认返回字段的可见性设置
        return columnRule.isVisible();
    }

    @Override
    public void filter(Object obj, String resource) {
        if (obj == null) {
            log.debug("Object is null, skip filtering");
            return;
        }

        Class<?> clazz = obj.getClass();
        String className = clazz.getName();

        // 通过缓存检查是否有 @DataScopeRule 注解
        DataScopeResourceMeta meta = DataScopeClassCache.get(className);
        if (meta != null) {
            resource = meta.getResource();
        } else if (clazz.isAnnotationPresent(DataScopeRule.class)) {
            // 缓存未命中时使用反射（备用）
            DataScopeRule classAnnotation = clazz.getAnnotation(DataScopeRule.class);
            resource = classAnnotation != null ? classAnnotation.resource() : resource;
        }

        if (log.isDebugEnabled()) {
            log.debug("Filtering object of type {}, using resource: {}", clazz.getSimpleName(), resource);
        }

        // 首先检查 DataScopeInfo 中的 hiddenFields 配置
        if (DataScopeContext.getInfo() != null
                && DataScopeContext.getInfo().getHiddenFields() != null
                && !DataScopeContext.getInfo().getHiddenFields().isEmpty()) {
            filterUsingScopeInfo(obj, resource);
            return;
        }

        // 如果没有 hiddenFields 配置，回退到使用注解方式
        filterUsingAnnotations(obj, resource);
    }

    /**
     * 使用 DataScopeInfo 中的配置过滤字段
     */
    private void filterUsingScopeInfo(Object obj, String resource) {
        if (obj == null || resource == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        Map<String, List<String>> allHiddenFields = DataScopeContext.getInfo() != null
                ? DataScopeContext.getInfo().getHiddenFields()
                : null;

        if (allHiddenFields == null || allHiddenFields.isEmpty()) {
            log.debug("No hidden fields configured in DataScopeInfo");
            return;
        }

        List<String> hiddenFieldList = allHiddenFields.get(resource);
        if (hiddenFieldList == null || hiddenFieldList.isEmpty()) {
            log.debug("No hidden fields configured for resource: {}", resource);
            return;
        }

        Field[] fields = clazz.getDeclaredFields();
        int filteredCount = 0;

        for (Field field : fields) {
            String fieldName = field.getName();
            if (hiddenFieldList.contains(fieldName)) {
                try {
                    field.setAccessible(true);
                    field.set(obj, null);
                    filteredCount++;
                    log.debug("Field '{}' set to null (hidden by DataScopeInfo config)", fieldName);
                } catch (IllegalAccessException e) {
                    log.warn("Failed to set field {} to null", fieldName, e);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Filtered {} fields for object of type {} using DataScopeInfo", filteredCount, clazz.getSimpleName());
        }
    }

    /**
     * 使用注解方式过滤字段（向后兼容）
     */
    private void filterUsingAnnotations(Object obj, String resource) {
        Class<?> clazz = obj.getClass();

        Field[] fields = clazz.getDeclaredFields();
        int filteredCount = 0;

        for (Field field : fields) {
            DataScopeField fieldAnnotation = field.getAnnotation(DataScopeField.class);
            String fieldName = fieldAnnotation != null && !fieldAnnotation.name().isEmpty()
                    ? fieldAnnotation.name()
                    : field.getName();

            boolean visible = true;

            if (fieldAnnotation != null) {
                visible = evaluateFieldAnnotation(fieldAnnotation);
            } else {
                visible = canSeeField(resource, fieldName);
            }

            if (!visible) {
                try {
                    field.setAccessible(true);
                    field.set(obj, null);
                    filteredCount++;
                    log.debug("Field '{}' set to null because not visible", fieldName);
                } catch (IllegalAccessException e) {
                    log.warn("Failed to set field {} to null", fieldName, e);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Filtered {} fields for object of type {} using annotations", filteredCount, clazz.getSimpleName());
        }
    }

    /**
     * 评估字段注解（支持角色和权限两种维度）
     *
     * @param annotation 字段注解
     * @return true 表示字段可见
     */
    protected boolean evaluateFieldAnnotation(DataScopeField annotation) {
        List<String> userRoles = DataScopeContext.getRoles();
        List<String> userPermissions = DataScopeContext.getPermissions();

        if (log.isTraceEnabled()) {
            log.trace("Evaluating field annotation: {}, user roles: {}, user permissions: {}",
                    annotation, userRoles, userPermissions);
        }

        // 优先检查隐藏规则（角色维度）
        if (annotation.hiddenIfRoleIn().length > 0) {
            for (String role : annotation.hiddenIfRoleIn()) {
                if (userRoles.contains(role)) {
                    log.debug("Field hidden by annotation because user has role: {}", role);
                    return false;
                }
            }
        }

        // 优先检查隐藏规则（权限维度）
        if (annotation.hiddenIfPermissionIn().length > 0) {
            for (String permission : annotation.hiddenIfPermissionIn()) {
                if (userPermissions.contains(permission)) {
                    log.debug("Field hidden by annotation because user has permission: {}", permission);
                    return false;
                }
            }
        }

        // 检查可见规则（角色维度）
        if (annotation.visibleIfRoleIn().length > 0) {
            for (String role : annotation.visibleIfRoleIn()) {
                if (userRoles.contains(role)) {
                    log.debug("Field visible by annotation because user has role: {}", role);
                    return true;
                }
            }

            // 如果配置了角色但用户没有任何所需角色，则继续检查权限
            if (annotation.visibleIfPermissionIn().length == 0) {
                log.debug("Field hidden by annotation because user does not have any of the required roles");
                return false;
            }
        }

        // 检查可见规则（权限维度）
        if (annotation.visibleIfPermissionIn().length > 0) {
            for (String permission : annotation.visibleIfPermissionIn()) {
                if (userPermissions.contains(permission)) {
                    log.debug("Field visible by annotation because user has permission: {}", permission);
                    return true;
                }
            }
            log.debug("Field hidden by annotation because user does not have any of the required permissions");
            return false;
        }

        return annotation.visible();
    }

    @Override
    public String buildRowCondition(String resource) {
        DataScopeInfo scopeInfo = DataScopeContext.getInfo();
        if (scopeInfo == null) {
            log.debug("No DataScopeInfo found in context");
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 根据 DataScopeInfo 自动构建行级条件
        // orgId 字段过滤
        if (scopeInfo.getOrgId() != null && !scopeInfo.getOrgId().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append("org_id = '").append(scopeInfo.getOrgId()).append("'");
        }

        // deptIds 字段过滤
        List<String> deptIds = scopeInfo.getDeptIds();
        if (deptIds != null && !deptIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append("dept_id IN (");
            for (int i = 0; i < deptIds.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("'").append(deptIds.get(i)).append("'");
            }
            sb.append(")");
        }

        String condition = sb.toString();
        if (!condition.isEmpty()) {
            log.info("Built row condition for resource '{}': {}", resource, condition);
        } else {
            log.debug("No row condition built for resource '{}': no orgId or deptIds in context", resource);
        }
        return condition;
    }

    /**
     * 构建单个行级规则的 SQL 条件片段
     *
     * @param rule 行级规则
     * @return SQL 条件片段
     */
    protected String buildCondition(RowRule rule) {
        String field = rule.getField();
        String op = rule.getOp();
        Object value = rule.getValue();

        // 解析占位符 ${...}，从 DataScopeContext 获取实际值
        if (value instanceof String && ((String) value).startsWith("${") && ((String) value).endsWith("}")) {
            String contextKey = ((String) value).substring(2, ((String) value).length() - 1);
            value = resolveContextValue(contextKey);
        }

        if ("IN".equalsIgnoreCase(op)) {
            if (value instanceof List) {
                List<?> values = (List<?>) value;
                StringBuilder sb = new StringBuilder();
                sb.append(field).append(" IN (");
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    Object v = values.get(i);
                    if (v instanceof String) {
                        sb.append("'").append(v).append("'");
                    } else {
                        sb.append(v);
                    }
                }
                sb.append(")");
                return sb.toString();
            }
        } else if ("=".equalsIgnoreCase(op)) {
            if (value instanceof String) {
                return field + " = '" + value + "'";
            }
            return field + " = " + value;
        } else if (">".equalsIgnoreCase(op)) {
            return field + " > " + value;
        } else if ("<".equalsIgnoreCase(op)) {
            return field + " < " + value;
        } else if (">=".equalsIgnoreCase(op)) {
            return field + " >= " + value;
        } else if ("<=".equalsIgnoreCase(op)) {
            return field + " <= " + value;
        } else if ("LIKE".equalsIgnoreCase(op)) {
            return field + " LIKE '%" + value + "%'";
        }

        return field + " " + op + " " + value;
    }

    /**
     * 从 DataScopeContext 解析占位符对应的值
     *
     * @param contextKey 上下文字段名
     * @return 实际值
     */
    protected Object resolveContextValue(String contextKey) {
        if ("orgId".equals(contextKey)) {
            return DataScopeContext.getOrgId();
        } else if ("deptIds".equals(contextKey)) {
            return DataScopeContext.getDeptIds();
        } else if ("userId".equals(contextKey)) {
            return DataScopeContext.getUserId();
        }
        return null;
    }

    @Override
    public void clearRules() {
        ruleCache.clear();
        log.info("All data rules cleared");
    }

    /**
     * 获取当前字段配置
     *
     * @return DataScopeFieldConfig
     */
    public DataScopeFieldConfig getFieldConfig() {
        return fieldConfig;
    }

    /**
     * 设置字段配置
     *
     * @param fieldConfig 字段配置
     */
    public void setFieldConfig(DataScopeFieldConfig fieldConfig) {
        this.fieldConfig = fieldConfig != null ? fieldConfig : new DataScopeFieldConfig();
        if (log.isDebugEnabled()) {
            log.debug("Field config updated: orgIdField={}, deptIdField={}, userIdField={}",
                    this.fieldConfig.getOrgIdField(),
                    this.fieldConfig.getDeptIdField(),
                    this.fieldConfig.getUserIdField());
        }
    }

    /**
     * 获取组织ID字段名
     *
     * @return 组织ID字段名
     */
    protected String getOrgIdField() {
        return fieldConfig.getOrgIdField();
    }

    /**
     * 获取部门ID字段名
     *
     * @return 部门ID字段名
     */
    protected String getDeptIdField() {
        return fieldConfig.getDeptIdField();
    }

    /**
     * 获取用户ID字段名
     *
     * @return 用户ID字段名
     */
    protected String getUserIdField() {
        return fieldConfig.getUserIdField();
    }
}