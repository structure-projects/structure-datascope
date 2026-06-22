package cn.structured.datascope.mybatis.plus.config;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.mybatis.interceptor.DataScopeInterceptor;
import cn.structured.datascope.mybatis.plus.handler.StructureTenantLineHandler;
import cn.structured.datascope.mybatis.properties.DataScopeMybatisProperties;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * MyBatis-Plus 数据权限配置
 * <p>
 * 自动注册数据权限拦截器和租户拦截器，对业务层完全透明
 * </p>
 */
@Configuration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor")
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DataScopeMybatisProperties.class)
public class DataScopeMyBatisPlusConfiguration {

    @Bean
    @ConditionalOnMissingBean(StructureTenantLineHandler.class)
    public StructureTenantLineHandler structureTenantLineHandler(DataScopeMybatisProperties properties) {
        return new StructureTenantLineHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean(TenantLineInnerInterceptor.class)
    public TenantLineInnerInterceptor tenantLineInnerInterceptor(StructureTenantLineHandler handler) {
        TenantLineInnerInterceptor interceptor = new TenantLineInnerInterceptor();
        interceptor.setTenantLineHandler(handler);
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(DataScopeInterceptor.class)
    public DataScopeInterceptor dataScopeInterceptor(DataScopeFieldConfig fieldConfig) {
        return new DataScopeInterceptor(fieldConfig);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            TenantLineInnerInterceptor tenantLineInnerInterceptor,
            DataScopeInterceptor dataScopeInterceptor,
            DataScopeMybatisProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (Boolean.TRUE.equals(properties.getEnableTenant())) {
            interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        }
        if (Boolean.TRUE.equals(properties.getEnableDataScope())) {
            interceptor.addInnerInterceptor(dataScopeInterceptor);
        }
        return interceptor;
    }
}