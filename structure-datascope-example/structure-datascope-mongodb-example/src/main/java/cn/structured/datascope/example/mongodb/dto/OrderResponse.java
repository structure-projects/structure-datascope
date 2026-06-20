package cn.structured.datascope.example.mongodb.dto;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DataScopeRule(resource = "order")
public class OrderResponse {

    private Long id;
    private String orderNo;

    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN", "FINANCE"})
    private BigDecimal amount;

    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String phone;

    @DataScopeField(visible = true)
    private String email;

    @DataScopeField(hiddenIfRoleIn = {"EMPLOYEE"})
    private String remark;

    private Long orgId;
    private Long deptId;
    private LocalDateTime createTime;
    private String createBy;
}