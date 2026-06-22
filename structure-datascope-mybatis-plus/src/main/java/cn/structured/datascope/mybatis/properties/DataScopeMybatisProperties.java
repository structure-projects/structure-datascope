package cn.structured.datascope.mybatis.properties;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 数据权限 MyBatis 配置属性
 * <p>
 * 支持配置数据权限的各种参数，包括提供者类型选择
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "structure.data-scope")
public class DataScopeMybatisProperties {

    /**
     * 是否启用数据权限
     */
    private Boolean enable = true;

    /**
     * 是否启用租户隔离（组织级别）
     */
    private Boolean enableTenant = true;

    /**
     * 是否启用分页插件
     */
    private Boolean enablePagination = true;

    /**
     * 是否启用数据权限（部门级别）
     */
    private Boolean enableDataScope = true;

    /**
     * 数据库类型
     */
    private DbType dbType = DbType.MYSQL;

    /**
     * 租户ID字段名
     */
    private String tenantIdColumn = "organization_id";

    /**
     * 默认租户ID
     */
    private String defaultTenantId = "1";

    /**
     * 排除表列表（这些表不应用数据权限）
     */
    private List<String> excludeTables = new java.util.ArrayList<>();
}