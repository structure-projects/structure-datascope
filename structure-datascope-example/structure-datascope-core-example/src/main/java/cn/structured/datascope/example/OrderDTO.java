package cn.structured.datascope.example;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单数据传输对象
 * <p>
 * 使用 @DataScopeRule 和 @DataScopeField 注解定义数据权限规则
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DataScopeRule(resource = "order")
public class OrderDTO {

    /**
     * 订单ID（所有人都可见）
     */
    private Long id;

    /**
     * 订单号（所有人都可见）
     */
    private String orderNo;

    /**
     * 订单金额（仅系统管理员和财务可见）
     */
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN", "FINANCE"})
    private BigDecimal amount;

    /**
     * 客户手机号（仅系统管理员可见）
     */
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String phone;

    /**
     * 客户邮箱（所有人都可见）
     */
    @DataScopeField(visible = true)
    private String email;

    /**
     * 内部备注（对员工隐藏）
     */
    @DataScopeField(hiddenIfRoleIn = {"EMPLOYEE"})
    private String remark;

    /**
     * 组织ID（所有人都可见）
     */
    private Long orgId;
}