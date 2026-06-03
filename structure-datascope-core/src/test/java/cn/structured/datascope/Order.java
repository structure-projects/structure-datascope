package cn.structured.datascope;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@DataScopeRule(resource = "order")
public class Order {
    
    private Long id;
    
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN", "FINANCE"})
    private BigDecimal amount;
    
    @DataScopeField(visibleIfRoleIn = {"SYS_ADMIN"})
    private String phone;
    
    @DataScopeField(visible = true)
    private String email;
    
    @DataScopeField(hiddenIfRoleIn = {"EMPLOYEE"})
    private String secretField;
    
    private Long orgId;
    
    private List<Long> deptIds;
    
    private String userId;
}