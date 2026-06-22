package cn.structured.datascope.example.cache.config;

import cn.structured.datascope.DataScopeContext;
import cn.structured.datascope.DataScopeInfo;
import cn.structured.datascope.example.cache.dto.OrderResponse;
import cn.structured.datascope.example.cache.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 测试数据初始化配置
 * <p>
 * 在测试环境初始化模拟订单数据到缓存
 * </p>
 */
@Slf4j
@Configuration
public class TestDataInitializer {

    @Bean
    public CommandLineRunner initTestData(OrderService orderService) {
        return args -> {
            // 设置管理员上下文，确保数据能够被正确写入
            DataScopeInfo adminContext = new DataScopeInfo();
            adminContext.setUserId("test-init");
            adminContext.setRoles(Arrays.asList("SYS_ADMIN"));
            adminContext.setOrgId("10");
            adminContext.setDeptIds(Arrays.asList("1"));
            DataScopeContext.set(adminContext);

            try {
                log.info("Initializing test order data for cache...");

                // 创建测试订单数据
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

                    orderService.createOrder(order);
                    log.debug("Saved test order: {}", order.getOrderNo());
                }

                log.info("Test order data initialized");
            } finally {
                DataScopeContext.remove();
            }
        };
    }
}