package cn.structured.datascope.example.cache.controller;

import cn.structure.common.entity.ResResultVO;
import cn.structure.common.utils.ResultUtilSimpleImpl;
import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.cache.dto.OrderResponse;
import cn.structured.datascope.example.cache.service.OrderService;
import lombok.extern.slf4j.Slf4j;
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
    public ResResultVO<List<OrderResponse>> getOrderList() {
        log.info("API: GET /api/orders");
        List<OrderResponse> orders = orderService.getOrderList();
        return ResultUtilSimpleImpl.success(orders);
    }

    @GetMapping("/list")
    public List<OrderResponse> getOrderListDirect() {
        log.info("API: GET /api/orders/list");
        return orderService.getOrderList();
    }

    @GetMapping("/{id}")
    public ResResultVO<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("API: GET /api/orders/{}", id);
        OrderResponse order = orderService.getOrderById(id);
        return ResultUtilSimpleImpl.success(order);
    }

    @PostMapping
    public OrderResponse createOrder(@RequestBody OrderResponse request) {
        log.info("API: POST /api/orders");
        OrderResponse order = orderService.createOrder(request);
        return order;
    }

    @PutMapping("/{id}")
    public OrderResponse updateOrder(
            @PathVariable Long id,
            @RequestBody OrderResponse request) {
        log.info("API: PUT /api/orders/{}", id);
        OrderResponse order = orderService.updateOrder(id, request);
        return order;
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        log.info("API: DELETE /api/orders/{}", id);
        orderService.deleteOrder(id);
    }

    /**
     * 获取行级过滤条件
     */
    @GetMapping("/row-condition")
    public ResResultVO<String> getRowCondition() {
        log.info("API: GET /api/orders/row-condition");
        String condition = orderService.getRowCondition();
        return ResultUtilSimpleImpl.success(condition);
    }

    /**
     * 获取缓存键前缀
     */
    @GetMapping("/cache-key-prefix")
    public ResResultVO<String> getCacheKeyPrefix() {
        log.info("API: GET /api/orders/cache-key-prefix");
        String prefix = orderService.getCacheKeyPrefix();
        return ResultUtilSimpleImpl.success(prefix);
    }

    /**
     * 构建完整缓存键
     */
    @GetMapping("/full-cache-key/{id}")
    public ResResultVO<String> buildFullCacheKey(@PathVariable Long id) {
        log.info("API: GET /api/orders/full-cache-key/{}", id);
        String fullKey = orderService.buildFullCacheKey(id);
        return ResultUtilSimpleImpl.success(fullKey);
    }

    /**
     * 获取当前数据范围上下文信息
     */
    @GetMapping("/context")
    public ResResultVO<Object> getContext() {
        log.info("API: GET /api/orders/context");
        return ResultUtilSimpleImpl.success(DataScopeContext.get());
    }
}