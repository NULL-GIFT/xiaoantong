package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.mapper.SysLogMapper;
import com.example.securityplatform.model.entity.SysEdgeBox;
import com.example.securityplatform.model.entity.SysEdgeBoxSkill;
import com.example.securityplatform.model.entity.SysLog;
import com.example.securityplatform.model.entity.SysSkill;
import com.example.securityplatform.service.EdgeBoxService;
import com.example.securityplatform.service.EdgeBoxSkillService;
import com.example.securityplatform.service.MqttPublishService;
import com.example.securityplatform.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/skill")
public class SkillController {

    @Autowired private SkillService skillService;

    // 🌟 核心修改 1：彻底移除 DeviceService 和 DeviceSkillService
    // 换成边缘盒子专属的 Service
    @Autowired private EdgeBoxService edgeBoxService;
    @Autowired private EdgeBoxSkillService edgeBoxSkillService;

    @Autowired private MqttPublishService mqttPublishService;
    @Autowired private SysLogMapper sysLogMapper;
    @Autowired private HttpServletRequest request;

    @GetMapping("/list")
    public Result<List<SysSkill>> listSkills(
            @RequestParam(required = false) Integer configStatus,
            @RequestParam(required = false) Integer activationStatus) {
        QueryWrapper<SysSkill> qw = new QueryWrapper<>();
        if (configStatus != null) qw.eq("config_status", configStatus);
        if (activationStatus != null) qw.eq("activation_status", activationStatus);
        qw.orderByDesc("create_time");
        return Result.success(skillService.list(qw));
    }

    @PostMapping("/add")
    public Result<?> addSkill(@RequestBody SysSkill skill) {
        try {
            if (skill.getSkillName() == null) return Result.error(400, "参数缺失");
            if (skill.getModelFilePath() == null) skill.setModelFilePath("");
            skill.setValidDays(-1); skill.setConfigStatus(0); skill.setActivationStatus(1);

            boolean success = skillService.save(skill);

            String details = String.format("模型版本: %s, 算法标识码: %s", skill.getVersion(), skill.getSkillCode());
            recordLog("business", "联邦算法治理", "云端部署了新算法模型: " + skill.getSkillName(), "SkillController.add", details);

            return success ? Result.success("技能安装成功！") : Result.error(500, "数据库保存失败");
        } catch (Exception e) { return Result.error(500, "安装异常"); }
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> deleteSkill(@PathVariable Integer id, @RequestParam(required = false) String reason) {
        SysSkill skill = skillService.getById(id);
        boolean success = skillService.removeById(id);
        if(skill != null) {
            String finalReason = org.springframework.util.StringUtils.hasText(reason) ? reason : "无";
            recordLog("business", "联邦算法治理", "卸载了云端算法模型: " + skill.getSkillName(), "SkillController.delete", finalReason);
        }
        return success ? Result.success("删除成功") : Result.error(500, "删除失败");
    }

    @PostMapping("/batchActivate")
    public Result<?> batchActivate(@RequestBody Map<String, Object> body) {
        try {
            Boolean isAll = (Boolean) body.get("isAll");
            List<Integer> ids = (List<Integer>) body.get("ids");
            UpdateWrapper<SysSkill> uw = new UpdateWrapper<>();
            uw.set("activation_status", 1);
            if (isAll != null && isAll) skillService.update(uw);
            else {
                uw.in("id", ids);
                skillService.update(uw);
            }
            recordLog("business", "联邦算法治理", "批量唤醒/激活了平台算法库", "SkillController.batchActivate", "操作ID:" + ids);
            return Result.success("激活成功！");
        } catch (Exception e) {
            return Result.error(500, "异常: " + e.getMessage());
        }
    }

    // 🌟 核心修改 2：专属于盒子的下发接口
    @PostMapping("/deployToBox")
    public Result<Map<String, Object>> deployToBox(@RequestBody Map<String, Integer> req) {
        try {
            Integer skillId = req.get("skillId");
            Integer boxId = req.get("boxId");

            if (skillId == null || boxId == null) {
                return Result.error(400, "参数缺失，无法下发");
            }

            SysEdgeBox box = edgeBoxService.getById(boxId);
            SysSkill skill = skillService.getById(skillId);
            if (box == null || skill == null) return Result.error(404, "网关或算法模型不存在");

            // 1. 更新主技能库的状态为“已配置”
            if (skill.getConfigStatus() == null || skill.getConfigStatus() == 0) {
                skill.setConfigStatus(1);
                skillService.updateById(skill);
            }

            // 2. 存入全新的关联表 sys_edge_box_skill
            LambdaQueryWrapper<SysEdgeBoxSkill> query = new LambdaQueryWrapper<>();
            query.eq(SysEdgeBoxSkill::getBoxId, boxId).eq(SysEdgeBoxSkill::getSkillId, skillId);
            SysEdgeBoxSkill bs = edgeBoxSkillService.getOne(query);

            if (bs == null) {
                bs = new SysEdgeBoxSkill();
                bs.setBoxId(boxId);
                bs.setSkillId(skillId);
                bs.setStatus(1);
                bs.setThreshold(0.85); // 默认置信度阈值
                edgeBoxSkillService.save(bs);
            } else {
                bs.setStatus(1);
                edgeBoxSkillService.updateById(bs);
            }

            // 3. MQTT 指令下发给边缘盒子 (改传盒子的参数)
            Map<String, Object> payload = new HashMap<>();
            payload.put("box_ip", box.getIpAddress());
            payload.put("action", "deploy_skill");
            payload.put("skill_id", skill.getId());
            payload.put("skill_code", skill.getSkillCode());
            payload.put("version", skill.getVersion());
            payload.put("threshold", bs.getThreshold());
            mqttPublishService.publish("device/command", new com.alibaba.fastjson.JSONObject(payload).toJSONString());

            // 4. 落日志
            String details = String.format("目标算力节点IP: %s, 部署版本: %s, 阈值: %.2f", box.getIpAddress(), skill.getVersion(), bs.getThreshold());
            recordLog("business", "边缘算力调度", String.format("向算力网关 [%s] 下发算法 [%s]", box.getBoxName(), skill.getSkillName()), "SkillController.deployToBox", details);

            Map<String, Object> resp = new HashMap<>(); resp.put("status", "success");
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("下发网关失败: " + e.getMessage());
        }
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



    /**
     * 🌟 新增：从指定的边缘盒子上卸载算法
     */
    @PostMapping("/removeFromBox")
    public Result<?> removeFromBox(@RequestBody Map<String, Integer> req) {
        try {
            Integer skillId = req.get("skillId");
            Integer boxId = req.get("boxId");

            if (skillId == null || boxId == null) {
                return Result.error(400, "参数缺失，无法卸载");
            }

            // 1. 删除关联表里的记录 (剥离挂载关系)
            QueryWrapper<SysEdgeBoxSkill> qw = new QueryWrapper<>();
            qw.eq("box_id", boxId).eq("skill_id", skillId);
            boolean removed = edgeBoxSkillService.remove(qw);

            if (removed) {
                // 2. 检查这个算法是否还有其他盒子在用，如果没有任何盒子用了，就把主表的配置状态改为 0 (未配置)
                QueryWrapper<SysEdgeBoxSkill> checkQw = new QueryWrapper<>();
                checkQw.eq("skill_id", skillId);
                long count = edgeBoxSkillService.count(checkQw);
                if (count == 0) {
                    SysSkill skill = skillService.getById(skillId);
                    if (skill != null) {
                        skill.setConfigStatus(0); // 设为未配置闲置状态
                        skillService.updateById(skill);
                    }
                }

                // (可选) 3. 此时也可以通过 mqttPublishService 往盒子里发一条 remove_skill 的指令让 Python 停掉它

                return Result.success("成功从网关中卸载该算法");
            } else {
                return Result.error(400, "卸载失败或该关联已不存在");
            }
        } catch (Exception e) {
            return Result.error(500, "卸载异常：" + e.getMessage());
        }
    }
}