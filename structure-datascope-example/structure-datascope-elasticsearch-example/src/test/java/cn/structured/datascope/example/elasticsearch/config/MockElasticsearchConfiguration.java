package cn.structured.datascope.example.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
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
        orderStore.put("1", createOrder(1L, "ORD-2024-001", 10L, 1L, new BigDecimal("99.99"), "13800138001", "customer1@example.com", "First order remark"));
        orderStore.put("2", createOrder(2L, "ORD-2024-002", 10L, 2L, new BigDecimal("199.99"), "13800138002", "customer2@example.com", "Second order remark"));
        orderStore.put("3", createOrder(3L, "ORD-2024-003", 10L, 3L, new BigDecimal("299.99"), "13800138003", "customer3@example.com", "Third order remark"));
        orderStore.put("4", createOrder(4L, "ORD-2024-004", 20L, 6L, new BigDecimal("399.99"), "13800138004", "customer4@example.com", "Fourth order remark"));
        orderStore.put("5", createOrder(5L, "ORD-2024-005", 20L, 7L, new BigDecimal("499.99"), "13800138005", "customer5@example.com", "Fifth order remark"));
    }

    private OrderResponse createOrder(Long id, String orderNo, Long orgId, Long deptId, BigDecimal amount, String phone, String email, String remark) {
        OrderResponse order = new OrderResponse();
        order.setId(String.valueOf(id));
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
    @SuppressWarnings("unchecked")
    public ElasticsearchClient elasticsearchClient() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);

        when(client.search(any(co.elastic.clients.elasticsearch.core.SearchRequest.class), any(Class.class)))
                .thenAnswer(invocation -> {
                    SearchResponse<OrderResponse> response = mock(SearchResponse.class);
                    co.elastic.clients.elasticsearch.core.search.HitsMetadata<OrderResponse> hitsMetadata = mock(co.elastic.clients.elasticsearch.core.search.HitsMetadata.class);
                    TotalHits totalHits = mock(TotalHits.class);

                    List<Hit<OrderResponse>> hits = new ArrayList<>();
                    for (Map.Entry<String, OrderResponse> entry : orderStore.entrySet()) {
                        Hit<OrderResponse> hit = mock(Hit.class);
                        when(hit.id()).thenReturn(entry.getKey());
                        when(hit.source()).thenReturn(entry.getValue());
                        hits.add(hit);
                    }

                    when(hitsMetadata.hits()).thenReturn(hits);
                    when(hitsMetadata.total()).thenReturn(totalHits);
                    when(totalHits.value()).thenReturn((long) hits.size());
                    when(totalHits.relation()).thenReturn(TotalHitsRelation.Eq);
                    when(response.hits()).thenReturn(hitsMetadata);

                    return response;
                });

        when(client.get(any(co.elastic.clients.elasticsearch.core.GetRequest.class), any(Class.class)))
                .thenAnswer(invocation -> {
                    GetResponse<OrderResponse> response = mock(GetResponse.class);
                    when(response.found()).thenReturn(true);
                    when(response.source()).thenReturn(orderStore.values().iterator().next());
                    return response;
                });

        when(client.index(any(co.elastic.clients.elasticsearch.core.IndexRequest.class)))
                .thenAnswer(invocation -> {
                    co.elastic.clients.elasticsearch.core.IndexResponse indexResponse = mock(co.elastic.clients.elasticsearch.core.IndexResponse.class);
                    when(indexResponse.id()).thenReturn(UUID.randomUUID().toString());
                    when(indexResponse.result()).thenReturn(co.elastic.clients.elasticsearch._types.Result.Created);
                    return indexResponse;
                });

        when(client.delete(any(co.elastic.clients.elasticsearch.core.DeleteRequest.class)))
                .thenAnswer(invocation -> {
                    co.elastic.clients.elasticsearch.core.DeleteResponse deleteResponse = mock(co.elastic.clients.elasticsearch.core.DeleteResponse.class);
                    when(deleteResponse.id()).thenReturn("1");
                    when(deleteResponse.result()).thenReturn(co.elastic.clients.elasticsearch._types.Result.Deleted);
                    return deleteResponse;
                });

        return client;
    }
}