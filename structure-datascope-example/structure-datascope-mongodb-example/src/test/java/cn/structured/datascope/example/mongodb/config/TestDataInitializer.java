package cn.structured.datascope.example.mongodb.config;

import cn.structured.datascope.example.mongodb.document.OrderDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测试数据初始化配置
 * <p>
 * 在测试环境初始化模拟订单数据到 MongoDB
 * </p>
 */
@Slf4j
@TestConfiguration
public class TestDataInitializer {

    @Bean
    public CommandLineRunner initTestData(MongoTemplate mongoTemplate) {
        return args -> {
            log.info("Initializing test order data...");

            // 清理旧的测试数据
            mongoTemplate.remove(new Query(), OrderDocument.class);

            // 创建测试订单数据
            for (int i = 1; i <= 3; i++) {
                OrderDocument order = OrderDocument.builder()
                        .orderNo("ORD-2024-00" + i)
                        .amount(new BigDecimal("9999.99"))
                        .phone("1380013800" + i)
                        .email("customer" + i + "@example.com")
                        .remark("这是内部机密备注 " + i)
                        .orgId("10")
                        .deptId(String.valueOf(i))
                        .createTime(LocalDateTime.now())
                        .createBy("admin")
                        .build();

                mongoTemplate.save(order);
                log.debug("Saved test order: {}", order.getOrderNo());
            }

            log.info("Test order data initialized successfully");
        };
    }
}
