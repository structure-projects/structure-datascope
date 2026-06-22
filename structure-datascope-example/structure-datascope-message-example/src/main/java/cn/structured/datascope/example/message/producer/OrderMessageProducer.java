package cn.structured.datascope.example.message.producer;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.message.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单消息生产者服务
 *
 * <p>基于Spring Cloud Stream实现消息发送，自动透传数据权限信息</p>
 */
@Slf4j
@Service
public class OrderMessageProducer {

    private static final String OUTPUT_BINDING = "orderEventOutput";

    private final StreamBridge streamBridge;

    public OrderMessageProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /**
     * 发送订单创建消息
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void sendOrderCreatedEvent(Long orderId, String orderNo) {
        log.info("Producer: Sending order created event for order: {}", orderId);

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .eventType("ORDER_CREATED")
                .orgId(DataScopeContext.getOrgId())
                .deptId(DataScopeContext.getDeptIds().isEmpty() ? null : DataScopeContext.getDeptIds().get(0))
                .userId(DataScopeContext.getUserId())
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(event);
        log.info("Producer: Order created event sent successfully");
    }

    /**
     * 发送订单更新消息
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void sendOrderUpdatedEvent(Long orderId, String orderNo) {
        log.info("Producer: Sending order updated event for order: {}", orderId);

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .eventType("ORDER_UPDATED")
                .orgId(DataScopeContext.getOrgId())
                .deptId(DataScopeContext.getDeptIds().isEmpty() ? null : DataScopeContext.getDeptIds().get(0))
                .userId(DataScopeContext.getUserId())
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(event);
        log.info("Producer: Order updated event sent successfully");
    }

    /**
     * 发送订单删除消息
     *
     * @param orderId 订单ID
     */
    public void sendOrderDeletedEvent(Long orderId) {
        log.info("Producer: Sending order deleted event for order: {}", orderId);

        OrderEvent event = OrderEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .eventType("ORDER_DELETED")
                .orgId(DataScopeContext.getOrgId())
                .deptId(DataScopeContext.getDeptIds().isEmpty() ? null : DataScopeContext.getDeptIds().get(0))
                .userId(DataScopeContext.getUserId())
                .timestamp(LocalDateTime.now())
                .build();

        sendMessage(event);
        log.info("Producer: Order deleted event sent successfully");
    }

    /**
     * 使用StreamBridge发送消息
     *
     * <p>数据权限信息会通过DataScopeProducerInterceptor自动注入到消息头</p>
     *
     * @param event 订单事件
     */
    private void sendMessage(OrderEvent event) {
        Message<OrderEvent> message = MessageBuilder.withPayload(event).build();

        log.debug("Producer: Sending message via StreamBridge, binding: {}, eventId: {}",
                OUTPUT_BINDING, event.getEventId());

        boolean sent = streamBridge.send(OUTPUT_BINDING, message);
        if (!sent) {
            throw new RuntimeException("Failed to send message to binding: " + OUTPUT_BINDING);
        }
    }
}