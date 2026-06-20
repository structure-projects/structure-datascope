package cn.structured.datascope.example.message.producer;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.message.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * 订单消息生产者服务
 *
 * <p>负责向 RabbitMQ 发送订单相关消息，支持三种数据权限传递模式：</p>
 * <ul>
 *     <li><strong>Header 模式（推荐）</strong>：通过消息头传递数据权限信息</li>
 *     <li><strong>消息体内嵌模式</strong>：在消息体中包含 orgId、deptId 等权限字段</li>
 *     <li><strong>Exchange 隔离模式</strong>：通过不同的 Exchange 实现租户隔离</li>
 * </ul>
 *
 * @see cn.structured.datascope.example.message.consumer.OrderMessageConsumer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMessageProducer {

    private static final String EXCHANGE_NAME = "order.exchange";
    private static final String ROUTING_KEY = "order.event";

    private static final String HEADER_ORG_ID = "X-DataScope-OrgId";
    private static final String HEADER_DEPT_IDS = "X-DataScope-DeptIds";
    private static final String HEADER_USER_ID = "X-DataScope-UserId";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送订单创建消息
     *
     * <p>使用 Header 模式传递数据权限信息</p>
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
                .timestamp(java.time.LocalDateTime.now())
                .build();

        sendMessageWithDataScope(event, ROUTING_KEY);
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
                .timestamp(java.time.LocalDateTime.now())
                .build();

        sendMessageWithDataScope(event, ROUTING_KEY);
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
                .timestamp(java.time.LocalDateTime.now())
                .build();

        sendMessageWithDataScope(event, ROUTING_KEY);
        log.info("Producer: Order deleted event sent successfully");
    }

    /**
     * 发送消息（包含数据权限 Header）
     *
     * <p>使用 Header 模式传递数据权限信息到消费者</p>
     *
     * @param event      订单事件
     * @param routingKey 路由键
     */
    private void sendMessageWithDataScope(OrderEvent event, String routingKey) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);

        // 设置数据权限 Header
        String orgId = DataScopeContext.getOrgId();
        java.util.List<String> deptIds = DataScopeContext.getDeptIds();
        String userId = DataScopeContext.getUserId();

        if (orgId != null) {
            properties.setHeader(HEADER_ORG_ID, orgId);
        }
        if (deptIds != null && !deptIds.isEmpty()) {
            properties.setHeader(HEADER_DEPT_IDS, String.join(",", deptIds));
        }
        if (userId != null) {
            properties.setHeader(HEADER_USER_ID, userId);
        }

        try {
            byte[] body = objectMapper.writeValueAsBytes(event);
            Message message = new Message(body, properties);

            log.debug("Producer: Sending message to exchange: {}, routingKey: {}, headers: orgId={}, deptIds={}, userId={}",
                    EXCHANGE_NAME, routingKey, orgId, deptIds, userId);

            rabbitTemplate.send(EXCHANGE_NAME, routingKey, message);
        } catch (Exception e) {
            log.error("Producer: Failed to send message", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    /**
     * 发送消息到指定组织的 Exchange
     *
     * <p>使用 Exchange 隔离模式，不同组织使用不同的 Exchange</p>
     *
     * @param event 订单事件
     * @param orgId 组织ID
     */
    public void sendOrderEventToOrg(OrderEvent event, String orgId) {
        log.info("Producer: Sending order event to org: {}", orgId);

        // 为每个组织创建专属的 Exchange
        String orgExchange = EXCHANGE_NAME + "." + orgId;
        String routingKey = "order." + event.getEventType().toLowerCase();

        rabbitTemplate.convertAndSend(orgExchange, routingKey, event);
        log.info("Producer: Order event sent to org exchange: {}", orgExchange);
    }

    /**
     * 发送消息（无数据权限 Header）
     *
     * <p>使用消息体内嵌模式，权限信息包含在消息体中</p>
     *
     * @param event 订单事件
     */
    public void sendMessageWithoutHeader(OrderEvent event) {
        log.debug("Producer: Sending message without header: {}", event.getEventId());
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, event);
    }
}
