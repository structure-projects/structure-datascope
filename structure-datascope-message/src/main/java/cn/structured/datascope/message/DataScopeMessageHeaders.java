package cn.structured.datascope.message;

/**
 * 数据权限消息头常量类
 * <p>
 * 定义用于在消息传递过程中携带数据权限信息的消息头名称
 * </p>
 */
public final class DataScopeMessageHeaders {

    private DataScopeMessageHeaders() {
    }

    /**
     * 数据权限信息JSON序列化后的消息头名称
     * <p>
     * 该消息头用于在生产者和消费者之间传递完整的数据权限上下文信息
     * </p>
     */
    public static final String DATA_SCOPE_INFO = "X-DataScope-Info";

    /**
     * 用户ID消息头名称
     */
    public static final String USER_ID = "X-DataScope-User-Id";

    /**
     * 组织ID消息头名称
     */
    public static final String ORG_ID = "X-DataScope-Org-Id";

    /**
     * 部门ID列表消息头名称（逗号分隔）
     */
    public static final String DEPT_IDS = "X-DataScope-Dept-Ids";

    /**
     * 角色列表消息头名称（逗号分隔）
     */
    public static final String ROLES = "X-DataScope-Roles";

    /**
     * 权限列表消息头名称（逗号分隔）
     */
    public static final String PERMISSIONS = "X-DataScope-Permissions";

    /**
     * 数据权限消息头前缀
     */
    public static final String HEADER_PREFIX = "X-DataScope-";
}