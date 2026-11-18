package com.example.securityplatform.service.impl;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.example.securityplatform.websocket.AlarmWebSocket;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;

@Service
public class SimpleAlarmService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String uploadDir = "storage/alarm_photos";

    /**
     * 完整的报警处理方法（不使用MyBatis，直接JDBC）
     */
    public void processAlarm(String payload) {
        System.out.println("=== SimpleAlarmService开始处理报警 ===");

        try {
            // 1. 解析JSON
            var json = JSON.parseObject(payload);
            String deviceId = json.getString("device_id");
            String alarmType = json.getString("type");
            Long timestamp = json.getLong("timestamp");
            String base64Image = json.getString("img_base64");
            String location = json.getString("location");

            System.out.println("解析成功 - 设备: " + deviceId + ", 类型: " + alarmType);

            // 2. 保存图片
            String fileName = saveImage(base64Image);
            if (fileName == null) {
                System.err.println("图片保存失败，终止处理");
                return;
            }
            System.out.println("图片保存成功: " + fileName);

            // 3. 时间转换
            LocalDateTime alarmTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );

            // 4. 插入数据库
            String sql = "INSERT INTO sys_alarm (device_id, alarm_type, alarm_time, img_path, confidence, is_handled, location) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            int rows = jdbcTemplate.update(sql,
                    deviceId,
                    alarmType,
                    alarmTime,
                    "/alarm_photos/" + fileName,
                    0.9f,
                    0,
                    location != null ? location : "未知位置"
            );

            if (rows > 0) {
                System.out.println("✅ 数据库插入成功，影响行数: " + rows);

                // 5. 打印最新的记录
                String querySql = "SELECT * FROM sys_alarm ORDER BY id DESC LIMIT 1";
                var result = jdbcTemplate.queryForMap(querySql);
                System.out.println("最新记录: " + result);
                try {
                    Object idObj = result.get("id");
                    Long alarmId = null;
                    if (idObj instanceof Number) {
                        alarmId = ((Number) idObj).longValue();
                    }
                    String imgPath = (String) result.get("img_path");
                    AlarmWebSocket.sendAlarm(deviceId, alarmType, imgPath, alarmId);
                    System.out.println("✅ 已通过WebSocket广播报警");
                } catch (Exception wsEx) {
                    System.err.println("⚠️ WebSocket广播失败: " + wsEx.getMessage());
                }
            } else {
                System.err.println("❌ 数据库插入失败");
            }

        } catch (Exception e) {
            System.err.println("❌ 处理报警异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== SimpleAlarmService报警处理结束 ===\n");
    }

    /**
     * 保存Base64图片
     */
    private String saveImage(String base64Str) {
        try {
            // 检查Base64字符串
            if (base64Str == null || base64Str.isEmpty()) {
                System.err.println("Base64字符串为空");
                return null;
            }

            // 去除可能的data:image前缀
            if (base64Str.startsWith("data:image")) {
                base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
            }

            // 创建目录
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("创建目录 " + uploadDir + ": " + (created ? "成功" : "失败"));
            }

            // 生成唯一文件名
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            File imageFile = new File(dir, fileName);

            // 解码并保存
            byte[] imageBytes = Base64.getDecoder().decode(base64Str);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageBytes);
            }

            System.out.println("图片已保存: " + imageFile.getAbsolutePath() + " (" + imageBytes.length + " bytes)");
            return fileName;

        } catch (Exception e) {
            System.err.println("保存图片失败: " + e.getMessage());
            return null;
        }
    }
}
