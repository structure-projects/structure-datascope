package cn.structured.datascope.engine;

import cn.structured.datascope.rule.DataRule;

/**
 * 数据规则引擎接口
 * <p>
 * 定义数据权限规则处理的标准契约，不同数据源（MySQL、Redis、MongoDB等）需自行实现此接口。
 * </p>
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>数据规则注册与获取</li>
 *     <li>列级权限验证（判断字段是否可见）</li>
 *     <li>对象字段过滤（根据权限处理字段）</li>
 *     <li>行级条件构建（不同数据源格式不同）</li>
 * </ul>
 *
 * <p>实现说明：</p>
 * <ul>
 *     <li>core 模块提供 {@link cn.structured.datascope.engine.impl.DefaultDataRuleEngine} 作为默认实现</li>
 *     <li>MySQL 实现：参考 structure-datascope-mybatis-plus 模块</li>
 *     <li>Redis 实现：参考 structure-datascope-redis 模块</li>
 *     <li>其他数据源请在各模块中自行实现</li>
 * </ul>
 */
public interface DataRuleEngine {

    /**
     * 注册数据规则
     *
     * @param rule 数据规则
     */
    void registerRule(DataRule rule);

    /**
     * 获取指定资源的数据规则
     *
     * @param resource 资源名称
     * @return 数据规则，若未注册则返回 null
     */
    DataRule getRule(String resource);

    /**
     * 判断指定资源的字段是否对当前用户可见
     *
     * @param resource  资源名称
     * @param fieldName 字段名称
     * @return true 表示可见
     */
    boolean canSeeField(String resource, String fieldName);

    /**
     * 根据数据权限规则过滤对象的字段
     * <p>
     * 不可见的字段会被设置为 null 或根据实现进行相应处理
     * </p>
     *
     * @param obj      待过滤的对象
     * @param resource 资源名称（如果对象有 @DataScopeRule 注解则忽略此参数）
     */
    void filter(Object obj, String resource);

    /**
     * 构建行级数据访问条件
     * <p>
     * 不同数据源返回不同格式的条件：
     * <ul>
     *     <li>MySQL: 返回 SQL WHERE 条件片段，如 "org_id = ? AND dept_id IN (?, ?)"</li>
     *     <li>Redis: 返回 Redis 键前缀或条件表达式</li>
     *     <li>MongoDB: 返回查询过滤器对象</li>
     *     <li>Elasticsearch: 返回查询 DSL</li>
     * </ul>
     * </p>
     *
     * @param resource 资源名称
     * @return 条件表达式，如果没有规则则返回空字符串或 null
     */
    String buildRowCondition(String resource);

    /**
     * 清除所有注册的规则
     */
    void clearRules();
}