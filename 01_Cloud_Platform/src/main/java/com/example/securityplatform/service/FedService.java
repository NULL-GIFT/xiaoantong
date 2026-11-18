package com.example.securityplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.securityplatform.model.entity.SysFedModel;
import java.io.File;

// 🌟 完美融合：既继承了 MyBatis-Plus 的 IService，又保留了你的文件流聚合接口
public interface FedService extends IService<SysFedModel> {

    // 你的核心工业级方法：从落盘文件执行解析和聚合
    void aggregateFromEdgeFile(String clientId, Integer round, String skillCode, String baseVersion, File edgeGradientFile);
}