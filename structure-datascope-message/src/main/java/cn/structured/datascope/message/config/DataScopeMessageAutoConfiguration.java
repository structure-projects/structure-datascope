package cn.structured.datascope.message.config;

import cn.structured.datascope.message.interceptor.DataScopeConsumerInterceptor;
import cn.structured.datascope.message.interceptor.DataScopeProducerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

/**
 * 数据权限消息传递自动配置类
 * <p>
 * 自动配置Spring Cloud Stream的生产者和消费者拦截器，
 * 实现数据权限信息从生产者到消费者的透传
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "structure.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataScopeMessageAutoConfiguration {

    /**
     * 注册数据权限生产者拦截器
     * <p>
     * 在消息发送前将DataScopeContext中的数据权限信息注入到消息头
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeProducerInterceptor.class)
    public DataScopeProducerInterceptor dataScopeProducerInterceptor() {
        log.info("Registering DataScopeProducerInterceptor for Spring Cloud Stream");
        return new DataScopeProducerInterceptor();
    }

    /**
     * 注册数据权限消费者拦截器
     * <p>
     * 在消息消费前从消息头中提取数据权限信息并设置到DataScopeContext
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(DataScopeConsumerInterceptor.class)
    public DataScopeConsumerInterceptor dataScopeConsumerInterceptor() {
        log.info("Registering DataScopeConsumerInterceptor for Spring Cloud Stream");
        return new DataScopeConsumerInterceptor();
    }

    /**
     * 注册BeanPostProcessor来自动将拦截器添加到所有Stream通道
     */
    @Bean
    public BeanPostProcessor dataScopeChannelBeanPostProcessor(
            DataScopeProducerInterceptor producerInterceptor,
            DataScopeConsumerInterceptor consumerInterceptor) {
        log.info("Registering DataScopeChannelBeanPostProcessor");
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof InterceptableChannel channel) {
                    if (beanName != null) {
                        if (beanName.endsWith("-out-0")) {
                            log.debug("Adding producer interceptor to channel: {}", beanName);
                            channel.addInterceptor(producerInterceptor);
                        } else if (beanName.endsWith("-in-0")) {
                            log.debug("Adding consumer interceptor to channel: {}", beanName);
                            channel.addInterceptor(consumerInterceptor);
                        }
                    }
                }
                return bean;
            }
        };
    }
}