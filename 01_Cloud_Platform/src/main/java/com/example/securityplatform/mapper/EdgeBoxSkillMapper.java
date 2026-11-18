package com.example.securityplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.securityplatform.model.entity.SysEdgeBoxSkill;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EdgeBoxSkillMapper extends BaseMapper<SysEdgeBoxSkill> {
    // 继承 BaseMapper，自带单表增删改查，无需手写 SQL
}