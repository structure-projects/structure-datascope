package cn.structured.datascope.example.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Message 数据权限示例启动类
 *
 * <p>基于Spring Cloud Stream实现数据权限从生产者到消费者的透传</p>
 */
@SpringBootApplication
public class DataScopeMessageExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataScopeMessageExampleApplication.class, args);
    }
}