package com.example.securityplatform.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 接收边缘端发来的报警数据
 * 注意：字段名必须和边缘端发送的 JSON 完全一致！
 */
@Data
public class AlarmUploadDTO {
    private String device_id;      // 设备编号，如 "box_001"
    private String type;           // 报警类型，如 "fire", "smoke"
    private Long timestamp;        // 时间戳（毫秒）
    private String img_base64;     // Base64编码的图片字符串
    private String location;       // 可选，位置信息

    // 👇【新增】接收 Python 端的复杂逻辑产物
    private List<Float> confs;     // 识别框的置信度数组，例如 [0.55, 0.92]
    private List<String> types;    // 识别框的类型数组，例如 ["pseudo_label", "normal"]
    // 👇 【新增】用于接收边缘端的文件名（如 fire_20260325...），方便大模型精准点名
    private String record_file_name;

    // 👇【新增两个字段】
    private String track_id;       // 追踪ID，边缘端生成
    private List<Integer> bbox;    // 局部图在原图中的坐标 [x, y, w, h]
}