package cn.structured.datascope.example.message.service;

import cn.structured.datascope.example.message.dto.OrderEvent;
import cn.structured.datascope.example.message.producer.OrderMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单业务服务
 *
 * <p>封装订单相关的业务逻辑，包括订单创建、更新、删除等操作</p>
 *
 * <h2>职责</h2>
 * <ul>
 *     <li>处理业务逻辑（如验证、计算等）</li>
 *     <li>调用消息生产者发送事件</li>
 *     <li>与数据权限上下文交互</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 在业务服务中注入消息生产者
 * @Service
 * public class OrderService {
 *     private final OrderBusinessService orderBusinessService;
 *
 *     public void createOrder(Order order) {
 *         // 执行业务逻辑
 *         validateOrder(order);
 *
 *         // 保存订单
 *         orderRepository.save(order);
 *
 *         // 发送消息事件（自动携带数据权限）
 *         orderBusinessService.sendOrderCreatedEvent(order.getId(), order.getOrderNo());
 *     }
 * }
 * }</pre>
 *
 * @see cn.structured.datascope.example.message.producer.OrderMessageProducer
 * @see cn.structured.datascope.example.message.consumer.OrderMessageConsumer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBusinessService {

    private final OrderMessageProducer messageProducer;

    /**
     * 创建订单并发送消息
     *
     * <p>执行订单创建的业务逻辑，然后发送订单创建事件</p>
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void createOrder(Long orderId, String orderNo) {
        log.info("Business: Creating order: {} - {}", orderId, orderNo);

        // 业务验证逻辑
        validateOrder(orderId, orderNo);

        // 执行业务操作（如保存到数据库）
        // orderRepository.save(order);

        // 发送消息事件
        messageProducer.sendOrderCreatedEvent(orderId, orderNo);

        log.info("Business: Order created successfully: {}", orderId);
    }

    /**
     * 更新订单并发送消息
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void updateOrder(Long orderId, String orderNo) {
        log.info("Business: Updating order: {} - {}", orderId, orderNo);

        // 业务验证逻辑
        validateOrder(orderId, orderNo);

        // 执行更新操作
        // orderRepository.update(order);

        // 发送消息事件
        messageProducer.sendOrderUpdatedEvent(orderId, orderNo);

        log.info("Business: Order updated successfully: {}", orderId);
    }

    /**
     * 删除订单并发送消息
     *
     * @param orderId 订单ID
     */
    public void deleteOrder(Long orderId) {
        log.info("Business: Deleting order: {}", orderId);

        // 执行删除操作
        // orderRepository.delete(orderId);

        // 发送消息事件
        messageProducer.sendOrderDeletedEvent(orderId);

        log.info("Business: Order deleted successfully: {}", orderId);
    }

    /**
     * 发送订单创建事件
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void sendOrderCreatedEvent(Long orderId, String orderNo) {
        messageProducer.sendOrderCreatedEvent(orderId, orderNo);
    }

    /**
     * 发送订单更新事件
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void sendOrderUpdatedEvent(Long orderId, String orderNo) {
        messageProducer.sendOrderUpdatedEvent(orderId, orderNo);
    }

    /**
     * 发送订单删除事件
     *
     * @param orderId 订单ID
     */
    public void sendOrderDeletedEvent(Long orderId) {
        messageProducer.sendOrderDeletedEvent(orderId);
    }

    /**
     * 验证订单信息
     *
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    private void validateOrder(Long orderId, String orderNo) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (orderNo == null || orderNo.isEmpty()) {
            throw new IllegalArgumentException("Order number cannot be empty");
        }
        log.debug("Business: Order validation passed for: {} - {}", orderId, orderNo);
    }
}
