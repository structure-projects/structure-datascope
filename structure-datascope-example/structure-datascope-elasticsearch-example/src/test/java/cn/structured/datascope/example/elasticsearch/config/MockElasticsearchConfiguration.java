package cn.structured.datascope.example.elasticsearch.config;

import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class MockElasticsearchConfiguration {

    private final Map<String, OrderResponse> orderStore = new HashMap<>();

    public MockElasticsearchConfiguration() {
        initMockData();
    }

    private void initMockData() {
        orderStore.put("1", createOrder("1", "ORD-2024-001", 10L, 1L, new BigDecimal("99.99"), "13800138001", "customer1@example.com", "First order remark"));
        orderStore.put("2", createOrder("2", "ORD-2024-002", 10L, 2L, new BigDecimal("199.99"), "13800138002", "customer2@example.com", "Second order remark"));
        orderStore.put("3", createOrder("3", "ORD-2024-003", 10L, 3L, new BigDecimal("299.99"), "13800138003", "customer3@example.com", "Third order remark"));
        orderStore.put("4", createOrder("4", "ORD-2024-004", 20L, 6L, new BigDecimal("399.99"), "13800138004", "customer4@example.com", "Fourth order remark"));
        orderStore.put("5", createOrder("5", "ORD-2024-005", 20L, 7L, new BigDecimal("499.99"), "13800138005", "customer5@example.com", "Fifth order remark"));
    }

    private OrderResponse createOrder(String id, String orderNo, Long orgId, Long deptId, BigDecimal amount, String phone, String email, String remark) {
        OrderResponse order = new OrderResponse();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setOrgId(orgId);
        order.setDeptId(deptId);
        order.setAmount(amount);
        order.setPhone(phone);
        order.setEmail(email);
        order.setRemark(remark);
        order.setCreateTime(LocalDateTime.now());
        order.setCreateBy("admin");
        return order;
    }

    @Bean
    @Primary
    public ElasticsearchTemplate elasticsearchTemplate() {
        ElasticsearchTemplate template = mock(ElasticsearchTemplate.class);

        when(template.search(any(Query.class), any(Class.class), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    Query queryArg = invocation.getArgument(0);
                    List<SearchHit<OrderResponse>> searchHits = new ArrayList<>();
                    for (Map.Entry<String, OrderResponse> entry : orderStore.entrySet()) {
                        SearchHit<OrderResponse> hit = mock(SearchHit.class);
                        when(hit.getId()).thenReturn(entry.getKey());
                        when(hit.getContent()).thenReturn(entry.getValue());
                        when(hit.getIndex()).thenReturn("orders");
                        searchHits.add(hit);
                    }

                    return new SearchHitsImpl<>(
                            searchHits.size(),
                            TotalHitsRelation.EQUAL_TO,
                            0.0f,
                            Duration.ZERO,
                            null,
                            null,
                            searchHits,
                            null,
                            null,
                            null
                    );
                });

        when(template.get(anyString(), any(Class.class), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(0);
                    return orderStore.getOrDefault(id, orderStore.values().iterator().next());
                });

        when(template.save(any(Object.class), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    Object entity = invocation.getArgument(0);
                    if (entity instanceof OrderResponse order) {
                        if (order.getId() == null || order.getId().isEmpty()) {
                            order.setId(UUID.randomUUID().toString());
                        }
                        orderStore.put(order.getId(), order);
                        return order;
                    }
                    return entity;
                });

        when(template.delete(anyString(), any(IndexCoordinates.class)))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(0);
                    orderStore.remove(id);
                    return id;
                });

        return template;
    }
}