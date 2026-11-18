package com.example.securityplatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.securityplatform.mapper.EdgeBoxMapper;
import com.example.securityplatform.model.entity.SysEdgeBox;
import com.example.securityplatform.service.EdgeBoxService;
import org.springframework.stereotype.Service;

@Service
public class EdgeBoxServiceImpl extends ServiceImpl<EdgeBoxMapper, SysEdgeBox> implements EdgeBoxService {
}