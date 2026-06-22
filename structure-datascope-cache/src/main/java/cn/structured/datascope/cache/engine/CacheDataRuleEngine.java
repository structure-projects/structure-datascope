package cn.structured.datascope.cache.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 缓存数据规则引擎实现
 * <p>
 * 实现基于 Spring Cache 抽象的数据权限缓存键构建，
 * 支持多种缓存技术（Caffeine、Ehcache、Redis 等）。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * CacheDataRuleEngine engine = new CacheDataRuleEngine();
 *
 * // 构建缓存键前缀
 * String prefix = engine.buildCacheKeyPrefix("order");
 * // 输出: order:orgId:10:deptId:dept-1
 *
 * // 构建完整缓存键
 * String cacheKey = engine.buildCacheKey("order", "123");
 * // 输出: order:orgId:10:deptId:dept-1:123
 * </pre>
 *
 * <p>支持的缓存技术：</p>
 * <ul>
 *     <li>Caffeine：本地高性能缓存</li>
 *     <li>Ehcache：本地/分布式缓存</li>
 *     <li>Redis：分布式缓存</li>
 *     <li>其他 Spring Cache 支持的缓存实现</li>
 * </ul>
 */
@Setter
@Slf4j
public class CacheDataRuleEngine extends DefaultDataRuleEngine {

    /**
     * 缓存键分隔符
     */
    private static final String CACHE_KEY_SEPARATOR = ":";

    public CacheDataRuleEngine() {
    }

    public CacheDataRuleEngine(DataScopeFieldConfig fieldConfig) {
        super(fieldConfig);
    }

    @Override
    public String buildRowCondition(String resource) {
        return buildCacheKeyPrefix(resource);
    }

    /**
     * 构建缓存键前缀
     * <p>
     * 根据当前用户的数据权限上下文构建缓存键前缀，
     * 用于实现不同数据权限范围的缓存隔离。
     * 格式: {resourceName}:{orgId}:{deptId}
     * </p>
     *
     * @param resource 资源名称（如 "order", "user"）
     * @return 缓存键前缀，如 "order:10:1"
     */
    public String buildCacheKeyPrefix(String resource) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        StringBuilder sb = new StringBuilder();
        sb.append(resource);

        if (orgId != null && !orgId.isEmpty()) {
            sb.append(CACHE_KEY_SEPARATOR).append(orgId);
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            sb.append(CACHE_KEY_SEPARATOR).append(deptIds.get(0));
        }

        String prefix = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("Built cache key prefix for resource '{}': {}", resource, prefix);
        }
        return prefix;
    }

    /**
     * 构建完整缓存键
     * <p>
     * 将业务键与数据权限前缀组合，生成完整的缓存键。
     * 格式: {resourceName}:{orgId}:{deptId}:{businessKey}
     * </p>
     *
     * @param resource   资源名称
     * @param businessKey 业务键（如订单ID、用户ID等）
     * @return 完整缓存键，如 "order:10:1:123"
     */
    public String buildCacheKey(String resource, String businessKey) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        StringBuilder sb = new StringBuilder();
        sb.append(resource);

        if (orgId != null && !orgId.isEmpty()) {
            sb.append(CACHE_KEY_SEPARATOR).append(orgId);
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            sb.append(CACHE_KEY_SEPARATOR).append(deptIds.get(0));
        }

        sb.append(CACHE_KEY_SEPARATOR).append(businessKey);

        String fullKey = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("Built full cache key: {} -> {}", businessKey, fullKey);
        }
        return fullKey;
    }

    /**
     * 构建缓存名称
     * <p>
     * 返回带数据权限前缀的缓存名称，用于 Spring Cache 的 @Cacheable 等注解。
     * </p>
     *
     * @param resource 资源名称
     * @return 缓存名称，如 "order:orgId:10:deptId:dept-1"
     */
    public String buildCacheName(String resource) {
        return buildCacheKeyPrefix(resource);
    }

    /**
     * 从完整缓存键中提取业务键
     *
     * @param resource 资源名称
     * @param fullKey  完整缓存键
     * @return 业务键
     */
    public String extractBusinessKey(String resource, String fullKey) {
        String prefix = buildCacheKeyPrefix(resource);
        if (fullKey.startsWith(prefix + CACHE_KEY_SEPARATOR)) {
            return fullKey.substring(prefix.length() + CACHE_KEY_SEPARATOR.length());
        }
        return fullKey;
    }

    /**
     * 判断指定的缓存键是否符合当前数据范围
     *
     * @param cacheKey 缓存键
     * @param resource 资源名称
     * @return true 表示符合当前数据范围
     */
    public boolean matchesScope(String cacheKey, String resource) {
        String expectedPrefix = buildCacheKeyPrefix(resource);
        return cacheKey.startsWith(expectedPrefix);
    }
}