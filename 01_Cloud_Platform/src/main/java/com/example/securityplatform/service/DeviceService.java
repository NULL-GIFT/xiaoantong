package com.example.securityplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.securityplatform.model.entity.SysDevice;
import com.example.securityplatform.model.vo.DeviceStatusVo;

import java.util.List;
import java.util.Map;

// 继承 IService 以获得 save() 和 updateById() 方法
public interface DeviceService extends IService<SysDevice> {

    // 原有自定义方法
    void updateDeviceStatus(String deviceId, String deviceIp, Integer status);
    List<SysDevice> getAllDevices();
    List<SysDevice> getOnlineDevices();
    SysDevice getByDeviceId(String deviceId);
    long countOnlineDevices();
    Integer countTotalDevices();
    List<Map<String, Object>> getDeviceStatusDistribution();
    void updateLastActiveTime(String deviceId);

    // ============== 新增：视频流地址相关方法 ==============

    /**
     * 获取设备列表（包含视频流地址） - 第三阶段新增
     */
    List<DeviceStatusVo> getDeviceListWithVideoStream();

    /**
     * 根据设备ID获取视频流地址
     */
    String getVideoStreamUrl(String deviceId);

    /**
     * 根据设备ID获取视频流状态
     */
    Map<String, Object> getVideoStreamStatus(String deviceId);

    /**
     * 获取所有在线设备的视频流地址列表
     */
    List<Map<String, String>> getOnlineVideoStreams();

    /**
     * 批量更新设备视频流状态
     */
    void updateDeviceStreamStatus(Map<String, Boolean> deviceStreamStatusMap);

    /**
     * 获取设备视频流配置信息
     */
    Map<String, Object> getStreamConfig(String deviceId);
}