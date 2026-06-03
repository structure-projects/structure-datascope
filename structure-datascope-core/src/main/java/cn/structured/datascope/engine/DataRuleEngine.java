package cn.structured.datascope.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import cn.structured.datascope.rule.ColumnRule;
import cn.structured.datascope.rule.DataRule;
import cn.structured.datascope.rule.RowRule;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据规则引擎
 * <p>
 * 提供以下功能：
 * <ul>
 *     <li>数据规则注册与获取</li>
 *     <li>列级权限验证（判断字段是否可见，支持角色和权限两种维度）</li>
 *     <li>对象字段过滤（根据权限设置字段为 null）</li>
 *     <li>行级 SQL 条件构建</li>
 * </ul>
 * </p>
 */
@Slf4j
public class DataRuleEngine {

    /**
     * 数据规则缓存，key 为资源名称
     */
    private final Map<String, DataRule> ruleCache = new HashMap<>();

    /**
     * 注册数据规则
     *
     * @param rule 数据规则
     */
    public void registerRule(DataRule rule) {
        ruleCache.put(rule.getResource(), rule);
        log.info("Registered data rule for resource: {}", rule.getResource());
    }

    /**
     * 获取指定资源的数据规则
     *
     * @param resource 资源名称
     * @return 数据规则，若未注册则返回 null
     */
    public DataRule getRule(String resource) {
        return ruleCache.get(resource);
    }

    /**
     * 判断指定资源的字段是否对当前用户可见
     *
     * @param resource  资源名称
     * @param fieldName 字段名称
     * @return true 表示可见
     */
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
    private boolean evaluateColumnRule(ColumnRule columnRule) {
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
            if (!columnRule.getVisibleIfRoleIn().isEmpty()) {
                // 已在角色检查中返回，这里不需要额外处理
            } else {
                log.debug("Field hidden because user does not have any of the required permissions");
                return false;
            }
        }

        return columnRule.isVisible();
    }

    /**
     * 根据数据权限规则过滤对象的字段
     * <p>
     * 不可见的字段会被设置为 null
     * </p>
     *
     * @param obj      待过滤的对象
     * @param resource 资源名称（如果对象有 @DataScopeRule 注解则忽略此参数）
     */
    public void filter(Object obj, String resource) {
        if (obj == null) {
            log.debug("Object is null, skip filtering");
            return;
        }

        Class<?> clazz = obj.getClass();
        DataScopeRule classAnnotation = clazz.getAnnotation(DataScopeRule.class);
        String ruleResource = classAnnotation != null ? classAnnotation.resource() : resource;

        if (log.isDebugEnabled()) {
            log.debug("Filtering object of type {}, using resource: {}", clazz.getSimpleName(), ruleResource);
        }

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
                visible = canSeeField(ruleResource, fieldName);
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
            log.debug("Filtered {} fields for object of type {}", filteredCount, clazz.getSimpleName());
        }
    }

    /**
     * 评估字段注解（支持角色和权限两种维度）
     *
     * @param annotation 字段注解
     * @return true 表示字段可见
     */
    private boolean evaluateFieldAnnotation(DataScopeField annotation) {
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

    /**
     * 构建行级 SQL WHERE 条件
     *
     * @param resource 资源名称
     * @return SQL 条件字符串，如果没有规则则返回空字符串
     */
    public String buildRowCondition(String resource) {
        DataRule rule = getRule(resource);
        if (rule == null || rule.getRowRules() == null || rule.getRowRules().isEmpty()) {
            log.debug("No row rules for resource: {}", resource);
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (RowRule rowRule : rule.getRowRules()) {
            if (sb.length() > 0) {
                sb.append(" AND ");
            }
            sb.append(buildCondition(rowRule));
        }

        String condition = sb.toString();
        log.info("Built row condition for resource '{}': {}", resource, condition);
        return condition;
    }

    /**
     * 构建单个行级规则的 SQL 条件片段
     *
     * @param rule 行级规则
     * @return SQL 条件片段
     */
    private String buildCondition(RowRule rule) {
        String field = rule.getField();
        String op = rule.getOp();
        Object value = rule.getValue();

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
     * 清除所有注册的规则
     */
    public void clearRules() {
        ruleCache.clear();
        log.info("All data rules cleared");
    }
}