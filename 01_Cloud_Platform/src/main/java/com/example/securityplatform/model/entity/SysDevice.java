package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_device")
public class SysDevice {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String deviceId;        // 设备唯一标识
    private String deviceName;      // 设备名称

    // 注意：数据库列名是 device_ip，不是 ip_address
    // 通过注解将 ipAddress 字段映射到 device_ip 列
    // 1. 摄像机本机的真实 IP
    @TableField("device_ip")
    private String deviceIp;

    // 2. 边缘盒子（算力网关）的 IP
    @TableField("ipAddress")
    private String ipAddress;

    private Integer status;         // 状态：0=离线，1=在线，2=故障
    private String firmwareVersion; // 固件版本

    // location 字段在 Service 中有用到（getOnlineVideoStreams），但表中无此列
    // 设置 exist = false，表示该字段不与数据库任何列映射
    @TableField(exist = false)
    private String location;        // 安装位置（暂不从数据库读写）

    private Date lastActiveTime;    // 最后活跃时间
    private Date createTime;        // 创建时间
    private Date updateTime;        // 更新时间
    private String channelNo;
    private String serviceMode;
    private String deviceType;
    private String protocol;
}