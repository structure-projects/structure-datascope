package cn.structured.datascope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据范围管理配置属性
 * <p>
 * 配置前缀：structure.data-scope
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "structure.data-scope")
public class DataScopeProperties {

    /**
     * 是否启用数据范围管理
     */
    private boolean enabled = true;

    /**
     * 是否自动注册默认规则
     */
    private boolean autoRegisterRules = true;

    /**
     * 是否自动过滤响应体中的敏感字段
     * <p>
     * 启用后，框架会自动在响应序列化前过滤带有 @DataScopeRule 注解的 DTO 敏感字段
     * </p>
     */
    private boolean autoFilterResponse = true;


    /**
     * 规则扫描包路径
     * <p>
     * 用于扫描带有 @DataScopeRule 注解的实体类并自动注册规则
     * </p>
     */
    private String[] scanPackages = new String[0];

}