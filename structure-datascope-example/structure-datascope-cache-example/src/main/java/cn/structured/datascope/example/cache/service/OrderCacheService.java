package cn.structured.datascope.example.cache.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.cache.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单缓存服务（使用 Spring Cache 注解）
 * <p>
 * 演示如何使用 @Cacheable、@CachePut、@CacheEvict 注解，
 * DataScopeCacheManager 会自动为缓存键添加数据权限前缀
 * </p>
 *
 * <p>缓存键格式示例：</p>
 * <pre>
 * // 原始缓存名称: order
 * // 数据权限前缀: order:orgId:10:deptId:1
 * // 完整缓存键: order:orgId:10:deptId:1::123 (Redis 格式)
 * </pre>
 */
@Slf4j
@Service
public class OrderCacheService {

    /**
     * 获取订单（使用 @Cacheable 注解）
     * <p>
     * 如果缓存中存在则直接返回，不存在则执行方法并缓存结果。
     * 缓存键会自动添加数据权限前缀。
     * </p>
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @Cacheable(value = "order", key = "#id")
    public OrderResponse getOrderByIdWithCache(Long id) {
        log.info("Executing getOrderByIdWithCache for id: {} (cache miss)", id);
        // 模拟从数据库获取数据
        return createMockOrder(id);
    }

    /**
     * 获取订单列表（使用 @Cacheable 注解）
     * <p>
     * 缓存整个订单列表，键为固定的 "list"
     * </p>
     */
    @Cacheable(value = "order", key = "'list'")
    public List<OrderResponse> getOrderListWithCache() {
        log.info("Executing getOrderListWithCache (cache miss)");
        // 模拟从数据库获取数据
        List<OrderResponse> orders = new java.util.ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            orders.add(createMockOrder(i));
        }
        return orders;
    }

    /**
     * 创建或更新订单（使用 @CachePut 注解）
     * <p>
     * 每次执行都会更新缓存，适用于需要强制更新缓存的场景。
     * </p>
     *
     * @param order 订单信息
     * @return 订单信息
     */
    @CachePut(value = "order", key = "#order.id")
    public OrderResponse saveOrderWithCache(OrderResponse order) {
        log.info("Executing saveOrderWithCache for order: {}", order.getId());
        // 模拟保存到数据库
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        return order;
    }

    /**
     * 删除订单（使用 @CacheEvict 注解）
     * <p>
     * 删除缓存中的指定订单
     * </p>
     *
     * @param id 订单ID
     */
    @CacheEvict(value = "order", key = "#id")
    public void deleteOrderWithCache(Long id) {
        log.info("Executing deleteOrderWithCache for id: {}", id);
        // 模拟从数据库删除
    }

    /**
     * 清空所有订单缓存（使用 @CacheEvict 注解）
     * <p>
     * 清空整个缓存区域，注意：只会清空当前数据权限范围内的缓存
     * </p>
     */
    @CacheEvict(value = "order", allEntries = true)
    public void clearAllOrderCache() {
        log.info("Executing clearAllOrderCache");
        // 清空缓存操作
    }

    /**
     * 条件缓存（使用 condition 属性）
     * <p>
     * 只有当 condition 为 true 时才缓存
     * </p>
     */
    @Cacheable(value = "order", key = "#id", condition = "#id > 100")
    public OrderResponse getOrderConditionally(Long id) {
        log.info("Executing getOrderConditionally for id: {} (only cached if id > 100)", id);
        return createMockOrder(id);
    }

    /**
     * 排除缓存（使用 unless 属性）
     * <p>
     * 当 unless 为 true 时不缓存结果
     * </p>
     */
    @Cacheable(value = "order", key = "#id", unless = "#result.amount < 100")
    public OrderResponse getOrderUnlessCondition(Long id) {
        log.info("Executing getOrderUnlessCondition for id: {} (not cached if amount < 100)", id);
        OrderResponse order = createMockOrder(id);
        // 小 ID 的订单金额会小于 100
        return order;
    }

    /**
     * 多缓存操作（使用 @Caching 注解组合多个缓存操作）
     * <p>
     * 同时执行多个缓存操作
     * </p>
     */
    @org.springframework.cache.annotation.Caching(
            cacheable = @Cacheable(value = "order", key = "#id"),
            evict = @CacheEvict(value = "order", key = "'list'")
    )
    public OrderResponse getOrderAndEvictList(Long id) {
        log.info("Executing getOrderAndEvictList for id: {}", id);
        // 获取订单并清空列表缓存
        return createMockOrder(id);
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
        String orgId = DataScopeContext.getOrgId();
        List<String> deptIds = DataScopeContext.getDeptIds();
        order.setOrgId(orgId != null ? Long.parseLong(orgId.replace("org-", "")) : 1L);
        order.setDeptId(deptIds != null && !deptIds.isEmpty() 
                ? Long.parseLong(deptIds.get(0).replace("dept-", "")) : 1L);
        return order;
    }
}