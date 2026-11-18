package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_fed_model")
public class SysFedModel {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String skillCode;
    private String version;
    private Integer round;
    private BigDecimal accuracy;
    private Integer nodes;
    private String filePath;
    private String status; // active, archived, training
    private LocalDateTime createTime;
}