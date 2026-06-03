package cn.structured.datascope;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据范围上下文管理器
 * <p>
 * 使用 ThreadLocal 存储当前线程的数据范围信息，
 * 确保在同一请求/任务执行过程中数据范围上下文能够被正确传递。
 * </p>
 */
@Slf4j
public class DataScopeContext {

    /**
     * ThreadLocal 存储数据范围上下文信息
     */
    private static final ThreadLocal<DataScopeInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 获取当前线程的数据范围上下文信息
     *
     * @return 数据范围上下文信息，可能为 null
     */
    public static DataScopeInfo get() {
        return CONTEXT.get();
    }

    /**
     * 设置当前线程的数据范围上下文信息
     *
     * @param info 数据范围上下文信息
     */
    public static void set(DataScopeInfo info) {
        if (log.isDebugEnabled()) {
            log.debug("Setting DataScope context: {}", info);
        }
        CONTEXT.set(info);
    }

    /**
     * 清除当前线程的数据范围上下文信息
     * <p>
     * 必须在请求/任务结束时调用，避免内存泄漏
     * </p>
     */
    public static void remove() {
        CONTEXT.remove();
        if (log.isDebugEnabled()) {
            log.debug("DataScope context cleared");
        }
    }

    /**
     * 获取数据范围ID
     *
     * @return 数据范围ID，可能为 null
     */
    public static String getDataScopeId() {
        DataScopeInfo info = get();
        return info != null ? info.getDataScopeId() : null;
    }

    /**
     * 获取当前用户角色列表
     *
     * @return 角色列表，永远不为 null（为空时返回空列表）
     */
    public static List<String> getRoles() {
        DataScopeInfo info = get();
        return info != null ? info.getRoles() : new ArrayList<>();
    }

    /**
     * 获取当前用户权限列表
     *
     * @return 权限列表，永远不为 null（为空时返回空列表）
     */
    public static List<String> getPermissions() {
        DataScopeInfo info = get();
        return info != null ? info.getPermissions() : new ArrayList<>();
    }

    /**
     * 检查当前用户是否拥有指定角色
     *
     * @param role 角色名称
     * @return true 表示拥有该角色
     */
    public static boolean hasRole(String role) {
        List<String> roles = getRoles();
        boolean result = roles != null && roles.contains(role);
        if (log.isTraceEnabled()) {
            log.trace("Check role '{}': {}", role, result);
        }
        return result;
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param permission 权限标识
     * @return true 表示拥有该权限
     */
    public static boolean hasPermission(String permission) {
        List<String> permissions = getPermissions();
        boolean result = permissions != null && permissions.contains(permission);
        if (log.isTraceEnabled()) {
            log.trace("Check permission '{}': {}", permission, result);
        }
        return result;
    }

    /**
     * 检查当前用户是否拥有指定角色列表中的任意角色
     *
     * @param roles 角色列表
     * @return true 表示拥有任意一个角色
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        List<String> userRoles = getRoles();
        for (String role : roles) {
            if (userRoles != null && userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否拥有指定权限列表中的任意权限
     *
     * @param permissions 权限列表
     * @return true 表示拥有任意一个权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        List<String> userPermissions = getPermissions();
        for (String permission : permissions) {
            if (userPermissions != null && userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置数据范围ID
     *
     * @param dataScopeId 数据范围ID
     */
    public static void setDataScopeId(String dataScopeId) {
        DataScopeInfo info = get();
        if (info == null) {
            info = new DataScopeInfo();
            CONTEXT.set(info);
        }
        info.setDataScopeId(dataScopeId);
        if (log.isDebugEnabled()) {
            log.debug("DataScope ID set to: {}", dataScopeId);
        }
    }

    /**
     * 设置当前用户角色列表
     *
     * @param roles 角色列表
     */
    public static void setRoles(List<String> roles) {
        DataScopeInfo info = get();
        if (info == null) {
            info = new DataScopeInfo();
            CONTEXT.set(info);
        }
        info.setRoles(roles);
        if (log.isDebugEnabled()) {
            log.debug("Roles set to: {}", roles);
        }
    }

    /**
     * 设置当前用户权限列表
     *
     * @param permissions 权限列表
     */
    public static void setPermissions(List<String> permissions) {
        DataScopeInfo info = get();
        if (info == null) {
            info = new DataScopeInfo();
            CONTEXT.set(info);
        }
        info.setPermissions(permissions);
        if (log.isDebugEnabled()) {
            log.debug("Permissions set to: {}", permissions);
        }
    }

    /**
     * 设置组织ID
     *
     * @param orgId 组织ID
     */
    public static void setOrgId(String orgId) {
        DataScopeInfo info = get();
        if (info == null) {
            info = new DataScopeInfo();
            CONTEXT.set(info);
        }
        info.setOrgId(orgId);
    }

    /**
     * 设置部门ID列表
     *
     * @param deptIds 部门ID列表
     */
    public static void setDeptIds(List<String> deptIds) {
        DataScopeInfo info = get();
        if (info == null) {
            info = new DataScopeInfo();
            CONTEXT.set(info);
        }
        info.setDeptIds(deptIds);
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(String userId) {
        DataScopeInfo info = get();
        if (info == null) {
            info = new DataScopeInfo();
            CONTEXT.set(info);
        }
        info.setUserId(userId);
    }
}