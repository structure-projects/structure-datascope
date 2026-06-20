package cn.structured.datascope.example.redis.service;

import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.example.redis.dto.OrderResponse;
import cn.structured.datascope.redis.template.DataScopeRedisTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务
 * <p>
 * 业务代码无需感知数据权限实现，Redis键命名策略通过框架自动处理
 * </p>
 */
@Slf4j
@Service
public class OrderService {

    private final DataScopeRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DataRuleEngine ruleEngine;

    private static final String RESOURCE = "order";

    public OrderService(DataScopeRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       @Qualifier("redisDataRuleEngine") DataRuleEngine ruleEngine) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ruleEngine = ruleEngine;
    }

    /**
     * 获取订单列表
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list from Redis...");

        Set<String> keys = redisTemplate.keys(RESOURCE);
        log.info("Found {} keys in Redis: {}", keys != null ? keys.size() : 0, keys);
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        return keys.stream()
                .map(this::getOrderFromRedis)
                .filter(order -> order != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取订单详情
     */
    public OrderResponse getOrderById(Long id) {
        log.info("Fetching order by id: {}", id);

        String json = redisTemplate.get(RESOURCE, id.toString());
        return deserializeOrder(json);
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(OrderResponse request) {
        log.info("Creating order: {}", request.getOrderNo());

        Long id = System.currentTimeMillis();
        request.setId(id);
        request.setCreateTime(LocalDateTime.now());
        // 注意：创建时不应该使用数据权限上下文，应该从用户信息获取组织ID和部门ID
        // 这里仅作为示例，实际应用中应该从用户上下文获取

        String key = id.toString();
        redisTemplate.set(RESOURCE, key, serializeOrder(request));
        log.debug("Saved order to Redis with key: {}", key);

        return request;
    }

    /**
     * 更新订单
     */
    public OrderResponse updateOrder(Long id, OrderResponse request) {
        log.info("Updating order: {}", id);

        String existingJson = redisTemplate.get(RESOURCE, id.toString());

        if (existingJson == null) {
            return null;
        }

        OrderResponse existing = deserializeOrder(existingJson);
        existing.setAmount(request.getAmount());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setRemark(request.getRemark());
        existing.setUpdateTime(LocalDateTime.now());

        redisTemplate.set(RESOURCE, id.toString(), serializeOrder(existing));

        return existing;
    }

    /**
     * 删除订单
     */
    public void deleteOrder(Long id) {
        log.info("Deleting order: {}", id);
        redisTemplate.delete(RESOURCE, id.toString());
    }

    /**
     * 获取订单数量
     */
    public long getOrderCount() {
        return redisTemplate.count(RESOURCE);
    }

    /**
     * 获取当前用户的行级过滤条件
     * <p>
     * 用于在 Redis 键层面构建数据隔离前缀
     * </p>
     */
    public String getRowCondition() {
        String condition = ruleEngine.buildRowCondition(RESOURCE);
        log.info("Generated row condition: {}", condition);
        return condition;
    }

    private OrderResponse getOrderFromRedis(String fullKey) {
        // 从完整键中提取业务键（去掉数据权限前缀）
        // 前缀格式: order:orgId:10:deptId:1:
        // 完整键: order:orgId:10:deptId:1:1
        // 需要提取: 1
        String rowCondition = ruleEngine.buildRowCondition(RESOURCE);
        if (fullKey.startsWith(rowCondition)) {
            String businessKey = fullKey.substring(rowCondition.length());
            log.debug("Extracted business key: '{}' from full key: '{}'", businessKey, fullKey);
            String json = redisTemplate.get(RESOURCE, businessKey);
            log.debug("Retrieved JSON for business key '{}': {}", businessKey, json);
            return deserializeOrder(json);
        }
        // 兼容处理：如果键不匹配，返回null
        log.warn("Key '{}' does not start with row condition '{}'", fullKey, rowCondition);
        return null;
    }

    private String serializeOrder(OrderResponse order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order", e);
            return null;
        }
    }

    private OrderResponse deserializeOrder(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OrderResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize order", e);
            return null;
        }
    }
}