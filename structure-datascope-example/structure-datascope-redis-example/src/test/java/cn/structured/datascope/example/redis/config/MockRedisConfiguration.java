package cn.structured.datascope.example.redis.config;

import cn.structured.datascope.example.redis.dto.OrderResponse;
import cn.structured.datascope.redis.template.DataScopeRedisTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class MockRedisConfiguration {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Map<String, String> mockData = new HashMap<>();

    public MockRedisConfiguration() {
        initMockData();
    }

    private void initMockData() {
        try {
            OrderResponse order1 = new OrderResponse(1L, "ORD2024001", new BigDecimal("100.00"), "13800138001", "test@example.com", "测试订单1", 10L, 1L, LocalDateTime.now(), "admin", null, null);
            OrderResponse order2 = new OrderResponse(2L, "ORD2024002", new BigDecimal("200.00"), "13900139002", "test2@example.com", "测试订单2", 10L, 2L, LocalDateTime.now(), "admin", null, null);
            OrderResponse order3 = new OrderResponse(3L, "ORD2024003", new BigDecimal("300.00"), "13700137003", "test3@example.com", "测试订单3", 10L, 1L, LocalDateTime.now(), "admin", null, null);

            mockData.put("order:orgId:10:deptId:1:1", objectMapper.writeValueAsString(order1));
            mockData.put("order:orgId:10:deptId:2:2", objectMapper.writeValueAsString(order2));
            mockData.put("order:orgId:10:deptId:1:3", objectMapper.writeValueAsString(order3));
            mockData.put("order:orgId:10:deptId:1:1", objectMapper.writeValueAsString(order1));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisKeyValueAdapter redisKeyValueAdapter() {
        return mock(RedisKeyValueAdapter.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public DataScopeRedisTemplate dataScopeRedisTemplate() {
        return new DataScopeRedisTemplate(null, null) {
            @Override
            public Set<String> keys(String resource) {
                Set<String> result = new HashSet<>();
                String prefix = resource + ":";
                for (String key : mockData.keySet()) {
                    if (key.startsWith(prefix)) {
                        result.add(key);
                    }
                }
                return result;
            }

            @Override
            public String get(String resource, String key) {
                String fullKey = resource + ":orgId:10:deptId:1:" + key;
                if (mockData.containsKey(fullKey)) {
                    return mockData.get(fullKey);
                }
                fullKey = resource + ":orgId:10:deptId:2:" + key;
                return mockData.getOrDefault(fullKey, null);
            }

            @Override
            public Long count(String resource) {
                return (long) keys(resource).size();
            }

            @Override
            public void set(String resource, String key, String value) {
                String fullKey = resource + ":orgId:10:deptId:1:" + key;
                mockData.put(fullKey, value);
            }
        };
    }

    @Bean
    @Primary
    public CommandLineRunner initTestData() {
        return args -> {
        };
    }
}
