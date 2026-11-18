package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_skill")
public class SysSkill {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String skillName;
    private String skillCode;
    private String version;
    private String modelFilePath;

    // 🌟 核心：补齐这4个真实业务字段
    private String description;
    private Integer validDays;
    private Integer configStatus;
    private Integer activationStatus;

    private Date createTime;
    private Date updateTime;
}