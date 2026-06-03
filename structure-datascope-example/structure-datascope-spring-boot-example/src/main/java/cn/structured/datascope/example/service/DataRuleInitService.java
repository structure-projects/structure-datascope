package cn.structured.datascope.example.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.rule.ColumnRule;
import cn.structured.datascope.rule.DataRule;
import cn.structured.datascope.rule.RowRule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 数据规则初始化服务
 * <p>
 * 在应用启动时注册数据权限规则
 * </p>
 */
@Slf4j
@Service
public class DataRuleInitService {

    private final DataRuleEngine ruleEngine;

    public DataRuleInitService(DataRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 应用启动后初始化数据规则
     */
    @PostConstruct
    public void initRules() {
        log.info("========== 开始初始化数据权限规则 ==========");

        // 注册订单数据规则
        registerOrderRules();

        // 注册用户数据规则
        registerUserRules();

        log.info("========== 数据权限规则初始化完成 ==========");
    }

    /**
     * 注册订单数据规则
     */
    private void registerOrderRules() {
        log.info("Registering order data rules...");

        DataRule orderRule = new DataRule();
        orderRule.setResource("order");

        // 行级规则：基于组织ID和部门ID的过滤
        RowRule orgRule = new RowRule();
        orgRule.setField("org_id");
        orgRule.setOp("=");
        orgRule.setValue(getCurrentOrgId());

        RowRule deptRule = new RowRule();
        deptRule.setField("dept_id");
        deptRule.setOp("IN");
        deptRule.setValue(Arrays.asList(getCurrentDeptIds()));

        orderRule.setRowRules(Arrays.asList(orgRule, deptRule));

        // 列级规则
        ColumnRule amountRule = new ColumnRule();
        amountRule.setField("amount");
        amountRule.setVisibleIfRoleIn(Arrays.asList("SYS_ADMIN", "FINANCE"));

        ColumnRule phoneRule = new ColumnRule();
        phoneRule.setField("phone");
        phoneRule.setVisibleIfRoleIn(Arrays.asList("SYS_ADMIN"));

        ColumnRule remarkRule = new ColumnRule();
        remarkRule.setField("remark");
        remarkRule.setHiddenIfRoleIn(Arrays.asList("EMPLOYEE"));

        orderRule.setColumnRules(Arrays.asList(amountRule, phoneRule, remarkRule));

        ruleEngine.registerRule(orderRule);
        log.info("Order data rules registered successfully");
    }

    /**
     * 注册用户数据规则
     */
    private void registerUserRules() {
        log.info("Registering user data rules...");

        DataRule userRule = new DataRule();
        userRule.setResource("user");

        // 列级规则
        ColumnRule secretRule = new ColumnRule();
        secretRule.setField("secret");
        secretRule.setHiddenIfRoleIn(Arrays.asList("EMPLOYEE", "USER"));

        userRule.setColumnRules(Arrays.asList(secretRule));

        ruleEngine.registerRule(userRule);
        log.info("User data rules registered successfully");
    }

    /**
     * 获取当前组织ID（实际应从上下文获取）
     */
    private String getCurrentOrgId() {
        DataScopeInfo info = DataScopeContext.get();
        if (info != null && info.getOrgId() != null) {
            return info.getOrgId();
        }
        return "10"; // 默认值
    }

    /**
     * 获取当前部门ID列表（实际应从上下文获取）
     */
    private String[] getCurrentDeptIds() {
        DataScopeInfo info = DataScopeContext.get();
        if (info != null && info.getDeptIds() != null && !info.getDeptIds().isEmpty()) {
            return info.getDeptIds().toArray(new String[0]);
        }
        return new String[]{"1", "2", "3"}; // 默认值
    }
}