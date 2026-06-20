package cn.structured.datascope.mongodb.config;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.mongodb.aspect.DataScopeMongoAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB 数据权限自动配置
 * <p>
 * 自动注册数据权限AOP切面，对业务层完全透明
 * </p>
 *
 * @author chuck
 */
@Configuration
@ConditionalOnClass({MongoTemplate.class})
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataScopeMongoAutoConfiguration {

    /**
     * 注册MongoDB数据权限切面
     */
    @Bean
    @ConditionalOnBean(DataScopeFieldConfig.class)
    public DataScopeMongoAspect dataScopeMongoAspect(DataScopeFieldConfig fieldConfig) {
        return new DataScopeMongoAspect(fieldConfig);
    }
}
