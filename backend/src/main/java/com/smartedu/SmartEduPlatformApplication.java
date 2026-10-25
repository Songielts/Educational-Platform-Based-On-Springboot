package com.smartedu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 智慧教育平台启动
 */
@SpringBootApplication
@EnableCaching
@MapperScan("com.smartedu.modules.*.mapper")
public class  SmartEduPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEduPlatformApplication.class, args);
    }
}
