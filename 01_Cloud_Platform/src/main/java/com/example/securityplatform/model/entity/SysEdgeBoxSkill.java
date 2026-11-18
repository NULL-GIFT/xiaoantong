package com.example.securityplatform.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_edge_box_skill")
public class SysEdgeBoxSkill {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer boxId;       // 边缘盒子主键ID (关联 sys_edge_box)

    private Integer skillId;     // 技能ID (关联 sys_skill)

    private Integer status;      // 运行状态：0-停用, 1-启用

    private Double threshold;    // 报警置信度阈值

    private Date createTime;     // 下发时间

    private Date updateTime;     // 更新时间
}