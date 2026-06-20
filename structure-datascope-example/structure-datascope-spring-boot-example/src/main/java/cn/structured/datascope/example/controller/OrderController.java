package cn.structured.datascope.example.controller;

import cn.structure.common.entity.ResResultVO;
import cn.structure.common.utils.ResultUtilSimpleImpl;
import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.dto.OrderResponse;
import cn.structured.datascope.example.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * <p>
 * 演示数据范围管理在 REST API 中的使用
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
     * <p>
     * 数据权限信息通过 DataScopeProvider 自动获取
     * </p>
     */
    @GetMapping
    public ResResultVO<List<OrderResponse>> getOrderList() {
        log.info("GET /api/orders - Fetching order list");

        // 获取当前数据范围上下文信息
        List<String> roles = DataScopeContext.getRoles();

        log.info("Current request -, roles: {}", roles);

        List<OrderResponse> orders = orderService.getOrderList();

        return ResultUtilSimpleImpl.success(orders);
    }

    /**
     * 获取订单列表（测试专用 - 直接返回数据）
     * <p>
     * 此接口用于测试数据权限过滤功能，返回不带包装的数据
     * </p>
     */
    @GetMapping("/list")
    public List<OrderResponse> getOrderListDirect() {
        log.info("GET /api/orders/list - Fetching order list directly");
        return orderService.getOrderList();
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public ResResultVO<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("GET /api/orders/{} - Fetching order detail", id);

        OrderResponse order = orderService.getOrderById(id);

        return ResultUtilSimpleImpl.success(order);
    }

    /**
     * 获取行级过滤条件
     * <p>
     * 返回当前用户对 order 资源的 SQL WHERE 条件
     * </p>
     */
    @GetMapping("/row-condition")
    public ResResultVO<String> getRowCondition() {
        log.info("GET /api/orders/row-condition - Getting row condition");

        String condition = orderService.getRowCondition();

        return ResultUtilSimpleImpl.success(condition);
    }

    /**
     * 获取当前数据范围上下文信息
     */
    @GetMapping("/context")
    public ResResultVO<Object> getContext() {
        log.info("GET /api/orders/context - Getting data scope context");

        return ResultUtilSimpleImpl.success(DataScopeContext.get());
    }
}