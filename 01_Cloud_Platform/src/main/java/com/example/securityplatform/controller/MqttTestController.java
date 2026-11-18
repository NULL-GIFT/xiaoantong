package com.example.securityplatform.controller;

import com.example.securityplatform.common.Result;
import com.example.securityplatform.model.entity.SysDevice;
import com.example.securityplatform.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/mqtt-test")
public class MqttTestController {

    @Value("${spring.mqtt.server-uri}")
    private String mqttServerUri;

    @Autowired
    private DeviceService deviceService;

    /**
     * 测试MQTT服务器连接
     */
    @GetMapping("/connect")
    public Result<String> testConnection() {
        try {
            String clientId = "test-client-" + UUID.randomUUID().toString().substring(0, 8);
            MqttClient client = new MqttClient(mqttServerUri, clientId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(true);

            client.connect(options);
            boolean isConnected = client.isConnected();
            client.disconnect();

            if (isConnected) {
                return Result.success("✅ MQTT服务器连接成功: " + mqttServerUri);
            } else {
                return Result.error("❌ MQTT服务器连接失败");
            }
        } catch (Exception e) {
            return Result.error("MQTT连接异常: " + e.getMessage());
        }
    }

    /**
     * 模拟发送设备心跳消息
     */
    @PostMapping("/simulate-heartbeat")
    public Result<String> simulateHeartbeat(@RequestParam(defaultValue = "box_001") String deviceId,
                                            @RequestParam(defaultValue = "192.168.1.101") String deviceIp) {
        try {
            String clientId = "simulator-" + UUID.randomUUID().toString().substring(0, 8);
            MqttClient client = new MqttClient(mqttServerUri, clientId);
            client.connect();

            // 构造心跳消息
            String payload = String.format(
                    "{\"device_id\":\"%s\",\"device_ip\":\"%s\",\"status\":1,\"version\":\"v1.0\",\"timestamp\":%d}",
                    deviceId, deviceIp, System.currentTimeMillis()
            );

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            message.setRetained(false);

            client.publish("device/heartbeat", message);
            client.disconnect();

            log.info("模拟心跳发送成功: deviceId={}, ip={}", deviceId, deviceIp);
            return Result.success("✅ 模拟心跳发送成功: " + deviceId);
        } catch (Exception e) {
            return Result.error("模拟心跳失败: " + e.getMessage());
        }
    }

    /**
     * 查看当前设备状态
     */
    @GetMapping("/device-status")
    public Result<List<SysDevice>> getDeviceStatus() {
        List<SysDevice> devices = deviceService.getAllDevices();
        return Result.success(devices);
    }

    /**
     * 查看在线设备
     */
    @GetMapping("/online-devices")
    public Result<List<SysDevice>> getOnlineDevices() {
        List<SysDevice> devices = deviceService.getOnlineDevices();
        return Result.success(devices);
    }

    /**
     * 获取在线设备数量
     */
    @GetMapping("/online-count")
    public Result<Long> getOnlineCount() {
        long count = deviceService.countOnlineDevices();
        return Result.success(count);
    }
}