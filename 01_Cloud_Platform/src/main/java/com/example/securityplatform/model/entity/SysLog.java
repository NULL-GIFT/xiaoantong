package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String logType;   // 日志分类：account 或 business
    private String operator;  // 操作人账号
    private String module;    // 业务模块
    private String action;    // 具体动作
    private String method;    // 后台方法
    private String params;    // 请求传参
    private Integer status;   // 状态：1成功，0失败
    private String errorMsg;  // 失败信息

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;  // 操作时间
}