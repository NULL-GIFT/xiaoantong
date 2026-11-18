package com.example.securityplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.securityplatform.model.entity.SysFedModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysFedModelMapper extends BaseMapper<SysFedModel> {
    // 留空即可，BaseMapper 已经包揽了所有 CRUD 单表操作
}