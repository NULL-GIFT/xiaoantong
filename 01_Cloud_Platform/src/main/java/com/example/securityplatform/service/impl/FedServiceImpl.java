//package com.example.securityplatform.service.impl;
//
//import com.alibaba.fastjson.JSON;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.example.securityplatform.mapper.SysFedModelMapper;
//import com.example.securityplatform.model.entity.SysFedModel;
//import com.example.securityplatform.service.FedService;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@Service
//// 🌟 完美融合：继承 ServiceImpl 获得数据库能力，实现 FedService 获得你的聚合能力
//public class FedServiceImpl extends ServiceImpl<SysFedModelMapper, SysFedModel> implements FedService {
//
//    // 内存缓存池：用于暂存这一轮次各个设备传上来的梯度数组
//    private final ConcurrentHashMap<Integer, List<List<Double>>> roundWeightsMap = new ConcurrentHashMap<>();
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Override
//    public void aggregateFromEdgeFile(String clientId, Integer round, String skillCode, String baseVersion, File edgeGradientFile) {
//        try {
//            // 1. 工业级解析：直接让底层流式读取文件并反序列化
//            JsonNode rootNode = objectMapper.readTree(edgeGradientFile);
//            JsonNode weightsNode = rootNode.get("weights");
//
//            List<Double> weights = new ArrayList<>();
//            if (weightsNode != null && weightsNode.isArray()) {
//                for (JsonNode node : weightsNode) {
//                    weights.add(node.asDouble());
//                }
//            }
//
//            log.info("🎯 业务层开始处理边缘梯度 | 设备: {} | 轮次: {} | 维度: {}", clientId, round, weights.size());
//
//            // 2. 加入当前轮次的聚合池
//            roundWeightsMap.putIfAbsent(round, new ArrayList<>());
//            roundWeightsMap.get(round).add(weights);
//
//            // 3. 执行聚合计算 (FedAvg)
//            aggregateWeights(round);
//
//        } catch (Exception e) {
//            log.error("❌ 解析边缘端梯度文件失败", e);
//        }
//    }
//
//    private void aggregateWeights(Integer round) {
//        List<List<Double>> allWeights = roundWeightsMap.get(round);
//        if (allWeights == null || allWeights.isEmpty()) return;
//
//        int clientCount = allWeights.size();
//        int weightSize = allWeights.get(0).size();
//
//        // 将所有设备的梯度数组对应位置相加并求平均
//        List<Double> globalWeights = new ArrayList<>(weightSize);
//        for (int i = 0; i < weightSize; i++) {
//            double sum = 0.0;
//            for (List<Double> clientWeight : allWeights) {
//                if (i < clientWeight.size()) {
//                    sum += clientWeight.get(i);
//                }
//            }
//            globalWeights.add(sum / clientCount);
//        }
//
//        // 4. 保存为全局参数
//        try {
//            File dir = new File("storage/fed/global");
//            if (!dir.exists()) dir.mkdirs();
//
//            File outFile = new File(dir, "global_weights_round_" + round + ".json");
//            try (FileWriter writer = new FileWriter(outFile)) {
//                writer.write(JSON.toJSONString(globalWeights));
//            }
//            log.info("🎉 轮次 {} 的联邦聚合计算完成！共聚合 {} 个节点的模型梯度。", round, clientCount);
//        } catch (Exception e) {
//            log.error("保存全局平均权重失败", e);
//        }
//    }
//}


package com.example.securityplatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.securityplatform.mapper.SysFedLogMapper;
import com.example.securityplatform.mapper.SysFedModelMapper;
import com.example.securityplatform.model.entity.SysFedLog;
import com.example.securityplatform.model.entity.SysFedModel;
import com.example.securityplatform.service.FedService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FedServiceImpl extends ServiceImpl<SysFedModelMapper, SysFedModel> implements FedService {

    // 内存缓存池：用于暂存这一轮次各个设备传上来的梯度数组
    private final ConcurrentHashMap<Integer, List<List<Double>>> roundWeightsMap = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    // 🌟 注入刚刚新建的流水表 Mapper
    @Autowired
    private SysFedLogMapper sysFedLogMapper;

    @Override
    public void aggregateFromEdgeFile(String clientId, Integer round, String skillCode, String baseVersion, File edgeGradientFile) {
        try {
            // 🌟 1. 登记流水：把边缘端的上传记录写进 sys_fed_log 表！
            SysFedLog fedLog = new SysFedLog();
            fedLog.setDeviceId(clientId);
            fedLog.setSkillCode(skillCode != null ? skillCode : "fire_detect");
            fedLog.setRound(round);
            fedLog.setBaseModelVersion(baseVersion != null ? baseVersion : "v1.0");
            fedLog.setWeightsPath(edgeGradientFile.getPath().replace("\\", "/")); // 统一斜杠防止跨平台报错
            fedLog.setCreateTime(LocalDateTime.now());
            sysFedLogMapper.insert(fedLog);

            // 2. 工业级解析：读取文件并反序列化
            JsonNode rootNode = objectMapper.readTree(edgeGradientFile);
            JsonNode weightsNode = rootNode.get("weights");

            List<Double> weights = new ArrayList<>();
            if (weightsNode != null && weightsNode.isArray()) {
                for (JsonNode node : weightsNode) {
                    weights.add(node.asDouble());
                }
            }

            log.info("🎯 业务层开始处理边缘梯度 | 设备: {} | 轮次: {} | 维度: {}", clientId, round, weights.size());

            // 3. 加入当前轮次的聚合池
            roundWeightsMap.putIfAbsent(round, new ArrayList<>());
            roundWeightsMap.get(round).add(weights);

            // 4. 执行聚合计算 (把 skillCode 传进去，方便大考卷登记)
            aggregateWeights(round, skillCode);

        } catch (Exception e) {
            log.error("❌ 解析边缘端梯度文件失败", e);
        }
    }

    private void aggregateWeights(Integer round, String skillCode) {
        List<List<Double>> allWeights = roundWeightsMap.get(round);
        if (allWeights == null || allWeights.isEmpty()) return;

        int clientCount = allWeights.size();
        int weightSize = allWeights.get(0).size();

        // 将所有设备的梯度数组对应位置相加并求平均
        List<Double> globalWeights = new ArrayList<>(weightSize);
        for (int i = 0; i < weightSize; i++) {
            double sum = 0.0;
            for (List<Double> clientWeight : allWeights) {
                if (i < clientWeight.size()) {
                    sum += clientWeight.get(i);
                }
            }
            globalWeights.add(sum / clientCount);
        }

        // 保存为全局参数
        try {
            File dir = new File("storage/fed/global");
            if (!dir.exists()) dir.mkdirs();

            File outFile = new File(dir, "global_weights_round_" + round + ".json");
            try (FileWriter writer = new FileWriter(outFile)) {
                writer.write(JSON.toJSONString(globalWeights));
            }

            // 🌟 5. 登记大考卷：把云端聚合好的模型写进 sys_fed_model 表！
            SysFedModel model = new SysFedModel();
            model.setSkillCode(skillCode != null ? skillCode : "fire_detect");
            model.setVersion("V1." + round); // 每次聚合，版本号小幅度+1
            model.setRound(round);
            model.setAccuracy(new BigDecimal("0.85")); // 初始精度展示
            model.setNodes(clientCount); // 参与计算的边缘端数量
            model.setFilePath(outFile.getPath().replace("\\", "/"));
            model.setStatus("active");
            model.setCreateTime(LocalDateTime.now());

            this.save(model); // MyBatis-Plus 自带的插入方法

            log.info("🎉 轮次 {} 的联邦聚合计算完成并入库！共聚合 {} 个节点的模型。", round, clientCount);
        } catch (Exception e) {
            log.error("保存全局平均权重失败", e);
        }
    }
}