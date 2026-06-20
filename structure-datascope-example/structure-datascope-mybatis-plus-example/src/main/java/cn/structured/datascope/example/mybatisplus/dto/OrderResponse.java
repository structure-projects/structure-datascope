package cn.structured.datascope.example.mybatisplus.dto;

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

    private Long id;
    private String orderNo;

    /**
     * 订单金额（仅管理员和财务可见）
     */
    private BigDecimal amount;

    /**
     * 客户手机号（仅管理员可见）
     */
    private String phone;

    /**
     * 客户邮箱（所有人可见）
     */
    private String email;

    /**
     * 内部备注（对普通员工隐藏）
     */
    private String remark;

    private Long orgId;
    private Long deptId;
    private LocalDateTime createTime;
    private String createBy;
}