package cn.structured.datascope.provider;

import cn.structured.datascope.DataScopeInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认数据权限提供器
 * <p>
 * 当未配置远程权限服务时使用的默认实现，
 * 子类可继承此类并重写 {@link #doGetScopeInfo(String)} 方法实现自定义逻辑
 * </p>
 * <p>
 * 支持测试模式，通过设置系统属性或环境变量 {@code data.scope.test.mode=true} 启用
 * </p>
 */
@Slf4j
public class DefaultDataScopeProviderImpl extends DefaultDataScopeProvider {

    /**
     * 测试模式开关
     */
    private static final String TEST_MODE_PROPERTY = "data.scope.test.mode";
    private static final String TEST_MODE_ENV = "DATA_SCOPE_TEST_MODE";

    /**
     * 测试用户配置映射
     */
    private static final Map<String, DataScopeInfo> TEST_USER_MAP = new HashMap<>();

    static {
        // 测试模式下的预设用户配置
        initTestUsers();
    }

    /**
     * 初始化测试用户配置
     */
    private static void initTestUsers() {
        // 组织管理员 - 组织10
        DataScopeInfo org10Admin = new DataScopeInfo();
        org10Admin.setUserId("test-org10-admin");
        org10Admin.setRoles(Arrays.asList("ORG_ADMIN"));
        org10Admin.setOrgId("10");
        org10Admin.setDeptIds(Arrays.asList("1", "2", "3", "4", "5"));
        org10Admin.setPermissions(new ArrayList<>());
        TEST_USER_MAP.put("test-org10-admin", org10Admin);

        // 组织管理员 - 组织20
        DataScopeInfo org20Admin = new DataScopeInfo();
        org20Admin.setUserId("test-org20-admin");
        org20Admin.setRoles(Arrays.asList("ORG_ADMIN"));
        org20Admin.setOrgId("20");
        org20Admin.setDeptIds(Arrays.asList("6", "7", "8", "9", "10"));
        org20Admin.setPermissions(new ArrayList<>());
        TEST_USER_MAP.put("test-org20-admin", org20Admin);

        // 部门用户 - 部门1
        DataScopeInfo dept1User = new DataScopeInfo();
        dept1User.setUserId("test-dept1-user");
        dept1User.setRoles(Arrays.asList("EMPLOYEE"));
        dept1User.setOrgId("10");
        dept1User.setDeptIds(Arrays.asList("1"));
        dept1User.setPermissions(new ArrayList<>());
        TEST_USER_MAP.put("test-dept1-user", dept1User);

        // 部门主管 - 部门1和2
        DataScopeInfo dept12Manager = new DataScopeInfo();
        dept12Manager.setUserId("test-dept12-manager");
        dept12Manager.setRoles(Arrays.asList("DEPT_MANAGER"));
        dept12Manager.setOrgId("10");
        dept12Manager.setDeptIds(Arrays.asList("1", "2"));
        dept12Manager.setPermissions(new ArrayList<>());
        TEST_USER_MAP.put("test-dept12-manager", dept12Manager);

        // 财务角色
        DataScopeInfo financeUser = new DataScopeInfo();
        financeUser.setUserId("test-finance-user");
        financeUser.setRoles(Arrays.asList("FINANCE"));
        financeUser.setOrgId("10");
        financeUser.setDeptIds(Arrays.asList("1", "2", "3"));
        financeUser.setPermissions(Arrays.asList("order:view_amount"));
        TEST_USER_MAP.put("test-finance-user", financeUser);

        // 管理员角色（列级权限测试用）
        DataScopeInfo adminUser = new DataScopeInfo();
        adminUser.setUserId("test-admin-user");
        adminUser.setRoles(Arrays.asList("SYS_ADMIN"));
        adminUser.setOrgId("10");
        adminUser.setDeptIds(Arrays.asList("1", "2", "3"));
        adminUser.setPermissions(new ArrayList<>());
        TEST_USER_MAP.put("test-admin-user", adminUser);

        // 员工角色（列级权限测试用）
        DataScopeInfo employeeUser = new DataScopeInfo();
        employeeUser.setUserId("test-employee-user");
        employeeUser.setRoles(Arrays.asList("EMPLOYEE"));
        employeeUser.setOrgId("10");
        employeeUser.setDeptIds(Arrays.asList("1", "2", "3"));
        employeeUser.setPermissions(new ArrayList<>());
        TEST_USER_MAP.put("test-employee-user", employeeUser);
    }

    /**
     * 检查是否启用测试模式
     *
     * @return true if test mode is enabled
     */
    private boolean isTestMode() {
        String propertyValue = System.getProperty(TEST_MODE_PROPERTY);
        if (propertyValue != null) {
            return "true".equalsIgnoreCase(propertyValue);
        }
        String envValue = System.getenv(TEST_MODE_ENV);
        return "true".equalsIgnoreCase(envValue);
    }

    @Override
    protected DataScopeInfo doGetScopeInfo(String userId) {
        // 测试模式下返回预设的测试用户配置
        if (isTestMode()) {
            DataScopeInfo testUser = TEST_USER_MAP.get(userId);
            if (testUser != null) {
                log.info("Test mode: Returning predefined test user config for userId: {}", userId);
                return testUser;
            }
            log.warn("Test mode: User {} not found in test user map, using default config", userId);
        } else {
            log.warn("DefaultDataScopeProviderImpl is in use. Please override doGetScopeInfo() or enable test mode to provide actual data scope info for user: {}", userId);
        }

        DataScopeInfo scopeInfo = new DataScopeInfo();
        scopeInfo.setRoles(new ArrayList<String>());
        scopeInfo.setPermissions(new ArrayList<String>());
        scopeInfo.setOrgId("1");
        scopeInfo.setDeptIds(new ArrayList<String>());
        scopeInfo.setUserId(userId);
        return scopeInfo;
    }
}
