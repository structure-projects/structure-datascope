package cn.structured.datascope.engine;

import cn.structured.datascope.rule.DataRule;

import java.util.Set;

/**
 * 数据规则引擎管理器接口
 * <p>
 * 用于统一管理多个数据规则引擎实现（如 MySQL、Redis、MongoDB 等），
 * 提供规则注册、获取和执行的统一入口。
 * </p>
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>管理多个数据规则引擎实例</li>
 *     <li>统一注册规则到所有引擎</li>
 *     <li>根据数据源类型获取对应的规则引擎</li>
 *     <li>提供全局规则查询和管理能力</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * DataRuleEngineManager manager = new DefaultDataRuleEngineManager();
 * manager.registerEngine("redis", redisEngine);
 * manager.registerEngine("mysql", mysqlEngine);
 *
 * // 注册规则到所有引擎
 * manager.registerRule(orderRule);
 *
 * // 获取特定数据源的引擎
 * DataRuleEngine redisEngine = manager.getEngine("redis");
 * </pre>
 *
 * @author chuck
 */
public interface DataRuleEngineManager {

    /**
     * 注册数据规则引擎
     *
     * @param name   引擎名称（如 "redis", "mysql", "mongodb"）
     * @param engine 规则引擎实例
     */
    void registerEngine(String name, DataRuleEngine engine);

    /**
     * 获取指定名称的规则引擎
     *
     * @param name 引擎名称
     * @return 规则引擎实例，若不存在则返回 null
     */
    DataRuleEngine getEngine(String name);

    /**
     * 获取所有注册的规则引擎名称
     *
     * @return 引擎名称集合
     */
    Set<String> getEngineNames();

    /**
     * 注册数据规则到所有引擎
     * <p>
     * 该规则会被同步注册到所有已注册的规则引擎中
     * </p>
     *
     * @param rule 数据规则
     */
    void registerRule(DataRule rule);

    /**
     * 注册数据规则到指定引擎
     *
     * @param engineName 引擎名称
     * @param rule       数据规则
     */
    void registerRule(String engineName, DataRule rule);

    /**
     * 从所有引擎中获取指定资源的数据规则
     * <p>
     * 默认从第一个注册的引擎获取
     * </p>
     *
     * @param resource 资源名称
     * @return 数据规则，若未注册则返回 null
     */
    DataRule getRule(String resource);

    /**
     * 清除所有引擎中的所有规则
     */
    void clearAllRules();

    /**
     * 获取默认规则引擎
     * <p>
     * 用于处理通用规则场景
     * </p>
     *
     * @return 默认规则引擎
     */
    DataRuleEngine getDefaultEngine();
}