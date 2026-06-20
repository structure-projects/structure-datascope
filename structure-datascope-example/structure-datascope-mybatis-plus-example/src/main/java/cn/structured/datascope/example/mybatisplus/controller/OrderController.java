package cn.structured.datascope.example.mybatisplus.controller;

import cn.structure.common.entity.ResResultVO;
import cn.structure.common.utils.ResultUtilSimpleImpl;
import cn.structured.datascope.example.mybatisplus.dto.OrderResponse;
import cn.structured.datascope.example.mybatisplus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * <p>
 * 演示数据权限在REST API中的应用
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 获取订单列表
     */
    @GetMapping
    public ResResultVO<List<OrderResponse>> getOrderList() {
        log.info("API: GET /api/orders");
        List<OrderResponse> orders = orderService.getOrderList();
        return ResultUtilSimpleImpl.success(orders);
    }

    /**
     * 获取订单列表（用于列级数据权限测试）
     * <p>
     * 此接口直接返回 List，不经过 ResResultVO 包装，
     * 以便 DataScopeResponseBodyAdvice 能够处理内层数据的列级权限过滤
     * </p>
     */
    @GetMapping("/list")
    public List<OrderResponse> getOrderListDirect() {
        log.info("API: GET /api/orders/list");
        return orderService.getOrderList();
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public ResResultVO<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("API: GET /api/orders/{}", id);
        OrderResponse order = orderService.getOrderById(id);
        return ResultUtilSimpleImpl.success(order);
    }

    /**
     * 创建订单
     */
    @PostMapping
    public OrderResponse createOrder(@RequestBody OrderResponse request) {
        log.info("API: POST /api/orders");
        OrderResponse order = orderService.createOrder(request);
        return order;
    }

    /**
     * 更新订单
     */
    @PutMapping("/{id}")
    public OrderResponse updateOrder(
            @PathVariable Long id,
            @RequestBody OrderResponse request) {
        log.info("API: PUT /api/orders/{}", id);
        OrderResponse order = orderService.updateOrder(id, request);
        return order;
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        log.info("API: DELETE /api/orders/{}", id);
        orderService.deleteOrder(id);
    }

    /**
     * 获取订单数量
     */
    @GetMapping("/count")
    public long getOrderCount() {
        log.info("API: GET /api/orders/count");
        return orderService.getOrderCount();
    }
}