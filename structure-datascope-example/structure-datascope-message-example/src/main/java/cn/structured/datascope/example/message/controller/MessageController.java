package cn.structured.datascope.example.message.controller;

import cn.structured.datascope.example.message.dto.OrderEvent;
import cn.structured.datascope.example.message.producer.OrderMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 订单消息控制器
 *
 * <p>提供 REST API 接口，用于发送订单消息到 RabbitMQ</p>
 *
 * @see cn.structured.datascope.example.message.producer.OrderMessageProducer
 * @see cn.structured.datascope.example.message.consumer.OrderMessageConsumer
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final OrderMessageProducer messageProducer;

    public MessageController(OrderMessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @PostMapping("/order/created")
    public ResponseEntity<Void> sendOrderCreated(@RequestParam Long orderId, @RequestParam String orderNo) {
        log.info("API: POST /api/messages/order/created");
        messageProducer.sendOrderCreatedEvent(orderId, orderNo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order/updated")
    public ResponseEntity<Void> sendOrderUpdated(@RequestParam Long orderId, @RequestParam String orderNo) {
        log.info("API: POST /api/messages/order/updated");
        messageProducer.sendOrderUpdatedEvent(orderId, orderNo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order/deleted")
    public ResponseEntity<Void> sendOrderDeleted(@RequestParam Long orderId) {
        log.info("API: POST /api/messages/order/deleted");
        messageProducer.sendOrderDeletedEvent(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/process")
    public ResponseEntity<Void> processEvent(@RequestBody OrderEvent event) {
        log.info("API: POST /api/messages/process");
        // 直接处理消息（实际生产环境中应发送到队列）
        return ResponseEntity.ok().build();
    }
}