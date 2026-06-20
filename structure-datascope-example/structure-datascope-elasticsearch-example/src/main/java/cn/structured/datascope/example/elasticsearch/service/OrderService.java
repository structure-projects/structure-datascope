package cn.structured.datascope.example.elasticsearch.service;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务
 * <p>
 * 演示如何在Elasticsearch项目中集成数据权限
 * </p>
 */
@Slf4j
@Service
public class OrderService {

    private final ElasticsearchClient esClient;

    public OrderService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 获取订单列表（数据权限由AOP切面自动注入）
     */
    public List<OrderResponse> getOrderList() {
        log.info("Fetching order list from Elasticsearch...");

        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index("orders"));
            SearchResponse<OrderResponse> response = esClient.search(request, OrderResponse.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(java.util.stream.Collectors.toList());

        } catch (IOException e) {
            log.error("Failed to search orders from Elasticsearch", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取订单详情
     */
    public OrderResponse getOrderById(String id) {
        log.info("Fetching order by id: {}", id);

        try {
            GetResponse<OrderResponse> response = esClient.get(g -> g
                    .index("orders")
                    .id(id), OrderResponse.class);

            return response.source();

        } catch (IOException e) {
            log.error("Failed to get order from Elasticsearch", e);
            return null;
        }
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(OrderResponse request) {
        log.info("Creating order: {}", request.getOrderNo());

        try {
            request.setCreateTime(LocalDateTime.now());
            request.setCreateBy(DataScopeContext.getUserId());
            request.setOrgId(Long.parseLong(DataScopeContext.getOrgId()));
            request.setDeptId(Long.parseLong(DataScopeContext.getDeptIds().get(0)));

            esClient.index(i -> i
                    .index("orders")
                    .document(request));

            return request;

        } catch (IOException e) {
            log.error("Failed to index order to Elasticsearch", e);
            return null;
        }
    }

    /**
     * 更新订单
     */
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

            esClient.index(i -> i
                    .index("orders")
                    .id(id)
                    .document(existing));

            return existing;

        } catch (IOException e) {
            log.error("Failed to update order in Elasticsearch", e);
            return null;
        }
    }

    /**
     * 删除订单
     */
    public void deleteOrder(String id) {
        log.info("Deleting order: {}", id);

        try {
            esClient.delete(d -> d
                    .index("orders")
                    .id(id));
        } catch (IOException e) {
            log.error("Failed to delete order from Elasticsearch", e);
        }
    }

    /**
     * 获取订单数量
     */
    public long getOrderCount() {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index("orders"));
            SearchResponse<OrderResponse> response = esClient.search(request, OrderResponse.class);
            return response.hits().total().value();
        } catch (IOException e) {
            log.error("Failed to count orders", e);
            return 0;
        }
    }
}