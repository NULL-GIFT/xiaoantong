package com.example.securityplatform.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.example.securityplatform.service.MqttPublishService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List; // 🌟 修复：导入 List 解决编译报错
import java.util.UUID;

@Service
public class MqttPublishServiceImpl implements MqttPublishService {

    @Value("${spring.mqtt.server-uri}")
    private String mqttServerUri;

    @Override
    public void publish(String topic, String payload) throws Exception {
        String clientId = "publisher-" + UUID.randomUUID().toString().substring(0, 8);
        MqttClient client = new MqttClient(mqttServerUri, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(5);
        options.setKeepAliveInterval(60);
        client.connect(options);
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        message.setRetained(false);
        client.publish(topic, message);
        client.disconnect();
    }

    @Override
    public void publishModelUpdateCommand(String deviceId, String version, String downloadUrl) throws Exception {
        JSONObject command = new JSONObject();
        command.put("download_url", downloadUrl);
        command.put("target_version", version);

        String topic = "lwk/device/" + deviceId + "/model_update";

        this.publish(topic, command.toJSONString());
        System.out.println("成功下发模型更新指令 -> Topic: " + topic + ", Payload: " + command.toJSONString());
    }

    // 🌟 核心：通知边缘端“闭嘴”的反向抑制指令
    public void publishSuppressionCommand(String deviceId, String trackId, List<Integer> bbox, int ttl) throws Exception {
        JSONObject command = new JSONObject();
        command.put("action", "ignore");
        command.put("track_id", trackId);
        command.put("bbox", bbox);
        command.put("ttl", ttl); // 存活时间，例如 300 秒

        String topic = "lwk/device/suppress";

        this.publish(topic, command.toJSONString());
        System.out.println("🛑 [云边协同] 成功下发盲区抑制指令给边缘端 -> 坐标: " + bbox.toString());
    }

    // 发送确认真火的指令
    public void publishConfirmCommand(String deviceId, String trackId) throws Exception {
        JSONObject command = new JSONObject();
        command.put("action", "confirm");
        command.put("track_id", trackId);

        String topic = "lwk/device/confirm";
        this.publish(topic, command.toJSONString());
        System.out.println("成功下发真火确认指令 -> TrackID: " + trackId);
    }

}