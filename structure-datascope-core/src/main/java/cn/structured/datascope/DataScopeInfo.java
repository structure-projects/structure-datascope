package cn.structured.datascope;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据范围上下文信息
 * <p>
 * 存储当前请求的数据范围相关信息，包括用户ID、角色、权限、组织ID、部门ID等
 * </p>
 */
@Data
public class DataScopeInfo {

    /**
     * 用户角色列表
     */
    private List<String> roles = new ArrayList<>();
    
    /**
     * 用户权限列表（权限标识）
     */
    private List<String> permissions = new ArrayList<>();
    
    /**
     * 组织ID
     */
    private String orgId;
    
    /**
     * 部门ID列表
     */
    private List<String> deptIds = new ArrayList<>();
    
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 检查是否拥有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 检查是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}