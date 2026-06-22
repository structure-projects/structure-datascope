package cn.structured.datascope.example.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Cache 数据权限示例启动类
 * <p>
 * 基于 Spring Cache 抽象实现，支持多种缓存技术
 * </p>
 */
@SpringBootApplication
@EnableCaching
public class DataScopeCacheExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataScopeCacheExampleApplication.class, args);
    }
}