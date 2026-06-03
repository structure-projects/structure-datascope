package cn.structured.datascope.annotation;

import java.lang.annotation.*;

/**
 * 数据范围字段注解
 * <p>
 * 用于标记DTO字段的数据权限控制规则，支持角色和权限两种维度的控制
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeField {
    
    /**
     * 字段名称，默认为字段的实际名称
     */
    String name() default "";
    
    /**
     * 是否可见，默认为true
     */
    boolean visible() default true;
    
    /**
     * 当用户拥有指定角色时字段可见
     * <p>
     * 例如：{"SYS_ADMIN", "FINANCE"} 表示只有管理员或财务角色可以看到此字段
     * </p>
     */
    String[] visibleIfRoleIn() default {};
    
    /**
     * 当用户拥有指定角色时字段隐藏
     * <p>
     * 例如：{"EMPLOYEE"} 表示员工角色看不到此字段
     * </p>
     */
    String[] hiddenIfRoleIn() default {};
    
    /**
     * 当用户拥有指定权限时字段可见
     * <p>
     * 权限标识比角色更细粒度，例如：{"order:view_amount"}
     * </p>
     */
    String[] visibleIfPermissionIn() default {};
    
    /**
     * 当用户拥有指定权限时字段隐藏
     * <p>
     * 例如：{"order:hidden_phone"} 表示拥有此权限的用户看不到此字段
     * </p>
     */
    String[] hiddenIfPermissionIn() default {};
}