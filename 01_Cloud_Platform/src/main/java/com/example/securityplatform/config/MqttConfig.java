package com.example.securityplatform.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

@Slf4j
@Configuration
public class MqttConfig {

    @Value("${spring.mqtt.server-uri}")
    private String serverUri;

    @Value("${spring.mqtt.username:}")
    private String username;

    @Value("${spring.mqtt.password:}")
    private String password;

    @Value("${spring.mqtt.client-id:cloud-server}")
    private String clientId;

    /**
     * MQTT客户端工厂
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        log.info("初始化MQTT客户端工厂，服务器地址: {}", serverUri);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // 配置连接参数
        options.setServerURIs(new String[]{serverUri});

        // 如果有用户名密码就设置
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password != null ? password.toCharArray() : new char[0]);
        }

        // 其他配置
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        options.setMaxInflight(1000);

        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * 打印MQTT配置信息（用于调试）
     */
    @Bean
    public String mqttConfigInfo() {
        log.info("📡 MQTT配置信息:");
        log.info("  服务器地址: {}", serverUri);
        log.info("  客户端ID: {}", clientId);
        log.info("  用户名: {}", username.isEmpty() ? "无" : username);
        log.info("  MQTT服务准备就绪");
        return "MQTT Config Ready";
    }
}
