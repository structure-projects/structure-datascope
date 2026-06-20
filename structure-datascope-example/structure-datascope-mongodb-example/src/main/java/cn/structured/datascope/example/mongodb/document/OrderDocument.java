package cn.structured.datascope.example.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单MongoDB文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class OrderDocument {

    @Id
    private String id;

    private String orderNo;
    private BigDecimal amount;
    private String phone;
    private String email;
    private String remark;

    /**
     * 组织ID（数据权限隔离字段）
     */
    private String orgId;

    /**
     * 部门ID（数据权限隔离字段）
     */
    private String deptId;

    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
    private String updateBy;
}