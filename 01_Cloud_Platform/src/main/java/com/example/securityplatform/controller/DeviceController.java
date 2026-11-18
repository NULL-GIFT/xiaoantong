//package com.example.securityplatform.controller;
//
//import com.alibaba.fastjson.JSON;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.example.securityplatform.common.Result;
//import com.example.securityplatform.mapper.DeviceMapper;
//import com.example.securityplatform.model.entity.SysDevice;
//import com.example.securityplatform.model.entity.SysDeviceSkill;
//import com.example.securityplatform.model.entity.SysSkill;
//import com.example.securityplatform.service.DeviceSkillService;
//import com.example.securityplatform.service.SkillService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//// 顶部导入添加：
//import com.example.securityplatform.mapper.SysLogMapper;
//import com.example.securityplatform.model.entity.SysLog;
//import javax.servlet.http.HttpServletRequest;
//import java.util.Date;
//@RestController
//@RequestMapping("/api/device")
//public class DeviceController {
//
//    @Autowired
//    private DeviceMapper deviceMapper;
//
//    // 🌟 新增注入：引入技能相关的 Service，用于组装数据
//    @Autowired
//    private DeviceSkillService deviceSkillService;
//
//    @Autowired
//    private SkillService skillService;
//
//    /**
//     * 🌟 真实的分页与多条件筛选接口 (带技能组装)
//     */
//    @GetMapping("/list")
//    public Result<Map<String, Object>> getDeviceList(
//            @RequestParam(defaultValue = "1") Integer pageNum,
//            @RequestParam(defaultValue = "10") Integer pageSize,
//            @RequestParam(required = false) String deviceType,
//            @RequestParam(required = false) String status) {
//
//        try {
//            Page<SysDevice> page = new Page<>(pageNum, pageSize);
//            QueryWrapper<SysDevice> qw = new QueryWrapper<>();
//
//            // 1. 处理前端传来的设备类型筛选
//            if (StringUtils.hasText(deviceType)) {
//                qw.eq("device_type", deviceType);
//            }
//
//            // 2. 处理前端传来的中文状态筛选 ("在线" 或 "离线")
//            if ("在线".equals(status)) {
//                qw.eq("status", 1); // 1 代表在线
//            } else if ("离线".equals(status)) {
//                qw.eq("status", 0); // 0 代表离线
//            }
//
//            // 3. 默认按 ID 正序排列，保证列表稳定
//            qw.orderByAsc("id");
//
//            // 4. 执行真实的分页查询 (查出基础设备信息)
//            Page<SysDevice> resultPage = deviceMapper.selectPage(page, qw);
//            List<SysDevice> records = resultPage.getRecords();
//
//            // 🌟 5. 核心修复：遍历每一台设备，去关联表里查出它搭载了哪些算法
//            List<Map<String, Object>> formattedList = new ArrayList<>();
//            for (SysDevice device : records) {
//                // 利用 FastJSON 将实体类转成 Map，这样我们就能动态往里面塞新字段了
//                Map<String, Object> deviceMap = JSON.parseObject(JSON.toJSONString(device), Map.class);
//
//                // 查询这台设备在 sys_device_skill 表里的关联记录
//                QueryWrapper<SysDeviceSkill> dsQw = new QueryWrapper<>();
//                dsQw.eq("device_id", device.getId());
//                List<SysDeviceSkill> dsList = deviceSkillService.list(dsQw);
//
//                // 提取出真正的技能名称 (如："烟火检测", "摔倒检测")
//                List<String> skillNames = new ArrayList<>();
//                for (SysDeviceSkill ds : dsList) {
//                    SysSkill skill = skillService.getById(ds.getSkillId());
//                    if (skill != null) {
//                        skillNames.add(skill.getSkillName());
//                    }
//                }
//
//                // 🌟 把查出来的技能数组塞进 map，字段名严格对齐前端期望的 "configuredSkills"
//                deviceMap.put("configuredSkills", skillNames);
//                formattedList.add(deviceMap);
//            }
//
//            // 6. 封装前端需要的数据结构
//            Map<String, Object> data = new HashMap<>();
//            data.put("total", resultPage.getTotal());
//            data.put("list", formattedList); // 这里返回的是塞入了技能的新列表
//
//            return Result.success(data);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error("获取设备列表失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 详情接口
//     */
//    @GetMapping("/{deviceId}")
//    public Result<SysDevice> getDeviceDetail(@PathVariable String deviceId) {
//        try {
//            QueryWrapper<SysDevice> qw = new QueryWrapper<>();
//            qw.eq("device_id", deviceId);
//            SysDevice device = deviceMapper.selectOne(qw);
//
//            if (device == null) {
//                return Result.error(404, "设备不存在: " + deviceId);
//            }
//            return Result.success(device);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error("获取设备详情失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 🌟 真实的 ICMP Ping 连通性测试接口
//     */
//    @GetMapping("/ping")
//    public Result<Map<String, Object>> pingDevice(@RequestParam String ip) {
//        Map<String, Object> res = new HashMap<>();
//        try {
//            long startTime = System.currentTimeMillis();
//            // Java 底层调用网络层进行真 Ping，超时时间 2000ms
//            boolean isReachable = java.net.InetAddress.getByName(ip).isReachable(2000);
//            long latency = System.currentTimeMillis() - startTime;
//
//            if (isReachable) {
//                res.put("status", "success");
//                res.put("latency", latency);
//                return Result.success(res);
//            } else {
//                return Result.error(504, "ICMP 握手超时，设备无法触达");
//            }
//        } catch (Exception e) {
//            return Result.error(500, "网络解析异常");
//        }
//    }
//
//    @Autowired
//    private SysLogMapper sysLogMapper;
//
//    @Autowired
//    private HttpServletRequest request;
//
//    // 找到你的 addDevice 方法，修改为：
//    @PostMapping("/add")
//    public Result<?> addDevice(@RequestBody SysDevice device) {
//        try {
//            QueryWrapper<SysDevice> qw = new QueryWrapper<>();
//            qw.eq("device_id", device.getDeviceId());
//            if (deviceMapper.selectCount(qw) > 0) {
//                return Result.error(400, "该 SN 码已被注册，请检查！");
//            }
//
//            device.setCreateTime(new Date());
//            device.setUpdateTime(new Date());
//
//            int rows = deviceMapper.insert(device);
//
//            if (rows > 0) {
//                // 🌟 核心修改4：提取设备关键信息写入明文日志
//                String details = String.format("设备定名: %s\n设备SN码: %s",
//                        device.getDeviceName(), device.getDeviceId());
//                recordLog("business", "设备网关纳管", "云端成功登记并准入新计算节点", "DeviceController.addDevice", details);
//
//                return Result.success("注册成功");
//            } else {
//                return Result.error(500, "落库失败");
//            }
//        } catch (Exception e) {
//            return Result.error(500, "注册异常: " + e.getMessage());
//        }
//    }
//
//
//
//    /**
//     * 🌟 真实物理设备卸载与关联清理接口
//     */
//    @DeleteMapping("/delete/{id}")
//    public Result<?> deleteDevice(@PathVariable String id) {
//        try {
//            // 1. 先查询设备是否存在 (兼容按主键 ID 或按 SN 码 device_id 查找)
//            SysDevice device = deviceMapper.selectById(id);
//            if (device == null) {
//                QueryWrapper<SysDevice> qw = new QueryWrapper<>();
//                qw.eq("device_id", id);
//                device = deviceMapper.selectOne(qw);
//                if (device == null) {
//                    return Result.error(404, "找不到该设备，可能已被其他管理员卸载");
//                }
//            }
//
//            // 2. 级联清理：删除该设备挂载的所有 AI 算法技能 (防止产生脏数据)
//            QueryWrapper<SysDeviceSkill> dsQw = new QueryWrapper<>();
//            dsQw.eq("device_id", device.getId());
//            deviceSkillService.remove(dsQw); // 注意：这需要引入 deviceSkillService，你类里已经 @Autowired 过了
//
//            // 3. 执行核心物理删除
//            int rows = deviceMapper.deleteById(device.getId());
//
//            if (rows > 0) {
//                // 🌟 4. 核心安全机制：将卸载高危操作写入审计日志
//                String details = String.format("已卸载设备名称: %s\n设备SN码: %s\n最后已知IP: %s",
//                        device.getDeviceName(), device.getDeviceId(), device.getIpAddress() != null ? device.getIpAddress() : "未知");
//                recordLog("business", "设备资产销毁", "管理员从云端强制卸载边缘计算节点", "DeviceController.deleteDevice", details);
//
//                return Result.success("设备已成功卸载，相关算法配置已清空");
//            } else {
//                return Result.error(500, "卸载失败，数据库受影响行数为 0");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error(500, "卸载异常: " + e.getMessage());
//        }
//    }
//
//
//
//
//
//    // 🌟 在类最底下加上写日志的方法
//    private void recordLog(String type, String module, String action, String method, String params) {
//        SysLog log = new SysLog();
//        log.setLogType(type);
//        try {
//            String operator = request.getHeader("X-Operator");
//            log.setOperator(operator != null ? java.net.URLDecoder.decode(operator, "UTF-8") : "SYSTEM");
//        } catch (Exception e) { log.setOperator("SYSTEM"); }
//        log.setModule(module); log.setAction(action); log.setMethod(method); log.setParams(params); log.setStatus(1); log.setCreateTime(new Date());
//        sysLogMapper.insert(log);
//    }
//
//
//
//
//
//}






package com.example.securityplatform.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.mapper.DeviceMapper;
import com.example.securityplatform.mapper.SysLogMapper;
import com.example.securityplatform.model.entity.*;
import com.example.securityplatform.service.EdgeBoxService;
import com.example.securityplatform.service.EdgeBoxSkillService;
import com.example.securityplatform.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    @Autowired
    private DeviceMapper deviceMapper;

    // 🌟 核心替换：彻底移除废弃的 DeviceSkillService，引入盒子与算法的新架构 Service
    @Autowired
    private EdgeBoxService edgeBoxService;

    @Autowired
    private EdgeBoxSkillService edgeBoxSkillService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private HttpServletRequest request;

    /**
     * 分页与多条件筛选接口 (带全新架构的技能组装)
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getDeviceList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status) {

        try {
            Page<SysDevice> page = new Page<>(pageNum, pageSize);
            QueryWrapper<SysDevice> qw = new QueryWrapper<>();

            if (StringUtils.hasText(deviceType)) qw.eq("device_type", deviceType);

            if ("在线".equals(status)) qw.eq("status", 1);
            else if ("离线".equals(status)) qw.eq("status", 0);

            qw.orderByAsc("id");

            Page<SysDevice> resultPage = deviceMapper.selectPage(page, qw);
            List<SysDevice> records = resultPage.getRecords();

            List<Map<String, Object>> formattedList = new ArrayList<>();
            for (SysDevice device : records) {
                Map<String, Object> deviceMap = JSON.parseObject(JSON.toJSONString(device), Map.class);
                List<String> skillNames = new ArrayList<>();

                // 🌟 全新云边端逻辑：通过摄像头的 IP 找到它挂在哪个盒子上，再查盒子的算法！
                if (StringUtils.hasText(device.getIpAddress())) {
                    QueryWrapper<SysEdgeBox> boxQw = new QueryWrapper<>();
                    boxQw.eq("ip_address", device.getIpAddress());
                    SysEdgeBox box = edgeBoxService.getOne(boxQw);

                    if (box != null) {
                        // 查这个盒子搭载了什么算法
                        QueryWrapper<SysEdgeBoxSkill> bsQw = new QueryWrapper<>();
                        bsQw.eq("box_id", box.getId()).eq("status", 1);
                        List<SysEdgeBoxSkill> bsList = edgeBoxSkillService.list(bsQw);

                        for (SysEdgeBoxSkill bs : bsList) {
                            SysSkill skill = skillService.getById(bs.getSkillId());
                            if (skill != null) skillNames.add(skill.getSkillName());
                        }
                    }
                }

                deviceMap.put("configuredSkills", skillNames);
                formattedList.add(deviceMap);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("list", formattedList);

            return Result.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取感知通道列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{deviceId}")
    public Result<SysDevice> getDeviceDetail(@PathVariable String deviceId) {
        try {
            QueryWrapper<SysDevice> qw = new QueryWrapper<>();
            qw.eq("device_id", deviceId);
            SysDevice device = deviceMapper.selectOne(qw);
            if (device == null) return Result.error(404, "设备不存在: " + deviceId);
            return Result.success(device);
        } catch (Exception e) {
            return Result.error("获取设备详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/ping")
    public Result<Map<String, Object>> pingDevice(@RequestParam String ip) {
        Map<String, Object> res = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            boolean isReachable = java.net.InetAddress.getByName(ip).isReachable(2000);
            long latency = System.currentTimeMillis() - startTime;

            if (isReachable) {
                res.put("status", "success");
                res.put("latency", latency);
                return Result.success(res);
            } else {
                return Result.error(504, "ICMP 握手超时，设备无法触达");
            }
        } catch (Exception e) { return Result.error(500, "网络解析异常"); }
    }

    @PostMapping("/add")
    public Result<?> addDevice(@RequestBody SysDevice device) {
        try {
            QueryWrapper<SysDevice> qw = new QueryWrapper<>();
            qw.eq("device_id", device.getDeviceId());
            if (deviceMapper.selectCount(qw) > 0) {
                return Result.error(400, "该 SN 码已被注册，请检查！");
            }

            device.setCreateTime(new Date());
            device.setUpdateTime(new Date());

            int rows = deviceMapper.insert(device);

            if (rows > 0) {
                String details = String.format("感知通道名称: %s\n通道SN码: %s", device.getDeviceName(), device.getDeviceId());
                recordLog("business", "感知通道纳管", "成功登记并准入新摄像头通道", "DeviceController.addDevice", details);
                return Result.success("注册成功");
            } else {
                return Result.error(500, "落库失败");
            }
        } catch (Exception e) { return Result.error(500, "注册异常: " + e.getMessage()); }
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> deleteDevice(@PathVariable String id) {
        try {
            SysDevice device = deviceMapper.selectById(id);
            if (device == null) {
                QueryWrapper<SysDevice> qw = new QueryWrapper<>();
                qw.eq("device_id", id);
                device = deviceMapper.selectOne(qw);
                if (device == null) return Result.error(404, "找不到该设备");
            }

            // 🌟 核心清理：彻底移除了之前去旧表删除关联技能的逻辑！
            // 现在的架构下，删除摄像头，不会影响盒子上的算法，直接删除物理设备记录即可。
            int rows = deviceMapper.deleteById(device.getId());

            if (rows > 0) {
                String details = String.format("已卸载通道名称: %s\n通道SN码: %s\n所属网关IP: %s",
                        device.getDeviceName(), device.getDeviceId(), device.getIpAddress() != null ? device.getIpAddress() : "未知");
                recordLog("business", "感知通道销毁", "管理员从云端强制卸载感知通道", "DeviceController.deleteDevice", details);

                return Result.success("感知通道已成功卸载");
            } else {
                return Result.error(500, "卸载失败");
            }
        } catch (Exception e) { return Result.error(500, "卸载异常: " + e.getMessage()); }
    }

    private void recordLog(String type, String module, String action, String method, String params) {
        SysLog log = new SysLog();
        log.setLogType(type);
        try {
            String operator = request.getHeader("X-Operator");
            log.setOperator(operator != null ? java.net.URLDecoder.decode(operator, "UTF-8") : "SYSTEM");
        } catch (Exception e) { log.setOperator("SYSTEM"); }
        log.setModule(module); log.setAction(action); log.setMethod(method); log.setParams(params); log.setStatus(1); log.setCreateTime(new Date());
        sysLogMapper.insert(log);
    }
}