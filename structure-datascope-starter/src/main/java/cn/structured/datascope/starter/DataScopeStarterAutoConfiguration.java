package cn.structured.datascope.starter;

import cn.structured.datascope.config.DataScopeAutoConfiguration;
import cn.structured.datascope.config.DataScopeProperties;
import cn.structured.datascope.engine.DataRuleEngine;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DataScopeProperties.class)
@ImportAutoConfiguration({
    DataScopeAutoConfiguration.class
})
public class DataScopeStarterAutoConfiguration {

    @Bean
    public DataRuleEngine dataRuleEngine() {
        return new DataRuleEngine();
    }
}
