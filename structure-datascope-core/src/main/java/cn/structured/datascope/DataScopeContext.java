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
}