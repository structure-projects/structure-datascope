package cn.structured.datascope.rule;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 列级权限规则
 * <p>
 * 定义字段的可见性规则，支持角色和权限两种维度的控制
 * </p>
 */
@Data
public class ColumnRule {

    /**
     * 字段名称
     */
    private String field;
    
    /**
     * 是否可见，默认为true
     */
    private boolean visible = true;
    
    /**
     * 当用户拥有指定角色时字段可见
     */
    private List<String> visibleIfRoleIn = new ArrayList<>();
    
    /**
     * 当用户拥有指定角色时字段隐藏
     */
    private List<String> hiddenIfRoleIn = new ArrayList<>();
    
    /**
     * 当用户拥有指定权限时字段可见
     * <p>
     * 权限标识比角色更细粒度，例如：{"order:view_amount"}
     * </p>
     */
    private List<String> visibleIfPermissionIn = new ArrayList<>();
    
    /**
     * 当用户拥有指定权限时字段隐藏
     * <p>
     * 例如：{"order:hidden_phone"} 表示拥有此权限的用户看不到此字段
     * </p>
     */
    private List<String> hiddenIfPermissionIn = new ArrayList<>();
}