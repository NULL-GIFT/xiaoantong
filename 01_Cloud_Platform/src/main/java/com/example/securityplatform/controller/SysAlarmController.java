//package com.example.securityplatform.controller;
//
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.example.securityplatform.common.Result;
//import com.example.securityplatform.mapper.AlarmMapper;
//import com.example.securityplatform.model.entity.SysAlarm;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/alarm")
//public class SysAlarmController {
//
//    @Autowired
//    private AlarmMapper alarmMapper;
//
//    @GetMapping("/list")
//    public Result<Map<String, Object>> list(
//            @RequestParam(defaultValue = "1") Integer pageNum,
//            @RequestParam(defaultValue = "10") Integer pageSize,
//            @RequestParam(required = false) String type,
//            @RequestParam(required = false) String startTime,
//            @RequestParam(required = false) String endTime,
//            @RequestParam(required = false) String location,
//            @RequestParam(required = false) Integer isFalseAlarm,
//            @RequestParam(required = false) String deviceId,
//            @RequestParam(required = false) String handleStatus) { // 🌟 修复关键点：在这里声明 handleStatus 参数
//
//        Page<SysAlarm> page = new Page<>(pageNum, pageSize);
//        QueryWrapper<SysAlarm> qw = new QueryWrapper<>();
//
//        if (StringUtils.hasText(type)) {
//            qw.eq("alarm_type", type);
//        }
//        if (StringUtils.hasText(deviceId)) {
//            qw.eq("device_id", deviceId);
//        }
//        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
//            qw.between("alarm_time", startTime, endTime);
//        }
//        if (StringUtils.hasText(location)) {
//            qw.like("location", location);
//        }
//
//        // 🌟 工业级三态业务流转筛选
//        if (StringUtils.hasText(handleStatus)) {
//            if ("pending".equals(handleStatus)) {
//                // 待处置：既没被处理，也不是误报
//                qw.eq("is_handled", 0).eq("is_false_alarm", 0);
//            } else if ("resolved".equals(handleStatus)) {
//                // 有效已闭环：处理完了，且不是误报
//                qw.eq("is_handled", 1).eq("is_false_alarm", 0);
//            } else if ("false_alarm".equals(handleStatus)) {
//                // 负样本归档：只要是误报就行
//                qw.eq("is_false_alarm", 1);
//            }
//        }
//
//        qw.orderByDesc("alarm_time");
//        Page<SysAlarm> resultPage = alarmMapper.selectPage(page, qw);
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("total", resultPage.getTotal());
//        data.put("list", resultPage.getRecords());
//
//        return Result.success(data);
//    }
//
//    /**
//     * 🌟 新增接口：一键标记为误报，归档为负样本
//     */
//    @PostMapping("/markFalse")
//    public Result<?> markFalseAlarm(@RequestBody Map<String, Object> params) {
//        Integer id = (Integer) params.get("id");
//        String reason = (String) params.get("reason");
//
//        if (id == null) {
//            return Result.error(400, "记录ID不能为空");
//        }
//
//        // 1. 使用 alarmMapper 查出这条报警记录
//        SysAlarm alarm = alarmMapper.selectById(id);
//        if (alarm == null) {
//            return Result.error(404, "报警记录不存在");
//        }
//
//        // 2. 修改它的属性
//        alarm.setIsFalseAlarm(1);
//        alarm.setFalseReason(reason);
//        alarm.setIsHandled(1);
//
//        // 🌟 新增这行：记录归档时间
//        alarm.setHandledTime(new java.util.Date());
//
//        // 3. 更新回数据库
//        int rows = alarmMapper.updateById(alarm);
//
//        if (rows > 0) {
//            return Result.success("误报标记成功，已归档为联邦学习负样本！");
//        } else {
//            return Result.error(500, "数据库操作失败");
//        }
//    }
//
//    /**
//     * 🌟 新增接口：真实删除报警记录
//     */
//    @DeleteMapping("/delete/{id}")
//    public Result<?> deleteAlarm(@PathVariable Integer id) {
//        if (id == null) {
//            return Result.error(400, "要删除的记录ID不能为空");
//        }
//
//        // 使用 alarmMapper 真实删除数据库里的记录
//        int rows = alarmMapper.deleteById(id);
//
//        if (rows > 0) {
//            return Result.success("删除成功");
//        } else {
//            return Result.error(500, "删除失败，该记录可能已被删除");
//        }
//    }
//
//    /**
//     * 🌟 新增接口：管理员确认警情已处理完毕 (将 is_handled 改为 1)
//     */
//    @PostMapping("/resolve/{id}")
//    public Result<?> resolveAlarm(@PathVariable Integer id) {
//        SysAlarm alarm = alarmMapper.selectById(id);
//        if (alarm == null) {
//            return Result.error(404, "报警记录不存在");
//        }
//
//        alarm.setIsHandled(1); // 1 代表已处置
//        // 🌟 新增这行：记录闭环时间
//        alarm.setHandledTime(new java.util.Date());
//        int rows = alarmMapper.updateById(alarm);
//
//        if (rows > 0) {
//            return Result.success("警情已成功标记为已处置！");
//        } else {
//            return Result.error(500, "状态更新失败");
//        }
//    }
//}
//
//
//
//




package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.mapper.AlarmMapper;
import com.example.securityplatform.model.entity.SysAlarm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alarm")
public class SysAlarmController {

    @Autowired
    private AlarmMapper alarmMapper;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer isFalseAlarm,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String handleStatus) {

        Page<SysAlarm> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysAlarm> qw = new QueryWrapper<>();

        if (StringUtils.hasText(type)) {
            qw.eq("alarm_type", type);
        }
        if (StringUtils.hasText(deviceId)) {
            qw.eq("device_id", deviceId);
        }
        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
            qw.between("alarm_time", startTime, endTime);
        }
        if (StringUtils.hasText(location)) {
            qw.like("location", location);
        }

        // 🌟 工业级三态业务流转筛选
        if (StringUtils.hasText(handleStatus)) {
            if ("pending".equals(handleStatus)) {
                qw.eq("is_handled", 0).eq("is_false_alarm", 0);
            } else if ("resolved".equals(handleStatus)) {
                qw.eq("is_handled", 1).eq("is_false_alarm", 0);
            } else if ("false_alarm".equals(handleStatus)) {
                qw.eq("is_false_alarm", 1);
            }
        }

        qw.orderByDesc("alarm_time");
        Page<SysAlarm> resultPage = alarmMapper.selectPage(page, qw);
        List<SysAlarm> records = resultPage.getRecords();

        // 🌟 核心修复区：在数据传给前端之前，进行强制清洗和翻译
        for (SysAlarm alarm : records) {
            // 1. 修复行为分析的英文代码 -> 中文映射
            String currentType = alarm.getAlarmType();
            if (currentType != null) {
                switch (currentType) {
                    case "good_deed_early":
                        alarm.setAlarmType("早起打卡");
                        break;
                    case "good_deed_reading":
                        alarm.setAlarmType("晨读签到");
                        break;
                    case "good_deed_lights_off":
                        alarm.setAlarmType("随手关灯");
                        break;
                    case "good_deed_volunteer":
                        alarm.setAlarmType("志愿者服务");
                        break;
                    case "good_deed_helping_others":
                        alarm.setAlarmType("助人为乐");
                        break;
                    case "好人好事-捡拾垃圾":
                        alarm.setAlarmType("捡拾垃圾");
                        break;
                }
            }

            // 2. 修复“未知区域”问题 (删除了错误的 deviceName 逻辑，直接通过 deviceId 判断)
            String loc = alarm.getLocation();
            String devId = alarm.getDeviceId();

            // 如果边缘端传过来的是 box_001，把它的位置强制更正
            if ("box_001".equals(devId)) {
                if ("未知区域".equals(loc) || !StringUtils.hasText(loc)) {
                    alarm.setLocation("主教学楼大厅");
                }
            }
            else if ("box_002".equals(devId)) {
                if ("未知区域".equals(loc) || !StringUtils.hasText(loc)) {
                    alarm.setLocation("初二(3)班教室");
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", resultPage.getTotal());
        data.put("list", records); // 返回洗白后的数据

        return Result.success(data);
    }

    /**
     * 🌟 新增接口：一键标记为误报，归档为负样本
     */
    @PostMapping("/markFalse")
    public Result<?> markFalseAlarm(@RequestBody Map<String, Object> params) {
        // 修复类型转换异常：统一用 Number 接收，再转为 Long
        Number idNum = (Number) params.get("id");
        String reason = (String) params.get("reason");

        if (idNum == null) {
            return Result.error(400, "记录ID不能为空");
        }
        Long id = idNum.longValue();

        SysAlarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            return Result.error(404, "报警记录不存在");
        }

        alarm.setIsFalseAlarm(1);
        alarm.setFalseReason(reason);
        alarm.setIsHandled(1);
        alarm.setHandledTime(new java.util.Date());

        int rows = alarmMapper.updateById(alarm);

        if (rows > 0) {
            return Result.success("误报标记成功，已归档为联邦学习负样本！");
        } else {
            return Result.error(500, "数据库操作失败");
        }
    }

    /**
     * 🌟 新增接口：真实删除报警记录
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteAlarm(@PathVariable Long id) { // 修改这里为 Long
        if (id == null) {
            return Result.error(400, "要删除的记录ID不能为空");
        }

        int rows = alarmMapper.deleteById(id);

        if (rows > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error(500, "删除失败，该记录可能已被删除");
        }
    }

    /**
     * 🌟 新增接口：管理员确认警情已处理完毕 (将 is_handled 改为 1)
     */
    @PostMapping("/resolve/{id}")
    public Result<?> resolveAlarm(@PathVariable Long id) { // 修改这里为 Long
        SysAlarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            return Result.error(404, "报警记录不存在");
        }

        alarm.setIsHandled(1);
        alarm.setHandledTime(new java.util.Date());
        int rows = alarmMapper.updateById(alarm);

        if (rows > 0) {
            return Result.success("警情已成功标记为已处置！");
        } else {
            return Result.error(500, "状态更新失败");
        }
    }
}