package com.example.securityplatform.model.vo;

import lombok.Data;
import java.util.Date;

@Data
public class DeviceStatusVo {
    private Integer id;
    private String deviceId;           // 设备唯一编号 box_001
    private String deviceName;         // 设备名称
    private String ipAddress;          // 设备IP
    private Integer status;            // 状态：0=离线，1=在线，2=故障
    private Date lastActiveTime;       // 最后活跃时间
    private String firmwareVersion;    // 固件版本
    private String location;           // 安装位置

    // 新增：视频流相关
    private String videoStreamUrl;     // 视频流地址
    private Boolean videoStreamActive; // 视频流是否可用
    private Integer streamPort;        // 视频流端口（默认5000）

    // 注意：删除了 activeSkills 字段，因为第三阶段用不到
    // 如果需要，可以在后续阶段添加
}