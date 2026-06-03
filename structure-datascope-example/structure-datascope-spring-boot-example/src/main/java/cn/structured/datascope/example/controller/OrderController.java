package cn.structured.datascope.example.controller;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.dto.ApiResponse;
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
     * 通过请求头传递数据范围信息：
     * - X-DataScope-Id: 数据范围ID
     * - X-DataScope-Roles: 用户角色（多个用逗号分隔）
     * - X-Org-Id: 组织ID
     * - X-Dept-Ids: 部门ID列表
     * - X-User-Id: 用户ID
     * </p>
     */
    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrderList() {
        log.info("GET /api/orders - Fetching order list");

        // 获取当前数据范围上下文信息
        String dataScopeId = DataScopeContext.getDataScopeId();
        List<String> roles = DataScopeContext.getRoles();

        log.info("Current request - dataScopeId: {}, roles: {}", dataScopeId, roles);

        List<OrderResponse> orders = orderService.getOrderList();

        return ApiResponse.success(orders, dataScopeId, roles);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("GET /api/orders/{} - Fetching order detail", id);

        String dataScopeId = DataScopeContext.getDataScopeId();
        List<String> roles = DataScopeContext.getRoles();

        OrderResponse order = orderService.getOrderById(id);

        return ApiResponse.success(order, dataScopeId, roles);
    }

    /**
     * 获取行级过滤条件
     * <p>
     * 返回当前用户对 order 资源的 SQL WHERE 条件
     * </p>
     */
    @GetMapping("/row-condition")
    public ApiResponse<String> getRowCondition() {
        log.info("GET /api/orders/row-condition - Getting row condition");

        String condition = orderService.getRowCondition();

        return ApiResponse.success(condition);
    }

    /**
     * 获取当前数据范围上下文信息
     */
    @GetMapping("/context")
    public ApiResponse<Object> getContext() {
        log.info("GET /api/orders/context - Getting data scope context");

        return ApiResponse.success(DataScopeContext.get());
    }
}