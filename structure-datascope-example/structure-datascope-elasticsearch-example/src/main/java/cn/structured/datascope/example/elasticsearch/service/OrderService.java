package cn.structured.datascope.example.elasticsearch.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OrderService {

    private final ElasticsearchTemplate elasticsearchTemplate;

//    private final ElasticsearchRepository repository;

    public OrderService(ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list from Elasticsearch...");

        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.matchAll(m -> m))
                    .build();

            SearchHits<OrderResponse> searchHits = elasticsearchTemplate.search(
                    query, OrderResponse.class, IndexCoordinates.of("orders"));

            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to search orders from Elasticsearch", e);
            return new ArrayList<>();
        }
    }

    public OrderResponse getOrderById(String id) {
        log.info("Fetching order by id: {}", id);

        try {
            return elasticsearchTemplate.get(id, OrderResponse.class, IndexCoordinates.of("orders"));
        } catch (Exception e) {
            log.error("Failed to get order from Elasticsearch", e);
            return null;
        }
    }

    public OrderResponse createOrder(OrderResponse request) {
        log.info("Creating order: {}", request.getOrderNo());

        try {
            request.setCreateTime(LocalDateTime.now());
            request.setCreateBy(DataScopeContext.getUserId());
            request.setOrgId(Long.parseLong(DataScopeContext.getOrgId()));
            request.setDeptId(Long.parseLong(DataScopeContext.getDeptIds().get(0)));

            elasticsearchTemplate.save(request, IndexCoordinates.of("orders"));

            return request;

        } catch (Exception e) {
            log.error("Failed to index order to Elasticsearch", e);
            return null;
        }
    }

    public OrderResponse updateOrder(String id, OrderResponse request) {
        log.info("Updating order: {}", id);

        try {
            OrderResponse existing = getOrderById(id);
            if (existing == null) {
                return null;
            }

            existing.setAmount(request.getAmount());
            existing.setPhone(request.getPhone());
            existing.setEmail(request.getEmail());
            existing.setRemark(request.getRemark());
            existing.setUpdateTime(LocalDateTime.now());
            existing.setUpdateBy(DataScopeContext.getUserId());

            existing.setId(id);
            elasticsearchTemplate.save(existing, IndexCoordinates.of("orders"));

            return existing;

        } catch (Exception e) {
            log.error("Failed to update order in Elasticsearch", e);
            return null;
        }
    }

    public void deleteOrder(String id) {
        log.info("Deleting order: {}", id);

        try {
            elasticsearchTemplate.delete(id, IndexCoordinates.of("orders"));
        } catch (Exception e) {
            log.error("Failed to delete order from Elasticsearch", e);
        }
    }

    public long getOrderCount() {
        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.matchAll(m -> m))
                    .build();

            SearchHits<OrderResponse> searchHits = elasticsearchTemplate.search(
                    query, OrderResponse.class, IndexCoordinates.of("orders"));
            return searchHits.getTotalHits();
        } catch (Exception e) {
            log.error("Failed to count orders", e);
            return 0;
        }
    }
}