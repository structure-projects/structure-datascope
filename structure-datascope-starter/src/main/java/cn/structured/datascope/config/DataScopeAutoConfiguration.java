package cn.structured.datascope.config;

import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.engine.DataRuleEngineManager;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngine;
import cn.structured.datascope.engine.impl.DefaultDataRuleEngineManager;
import cn.structured.datascope.filter.DataScopeContextFilter;
import cn.structured.datascope.provider.DataScopeProvider;
import cn.structured.datascope.provider.DefaultDataScopeProviderImpl;
import cn.structured.datascope.provider.RemoteDataScopeProvider;
import cn.structured.datascope.scanner.DataRuleScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@EnableConfigurationProperties(DataScopeProperties.class)
@ConditionalOnWebApplication
public class DataScopeAutoConfiguration {

    private final DataScopeProperties properties;

    public DataScopeAutoConfiguration(DataScopeProperties properties) {
        this.properties = properties;
        log.info("DataScopeAutoConfiguration initialized with properties: {}", properties);
    }

    /**
     * 注册字段配置 Bean
     * <p>
     * 用于配置数据权限隔离时使用的字段名称（如 orgId、deptId、userId）
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeFieldConfig.class)
    public DataScopeFieldConfig dataScopeFieldConfig() {
        DataScopeFieldConfig fieldConfig = properties.getFieldConfig();
        if (fieldConfig == null) {
            fieldConfig = new DataScopeFieldConfig();
        }
        log.info("Registering DataScopeFieldConfig: orgIdField={}, deptIdField={}, userIdField={}",
                fieldConfig.getOrgIdField(),
                fieldConfig.getDeptIdField(),
                fieldConfig.getUserIdField());
        return fieldConfig;
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
    @ConditionalOnMissingBean(DataRuleEngine.class)
    public DataRuleEngine dataRuleEngine(DataScopeFieldConfig fieldConfig, DataRuleEngineManager engineManager) {
        log.info("Registering default DataRuleEngine with field config...");
        DataRuleEngine engine = new DefaultDataRuleEngine(fieldConfig);
        engineManager.registerEngine("default", engine);
        return engine;
    }

    /**
     * 注册远程数据权限提供器
     * <p>
     * 当配置了 remote.serviceUrl 时启用，通过 HTTP 调用远程权限服务获取数据权限信息
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeProvider.class)
    @ConditionalOnProperty(prefix = "structure.data-scope.remote", name = "service-url", matchIfMissing = false)
    public DataScopeProvider remoteDataScopeProvider() {
        log.info("Registering RemoteDataScopeProvider, remote service URL: {}", properties.getRemote().getServiceUrl());
        return new RemoteDataScopeProvider(properties.getRemote());
    }

    /**
     * 注册默认数据权限提供器
     * <p>
     * 当用户未自定义 DataScopeProvider 且未配置远程服务时启用
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeProvider.class)
    @ConditionalOnProperty(prefix = "structure.data-scope.remote", name = "service-url", matchIfMissing = true)
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
     * 注册数据范围上下文过滤器
     * <p>
     * 从HTTP请求头提取数据权限信息并设置到上下文，
     * 请求处理完成后自动清理上下文。
     * </p>
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<DataScopeContextFilter> dataScopeContextFilter(
            @org.springframework.beans.factory.annotation.Autowired(required = false) DataScopeProvider dataScopeProvider) {
        log.info("Registering DataScopeContextFilter...");
        FilterRegistrationBean<DataScopeContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DataScopeContextFilter(properties, dataScopeProvider));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("dataScopeContextFilter");
        registrationBean.setOrder(Integer.MAX_VALUE); // 最后执行
        return registrationBean;
    }
}