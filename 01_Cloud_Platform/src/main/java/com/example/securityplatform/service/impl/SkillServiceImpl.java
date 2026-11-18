package com.example.securityplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.securityplatform.mapper.SysSkillMapper;
import com.example.securityplatform.model.entity.SysSkill;
import com.example.securityplatform.service.SkillService;
import org.springframework.stereotype.Service;

@Service
public class SkillServiceImpl extends ServiceImpl<SysSkillMapper, SysSkill> implements SkillService {
}
