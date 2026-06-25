package cn.structured.datascope.example.mongodb.config;

import cn.structured.datascope.example.mongodb.document.OrderDocument;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;

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
public class MockMongoConfiguration {

    private final Map<String, OrderDocument> orderStore = new HashMap<>();

    private final MongoMappingContext mappingContext = new MongoMappingContext();
    private final MongoConverter converter;
    private final MongoTemplate template;

    public MockMongoConfiguration() {
        initMockData();
        this.converter = createMongoConverter();
        this.template = createMongoTemplate();
    }

    private void initMockData() {
        orderStore.put("1", createOrder("1", "ORD-2024-001", "10", "1", new BigDecimal("9999.99"), "13800138001", "customer1@example.com", "这是内部机密备注 1"));
        orderStore.put("2", createOrder("2", "ORD-2024-002", "10", "2", new BigDecimal("9999.99"), "13800138002", "customer2@example.com", "这是内部机密备注 2"));
        orderStore.put("3", createOrder("3", "ORD-2024-003", "10", "3", new BigDecimal("9999.99"), "13800138003", "customer3@example.com", "这是内部机密备注 3"));
    }

    private OrderDocument createOrder(String id, String orderNo, String orgId, String deptId, BigDecimal amount, String phone, String email, String remark) {
        return OrderDocument.builder()
                .id(id)
                .orderNo(orderNo)
                .amount(amount)
                .phone(phone)
                .email(email)
                .remark(remark)
                .orgId(orgId)
                .deptId(deptId)
                .createTime(LocalDateTime.now())
                .createBy("admin")
                .build();
    }

    @SuppressWarnings("unchecked")
    private MongoConverter createMongoConverter() {
        MappingMongoConverter converter = mock(MappingMongoConverter.class);
        when(converter.getMappingContext()).thenReturn((org.springframework.data.mapping.context.MappingContext) mappingContext);
        return converter;
    }

    private MongoTemplate createMongoTemplate() {
        MongoTemplate template = mock(MongoTemplate.class);

        when(template.find(any(Query.class), any(Class.class)))
                .thenAnswer(invocation -> new ArrayList<>(orderStore.values()));

        when(template.findById(anyString(), any(Class.class)))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(0);
                    return orderStore.getOrDefault(id, orderStore.values().iterator().next());
                });

        when(template.save(any(OrderDocument.class)))
                .thenAnswer(invocation -> {
                    OrderDocument doc = invocation.getArgument(0);
                    if (doc.getId() == null || doc.getId().isEmpty()) {
                        doc.setId(UUID.randomUUID().toString());
                    }
                    orderStore.put(doc.getId(), doc);
                    return doc;
                });

        when(template.remove(any(OrderDocument.class)))
                .thenAnswer(invocation -> {
                    OrderDocument doc = invocation.getArgument(0);
                    if (doc != null && doc.getId() != null) {
                        orderStore.remove(doc.getId());
                    }
                    return null;
                });

        when(template.remove(any(Query.class), any(Class.class)))
                .thenAnswer(invocation -> {
                    orderStore.clear();
                    return null;
                });

        when(template.count(any(Query.class), any(Class.class)))
                .thenReturn((long) orderStore.size());

        when(template.getConverter()).thenReturn(converter);

        return template;
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return mock(MongoDatabaseFactory.class);
    }

    @Bean
    @Primary
    public MongoMappingContext mongoMappingContext() {
        return mappingContext;
    }

    @Bean
    @Primary
    public MongoConverter mongoConverter() {
        return converter;
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate() {
        return template;
    }
}