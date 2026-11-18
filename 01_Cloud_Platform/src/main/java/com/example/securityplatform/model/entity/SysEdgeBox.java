package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_edge_box")
public class SysEdgeBox {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String boxSn;       // 盒子SN码
    private String boxName;     // 盒子名称
    private String ipAddress;   // 盒子局域网IP (用于和 sys_device 表关联)
    private Integer status;     // 状态：0=离线，1=在线
    private Date createTime;    // 注册时间
}