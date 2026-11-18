package com.example.securityplatform.service.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@Slf4j
public abstract class BaseMqttReceiver implements MessageHandler {

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = getTopicFromMessage(message);
        String payload = message.getPayload().toString();

        log.info("📨 收到MQTT消息 - Topic: {}, Payload: {}", topic, payload);

        try {
            // 根据Topic分发处理
            if (topic.startsWith("device/heartbeat")) {
                handleHeartbeat(payload);
            } else if (topic.startsWith("device/alarm")) {
                handleAlarm(payload);
            } else if (topic.startsWith("device/register")) {
                handleRegister(payload);
            } else {
                log.warn("⚠️ 未知的Topic，未处理: {}", topic);
            }
        } catch (Exception e) {
            log.error("❌ 处理MQTT消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从消息中提取Topic
     */
    private String getTopicFromMessage(Message<?> message) {
        Object topicObj = message.getHeaders().get("mqtt_receivedTopic");
        return topicObj != null ? topicObj.toString() : "unknown";
    }

    /**
     * 处理设备心跳
     */
    protected abstract void handleHeartbeat(String payload);

    /**
     * 处理设备报警（第二阶段实现）
     */
    protected void handleAlarm(String payload) {
        log.info("收到报警消息（第二阶段处理）: {}", payload);
    }

    /**
     * 处理设备注册
     */
    protected void handleRegister(String payload) {
        log.info("收到设备注册消息: {}", payload);
    }

    /**
     * 解析JSON数据
     */
    protected JSONObject parseJson(String payload) {
        try {
            return JSON.parseObject(payload);
        } catch (Exception e) {
            log.error("JSON解析失败: {}", payload);
            return null;
        }
    }
}