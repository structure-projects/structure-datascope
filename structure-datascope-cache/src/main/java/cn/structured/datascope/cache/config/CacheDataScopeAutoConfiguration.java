package cn.structured.datascope.cache.config;

import cn.structured.datascope.cache.engine.CacheDataRuleEngine;
import cn.structured.datascope.cache.manager.DataScopeCacheManager;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.DataRuleEngineManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 缓存数据权限自动配置类
 * <p>
 * 自动配置数据权限缓存管理器和缓存规则引擎，
 * 支持多种缓存技术（Caffeine、Ehcache、Redis 等）。
 * </p>
 * <p>
 * 注意：此配置需要在 DataScopeAutoConfiguration 之后加载，
 * 以确保默认的 DataRuleEngine 能够正常注册。
 * </p>
 */
@Slf4j
@Configuration
@EnableCaching
@AutoConfigureAfter(name = "cn.structured.datascope.config.DataScopeAutoConfiguration")
@ConditionalOnClass(CacheManager.class)
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheDataScopeAutoConfiguration {

    /**
     * 注册缓存数据规则引擎
     * <p>
     * 用于构建缓存键的数据权限前缀，
     * 并自动注册到规则引擎管理器。
     * 注意：此引擎是缓存模块的独立实现，不影响 starter 提供的默认引擎。
     * </p>
     */
    @Bean("cacheDataRuleEngine")
    @ConditionalOnMissingBean(name = "cacheDataRuleEngine")
    public CacheDataRuleEngine cacheDataRuleEngine(DataScopeFieldConfig fieldConfig, DataRuleEngineManager engineManager) {
        log.info("Registering CacheDataRuleEngine with field config: orgId={}, deptId={}, userId={}",
                fieldConfig.getOrgIdField(), fieldConfig.getDeptIdField(), fieldConfig.getUserIdField());
        CacheDataRuleEngine engine = new CacheDataRuleEngine(fieldConfig);
        engineManager.registerEngine("cache", engine);
        return engine;
    }

    /**
     * 注册数据权限缓存管理器
     * <p>
     * 包装底层 CacheManager，自动为缓存操作添加数据权限前缀
     * </p>
     *
     * @param delegate        底层缓存管理器（如 CaffeineCacheManager、RedisCacheManager）
     * @param cacheRuleEngine 缓存规则引擎
     */
    @Bean
    @Primary
    @ConditionalOnBean({CacheManager.class, CacheDataRuleEngine.class})
    @ConditionalOnMissingBean(DataScopeCacheManager.class)
    @ConditionalOnProperty(prefix = "structure.data-scope.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataScopeCacheManager dataScopeCacheManager(
            CacheManager delegate,
            CacheDataRuleEngine cacheRuleEngine) {
        log.info("Registering DataScopeCacheManager with delegate: {}", delegate.getClass().getSimpleName());
        return new DataScopeCacheManager(delegate, cacheRuleEngine);
    }
}