package com.example.securityplatform.service;

import com.example.securityplatform.model.vo.DeviceStatusVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DeviceServiceTest {

    @Autowired
    private DeviceService deviceService;

    @Test
    void testGetDeviceListWithVideoUrl() {
        // 1. 获取设备列表 - 使用正确的方法名
        List<DeviceStatusVo> devices = deviceService.getDeviceListWithVideoStream();
        assertNotNull(devices, "设备列表不应为空");

        // 2. 验证在线设备有视频流地址
        for (DeviceStatusVo device : devices) {
            System.out.println("设备: " + device.getDeviceId() +
                    ", 状态: " + device.getStatus() +
                    ", 视频流地址: " + device.getVideoStreamUrl());

            if (device.getStatus() == 1 && device.getIpAddress() != null) {
                // 在线设备应有视频流地址
                assertNotNull(device.getVideoStreamUrl(),
                        "在线设备应有视频流地址");
                assertTrue(device.getVideoStreamUrl().contains(device.getIpAddress()),
                        "视频流地址应包含设备IP");
                assertTrue(device.getVideoStreamUrl().endsWith("/video_feed"),
                        "视频流地址应以/video_feed结尾");
            }
        }
    }
}