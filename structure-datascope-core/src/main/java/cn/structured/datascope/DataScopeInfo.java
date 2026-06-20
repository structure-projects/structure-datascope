package cn.structured.datascope;

import lombok.Data;

import java.util.*;

/**
 * 数据范围上下文信息（贫血模型）
 * <p>
 * 只存储数据，不包含业务逻辑方法
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
     * 列级字段可见性配置
     * <p>
     * key: 资源名称（如 "order"）
     * value: 该资源下隐藏的字段列表
     * </p>
     */
    private Map<String, List<String>> hiddenFields = new HashMap<>();
}