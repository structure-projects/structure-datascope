package cn.structured.datascope.example.dto;

import cn.structured.datascope.annotation.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应DTO
 * <p>
 * 使用 @DataScopeRule 注解标记资源类型
 * 行级和列级规则通过 DataScopeInfo 配置
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
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 客户手机号
     */
    private String phone;

    /**
     * 客户邮箱
     */
    private String email;

    /**
     * 内部备注
     */
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