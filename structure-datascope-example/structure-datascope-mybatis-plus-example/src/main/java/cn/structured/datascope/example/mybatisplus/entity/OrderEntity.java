package cn.structured.datascope.example.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class OrderEntity {

    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 订单金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 客户手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 客户邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 内部备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 组织ID（数据权限隔离字段）
     */
    @TableField("org_id")
    private Long orgId;

    /**
     * 部门ID（数据权限隔离字段）
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;
}