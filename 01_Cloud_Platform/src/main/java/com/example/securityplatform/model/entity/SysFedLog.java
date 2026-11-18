package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_fed_log")
public class SysFedLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String deviceId;
    private String skillCode;
    private Integer round;
    private String baseModelVersion;
    private String weightsPath;
    private LocalDateTime createTime;
}