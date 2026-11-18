package com.example.securityplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync   //激活异步能力
public class SecurityPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecurityPlatformApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校安通云端平台启动成功！");
        System.out.println("  MQTT监听: tcp://127.0.0.1:1883");
        System.out.println("  HTTP服务: http://localhost:8080");
        System.out.println("========================================");
    }
}