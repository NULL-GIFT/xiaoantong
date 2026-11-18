package com.example.securityplatform.model.dto;

import lombok.Data;

@Data
public class SkillDeployRequest {
    private String deviceId;
    private Integer skillId;
    private Integer status;
    private Double threshold;
}
