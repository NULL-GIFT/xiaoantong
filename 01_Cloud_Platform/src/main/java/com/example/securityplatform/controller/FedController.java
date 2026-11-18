package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.model.dto.FedUploadDTO;
import com.example.securityplatform.model.entity.SysFedModel;
import com.example.securityplatform.service.FedService;
import com.example.securityplatform.service.MqttPublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fed")
public class FedController {

    @Autowired
    private FedService fedService;

    @Autowired
    private MqttPublishService mqttPublishService;

    @Value("${server.port}")
    private String serverPort;

    // ==========================================
    // 1. 面向【边缘端】的接口 (使用你的优雅 DTO 映射)
    // ==========================================
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@ModelAttribute FedUploadDTO dto) {
        try {
            if (dto.getClientId() == null || dto.getWeights() == null || dto.getWeights().isEmpty()) {
                return Result.error(400, "缺少必要参数或梯度文件为空");
            }

            Integer round = dto.getRound() != null ? dto.getRound() : 1;
            MultipartFile file = dto.getWeights();

            // 落盘存储
            String baseDir = "storage/fed/round-" + round;
            File dir = new File(baseDir);
            if (!dir.exists()) dir.mkdirs();

            String filename = dto.getClientId() + "-" + System.currentTimeMillis() + ".json";
            File out = new File(dir, filename);
            file.transferTo(out);

            // 调用你的核心聚合逻辑
            fedService.aggregateFromEdgeFile(dto.getClientId(), round, dto.getSkillCode(), dto.getBaseVersion(), out);

            Map<String, Object> resp = new HashMap<>();
            resp.put("filePath", out.getPath());
            resp.put("round", round);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. 面向【前端大屏】的管理接口 (使用组员的 MyBatis-Plus 逻辑)
    // ==========================================
    @GetMapping("/list")
    public Result<?> getModelList(@RequestParam(required = false) String version,
                                  @RequestParam(required = false) String skillCode,
                                  @RequestParam(required = false) String status) {

        LambdaQueryWrapper<SysFedModel> queryWrapper = new LambdaQueryWrapper<>();

        if (version != null && !version.isEmpty()) {
            queryWrapper.like(SysFedModel::getVersion, version);
        }
        if (skillCode != null && !skillCode.isEmpty()) {
            queryWrapper.eq(SysFedModel::getSkillCode, skillCode);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(SysFedModel::getStatus, status);
        }

        queryWrapper.orderByDesc(SysFedModel::getCreateTime);
        List<SysFedModel> list = fedService.list(queryWrapper);
        return Result.success(list);
    }

    @PostMapping("/dispatch")
    public Result<?> dispatchModel(@RequestBody Map<String, Object> params) {
        try {
            // 这里以后可以改成从数据库动态获取版本号
            String version = "v1.2";
            String downloadUrl = "http://127.0.0.1:" + serverPort + "/api/fed/download?version=" + version;
            mqttPublishService.publishModelUpdateCommand("all", version, downloadUrl);
            return Result.success("模型下发指令已通过 MQTT 广播");
        } catch (Exception e) {
            return Result.error("下发失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> deleteModel(@PathVariable Integer id) {
        fedService.removeById(id);
        return Result.success("模型删除成功");
    }

    // ==========================================
    // 3. 面向【边缘端】的下载接口 (必须使用你的找 .json 的逻辑)
    // ==========================================
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam(required = false) String version) {
        try {
            File dir = new File("storage/fed/global");
            if (!dir.exists()) return ResponseEntity.notFound().build();

            // ⚠️ 核心防爆点：这里必须是 .json，不能是你组员写的 .onnx！
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json") && name.startsWith("global_weights"));
            if (files == null || files.length == 0) return ResponseEntity.notFound().build();

            File latest = files[0];
            for (File f : files) {
                if (f.lastModified() > latest.lastModified()) latest = f;
            }

            Resource resource = new FileSystemResource(latest);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(latest.getName()).build());
            headers.setContentLength(latest.length());

            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}