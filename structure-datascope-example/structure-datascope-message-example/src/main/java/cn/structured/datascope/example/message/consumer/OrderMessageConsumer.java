package cn.structured.datascope.example.message.consumer;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.message.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * 订单消息消费者服务
 *
 * <p>基于Spring Cloud Stream函数式编程模型实现消息消费</p>
 */
@Slf4j
@Configuration
public class OrderMessageConsumer {

    /**
     * 定义消息消费函数
     *
     * <p>数据权限信息会通过DataScopeConsumerInterceptor自动从消息头提取并设置到DataScopeContext</p>
     */
    @Bean
    public Consumer<OrderEvent> orderEventInput() {
        return event -> {
            log.info("Consumer: Received order event message: {} - {}", 
                    event.getEventType(), event.getOrderId());

            log.info("Consumer: Current DataScopeContext - userId: {}, orgId: {}, deptIds: {}",
                    DataScopeContext.getUserId(),
                    DataScopeContext.getOrgId(),
                    DataScopeContext.getDeptIds());

            processOrderEvent(event);
        };
    }

    /**
     * 处理订单事件
     *
     * @param event 订单事件
     */
    private void processOrderEvent(OrderEvent event) {
        log.info("Consumer: Processing order event: {} - {}", event.getEventType(), event.getOrderId());

        switch (event.getEventType()) {
            case "ORDER_CREATED":
                handleOrderCreated(event);
                break;
            case "ORDER_UPDATED":
                handleOrderUpdated(event);
                break;
            case "ORDER_DELETED":
                handleOrderDeleted(event);
                break;
            default:
                log.warn("Consumer: Unknown event type: {}", event.getEventType());
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        log.info("Consumer: Handling ORDER_CREATED event: orderId={}, orderNo={}, orgId={}",
                event.getOrderId(), event.getOrderNo(), DataScopeContext.getOrgId());
    }

    private void handleOrderUpdated(OrderEvent event) {
        log.info("Consumer: Handling ORDER_UPDATED event: orderId={}, orderNo={}, orgId={}",
                event.getOrderId(), event.getOrderNo(), DataScopeContext.getOrgId());
    }

    private void handleOrderDeleted(OrderEvent event) {
        log.info("Consumer: Handling ORDER_DELETED event: orderId={}, orgId={}",
                event.getOrderId(), DataScopeContext.getOrgId());
    }
}