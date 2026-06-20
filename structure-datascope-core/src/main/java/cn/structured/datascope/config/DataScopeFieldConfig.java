package cn.structured.datascope.config;

import lombok.Data;

/**
 * 数据范围字段配置类
 * <p>
 * 用于配置数据权限隔离时使用的字段名称，支持灵活配置不同数据源的字段命名规范。
 * </p>
 *
 * <p>配置示例（application.yml）：</p>
 * <pre>
 * structure:
 *   data-scope:
 *     field-config:
 *       org-id-field: orgId           # 组织ID字段名
 *       dept-id-field: deptId         # 部门ID字段名
 *       user-id-field: userId         # 用户ID字段名
 * </pre>
 */
@Data
public class DataScopeFieldConfig {

    /**
     * 组织ID字段名（用于数据库表/文档字段名）
     * 默认: orgId
     */
    private String orgIdField = "orgId";

    /**
     * 部门ID字段名（用于数据库表/文档字段名）
     * 默认: deptId
     */
    private String deptIdField = "deptId";

    /**
     * 用户ID字段名（用于数据库表/文档字段名）
     * 默认: userId
     */
    private String userIdField = "userId";
}