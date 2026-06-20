package cn.structured.datascope.example.message.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * RabbitMQ 配置类
 *
 * <p>配置队列、交换机、绑定关系以及消息转换器</p>
 *
 * <h2>队列配置</h2>
 * <ul>
 *     <li><strong>order-queue</strong>：用于 Header 模式消费</li>
 *     <li><strong>order-queue-inline</strong>：用于消息体内嵌模式消费</li>
 *     <li><strong>order-queue-org</strong>：用于组织专属队列消费（Exchange 隔离模式）</li>
 * </ul>
 *
 * <h2>交换机配置</h2>
 * <ul>
 *     <li><strong>order.exchange</strong>：主交换机（Direct Exchange）</li>
 *     <li><strong>order.exchange.{orgId}</strong>：组织专属交换机（按需动态创建）</li>
 * </ul>
 *
 * @see cn.structured.datascope.example.message.producer.OrderMessageProducer
 * @see cn.structured.datascope.example.message.consumer.OrderMessageConsumer
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order.exchange";
    public static final String QUEUE_NAME = "order.queue";
    public static final String ROUTING_KEY = "order.event";

    public static final String QUEUE_NAME_INLINE = "order.queue.inline";
    public static final String QUEUE_NAME_ORG = "order.queue.org";

    public static final String EXCHANGE_ORG_PREFIX = "order.exchange.";

    /**
     * 配置主交换机
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * 配置主队列（Header 模式）
     */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-message-ttl", 86400000) // 消息过期时间：24小时
                .build();
    }

    /**
     * 配置内嵌模式队列（消息体内嵌权限模式）
     */
    @Bean
    public Queue orderQueueInline() {
        return QueueBuilder.durable(QUEUE_NAME_INLINE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    /**
     * 配置组织专属队列（Exchange 隔离模式）
     */
    @Bean
    public Queue orderQueueOrg() {
        return QueueBuilder.durable(QUEUE_NAME_ORG)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    /**
     * 绑定主队列到主交换机
     */
    @Bean
    public Binding orderQueueBinding() {
        return BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with(ROUTING_KEY);
    }

    /**
     * 绑定内嵌模式队列到主交换机
     */
    @Bean
    public Binding orderQueueInlineBinding() {
        return BindingBuilder
                .bind(orderQueueInline())
                .to(orderExchange())
                .with(ROUTING_KEY + ".inline");
    }

    /**
     * 绑定组织专属队列到主交换机
     */
    @Bean
    public Binding orderQueueOrgBinding() {
        return BindingBuilder
                .bind(orderQueueOrg())
                .to(orderExchange())
                .with(ROUTING_KEY + ".org");
    }

    /**
     * 配置 RabbitTemplate
     *
     * <p>使用 Spring Boot 自动配置的 ObjectMapper（Jackson 3）</p>
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // Spring Boot 4 使用 Jackson 3，直接使用 ObjectMapper
        // 不需要额外的 MessageConverter，RabbitTemplate 会自动处理
        return template;
    }
}
