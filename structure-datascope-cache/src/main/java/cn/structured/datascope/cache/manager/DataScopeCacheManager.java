package cn.structured.datascope.cache.manager;

import cn.structured.datascope.cache.engine.CacheDataRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

/**
 * 数据权限缓存管理器
 * <p>
 * 包装底层 CacheManager，自动为缓存操作添加数据权限前缀，
 * 实现不同数据权限范围的缓存隔离。
 * </p>
 * <p>
 * 缓存键格式: cache::{resourceName}:{orgId}:{deptId}:{businessKey}
 * 例如: cache::order:10:1:123
 * </p>
 */
@Slf4j
public class DataScopeCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final CacheDataRuleEngine cacheRuleEngine;

    /**
     * 内部缓存名称，用于底层缓存管理器
     */
    private static final String INTERNAL_CACHE_NAME = "dataScope";

    /**
     * 构造数据权限缓存管理器
     *
     * @param delegate         底层缓存管理器（如 CaffeineCacheManager、RedisCacheManager）
     * @param cacheRuleEngine  缓存规则引擎
     */
    public DataScopeCacheManager(CacheManager delegate, CacheDataRuleEngine cacheRuleEngine) {
        this.delegate = delegate;
        this.cacheRuleEngine = cacheRuleEngine;
        log.info("DataScopeCacheManager initialized with delegate: {}", delegate.getClass().getSimpleName());
    }

    private static final String DEFAULT_CACHE_NAME = "cache";

    @Override
    public Cache getCache(String name) {
        // 使用固定的缓存名称 "cache"，Redis 存储键为: cache::{resourceName}:{orgId}:{deptId}:{businessKey}
        // 例如: cache::order:10:1:123
        Cache delegateCache = delegate.getCache(DEFAULT_CACHE_NAME);

        if (delegateCache == null) {
            return null;
        }

        return new DataScopeCache(delegateCache, name, cacheRuleEngine);
    }

    @Override
    public Collection<String> getCacheNames() {
        // 返回原始缓存名称，实际使用时会自动添加前缀
        return delegate.getCacheNames();
    }

    /**
     * 数据权限缓存包装类
     * <p>
     * 包装底层 Cache，自动为键添加数据权限前缀
     * </p>
     */
    private static class DataScopeCache implements Cache {

        private final Cache delegate;
        private final String resourceName;
        private final CacheDataRuleEngine ruleEngine;

        public DataScopeCache(Cache delegate, String resourceName, CacheDataRuleEngine ruleEngine) {
            this.delegate = delegate;
            this.resourceName = resourceName;
            this.ruleEngine = ruleEngine;
        }

        @Override
        public String getName() {
            return resourceName;
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            String scopedKey = buildScopedKey(key);
            if (log.isDebugEnabled()) {
                log.debug("Cache get: {} -> {}", key, scopedKey);
            }
            return delegate.get(scopedKey);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            String scopedKey = buildScopedKey(key);
            if (log.isDebugEnabled()) {
                log.debug("Cache get with type: {} -> {}", key, scopedKey);
            }
            return delegate.get(scopedKey, type);
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            String scopedKey = buildScopedKey(key);
            if (log.isDebugEnabled()) {
                log.debug("Cache get with loader: {} -> {}", key, scopedKey);
            }
            return delegate.get(scopedKey, valueLoader);
        }

        @Override
        public void put(Object key, Object value) {
            String scopedKey = buildScopedKey(key);
            if (log.isDebugEnabled()) {
                log.debug("Cache put: {} -> {}", key, scopedKey);
            }
            delegate.put(scopedKey, value);
        }

        @Override
        public void evict(Object key) {
            String scopedKey = buildScopedKey(key);
            if (log.isDebugEnabled()) {
                log.debug("Cache evict: {} -> {}", key, scopedKey);
            }
            delegate.evict(scopedKey);
        }

        @Override
        public void clear() {
            if (log.isDebugEnabled()) {
                log.debug("Cache clear for resource: {}", resourceName);
            }
            delegate.clear();
        }

        /**
         * 构建带数据权限前缀的缓存键
         */
        private String buildScopedKey(Object key) {
            return ruleEngine.buildCacheKey(resourceName, String.valueOf(key));
        }
    }
}