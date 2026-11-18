package com.example.securityplatform.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.securityplatform.model.dto.AlarmUploadDTO;
import com.example.securityplatform.service.DeviceService;
import com.example.securityplatform.service.impl.SimpleAlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Slf4j
@Configuration
public class MqttChannelConfig {

    private final MqttPahoClientFactory mqttClientFactory;
    private final String clientId;

    // 注入报警服务
    @Autowired
    private com.example.securityplatform.service.AlarmService alarmService;

    // 注入设备服务，用于处理心跳
    @Autowired
    private DeviceService deviceService;

    /**
     * 使用构造函数注入（推荐）
     */
    @Autowired
    public MqttChannelConfig(
            MqttPahoClientFactory mqttClientFactory,
            @Value("${spring.mqtt.client-id:cloud-server}") String clientId) {
        this.mqttClientFactory = mqttClientFactory;
        this.clientId = clientId;
        log.info("MQTT Channel Config 初始化，客户端ID: {}", clientId);
    }

    /**
     * 创建MQTT输入通道
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT消息生产者（订阅者）
     */
    @Bean
    public MessageProducer inbound() {
        // 订阅通配符主题，兼容有/无前缀的两套规范
        String[] topics = {"device/#", "lwk/device/#"};
        int[] qos = {1, 1};

        String inboundClientId = clientId + "-inbound";

        log.info("创建MQTT订阅适配器，客户端ID: {}, 订阅主题: {}", inboundClientId, String.join(", ", topics));

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        inboundClientId,
                        mqttClientFactory,
                        topics);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInputChannel());

        log.info("MQTT订阅适配器创建成功");
        return adapter;
    }

    /**
     * 绑定消息处理器
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageHandler() {
        // 这里可以直接返回一个匿名内部类，避免依赖 DeviceMqttReceiver
        return message -> {
            try {
                // 获取消息内容
                String payload = message.getPayload().toString();
                String topic = (String) message.getHeaders().get("mqtt_receivedTopic");

                log.info("📨 收到MQTT消息:");
                log.info("  主题: {}", topic);
                log.info("  内容: {}", payload);

                // 根据主题处理不同的消息
                if (topic != null) {
                    if (topic.contains("heartbeat")) {
                        // 处理心跳消息
                        log.info("处理设备心跳: {}", payload);
                        try {
                            JSONObject json = JSON.parseObject(payload);
                            String deviceId = json.getString("device_id");
                            String deviceIp = json.getString("device_ip");
                            Integer status = json.getInteger("status");
                            deviceService.updateDeviceStatus(deviceId, deviceIp, status != null ? status : 1);
                            deviceService.updateLastActiveTime(deviceId);
                            log.info("✅ 心跳处理完成: {}", deviceId);
                        } catch (Exception e) {
                            log.error("❌ 心跳处理失败: {}", e.getMessage(), e);
                        }
                    }else if (topic.contains("alarm")) {
                        log.info("⚡ 收到设备报警，准备转交 VLM 前台接单...");
                        try {
                            // 🌟 核心修复：将 JSON 转换为 DTO，并交给 AlarmServiceImpl
                            AlarmUploadDTO dto = JSON.parseObject(payload, AlarmUploadDTO.class);
                            alarmService.saveAlarm(dto);
                            log.info("✅ 报警包裹已成功移交，前台处理完毕");
                        } catch (Exception e) {
                            log.error("❌ 报警处理交接失败: {}", e.getMessage(), e);
                        }

                    } else if (topic.contains("register")) {
                        // 处理注册消息
                        log.info("处理设备注册: {}", payload);
                        // TODO: 可以添加设备注册逻辑
                    }
                }

            } catch (Exception e) {
                log.error("处理MQTT消息时发生错误", e);
            }
        };
    }
}
