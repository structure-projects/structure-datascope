package cn.structured.datascope.mybatis.plus.config;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.mybatis.interceptor.DataScopeInterceptor;
import cn.structured.datascope.mybatis.plus.handler.StructureTenantLineHandler;
import cn.structured.datascope.mybatis.properties.DataScopeMybatisProperties;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 数据权限自动配置
 * <p>
 * 自动注册数据权限拦截器，对业务层完全透明
 * </p>
 *
 * @author chuck
 */
@Configuration
@ConditionalOnClass({MybatisPlusInterceptor.class})
@EnableConfigurationProperties(DataScopeMybatisProperties.class)
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataScopeMyBatisPlusAutoConfiguration {

    /**
     * 注册数据权限拦截器
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeInterceptor.class)
    public DataScopeInterceptor dataScopeInterceptor(DataScopeFieldConfig fieldConfig) {
        return new DataScopeInterceptor(fieldConfig);
    }

    /**
     * 注册 MyBatis-Plus 拦截器链（包含数据权限拦截器）
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            DataScopeInterceptor dataScopeInterceptor,
            DataScopeMybatisProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页拦截器
        if (Boolean.TRUE.equals(properties.getEnablePagination())) {
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(properties.getDbType()));
        }
        // 添加租户拦截器
        if (Boolean.TRUE.equals(properties.getEnableTenant())) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(
                    new StructureTenantLineHandler(properties)
            ));
        }
        // 添加数据权限拦截器
        interceptor.addInnerInterceptor(dataScopeInterceptor);
        return interceptor;
    }
}
