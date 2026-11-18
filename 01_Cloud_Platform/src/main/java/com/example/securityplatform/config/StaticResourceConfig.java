package com.example.securityplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将本地 storage/alarm_photos 目录映射为 /alarm_photos URL
        registry.addResourceHandler("/alarm_photos/**")
                .addResourceLocations("file:storage/alarm_photos/");

        System.out.println("静态资源映射已配置：/alarm_photos/ -> file:storage/alarm_photos/");
    }
}
