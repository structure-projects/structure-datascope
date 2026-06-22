package cn.structured.datascope.example.cache.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.cache.engine.CacheDataRuleEngine;
import cn.structured.datascope.example.cache.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务
 * <p>
 * 使用 Spring Cache 注解进行缓存操作，
 * 数据权限前缀由 DataScopeCacheManager 自动处理
 * </p>
 */
@Slf4j
@Service
public class OrderService {

    private final CacheManager cacheManager;
    private final CacheDataRuleEngine cacheRuleEngine;

    private static final String RESOURCE = "order";

    public OrderService(CacheManager cacheManager,
                        @Qualifier("cacheDataRuleEngine") CacheDataRuleEngine cacheRuleEngine) {
        this.cacheManager = cacheManager;
        this.cacheRuleEngine = cacheRuleEngine;
    }

    /**
     * 获取订单列表
     * <p>
     * 注意：Spring Cache 的 @Cacheable 不支持获取所有缓存项，
     * 这里使用 CacheManager 直接操作演示数据权限缓存效果
     * </p>
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list from cache...");
        // 模拟数据，实际应用中应从数据库获取
        List<OrderResponse> orders = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            OrderResponse order = getOrderById(i);
            if (order != null) {
                orders.add(order);
            }
        }
        return orders;
    }

    /**
     * 获取订单详情
     * <p>
     * 使用 CacheManager 获取缓存，自动应用数据权限前缀
     * </p>
     */
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);
        Cache cache = cacheManager.getCache(RESOURCE);
        if (cache == null) {
            log.warn("Cache '{}' not found", RESOURCE);
            return null;
        }
        OrderResponse order = cache.get(id, OrderResponse.class);
        if (order == null) {
            log.info("Order {} not in cache, creating mock data", id);
            // 模拟数据
            order = createMockOrder(id);
            cache.put(id, order);
        }
        return order;
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(OrderResponse request) {
        log.info("Creating order: {}", request.getOrderNo());
        Long id = System.currentTimeMillis();
        request.setId(id);
        request.setCreateTime(LocalDateTime.now());

        Cache cache = cacheManager.getCache(RESOURCE);
        if (cache != null) {
            cache.put(id, request);
            log.debug("Saved order to cache with key: {}", id);
        }
        return request;
    }

    /**
     * 更新订单
     */
    public OrderResponse updateOrder(Long id, OrderResponse request) {
        log.info("Updating order: {}", id);
        Cache cache = cacheManager.getCache(RESOURCE);
        if (cache == null) {
            return null;
        }

        OrderResponse existing = cache.get(id, OrderResponse.class);
        if (existing == null) {
            return null;
        }

        existing.setAmount(request.getAmount());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setRemark(request.getRemark());
        existing.setUpdateTime(LocalDateTime.now());

        cache.put(id, existing);
        return existing;
    }

    /**
     * 删除订单
     */
    public void deleteOrder(Long id) {
        log.info("Deleting order: {}", id);
        Cache cache = cacheManager.getCache(RESOURCE);
        if (cache != null) {
            cache.evict(id);
        }
    }

    /**
     * 获取当前用户的行级过滤条件
     * <p>
     * 用于演示缓存键前缀构建
     * </p>
     */
    public String getRowCondition() {
        String condition = cacheRuleEngine.buildRowCondition(RESOURCE);
        log.info("Generated row condition: {}", condition);
        return condition;
    }

    /**
     * 获取缓存键前缀
     */
    public String getCacheKeyPrefix() {
        return cacheRuleEngine.buildCacheKeyPrefix(RESOURCE);
    }

    /**
     * 构建完整缓存键
     */
    public String buildFullCacheKey(Long id) {
        return cacheRuleEngine.buildCacheKey(RESOURCE, String.valueOf(id));
    }

    /**
     * 创建模拟订单数据
     */
    private OrderResponse createMockOrder(Long id) {
        OrderResponse order = new OrderResponse();
        order.setId(id);
        order.setOrderNo("ORD-" + id);
        order.setAmount(new BigDecimal("100.00").multiply(new BigDecimal(id)));
        order.setPhone("13800138000");
        order.setEmail("test@example.com");
        order.setRemark("Mock order data");
        order.setCreateTime(LocalDateTime.now());
        // 设置数据权限字段
        order.setOrgId(DataScopeContext.getOrgId() != null ? Long.parseLong(DataScopeContext.getOrgId()) : null);
        order.setDeptId(DataScopeContext.getDeptIds() != null && !DataScopeContext.getDeptIds().isEmpty() 
                ? Long.parseLong(DataScopeContext.getDeptIds().get(0)) : null);
        return order;
    }
}