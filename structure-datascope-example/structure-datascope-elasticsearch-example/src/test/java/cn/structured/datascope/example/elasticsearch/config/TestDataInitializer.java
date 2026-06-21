package cn.structured.datascope.example.elasticsearch.config;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.elasticsearch.dto.OrderResponse;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试数据初始化配置
 * <p>
 * 在测试环境初始化模拟订单数据到 Elasticsearch
 * </p>
 */
@Slf4j
@Configuration
public class TestDataInitializer {

    private static final String INDEX = "orders";

    @Bean
    public CommandLineRunner initTestData(ElasticsearchClient esClient) {
        return args -> {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 设置管理员上下文，确保数据能够被正确写入
            DataScopeInfo adminContext = new DataScopeInfo();
            adminContext.setUserId("test-init");
            adminContext.setRoles(Arrays.asList("SYS_ADMIN"));
            adminContext.setOrgId("10");
            adminContext.setDeptIds(Arrays.asList("1"));
            DataScopeContext.set(adminContext);

            try {
                log.info("Initializing test order data to Elasticsearch...");

                // 创建索引（如果不存在）
                createIndexIfNotExists(esClient);

                // 准备测试数据
                Map<String, OrderResponse> testOrders = new HashMap<>();
                for (int i = 1; i <= 4; i++) {
                    OrderResponse order = new OrderResponse();
                    order.setId(String.valueOf(i));
                    order.setOrderNo("ORD2024000" + i);
                    order.setAmount(new BigDecimal("1000.00").add(new BigDecimal("1000").multiply(new BigDecimal(i - 1))));
                    order.setPhone("138****" + String.format("%04d", i * 1234));
                    order.setEmail("user" + i + "@test.com");
                    order.setRemark(i == 1 ? "正常订单" : (i == 2 ? "加急订单" : (i == 3 ? "测试订单" : "VIP订单")));
                    order.setOrgId(10L);
                    order.setDeptId(i <= 2 ? 1L : (i == 3 ? 2L : 6L));
                    order.setCreateTime(LocalDateTime.now());
                    order.setCreateBy("system");
                    testOrders.put(order.getId(), order);
                }

                // 批量写入数据
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
                for (Map.Entry<String, OrderResponse> entry : testOrders.entrySet()) {
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index(INDEX)
                                    .id(entry.getKey())
                                    .document(entry.getValue())
                            )
                    );
                }

                BulkResponse bulkResponse = esClient.bulk(bulkBuilder.build());

                if (bulkResponse.errors()) {
                    for (BulkResponseItem item : bulkResponse.items()) {
                        if (item.error() != null) {
                            log.error("Failed to index document {}: {}", item.id(), item.error().reason());
                        }
                    }
                } else {
                    log.info("Successfully indexed {} test orders", testOrders.size());
                }

                // 刷新索引使数据立即可查
                esClient.indices().refresh(r -> r.index(INDEX));

                log.info("Test order data initialized successfully");

            } catch (Exception e) {
                log.error("Failed to initialize test data", e);
            } finally {
                DataScopeContext.remove();
            }
        };
    }

    private void createIndexIfNotExists(ElasticsearchClient client) throws Exception {
        boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
        if (!exists) {
            log.info("Creating index: {}", INDEX);
            client.indices().create(c -> c
                    .index(INDEX)
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("orderNo", p -> p.keyword(k -> k))
                            .properties("amount", p -> p.double_(d -> d))
                            .properties("phone", p -> p.keyword(k -> k))
                            .properties("email", p -> p.keyword(k -> k))
                            .properties("remark", p -> p.text(t -> t))
                            .properties("orgId", p -> p.long_(l -> l))
                            .properties("deptId", p -> p.long_(l -> l))
                            .properties("createTime", p -> p.date(d -> d))
                            .properties("createBy", p -> p.keyword(k -> k))
                            .properties("updateTime", p -> p.date(d -> d))
                            .properties("updateBy", p -> p.keyword(k -> k))
                    )
            );
            log.info("Index {} created successfully", INDEX);
        } else {
            log.info("Index {} already exists", INDEX);
        }
    }
}