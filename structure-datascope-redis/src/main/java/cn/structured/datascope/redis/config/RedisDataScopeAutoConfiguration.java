package cn.structured.datascope.redis.config;

import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.DataRuleEngineManager;
import cn.structured.datascope.redis.engine.RedisDataRuleEngine;
import cn.structured.datascope.redis.template.DataScopeRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 数据权限自动配置类
 * <p>
 * 自动配置 Redis 数据规则引擎和数据权限 Redis 模板，
 * 实现 Redis 操作的数据权限拦截
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisDataScopeAutoConfiguration {

    /**
     * 注册 Redis 数据规则引擎
     * <p>
     * 用于构建 Redis 键的数据权限前缀和模式匹配，
     * 并自动注册到规则引擎管理器
     * </p>
     */
    @Bean()
    @Qualifier("redisDataRuleEngine")
    @ConditionalOnMissingBean(RedisDataRuleEngine.class)
    public RedisDataRuleEngine redisDataRuleEngine(DataScopeFieldConfig fieldConfig, DataRuleEngineManager engineManager) {
        log.info("Registering RedisDataRuleEngine with field config: orgId={}, deptId={}, userId={}",
                fieldConfig.getOrgIdField(), fieldConfig.getDeptIdField(), fieldConfig.getUserIdField());
        RedisDataRuleEngine engine = new RedisDataRuleEngine(fieldConfig);
        engineManager.registerEngine("redis", engine);
        return engine;
    }

    /**
     * 注册数据权限 Redis 模板
     * <p>
     * 自动为 Redis 操作添加数据权限前缀，业务代码无需感知数据权限
     * </p>
     */
    @Bean
    @ConditionalOnBean({StringRedisTemplate.class, RedisDataRuleEngine.class})
    @ConditionalOnMissingBean(DataScopeRedisTemplate.class)
    public DataScopeRedisTemplate dataScopeRedisTemplate(
            StringRedisTemplate stringRedisTemplate,
            RedisDataRuleEngine redisDataRuleEngine) {
        log.info("Registering DataScopeRedisTemplate...");
        return new DataScopeRedisTemplate(stringRedisTemplate, redisDataRuleEngine);
    }
}