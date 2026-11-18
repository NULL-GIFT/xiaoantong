//package com.example.securityplatform.service.impl;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
//import com.example.securityplatform.model.dto.AlarmUploadDTO;
//import com.example.securityplatform.model.entity.SysDevice;
//import com.example.securityplatform.service.AlarmService;
//import com.example.securityplatform.service.DeviceService; // 🌟 引入设备服务
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.integration.mqtt.support.MqttHeaders;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageHandler;
//import org.springframework.messaging.MessagingException;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//public class MqttReceiver implements MessageHandler {
//
//    @Autowired
//    private AlarmService alarmService;
//
//    @Autowired
//    private DeviceSkillService deviceSkillService;
//
//    // 🌟 解决报错的关键：注入 DeviceService
//    @Autowired
//    private DeviceService deviceService;
//
//    @Override
//    public void handleMessage(Message<?> message) throws MessagingException {
//        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
//        String payload = message.getPayload().toString();
//
//        System.out.println("收到MQTT消息 - Topic: " + topic + ", Payload: " + payload);
//
//        if (topic.equals("lwk/device/heartbeat")) {
//            try {
//                JSONObject json = JSON.parseObject(payload);
//                String deviceId = json.getString("device_id");
//                String currentModelVersion = json.getString("current_model_version");
//                String skillCode = json.getString("skill_code");
//                Integer status = json.getInteger("status");
//
//                // 1. 更新 sys_device 表的在线状态和心跳时间
//                LocalDateTime now = LocalDateTime.now();
//                UpdateWrapper<SysDevice> deviceUpdateWrapper = new UpdateWrapper<>();
//                deviceUpdateWrapper.eq("device_id", deviceId)
//                        .set("status", status)
//                        .set("last_active_time", now);
//                // 现在这里不会报错了，因为 deviceService 已经注入了
//                deviceService.update(deviceUpdateWrapper);
//
//                // 2. 更新 sys_device_skill 表里的 current_version
//                if (deviceId != null && currentModelVersion != null) {
//                    deviceSkillService.updateDeviceRunningVersion(deviceId, skillCode, currentModelVersion);
//                }
//            } catch (Exception e) {
//                System.err.println("心跳消息解析失败: " + e.getMessage());
//            }
//        }
//        else if (topic.equals("lwk/device/alarm")) {
//            try {
//                // 1. 将接收到的字符串解析为外层 JSON 对象
//                JSONObject rootJson = JSON.parseObject(payload);
//
//                // 2. 提取外层数据
//                String deviceId = rootJson.getString("device_id");
//                String recordFileName = rootJson.getString("record_file_name"); // 提取文件名，为了之后退回
//
//                // 3. 提取核心嵌套的 event 对象
//                JSONObject eventJson = rootJson.getJSONObject("event");
//
//                if (eventJson != null) {
//                    // 4. 手动精准组装 DTO
//                    AlarmUploadDTO alarmDTO = new AlarmUploadDTO();
//                    alarmDTO.setDevice_id(deviceId);
//                    alarmDTO.setRecord_file_name(recordFileName); // 把文件名塞进去
//                    alarmDTO.setType(eventJson.getString("type"));
//                    alarmDTO.setImg_base64(eventJson.getString("img_base64"));
//
//                    // 提取数组
//                    com.alibaba.fastjson.JSONArray confsArray = eventJson.getJSONArray("confs");
//                    if (confsArray != null) {
//                        alarmDTO.setConfs(confsArray.toJavaList(Float.class));
//                    }
//
//                    com.alibaba.fastjson.JSONArray typesArray = eventJson.getJSONArray("types");
//                    if (typesArray != null) {
//                        alarmDTO.setTypes(typesArray.toJavaList(String.class));
//                    }
//
//                    alarmDTO.setTimestamp(System.currentTimeMillis()); // 补齐时间戳
//
//                    // 5. 送入大模型海关与报警服务
//                    alarmService.saveAlarm(alarmDTO);
//                    System.out.println("✅ 报警剥壳处理完成，设备: " + alarmDTO.getDevice_id());
//                } else {
//                    System.err.println("❌ 收到报警，但缺失核心 event 字段，丢弃！");
//                }
//            } catch (Exception e) {
//                System.err.println("❌ 报警消息剥壳解析失败: " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }
//}

package com.example.securityplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.securityplatform.model.dto.AlarmUploadDTO;
import com.example.securityplatform.model.entity.SysDevice;
import com.example.securityplatform.model.entity.SysEdgeBox;
import com.example.securityplatform.model.entity.SysEdgeBoxSkill;
import com.example.securityplatform.service.AlarmService;
import com.example.securityplatform.service.DeviceService;
import com.example.securityplatform.service.EdgeBoxService;
import com.example.securityplatform.service.EdgeBoxSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MqttReceiver implements MessageHandler {

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private DeviceService deviceService;

    // 🌟 核心修复 1：引入边缘盒子的两个新服务，替代被删除的 DeviceSkillService
    @Autowired
    private EdgeBoxService edgeBoxService;

    @Autowired
    private EdgeBoxSkillService edgeBoxSkillService;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
        String payload = message.getPayload().toString();

        System.out.println("收到MQTT消息 - Topic: " + topic + ", Payload: " + payload);

        if (topic.equals("lwk/device/heartbeat")) {
            try {
                JSONObject json = JSON.parseObject(payload);
                String deviceId = json.getString("device_id"); // 可能是摄像头的SN，也可能是盒子的IP/SN
                String currentModelVersion = json.getString("current_model_version");
                String skillCode = json.getString("skill_code");
                Integer status = json.getInteger("status");

                LocalDateTime now = LocalDateTime.now();

                // 1. 尝试更新感知通道(摄像头)的状态和心跳时间
                UpdateWrapper<SysDevice> deviceUpdateWrapper = new UpdateWrapper<>();
                deviceUpdateWrapper.eq("device_id", deviceId)
                        .set("status", status)
                        .set("last_active_time", now);
                deviceService.update(deviceUpdateWrapper);

                // 2. 🌟 尝试更新边缘算力网关(盒子)的状态
                UpdateWrapper<SysEdgeBox> boxUpdateWrapper = new UpdateWrapper<>();
                boxUpdateWrapper.eq("box_sn", deviceId).or().eq("ip_address", deviceId)
                        .set("status", status);
                edgeBoxService.update(boxUpdateWrapper);

                // 3. 🌟 核心修复 2：极简子查询更新 sys_edge_box_skill 的 current_version
                if (deviceId != null && skillCode != null && currentModelVersion != null) {
                    UpdateWrapper<SysEdgeBoxSkill> bsUpdate = new UpdateWrapper<>();
                    bsUpdate.inSql("box_id", "SELECT id FROM sys_edge_box WHERE box_sn = '" + deviceId + "' OR ip_address = '" + deviceId + "'")
                            .inSql("skill_id", "SELECT id FROM sys_skill WHERE skill_code = '" + skillCode + "'")
                            .set("current_version", currentModelVersion);
                    edgeBoxSkillService.update(bsUpdate);
                }
            } catch (Exception e) {
                System.err.println("心跳消息解析失败: " + e.getMessage());
            }
        }
        else if (topic.equals("lwk/device/alarm")) {
            try {
                JSONObject rootJson = JSON.parseObject(payload);
                String deviceId = rootJson.getString("device_id");
                String recordFileName = rootJson.getString("record_file_name");

                JSONObject eventJson = rootJson.getJSONObject("event");

                if (eventJson != null) {
                    AlarmUploadDTO alarmDTO = new AlarmUploadDTO();
                    alarmDTO.setDevice_id(deviceId);
                    alarmDTO.setRecord_file_name(recordFileName);
                    alarmDTO.setType(eventJson.getString("type"));
                    alarmDTO.setImg_base64(eventJson.getString("img_base64"));

                    com.alibaba.fastjson.JSONArray confsArray = eventJson.getJSONArray("confs");
                    if (confsArray != null) {
                        alarmDTO.setConfs(confsArray.toJavaList(Float.class));
                    }

                    com.alibaba.fastjson.JSONArray typesArray = eventJson.getJSONArray("types");
                    if (typesArray != null) {
                        alarmDTO.setTypes(typesArray.toJavaList(String.class));
                    }

                    alarmDTO.setTimestamp(System.currentTimeMillis());

                    alarmService.saveAlarm(alarmDTO);
                    System.out.println("✅ 报警剥壳处理完成，设备: " + alarmDTO.getDevice_id());
                } else {
                    System.err.println("❌ 收到报警，但缺失核心 event 字段，丢弃！");
                }
            } catch (Exception e) {
                System.err.println("❌ 报警消息剥壳解析失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}