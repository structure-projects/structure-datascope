package cn.structured.datascope.engine.impl;

import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.engine.DataRuleEngineManager;
import cn.structured.datascope.rule.DataRule;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据规则引擎管理器的默认实现
 * <p>
 * 统一管理多个数据规则引擎，提供规则的统一注册和分发能力
 * </p>
 *
 * @author chuck
 */
@Slf4j
public class DefaultDataRuleEngineManager implements DataRuleEngineManager {

    /**
     * 规则引擎注册表
     */
    private final Map<String, DataRuleEngine> engines = new ConcurrentHashMap<>();

    /**
     * 默认引擎名称
     */
    private String defaultEngineName = "default";

    @Override
    public void registerEngine(String name, DataRuleEngine engine) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Engine name cannot be null or empty");
        }
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null");
        }

        engines.put(name, engine);
        log.info("Registered data rule engine: {}", name);

        // 如果是第一个注册的引擎，设为默认引擎
        if (engines.size() == 1) {
            defaultEngineName = name;
            log.info("Set default engine to: {}", name);
        }
    }

    @Override
    public DataRuleEngine getEngine(String name) {
        return engines.get(name);
    }

    @Override
    public Set<String> getEngineNames() {
        return new HashSet<>(engines.keySet());
    }

    @Override
    public void registerRule(DataRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        log.info("Registering rule to all engines: resource={}", rule.getResource());

        // 注册到所有引擎
        for (Map.Entry<String, DataRuleEngine> entry : engines.entrySet()) {
            try {
                entry.getValue().registerRule(rule);
                log.debug("Rule registered to engine: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Failed to register rule to engine {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    @Override
    public void registerRule(String engineName, DataRule rule) {
        if (engineName == null || engineName.isEmpty()) {
            throw new IllegalArgumentException("Engine name cannot be null or empty");
        }
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        DataRuleEngine engine = engines.get(engineName);
        if (engine == null) {
            throw new IllegalArgumentException("Engine not found: " + engineName);
        }

        engine.registerRule(rule);
        log.info("Rule registered to engine {}: resource={}", engineName, rule.getResource());
    }

    @Override
    public DataRule getRule(String resource) {
        // 从默认引擎获取规则
        DataRuleEngine defaultEngine = getDefaultEngine();
        if (defaultEngine != null) {
            return defaultEngine.getRule(resource);
        }

        // 如果没有默认引擎，从第一个注册的引擎获取
        if (!engines.isEmpty()) {
            DataRuleEngine firstEngine = engines.values().iterator().next();
            return firstEngine.getRule(resource);
        }

        return null;
    }

    @Override
    public void clearAllRules() {
        log.info("Clearing all rules from all engines");
        for (Map.Entry<String, DataRuleEngine> entry : engines.entrySet()) {
            try {
                entry.getValue().clearRules();
                log.debug("Rules cleared from engine: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Failed to clear rules from engine {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    @Override
    public DataRuleEngine getDefaultEngine() {
        return engines.get(defaultEngineName);
    }

    /**
     * 设置默认引擎名称
     *
     * @param name 引擎名称
     */
    public void setDefaultEngineName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Default engine name cannot be null or empty");
        }
        if (!engines.containsKey(name)) {
            throw new IllegalArgumentException("Engine not found: " + name);
        }

        this.defaultEngineName = name;
        log.info("Default engine set to: {}", name);
    }
}