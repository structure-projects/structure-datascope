package cn.structured.datascope.example.message.dto;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DataScopeRule(resource = "order-event")
public class OrderEvent {

    private String eventId;
    private Long orderId;
    private String orderNo;
    private String eventType;

    @DataScopeField(visible = true)
    private String orgId;

    @DataScopeField(visible = true)
    private String deptId;

    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String userId;

    @DataScopeField(visible = true)
    private LocalDateTime timestamp;
}