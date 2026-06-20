package cn.structured.datascope.example.elasticsearch.controller;

import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import cn.structured.datascope.example.elasticsearch.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrderList() {
        log.info("API: GET /api/orders");
        List<OrderResponse> orders = orderService.getOrderList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        log.info("API: GET /api/orders/{}", id);
        OrderResponse order = orderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderResponse request) {
        log.info("API: POST /api/orders");
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable String id,
            @RequestBody OrderResponse request) {
        log.info("API: PUT /api/orders/{}", id);
        OrderResponse order = orderService.updateOrder(id, request);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        log.info("API: DELETE /api/orders/{}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getOrderCount() {
        log.info("API: GET /api/orders/count");
        long count = orderService.getOrderCount();
        return ResponseEntity.ok(count);
    }
}