package com.example.securityplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.securityplatform.mapper.EdgeBoxSkillMapper;
import com.example.securityplatform.model.entity.SysEdgeBoxSkill;
import com.example.securityplatform.service.EdgeBoxSkillService;
import org.springframework.stereotype.Service;

@Service
public class EdgeBoxSkillServiceImpl extends ServiceImpl<EdgeBoxSkillMapper, SysEdgeBoxSkill> implements EdgeBoxSkillService {
}