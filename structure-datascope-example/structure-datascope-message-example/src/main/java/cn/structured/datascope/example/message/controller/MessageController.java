package cn.structured.datascope.example.message.controller;

import cn.structure.common.entity.ResResultVO;
import cn.structure.common.utils.ResultUtilSimpleImpl;
import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.message.producer.OrderMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单消息控制器
 *
 * <p>提供 REST API 接口，用于发送订单消息并测试数据权限透传功能</p>
 * <p>数据权限上下文由过滤器/拦截器或测试代码设置，控制器不负责设置</p>
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
    public ResResultVO<Void> sendOrderCreated(
            @RequestParam(name = "orderId") Long orderId,
            @RequestParam(name = "orderNo") String orderNo) {

        log.info("API: POST /api/messages/order/created - orderId: {}, orderNo: {}, userId: {}", 
                orderId, orderNo, DataScopeContext.getUserId());

        messageProducer.sendOrderCreatedEvent(orderId, orderNo);

        return ResultUtilSimpleImpl.success(null);
    }

    @PostMapping("/order/updated")
    public ResResultVO<Void> sendOrderUpdated(
            @RequestParam(name = "orderId") Long orderId,
            @RequestParam(name = "orderNo") String orderNo) {

        log.info("API: POST /api/messages/order/updated - orderId: {}, orderNo: {}", orderId, orderNo);

        messageProducer.sendOrderUpdatedEvent(orderId, orderNo);
        return ResultUtilSimpleImpl.success(null);
    }

    @PostMapping("/order/deleted")
    public ResResultVO<Void> sendOrderDeleted(@RequestParam(name = "orderId") Long orderId) {

        log.info("API: POST /api/messages/order/deleted - orderId: {}", orderId);

        messageProducer.sendOrderDeletedEvent(orderId);
        return ResultUtilSimpleImpl.success(null);
    }

    @PostMapping("/test")
    public ResResultVO<String> testDataScopeTransmission(
            @RequestParam(name = "orderId") Long orderId,
            @RequestParam(name = "orderNo") String orderNo) {

        log.info("API: POST /api/messages/test - Testing data scope transmission");

        messageProducer.sendOrderCreatedEvent(orderId, orderNo);
        return ResultUtilSimpleImpl.success("Message sent successfully");
    }

    @GetMapping("/context")
    public ResResultVO<Object> getContext() {
        log.info("API: GET /api/messages/context");
        return ResultUtilSimpleImpl.success(DataScopeContext.get());
    }
}