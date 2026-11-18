package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.model.entity.SysEdgeBox;
import com.example.securityplatform.model.entity.SysEdgeBoxSkill;
import com.example.securityplatform.service.EdgeBoxService;
import com.example.securityplatform.service.EdgeBoxSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/edgeBox")
public class EdgeBoxController {

    @Autowired
    private EdgeBoxService edgeBoxService;

    // 🌟 新增注入：盒子与算法的关联表
    @Autowired
    private EdgeBoxSkillService edgeBoxSkillService;

    @GetMapping("/list")
    public Result<List<SysEdgeBox>> getBoxList() {
        List<SysEdgeBox> list = edgeBoxService.list();
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result<?> addEdgeBox(@RequestBody SysEdgeBox edgeBox) {
        QueryWrapper<SysEdgeBox> qw = new QueryWrapper<>();
        qw.eq("ip_address", edgeBox.getIpAddress());
        if (edgeBoxService.count(qw) > 0) {
            return Result.error(400, "该 IP 已被注册，请勿重复添加！");
        }
        edgeBox.setCreateTime(new Date());
        edgeBox.setStatus(1); // 默认在线

        boolean saved = edgeBoxService.save(edgeBox);
        if (saved) {
            return Result.success("边缘计算盒子注册成功");
        } else {
            return Result.error(500, "落库失败");
        }
    }

    /**
     * 🌟 核心新增接口：根据技能ID，反查出所有搭载了该算法的边缘盒子
     * 专门服务于前端的“模型档案详情”展示
     */
    @GetMapping("/listBySkill")
    public Result<List<SysEdgeBox>> listBySkill(@RequestParam Integer skillId) {
        if (skillId == null) {
            return Result.error(400, "技能ID不能为空");
        }

        // 1. 去关联表查出所有挂载了这个 skillId 的记录 (只查处于启用状态的)
        QueryWrapper<SysEdgeBoxSkill> qw = new QueryWrapper<>();
        qw.eq("skill_id", skillId).eq("status", 1);
        List<SysEdgeBoxSkill> relations = edgeBoxSkillService.list(qw);

        // 2. 如果没有任何盒子装了这个算法，返回空列表，前端渲染为空白或提示词
        if (relations == null || relations.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 3. 将取出的所有 boxId 打包，去盒子主表做一次联查，把详细名字和IP带出来
        List<Integer> boxIds = relations.stream().map(SysEdgeBoxSkill::getBoxId).collect(Collectors.toList());
        List<SysEdgeBox> boxes = edgeBoxService.listByIds(boxIds);

        return Result.success(boxes);
    }
}