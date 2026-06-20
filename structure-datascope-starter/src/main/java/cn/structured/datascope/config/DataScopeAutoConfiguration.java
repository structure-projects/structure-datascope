package cn.structured.datascope.config;

import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.engine.DataRuleEngineManager;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngineManager;
import cn.structured.datascope.provider.DataScopeProvider;
import cn.structured.datascope.provider.DefaultDataScopeProviderImpl;
import cn.structured.datascope.scanner.DataRuleScanner;
import cn.structured.datascope.web.DataScopeResponseBodyAdvice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

/**
 * 数据范围自动配置类
 * <p>
 * 完成数据权限上下文的完整初始化，包括：
 * 1. 字段配置 Bean
 * 2. 数据规则引擎（使用字段配置）
 * 3. 数据权限提供器（用户自定义实现）
 * 4. 数据范围路由组件
 * 5. HTTP过滤器（请求结束后自动清理上下文）
 * </p>
 * <p>
 * 数据权限获取方式：
 * - 通过 userId 去本地数据源获取（local模式）
 * - 通过 userId 去远程权限服务获取（remote模式）
 * </p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({DataScopeProperties.class, DataScopeFieldConfig.class})
@ConditionalOnWebApplication
public class DataScopeAutoConfiguration {

    private final DataScopeProperties properties;

    public DataScopeAutoConfiguration(DataScopeProperties properties) {
        this.properties = properties;
        log.info("DataScopeAutoConfiguration initialized with properties: {}", properties);
    }


    /**
     * 注册数据规则引擎管理器
     * <p>
     * 用于统一管理多个数据规则引擎实现
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataRuleEngineManager.class)
    public DataRuleEngineManager dataRuleEngineManager() {
        log.info("Registering DataRuleEngineManager...");
        return new DefaultDataRuleEngineManager();
    }

    /**
     * 注册默认数据规则引擎
     * <p>
     * 默认引擎用于处理通用规则场景，并自动注册到管理器
     * </p>
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataRuleEngine.class)
    public DataRuleEngine dataRuleEngine(DataScopeFieldConfig fieldConfig, DataRuleEngineManager engineManager) {
        log.info("Registering default DataRuleEngine with field config...");
        DataRuleEngine engine = new DefaultDataRuleEngine(fieldConfig);
        engineManager.registerEngine("default", engine);
        return engine;
    }

    /**
     * 注册默认数据权限提供器
     * <p>
     * 当用户未自定义 DataScopeProvider 且未配置远程服务时启用
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeProvider.class)
    public DataScopeProvider defaultDataScopeProvider() {
        log.info("Registering default DataScopeProvider (DefaultDataScopeProviderImpl)");
        return new DefaultDataScopeProviderImpl();
    }

    /**
     * 注册数据规则扫描器
     * <p>
     * 自动扫描带有 @DataScopeRule 注解的实体类并注册规则到管理器
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "structure.data-scope", name = "auto-register-rules", havingValue = "true", matchIfMissing = true)
    public DataRuleScanner dataRuleScanner(DataRuleEngineManager engineManager) {
        String[] scanPackages = properties.getScanPackages();
        log.info("Registering DataRuleScanner with packages: {}", Arrays.toString(scanPackages));
        DataRuleScanner dataRuleScanner = new DataRuleScanner(engineManager.getDefaultEngine(), scanPackages);
        dataRuleScanner.scanAndRegister();
        return dataRuleScanner;
    }

    /**
     * 注册数据权限响应体处理器
     * <p>
     * 自动过滤 Controller 返回对象中的敏感字段，对使用者透明
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "structure.data-scope", name = "auto-filter-response", havingValue = "true", matchIfMissing = true)
    @ConditionalOnWebApplication
    public DataScopeResponseBodyAdvice dataScopeResponseBodyAdvice(
            @Qualifier("dataRuleEngine") DataRuleEngine ruleEngine, DataScopeProperties properties) {
        log.info("Registering DataScopeResponseBodyAdvice for automatic field filtering");
        return new DataScopeResponseBodyAdvice(ruleEngine, properties);
    }
}