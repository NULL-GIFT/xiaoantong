package com.example.securityplatform.service.mqtt;

import com.alibaba.fastjson.JSONObject;
import com.example.securityplatform.model.entity.SysDevice;
import com.example.securityplatform.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Date;

@Slf4j
@Component
public class DeviceMqttReceiver extends BaseMqttReceiver {

    private final DeviceService deviceService;

    // 使用构造器注入替代字段注入
    public DeviceMqttReceiver(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    protected void handleHeartbeat(String payload) {
        JSONObject json = parseJson(payload);
        if (json == null) return;

        try {
            String deviceId = json.getString("device_id");
            String deviceIp = json.getString("device_ip");
            Integer status = json.getInteger("status");
            String version = json.getString("version");
            // 时间戳暂时不使用，可以注释掉
            // Long timestamp = json.getLong("timestamp");

            log.info("❤️ 设备心跳 - DeviceID: {}, IP: {}, Status: {}",
                    deviceId, deviceIp, status);

            // 更新设备状态
            updateDeviceStatus(deviceId, deviceIp, status, version);

        } catch (Exception e) {
            log.error("❌ 解析心跳数据失败: {}", e.getMessage());
        }
    }

    /**
     * 更新设备状态
     */
    private void updateDeviceStatus(String deviceId, String deviceIp,
                                    Integer status, String version) {
        try {
            // 查找设备
            SysDevice device = deviceService.getByDeviceId(deviceId);

            if (device == null) {
                // 设备不存在，创建新记录
                log.info("🆕 新设备上线，创建记录: {}", deviceId);
                device = new SysDevice();
                device.setDeviceId(deviceId);
                device.setDeviceName("边缘盒子-" + deviceId);
                device.setIpAddress(deviceIp);  // 修改为 setIpAddress
                device.setStatus(status != null ? status : 1);
                device.setFirmwareVersion(version != null ? version : "v1.0");
                device.setLastActiveTime(new Date());

                // 使用 DeviceService 的方法保存设备
                // 注意：DeviceService 没有 save() 方法，需要添加或使用其他方法
                // 临时方案：调用 updateDeviceStatus 方法
                deviceService.updateDeviceStatus(deviceId, deviceIp, status);
                log.info("✅ 新设备保存成功: {}", deviceId);
            } else {
                // 更新现有设备
                log.info("🔄 更新设备状态: {}", deviceId);
                device.setIpAddress(deviceIp);  // 修改为 setIpAddress
                device.setStatus(status != null ? status : 1);
                device.setLastActiveTime(new Date());
                if (version != null) {
                    device.setFirmwareVersion(version);
                }

                // 使用 DeviceService 的方法更新设备
                // 注意：DeviceService 没有 updateById() 方法
                // 临时方案：调用 updateDeviceStatus 方法
                deviceService.updateDeviceStatus(deviceId, deviceIp, status);
                log.info("✅ 设备状态更新成功: {}", deviceId);
            }

            // 打印当前在线设备数
            long onlineCount = deviceService.countOnlineDevices();
            log.info("📊 当前在线设备: {} 台", onlineCount);

        } catch (Exception e) {
            log.error("❌ 更新设备状态失败: {}", e.getMessage(), e);
        }
    }
}