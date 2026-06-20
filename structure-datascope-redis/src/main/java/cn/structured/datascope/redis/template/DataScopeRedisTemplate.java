package cn.structured.datascope.redis.template;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.redis.engine.RedisDataRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 数据权限 Redis 模板
 * <p>
 * 自动为 Redis 操作添加数据权限前缀，业务代码无需感知数据权限
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 业务代码直接使用，框架自动添加数据权限前缀
 * dataScopeRedisTemplate.opsForValue().set("order:123", orderJson);
 * // 实际存储的键：order:org:10:dept:dept-1:123
 * </pre>
 */
@Slf4j
public class DataScopeRedisTemplate {

    private final StringRedisTemplate redisTemplate;
    private final RedisDataRuleEngine redisEngine;

    public DataScopeRedisTemplate(StringRedisTemplate redisTemplate, RedisDataRuleEngine redisEngine) {
        this.redisTemplate = redisTemplate;
        this.redisEngine = redisEngine;
    }

    /**
     * 获取带数据权限前缀的键
     */
    private String buildKey(String resource, String key) {
        String prefix = redisEngine.buildRowCondition(resource);
        String fullKey = prefix + key;
        if (log.isDebugEnabled()) {
            log.debug("Built data scope key: {} -> {}", key, fullKey);
        }
        return fullKey;
    }

    /**
     * 获取值操作
     */
    public ValueOperations opsForValue(String resource) {
        return new ValueOperations(redisTemplate, redisEngine, resource);
    }

    /**
     * 设置值
     */
    public void set(String resource, String key, String value) {
        String fullKey = buildKey(resource, key);
        redisTemplate.opsForValue().set(fullKey, value);
    }

    /**
     * 设置值（带过期时间）
     */
    public void set(String resource, String key, String value, long timeout, TimeUnit unit) {
        String fullKey = buildKey(resource, key);
        redisTemplate.opsForValue().set(fullKey, value, timeout, unit);
    }

    /**
     * 获取值
     */
    public String get(String resource, String key) {
        String fullKey = buildKey(resource, key);
        return redisTemplate.opsForValue().get(fullKey);
    }

    /**
     * 删除键
     */
    public Boolean delete(String resource, String key) {
        String fullKey = buildKey(resource, key);
        return redisTemplate.delete(fullKey);
    }

    /**
     * 获取符合数据权限的所有键
     */
    public Set<String> keys(String resource) {
        String pattern = redisEngine.buildRowCondition(resource) + "*";
        return redisTemplate.keys(pattern);
    }

    /**
     * 获取键数量
     */
    public Long count(String resource) {
        Set<String> keys = keys(resource);
        return keys != null ? keys.size() : 0L;
    }

    /**
     * 值操作封装类
     */
    public class ValueOperations {
        private final StringRedisTemplate template;
        private final RedisDataRuleEngine engine;
        private final String resource;

        public ValueOperations(StringRedisTemplate template, RedisDataRuleEngine engine, String resource) {
            this.template = template;
            this.engine = engine;
            this.resource = resource;
        }

        public void set(String key, String value) {
            String fullKey = buildKey(resource, key);
            template.opsForValue().set(fullKey, value);
        }

        public void set(String key, String value, long timeout, TimeUnit unit) {
            String fullKey = buildKey(resource, key);
            template.opsForValue().set(fullKey, value, timeout, unit);
        }

        public String get(String key) {
            String fullKey = buildKey(resource, key);
            return template.opsForValue().get(fullKey);
        }

        public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
            String fullKey = buildKey(resource, key);
            return template.opsForValue().setIfAbsent(fullKey, value, timeout, unit);
        }
    }
}