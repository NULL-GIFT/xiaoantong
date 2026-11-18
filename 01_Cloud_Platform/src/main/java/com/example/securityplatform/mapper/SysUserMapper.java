package com.example.securityplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.securityplatform.model.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
