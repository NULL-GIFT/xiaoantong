package com.example.securityplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.securityplatform.mapper.DeviceMapper;
import com.example.securityplatform.model.entity.SysDevice;
import com.example.securityplatform.model.vo.DeviceStatusVo;
import com.example.securityplatform.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceServiceImpl
        extends ServiceImpl<DeviceMapper, SysDevice>
        implements DeviceService {

    // 视频流相关常量
    private static final int DEFAULT_STREAM_PORT = 5000;
    private static final String VIDEO_FEED_PATH = "/video_feed";

    @Override
    @Transactional
    public void updateDeviceStatus(String deviceId, String deviceIp, Integer status) {
        log.info("更新设备状态: deviceId={}, ip={}, status={}", deviceId, deviceIp, status);

        LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
        query.eq(SysDevice::getDeviceId, deviceId);

        SysDevice device = this.getOne(query);

        if (device == null) {
            // 新设备，插入记录
            device = new SysDevice();
            device.setDeviceId(deviceId);
            device.setDeviceName("边缘盒子-" + deviceId);
            device.setIpAddress(deviceIp);
            device.setStatus(status != null ? status : 1);
            device.setFirmwareVersion("v1.0");
            device.setLastActiveTime(new Date());
            this.save(device);
            log.info("新增设备: {}", deviceId);
        } else {
            // 更新现有设备
            device.setIpAddress(deviceIp);
            device.setStatus(status != null ? status : 1);
            device.setLastActiveTime(new Date());
            this.updateById(device);
            log.info("更新设备状态: {}", deviceId);
        }
    }

    @Override
    public List<SysDevice> getAllDevices() {
        LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
        query.orderByDesc(SysDevice::getUpdateTime);
        return this.list(query);
    }

    @Override
    public List<SysDevice> getOnlineDevices() {
        LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
        query.eq(SysDevice::getStatus, 1);  // 状态为在线
        query.orderByDesc(SysDevice::getLastActiveTime);
        return this.list(query);
    }

    @Override
    public SysDevice getByDeviceId(String deviceId) {
        LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
        query.eq(SysDevice::getDeviceId, deviceId);
        return this.getOne(query);
    }

    @Override
    public long countOnlineDevices() {
        LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
        query.eq(SysDevice::getStatus, 1);  // 状态为在线
        return this.count(query);
    }

    @Override
    public Integer countTotalDevices() {
        // 通过 baseMapper 调用自定义的统计方法
        return this.getBaseMapper().countTotalDevices();
    }

    @Override
    public List<Map<String, Object>> getDeviceStatusDistribution() {
        // 通过 baseMapper 调用自定义的统计方法
        return this.getBaseMapper().getDeviceStatusDistribution();
    }

    @Override
    @Transactional
    public void updateLastActiveTime(String deviceId) {
        LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
        query.eq(SysDevice::getDeviceId, deviceId);

        SysDevice device = this.getOne(query);
        if (device != null) {
            device.setLastActiveTime(new Date());
            this.updateById(device);
            log.debug("更新设备最后活跃时间: {}", deviceId);
        }
    }

    // ============== 新增：视频流地址相关方法 ==============

    /**
     * 获取设备列表（包含视频流地址） - 第三阶段新增
     * @return 包含视频流地址的设备列表
     */
    @Override
    public List<DeviceStatusVo> getDeviceListWithVideoStream() {
        // 获取所有设备
        List<SysDevice> devices = this.getAllDevices();

        // 转换为VO对象并添加视频流地址
        return devices.stream()
                .map(this::convertToDeviceStatusVo)
                .collect(Collectors.toList());
    }

    /**
     * 根据设备ID获取视频流地址
     * @param deviceId 设备ID
     * @return 视频流地址，如果设备不存在或离线则返回空字符串
     */
    @Override
    public String getVideoStreamUrl(String deviceId) {
        SysDevice device = this.getByDeviceId(deviceId);
        if (device == null) {
            log.warn("设备不存在: {}", deviceId);
            return "";
        }

        return buildVideoStreamUrl(device);
    }

    /**
     * 根据设备ID获取视频流状态
     * @param deviceId 设备ID
     * @return 视频流状态信息
     */
    @Override
    public Map<String, Object> getVideoStreamStatus(String deviceId) {
        SysDevice device = this.getByDeviceId(deviceId);
        Map<String, Object> status = new java.util.HashMap<>();

        if (device == null) {
            status.put("available", false);
            status.put("reason", "设备不存在");
            return status;
        }

        // 检查设备状态
        if (device.getStatus() != 1) {
            status.put("available", false);
            status.put("reason", "设备离线");
            status.put("deviceStatus", device.getStatus());
            return status;
        }

        // 检查IP地址
        if (device.getIpAddress() == null || device.getIpAddress().trim().isEmpty()) {
            status.put("available", false);
            status.put("reason", "设备IP地址为空");
            return status;
        }

        // 验证IP地址格式
        String ipAddress = device.getIpAddress();
        boolean ipValid = isValidIpAddress(ipAddress);

        status.put("available", ipValid);
        status.put("deviceId", deviceId);
        status.put("deviceName", device.getDeviceName());
        status.put("ipAddress", ipAddress);
        status.put("videoStreamUrl", ipValid ? buildVideoStreamUrl(device) : "");
        status.put("streamPort", DEFAULT_STREAM_PORT);
        status.put("reason", ipValid ? "正常" : "IP地址格式无效");

        return status;
    }

    /**
     * 获取所有在线设备的视频流地址列表
     * @return 在线设备的视频流地址列表
     */
    @Override
    public List<Map<String, String>> getOnlineVideoStreams() {
        List<SysDevice> onlineDevices = this.getOnlineDevices();

        return onlineDevices.stream()
                .filter(device -> device.getIpAddress() != null && !device.getIpAddress().trim().isEmpty())
                .map(device -> {
                    Map<String, String> streamInfo = new java.util.HashMap<>();
                    streamInfo.put("deviceId", device.getDeviceId());
                    streamInfo.put("deviceName", device.getDeviceName());
                    streamInfo.put("ipAddress", device.getIpAddress());
                    streamInfo.put("videoStreamUrl", buildVideoStreamUrl(device));
                    streamInfo.put("location", device.getLocation() != null ? device.getLocation() : "");
                    return streamInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量更新设备视频流状态
     * @param deviceStreamStatusMap 设备ID到视频流状态的映射
     */
    @Override
    @Transactional
    public void updateDeviceStreamStatus(Map<String, Boolean> deviceStreamStatusMap) {
        for (Map.Entry<String, Boolean> entry : deviceStreamStatusMap.entrySet()) {
            String deviceId = entry.getKey();
            Boolean streamActive = entry.getValue();

            LambdaQueryWrapper<SysDevice> query = new LambdaQueryWrapper<>();
            query.eq(SysDevice::getDeviceId, deviceId);

            SysDevice device = this.getOne(query);
            if (device != null) {
                // 这里可以添加视频流状态字段，如果没有则跳过
                log.debug("设备 {} 视频流状态更新为: {}", deviceId, streamActive);
            }
        }
    }

    // ============== 私有工具方法 ==============

    /**
     * 将SysDevice实体转换为DeviceStatusVo
     */
    private DeviceStatusVo convertToDeviceStatusVo(SysDevice device) {
        DeviceStatusVo vo = new DeviceStatusVo();

        // 复制基本属性
        BeanUtils.copyProperties(device, vo);

        // 构建视频流地址
        String videoStreamUrl = buildVideoStreamUrl(device);
        vo.setVideoStreamUrl(videoStreamUrl);

        // 判断视频流是否可用
        boolean isVideoStreamActive = isVideoStreamAvailable(device);
        vo.setVideoStreamActive(isVideoStreamActive);

        // 设置视频流端口
        vo.setStreamPort(DEFAULT_STREAM_PORT);

        return vo;
    }

    /**
     * 构建视频流URL
     */
    private String buildVideoStreamUrl(SysDevice device) {
        if (device == null || device.getIpAddress() == null || device.getIpAddress().trim().isEmpty()) {
            return "";
        }

        String ipAddress = device.getIpAddress().trim();

        // 清理IP地址（移除可能的协议前缀和路径）
        if (ipAddress.startsWith("http://")) {
            ipAddress = ipAddress.substring(7);
        }
        if (ipAddress.startsWith("https://")) {
            ipAddress = ipAddress.substring(8);
        }

        // 移除端口号（如果有）
        if (ipAddress.contains(":")) {
            ipAddress = ipAddress.split(":")[0];
        }

        // 构建视频流URL
        return String.format("http://%s:%d%s", ipAddress, DEFAULT_STREAM_PORT, VIDEO_FEED_PATH);
    }

    /**
     * 判断视频流是否可用
     */
    private boolean isVideoStreamAvailable(SysDevice device) {
        if (device == null) {
            return false;
        }

        // 设备必须在线
        if (device.getStatus() != 1) {
            return false;
        }

        // 必须有IP地址
        if (device.getIpAddress() == null || device.getIpAddress().trim().isEmpty()) {
            return false;
        }

        // IP地址格式必须有效
        String ipAddress = device.getIpAddress().trim();
        return isValidIpAddress(ipAddress);
    }

    /**
     * 验证IP地址格式
     */
    private boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }

        String ip = ipAddress.trim();

        // 移除可能的协议前缀
        if (ip.startsWith("http://")) {
            ip = ip.substring(7);
        } else if (ip.startsWith("https://")) {
            ip = ip.substring(8);
        }

        // 如果是localhost或127.0.0.1，认为是有效的（开发环境）
        if ("localhost".equalsIgnoreCase(ip) || "127.0.0.1".equals(ip)) {
            return true;
        }

        // 简单的IP地址格式验证（支持IPv4）
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipPattern);
    }

    /**
     * 获取设备视频流配置信息
     * @param deviceId 设备ID
     * @return 视频流配置信息
     */
    @Override
    public Map<String, Object> getStreamConfig(String deviceId) {
        SysDevice device = this.getByDeviceId(deviceId);
        Map<String, Object> config = new java.util.HashMap<>();

        if (device == null) {
            config.put("error", "设备不存在");
            return config;
        }

        config.put("deviceId", device.getDeviceId());
        config.put("deviceName", device.getDeviceName());
        config.put("ipAddress", device.getIpAddress());
        config.put("streamPort", DEFAULT_STREAM_PORT);
        config.put("videoFeedPath", VIDEO_FEED_PATH);
        config.put("fullUrl", buildVideoStreamUrl(device));
        config.put("streamType", "MJPEG");
        config.put("frameRate", 25);  // 默认25帧
        config.put("resolution", "640x480");  // 默认分辨率

        // 状态信息
        config.put("deviceOnline", device.getStatus() == 1);
        config.put("streamAvailable", isVideoStreamAvailable(device));
        config.put("lastActiveTime", device.getLastActiveTime());

        return config;
    }
}