package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
/**
 * 报警事件实体类
 * 对应数据库表：sys_alarm
 */
@Data
@TableName("sys_alarm") // 🌟 规范写法：显式指定对应的数据库表名
public class SysAlarm {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO) // 🌟 规范写法：指定主键且为自增
    private Long id;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 技能ID（暂时不用，预留）
     */
    @TableField(exist = false) // 🌟🌟🌟 核心救命代码：告诉 MyBatis-Plus，数据库表里没有这个字段，别去查它！
    private Long skillId;

    /**
     * 报警类型：fire-火灾，smoke-烟雾，fall-跌倒
     */
    private String alarmType;

    /**
     * 报警发生时间
     */
    private LocalDateTime alarmTime;

    /**
     * 报警图片路径
     */
    private String imgPath;

    /**
     * 置信度（0-1之间）
     */
    private Float confidence;

    /**
     * 处理状态：0-未处理，1-已确认，2-误报
     */
    private Integer isHandled;

    /**
     * 报警位置
     */
    private String location;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    private Integer isFalseAlarm;
    private String falseReason;
    @TableField("handled_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handledTime;
}