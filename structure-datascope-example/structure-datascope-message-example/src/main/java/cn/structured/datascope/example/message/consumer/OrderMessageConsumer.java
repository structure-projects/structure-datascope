package cn.structured.datascope.example.message.consumer;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.message.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;

/**
 * 订单消息消费者服务
 *
 * <p>负责从 RabbitMQ 接收订单相关消息，支持三种数据权限接收模式：</p>
 * <ul>
 *     <li><strong>Header 模式（推荐）</strong>：从消息头提取数据权限信息</li>
 *     <li><strong>消息体内嵌模式</strong>：从消息体中提取 orgId、deptId 等权限字段</li>
 *     <li><strong>Exchange 隔离模式</strong>：通过不同的队列接收特定租户的消息</li>
 * </ul>
 *
 * <h2>消费示例</h2>
 * <pre>{@code
 * // Header 模式消费
 * @RabbitListener(queues = "order-queue")
 * public void consumeOrderEvent(Message message) {
 *     // 提取 Header 中的数据权限信息
 *     String orgId = message.getMessageProperties().getHeader("X-DataScope-OrgId");
 *     String deptIdsStr = message.getMessageProperties().getHeader("X-DataScope-DeptIds");
 *
 *     // 设置数据权限上下文
 *     DataScopeInfo info = new DataScopeInfo();
 *     info.setOrgId(orgId);
 *     if (deptIdsStr != null) {
 *         info.setDeptIds(Arrays.asList(deptIdsStr.split(",")));
 *     }
 *     DataScopeContext.setInfo(info);
 *
 *     try {
 *         // 处理业务逻辑（自动应用数据权限）
 *         OrderEvent event = deserialize(message.getBody());
 *         processOrderEvent(event);
 *     } finally {
 *         DataScopeContext.remove();
 *     }
 * }
 *
 * // 消息体内嵌模式消费
 * @RabbitListener(queues = "order-queue-inline")
 * public void consumeOrderEventInline(OrderEvent event) {
 *     // 验证消息是否在当前用户权限范围内
 *     String currentOrgId = DataScopeContext.getOrgId();
 *     if (currentOrgId != null && !currentOrgId.equals(event.getOrgId())) {
 *         log.warn("Message orgId mismatch, discard: {}", event.getEventId());
 *         return;
 *     }
 *
 *     // 处理业务逻辑
 *     processOrderEvent(event);
 * }
 * }</pre>
 *
 * @see cn.structured.datascope.example.message.producer.OrderMessageProducer
 */
@Slf4j
@Component
public class OrderMessageConsumer {

    private static final String HEADER_ORG_ID = "X-DataScope-OrgId";
    private static final String HEADER_DEPT_IDS = "X-DataScope-DeptIds";
    private static final String HEADER_USER_ID = "X-DataScope-UserId";

    private final ObjectMapper objectMapper;

    public OrderMessageConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 监听并处理订单消息（Header 模式）
        log.info("Consumer: Received order event message with header mode");

        try {
            // 从消息 Header 中提取并设置数据权限上下文
            setContextFromHeader(message);

            // 处理消息
            processMessage(message);

        } finally {
            // 清理上下文
            DataScopeContext.remove();
            log.debug("Consumer: Data scope context removed");
        }
    }

    /**
     * 监听并处理订单消息（消息体内嵌模式）
     *
     * <p>直接从消息体中提取权限信息并验证</p>
     *
     * @param event 订单事件对象
     */
    @RabbitListener(queues = "order-queue-inline")
    public void consumeOrderEventInline(OrderEvent event) {
        log.info("Consumer: Received order event message with inline mode: {} - {}",
                event.getEventType(), event.getOrderId());

        // 验证消息是否在当前用户权限范围内
        String currentOrgId = DataScopeContext.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(event.getOrgId())) {
            log.warn("Consumer: Message orgId {} not in current scope {}, discard: {}",
                    event.getOrgId(), currentOrgId, event.getEventId());
            return;
        }

        // 处理订单事件
        processOrderEvent(event);
    }

    /**
     * 监听组织专属队列的消息（Exchange 隔离模式）
     *
     * <p>接收发送到特定组织 Exchange 的消息，无需额外权限验证</p>
     *
     * @param event 订单事件对象
     */
    @RabbitListener(queues = "order-queue-org")
    public void consumeOrderEventForOrg(OrderEvent event) {
        log.info("Consumer: Received order event for org queue: {} - {}",
                event.getEventType(), event.getOrderId());

        // Exchange 隔离模式下，消息已经按组织分离，无需额外验证
        processOrderEvent(event);
    }

    /**
     * 从消息 Header 中提取并设置数据权限上下文
     *
     * @param message RabbitMQ 消息对象
     */
    private void setContextFromHeader(Message message) {
        org.springframework.amqp.core.MessageProperties messageProperties = message.getMessageProperties();
        String orgId = messageProperties.getHeader(HEADER_ORG_ID);
        String deptIdsStr = messageProperties.getHeader(HEADER_DEPT_IDS);
        String userId = messageProperties.getHeader(HEADER_USER_ID);

        if (orgId != null || deptIdsStr != null || userId != null) {
            DataScopeInfo info = new DataScopeInfo();
            info.setOrgId(orgId);
            info.setUserId(userId);

            if (deptIdsStr != null && !deptIdsStr.isEmpty()) {
                info.setDeptIds(Arrays.asList(deptIdsStr.split(",")));
            }

            DataScopeContext.setInfo(info);
            log.debug("Consumer: Data scope context set from message header: orgId={}, deptIds={}, userId={}",
                    orgId, info.getDeptIds(), userId);
        }
    }

    /**
     * 处理消息
     *
     * @param message RabbitMQ 消息对象
     */
    private void processMessage(Message message) {
        // 获取消息内容类型
        String contentType = message.getMessageProperties().getContentType();

        if ("application/json".equals(contentType)) {
            try {
                // 反序列化消息
                OrderEvent event = objectMapper.readValue(message.getBody(), OrderEvent.class);

                log.info("Consumer: Processing order event: {} - {}", event.getEventType(), event.getOrderId());
                processOrderEvent(event);

            } catch (Exception e) {
                log.error("Consumer: Failed to deserialize message", e);
                throw new RuntimeException("Failed to deserialize message", e);
            }
        } else {
            log.warn("Consumer: Unsupported content type: {}", contentType);
        }
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

    /**
     * 处理订单创建事件
     */
    private void handleOrderCreated(OrderEvent event) {
        log.info("Consumer: Handling ORDER_CREATED event: orderId={}, orderNo={}",
                event.getOrderId(), event.getOrderNo());
        // 实际业务处理逻辑...
    }

    /**
     * 处理订单更新事件
     */
    private void handleOrderUpdated(OrderEvent event) {
        log.info("Consumer: Handling ORDER_UPDATED event: orderId={}, orderNo={}",
                event.getOrderId(), event.getOrderNo());
        // 实际业务处理逻辑...
    }

    /**
     * 处理订单删除事件
     */
    private void handleOrderDeleted(OrderEvent event) {
        log.info("Consumer: Handling ORDER_DELETED event: orderId={}", event.getOrderId());
        // 实际业务处理逻辑...
    }
}
