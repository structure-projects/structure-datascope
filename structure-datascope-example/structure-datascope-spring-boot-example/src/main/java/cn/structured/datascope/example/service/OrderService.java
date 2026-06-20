package cn.structured.datascope.example.service;

import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.example.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 订单服务
 * <p>
 * 演示如何在业务服务中使用数据规则引擎
 * </p>
 */
@Slf4j
@Service
public class OrderService {

    private final DataRuleEngine ruleEngine;

    public OrderService(DataRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 获取订单列表
     * <p>
     * 注意：此处不调用 ruleEngine.filter()，字段过滤由框架自动完成
     * （通过 DataScopeResponseBodyAdvice 在响应序列化前自动过滤）
     * </p>
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list...");
        return createMockOrders();
    }

    /**
     * 获取订单详情
     * <p>
     * 注意：此处不调用 ruleEngine.filter()，字段过滤由框架自动完成
     * </p>
     */
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);
        return createMockOrder(id, "ORD-" + id);
    }

    /**
     * 获取当前用户的行级过滤条件
     * <p>
     * 用于在 DAO/Mapper 层构建 WHERE 条件
     * </p>
     */
    public String getRowCondition() {
        String condition = ruleEngine.buildRowCondition("order");
        log.info("Generated row condition: {}", condition);
        return condition;
    }

    /**
     * 构建模拟订单列表
     */
    private List<OrderResponse> createMockOrders() {
        List<OrderResponse> orders = new ArrayList<>();
        orders.add(createMockOrder(1L, "ORD-2024-001"));
        orders.add(createMockOrder(2L, "ORD-2024-002"));
        orders.add(createMockOrder(3L, "ORD-2024-003"));
        return orders;
    }

    /**
     * 创建模拟订单
     */
    private OrderResponse createMockOrder(Long id, String orderNo) {
        return new OrderResponse(
                id,
                orderNo,
                new BigDecimal("9999.99"),
                "13800138000",
                "customer@example.com",
                "这是内部机密备注",
                10L,
                1L,
                LocalDateTime.now(),
                "admin"
        );
    }
}