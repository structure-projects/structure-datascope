package cn.structured.datascope.example.dto;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应DTO
 * <p>
 * 使用 @DataScopeRule 和 @DataScopeField 注解定义字段权限
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DataScopeRule(resource = "order")
public class OrderResponse {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单金额（仅管理员和财务可见）
     */
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN", "FINANCE"})
    private BigDecimal amount;

    /**
     * 客户手机号（仅管理员可见）
     */
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String phone;

    /**
     * 客户邮箱（所有人可见）
     */
    @DataScopeField(visible = true)
    private String email;

    /**
     * 内部备注（对普通员工隐藏）
     */
    @DataScopeField(hiddenIfRoleIn = {"EMPLOYEE"})
    private String remark;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createBy;
}