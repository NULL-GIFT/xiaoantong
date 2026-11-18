package com.example.securityplatform.service;

import java.util.List;

public interface MqttPublishService {
    void publish(String topic, String payload) throws Exception;

    // 🌟 新增：专门用于下发模型更新指令的方法
    void publishModelUpdateCommand(String deviceId, String version, String downloadUrl) throws Exception;
    public void publishConfirmCommand(String deviceId, String trackId) throws Exception;
    void publishSuppressionCommand(String deviceId, String trackId, List<Integer> bbox, int ttl) throws Exception;

}