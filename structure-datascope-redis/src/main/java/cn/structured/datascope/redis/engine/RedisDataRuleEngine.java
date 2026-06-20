package cn.structured.datascope.redis.engine;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.config.DataScopeFieldConfig;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Redis 数据规则引擎实现
 * <p>
 * 实现行级数据访问的 Redis 键前缀或模式匹配构建，
 * 用于在 Redis 操作中自动添加数据范围隔离。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * RedisDataRuleEngine engine = new RedisDataRuleEngine();
 * engine.registerRule(orderRule);
 *
 * // 获取 Redis 键前缀
 * String prefix = engine.buildRowCondition("order");
 * // 输出: order:orgId:10:deptId:dept-1:deptId:dept-2:
 * </pre>
 *
 * <p>支持的 Redis 数据隔离方式：</p>
 * <ul>
 *     <li>键前缀隔离：order:orgId:10:deptId:dept-1</li>
 *     <li>模式匹配：order:*orgId:10*:*deptId:dept-1*</li>
 * </ul>
 */
@Setter
@Slf4j
public class RedisDataRuleEngine extends DefaultDataRuleEngine {

    /**
     * 键分隔符
     */
    private static final String KEY_SEPARATOR = ":";

    /**
     * 键前缀隔离模式
     * -- SETTER --
     *  设置隔离模式

     */
    private IsolationMode isolationMode = IsolationMode.PREFIX;

    /**
     * 隔离模式枚举
     */
    public enum IsolationMode {
        /**
         * 键前缀模式：使用固定前缀进行精确匹配
         */
        PREFIX,

        /**
         * 模式匹配模式：使用通配符进行模糊匹配
         */
        PATTERN
    }

    public RedisDataRuleEngine() {
    }

    public RedisDataRuleEngine(DataScopeFieldConfig fieldConfig) {
        super(fieldConfig);
    }

    public RedisDataRuleEngine(IsolationMode isolationMode) {
        this.isolationMode = isolationMode;
    }

    public RedisDataRuleEngine(DataScopeFieldConfig fieldConfig, IsolationMode isolationMode) {
        super(fieldConfig);
        this.isolationMode = isolationMode;
    }

    @Override
    public String buildRowCondition(String resource) {
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();

        if (isolationMode == IsolationMode.PREFIX) {
            return buildPrefixCondition(resource, orgId, deptIds);
        } else {
            return buildPatternCondition(resource, orgId, deptIds);
        }
    }

    /**
     * 构建键前缀条件
     * <p>
     * 例如：order:orgId:10:deptId:1
     * </p>
     */
    private String buildPrefixCondition(String resource, String orgId, List<String> deptIds) {
        StringBuilder sb = new StringBuilder();
        sb.append(resource);

        if (orgId != null && !orgId.isEmpty()) {
            sb.append(KEY_SEPARATOR).append(getOrgIdField()).append(KEY_SEPARATOR).append(orgId);
        }

        // 只取第一个部门ID，避免多层级前缀
        if (deptIds != null && !deptIds.isEmpty()) {
            sb.append(KEY_SEPARATOR).append(getDeptIdField()).append(KEY_SEPARATOR).append(deptIds.get(0));
        }

        sb.append(KEY_SEPARATOR);

        String condition = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("Built Redis prefix for resource '{}': {}", resource, condition);
        }
        return condition;
    }

    /**
     * 构建模式匹配条件
     * <p>
     * 例如：order:*orgId:10*:*deptId:dept-1*:*deptId:dept-2*
     * </p>
     */
    private String buildPatternCondition(String resource, String orgId, List<String> deptIds) {
        StringBuilder sb = new StringBuilder();
        sb.append(resource);

        boolean hasCondition = false;

        if (orgId != null && !orgId.isEmpty()) {
            sb.append(KEY_SEPARATOR).append("*").append(getOrgIdField()).append(KEY_SEPARATOR).append(orgId).append("*");
            hasCondition = true;
        }

        if (deptIds != null && !deptIds.isEmpty()) {
            for (String deptId : deptIds) {
                sb.append(KEY_SEPARATOR).append("*").append(getDeptIdField()).append(KEY_SEPARATOR).append(deptId).append("*");
                hasCondition = true;
            }
        }

        if (!hasCondition) {
            return "*";
        }

        String condition = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("Built Redis pattern for resource '{}': {}", resource, condition);
        }
        return condition;
    }

    /**
     * 构建 SCAN 命令使用的模式
     */
    public String buildScanPattern(String resource) {
        String prefix = buildPrefixCondition(resource, DataScopeContext.getOrgId(), DataScopeContext.getDeptIds());
        return prefix + "*";
    }

    /**
     * 判断指定的键是否符合当前数据范围
     */
    public boolean matchesScope(String key, String resource) {
        String expectedPrefix = buildPrefixCondition(resource, DataScopeContext.getOrgId(), DataScopeContext.getDeptIds());
        return key.startsWith(expectedPrefix);
    }

    /**
     * 获取完整的 Redis 键
     * <p>
     * 根据数据范围自动添加前缀
     * </p>
     */
    public String buildKey(String resource, String key) {
        String prefix = buildPrefixCondition(resource, DataScopeContext.getOrgId(), DataScopeContext.getDeptIds());
        return prefix + key;
    }
}