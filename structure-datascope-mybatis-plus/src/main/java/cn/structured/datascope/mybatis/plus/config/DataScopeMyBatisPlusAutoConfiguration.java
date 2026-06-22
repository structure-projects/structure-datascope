package cn.structured.datascope.mybatis.plus.config;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.mybatis.interceptor.DataScopeInterceptor;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataScopeInterceptor dataScopeInterceptor) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(dataScopeInterceptor);
        return interceptor;
    }
}
