package com.example.securityplatform.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FedUploadDTO {
    // 🌟 完全对齐边缘端 form_data 的驼峰命名
    private String clientId;
    private Integer round;
    private String skillCode;
    private String baseVersion;

    // 🌟 核心：丢弃内存 List，直接用 Spring Boot 的文件接收类承接大模型权重！
    private MultipartFile weights;
}