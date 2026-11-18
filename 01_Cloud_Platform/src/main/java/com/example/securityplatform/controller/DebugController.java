package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.securityplatform.mapper.AlarmMapper;
import com.example.securityplatform.model.entity.SysAlarm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final JdbcTemplate jdbcTemplate;
    private final AlarmMapper alarmMapper;

    // 使用构造器注入代替字段注入
    @Autowired
    public DebugController(JdbcTemplate jdbcTemplate, AlarmMapper alarmMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.alarmMapper = alarmMapper;
    }

    /**
     * 诊断系统状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查数据库连接
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_alarm", Integer.class);
            result.put("database_connected", true);
            result.put("alarm_count", count);
        } catch (Exception e) {
            result.put("database_connected", false);
            result.put("database_error", e.getMessage());
        }

        // 2. 检查目录
        File dir = new File("storage/alarm_photos");
        result.put("directory_exists", dir.exists());
        result.put("directory_path", dir.getAbsolutePath());
        result.put("directory_writable", dir.canWrite());

        if (dir.exists()) {
            File[] files = dir.listFiles();
            result.put("file_count", files != null ? files.length : 0);

            if (files != null && files.length > 0) {
                List<String> fileNames = new java.util.ArrayList<>();
                for (File file : files) {
                    fileNames.add(file.getName() + " (" + file.length() + " bytes)");
                }
                result.put("files", fileNames);
            }
        }

        // 3. 检查最近报警记录 - 使用 MyBatis Plus 的方法
        try {
            // 使用 selectList 方法代替 selectAll
            QueryWrapper<SysAlarm> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("alarm_time"); // 按时间倒序排序
            List<SysAlarm> alarms = alarmMapper.selectList(queryWrapper);

            result.put("recent_alarms_count", alarms.size());
            if (!alarms.isEmpty()) {
                List<Map<String, Object>> alarmList = new java.util.ArrayList<>();
                for (SysAlarm alarm : alarms) {
                    Map<String, Object> alarmMap = new HashMap<>();
                    alarmMap.put("id", alarm.getId());
                    alarmMap.put("deviceId", alarm.getDeviceId());
                    alarmMap.put("alarmType", alarm.getAlarmType());
                    alarmMap.put("alarmTime", alarm.getAlarmTime());
                    alarmMap.put("imgPath", alarm.getImgPath());
                    alarmMap.put("confidence", alarm.getConfidence());
                    alarmMap.put("isHandled", alarm.getIsHandled());
                    alarmList.add(alarmMap);
                }
                result.put("recent_alarms", alarmList);
            }
        } catch (Exception e) {
            result.put("query_alarms_error", e.getMessage());
        }

        return result;
    }

    /**
     * 测试数据库插入
     */
    @GetMapping("/test-insert")
    public String testInsert() {
        try {
            String sql = "INSERT INTO sys_alarm (device_id, alarm_type, alarm_time, img_path, confidence, is_handled, location) " +
                    "VALUES ('test_box', 'test', NOW(), '/test.jpg', 0.9, 0, '测试位置')";

            int rows = jdbcTemplate.update(sql);
            return "测试插入成功，影响行数: " + rows;
        } catch (Exception e) {
            return "测试插入失败: " + e.getMessage();
        }
    }
}