package cn.structured.datascope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据范围管理配置属性
 * <p>
 * 配置前缀：structure.data-scope
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "structure.data-scope")
public class DataScopeProperties {

    /**
     * 是否启用数据范围管理
     */
    private boolean enabled = true;

    /**
     * 数据范围ID的请求头名称
     */
    private String headerName = "X-DataScope-Id";

    /**
     * 角色列表的请求头名称（多个角色用逗号分隔）
     */
    private String roleHeaderName = "X-DataScope-Roles";

    /**
     * 组织ID的请求头名称
     */
    private String orgIdHeaderName = "X-Org-Id";

    /**
     * 部门ID列表的请求头名称（多个部门用逗号分隔）
     */
    private String deptIdsHeaderName = "X-Dept-Ids";

    /**
     * 用户ID的请求头名称
     */
    private String userIdHeaderName = "X-User-Id";
}