package cn.structured.datascope.example.message.config;

import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.provider.DataScopeProvider;
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
        // ========== 列级权限测试用户 ==========

        // 员工用户 - 基本角色，隐藏 amount, phone, remark
        DataScopeInfo employeeInfo = new DataScopeInfo();
        employeeInfo.setUserId("user-001");
        employeeInfo.setRoles(Arrays.asList("EMPLOYEE"));
        employeeInfo.setOrgId("10");
        employeeInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        employeeInfo.getHiddenFields().put("order", Arrays.asList("amount", "phone", "remark"));
        USER_INFO_MAP.put("user-001", employeeInfo);

        // 财务用户 - 可见订单金额，隐藏 phone
        DataScopeInfo financeInfo = new DataScopeInfo();
        financeInfo.setUserId("user-002");
        financeInfo.setRoles(Arrays.asList("FINANCE"));
        financeInfo.setOrgId("10");
        financeInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        financeInfo.getHiddenFields().put("order", Arrays.asList("phone"));
        USER_INFO_MAP.put("user-002", financeInfo);

        // 管理员用户 - 可见所有敏感字段，不隐藏任何字段
        DataScopeInfo adminInfo = new DataScopeInfo();
        adminInfo.setUserId("user-003");
        adminInfo.setRoles(Arrays.asList("SYS_ADMIN"));
        adminInfo.setOrgId("10");
        adminInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-003", adminInfo);

        // 多角色用户 - 同时拥有员工和财务角色，隐藏 remark, phone
        DataScopeInfo multiInfo = new DataScopeInfo();
        multiInfo.setUserId("user-004");
        multiInfo.setRoles(Arrays.asList("EMPLOYEE", "FINANCE"));
        multiInfo.setOrgId("10");
        multiInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        multiInfo.getHiddenFields().put("order", Arrays.asList("remark", "phone"));
        USER_INFO_MAP.put("user-004", multiInfo);

        // ========== 行级权限测试用户 - 组织维度 ==========

        // 组织10的管理员 - 可见组织10的所有数据
        DataScopeInfo org10AdminInfo = new DataScopeInfo();
        org10AdminInfo.setUserId("user-row-org10-admin");
        org10AdminInfo.setRoles(Arrays.asList("ORG_ADMIN"));
        org10AdminInfo.setOrgId("10");
        org10AdminInfo.setDeptIds(Arrays.asList("1", "2", "3", "4", "5"));
        USER_INFO_MAP.put("user-row-org10-admin", org10AdminInfo);

        // 组织20的管理员 - 可见组织20的所有数据
        DataScopeInfo org20AdminInfo = new DataScopeInfo();
        org20AdminInfo.setUserId("user-row-org20-admin");
        org20AdminInfo.setRoles(Arrays.asList("ORG_ADMIN"));
        org20AdminInfo.setOrgId("20");
        org20AdminInfo.setDeptIds(Arrays.asList("6", "7", "8", "9", "10"));
        USER_INFO_MAP.put("user-row-org20-admin", org20AdminInfo);

        // ========== 行级权限测试用户 - 部门维度 ==========

        // 部门1的用户 - 只能看到本部门数据
        DataScopeInfo dept1Info = new DataScopeInfo();
        dept1Info.setUserId("user-row-dept1");
        dept1Info.setRoles(Arrays.asList("EMPLOYEE"));
        dept1Info.setOrgId("10");
        dept1Info.setDeptIds(Arrays.asList("1"));
        USER_INFO_MAP.put("user-row-dept1", dept1Info);

        // 部门1和2的主管 - 可以看到部门1和2的数据
        DataScopeInfo dept1and2ManagerInfo = new DataScopeInfo();
        dept1and2ManagerInfo.setUserId("user-row-dept1-2-manager");
        dept1and2ManagerInfo.setRoles(Arrays.asList("DEPT_MANAGER"));
        dept1and2ManagerInfo.setOrgId("10");
        dept1and2ManagerInfo.setDeptIds(Arrays.asList("1", "2"));
        USER_INFO_MAP.put("user-row-dept1-2-manager", dept1and2ManagerInfo);

        // ========== 权限维度测试用户 ==========

        // 拥有查看金额权限的用户
        DataScopeInfo viewAmountPermInfo = new DataScopeInfo();
        viewAmountPermInfo.setUserId("user-perm-view-amount");
        viewAmountPermInfo.setRoles(Arrays.asList("EMPLOYEE"));
        viewAmountPermInfo.setPermissions(Arrays.asList("order:view_amount"));
        viewAmountPermInfo.setOrgId("10");
        viewAmountPermInfo.setDeptIds(Arrays.asList("1", "2", "3"));
        USER_INFO_MAP.put("user-perm-view-amount", viewAmountPermInfo);
    }

    @Override
    public DataScopeInfo getScopeInfo(String userId) {
        return USER_INFO_MAP.getOrDefault(userId, createDefaultInfo(userId));
    }

    private DataScopeInfo createDefaultInfo(String userId) {
        DataScopeInfo info = new DataScopeInfo();
        info.setUserId(userId);
        info.setRoles(Arrays.asList("EMPLOYEE"));
        info.setOrgId("10");
        info.setDeptIds(Arrays.asList("1", "2", "3"));
        return info;
    }
}