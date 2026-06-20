package cn.structured.datascope.example.mybatisplus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyBatis-Plus 数据权限示例启动类
 */
@SpringBootApplication
@MapperScan("cn.structured.datascope.example.mybatisplus.mapper")
public class DataScopeMyBatisPlusExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataScopeMyBatisPlusExampleApplication.class, args);
    }
}