package cn.structured.datascope.example.redis.config;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.redis.dto.OrderResponse;
import cn.structured.datascope.redis.template.DataScopeRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

/**
 * 测试数据初始化配置
 * <p>
 * 在测试环境初始化模拟订单数据到 Redis
 * </p>
 */
@Slf4j
@Configuration
public class TestDataInitializer {

    private static final String RESOURCE = "order";

    @Bean
    public CommandLineRunner initTestData(DataScopeRedisTemplate redisTemplate, StringRedisTemplate stringRedisTemplate) {
        return args -> {
            // 创建配置好的 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // 设置管理员上下文，确保数据能够被正确写入
            DataScopeInfo adminContext = new DataScopeInfo();
            adminContext.setUserId("test-init");
            adminContext.setRoles(Arrays.asList("SYS_ADMIN"));
            adminContext.setOrgId("10");
            // 只设置一个部门ID
            adminContext.setDeptIds(Arrays.asList("1"));
            DataScopeContext.set(adminContext);

            try {
                log.info("Initializing test order data...");

                // 清理旧的测试数据
                Set<String> existingKeys = redisTemplate.keys(RESOURCE);
                if (existingKeys != null && !existingKeys.isEmpty()) {
                    log.info("Cleaning up {} existing keys", existingKeys.size());
                    existingKeys.forEach(stringRedisTemplate::delete);
                }

                for (int i = 1; i <= 3; i++) {
                    OrderResponse order = new OrderResponse();
                    order.setId((long) i);
                    order.setOrderNo("ORD-2024-00" + i);
                    order.setAmount(new BigDecimal("9999.99"));
                    order.setPhone("1380013800" + i);
                    order.setEmail("customer" + i + "@example.com");
                    order.setRemark("这是内部机密备注 " + i);
                    order.setOrgId(10L);
                    order.setDeptId(1L);
                    order.setCreateTime(LocalDateTime.now());
                    order.setCreateBy("admin");

                    try {
                        String json = objectMapper.writeValueAsString(order);
                        // businessKey 只需要 id，不需要包含 "order:" 前缀
                        // DataScopeRedisTemplate.buildKey() 会自动添加资源前缀和权限前缀
                        String businessKey = order.getId().toString();
                        redisTemplate.set(RESOURCE, businessKey, json);
                        log.debug("Saved test order with business key: {}", businessKey);
                    } catch (Exception e) {
                        log.error("Failed to save test order", e);
                    }
                }

                log.info("Test order data initialized");
            } finally {
                DataScopeContext.remove();
            }
        };
    }
}
