package cn.structured.datascope.example.config;

import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.provider.DataScopeProvider;
import cn.structured.datascope.rule.DataRule;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 测试用 Mock DataScopeProvider
 * <p>
 * 根据不同的userId返回对应的数据权限信息
 * </p>
 */
@Component
public class MockDataScopeProvider implements DataScopeProvider {

    private static final Map<String, DataScopeInfo> USER_INFO_MAP = new HashMap<>();

    static {
        // 员工用户
        DataScopeInfo employeeInfo = new DataScopeInfo();
        employeeInfo.setUserId("user-001");
        employeeInfo.setDataScopeId("scope-employee");
        employeeInfo.setRoles(Arrays.asList("EMPLOYEE"));
        employeeInfo.setOrgId("10");
        employeeInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-001", employeeInfo);

        // 财务用户
        DataScopeInfo financeInfo = new DataScopeInfo();
        financeInfo.setUserId("user-002");
        financeInfo.setDataScopeId("scope-finance");
        financeInfo.setRoles(Arrays.asList("FINANCE"));
        financeInfo.setOrgId("10");
        financeInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-002", financeInfo);

        // 管理员用户
        DataScopeInfo adminInfo = new DataScopeInfo();
        adminInfo.setUserId("user-003");
        adminInfo.setDataScopeId("scope-admin");
        adminInfo.setRoles(Arrays.asList("SYS_ADMIN"));
        adminInfo.setOrgId("10");
        adminInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-003", adminInfo);

        // 多角色用户
        DataScopeInfo multiInfo = new DataScopeInfo();
        multiInfo.setUserId("user-004");
        multiInfo.setDataScopeId("scope-multi");
        multiInfo.setRoles(Arrays.asList("EMPLOYEE", "FINANCE"));
        multiInfo.setOrgId("10");
        multiInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-004", multiInfo);

        // 测试用户
        DataScopeInfo testInfo = new DataScopeInfo();
        testInfo.setUserId("user-005");
        testInfo.setDataScopeId("scope-test");
        testInfo.setRoles(Arrays.asList("SYS_ADMIN"));
        testInfo.setOrgId("10");
        testInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-005", testInfo);

        // 管理员用户 (user-006)
        DataScopeInfo adminInfo2 = new DataScopeInfo();
        adminInfo2.setUserId("user-006");
        adminInfo2.setDataScopeId("scope-test");
        adminInfo2.setRoles(Arrays.asList("ADMIN"));
        adminInfo2.setOrgId("10");
        adminInfo2.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-006", adminInfo2);
    }

    @Override
    public DataScopeInfo getScopeInfo(String userId) {
        return USER_INFO_MAP.getOrDefault(userId, createDefaultInfo(userId));
    }

    private DataScopeInfo createDefaultInfo(String userId) {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setDataScopeId("scope-default");
        info.setRoles(Arrays.asList("EMPLOYEE"));
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2", "3"));
        return info;
    }
}
