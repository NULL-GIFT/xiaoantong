package com.example.securityplatform.controller;

import com.example.securityplatform.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Value("${spring.mqtt.server-uri:未配置}")
    private String mqttServerUri;

    @GetMapping("/health")
    public Result<String> healthCheck() {
        log.info("健康检查接口被调用");
        return Result.success("Cloud Platform is running! MQTT: " + mqttServerUri);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "running");
        status.put("port", 8080);
        status.put("mqtt", mqttServerUri);
        status.put("timestamp", System.currentTimeMillis());
        status.put("stage", "第二阶段 - MQTT通信");
        return Result.success(status);
    }
}