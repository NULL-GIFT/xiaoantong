package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.mapper.SysUserMapper;
import com.example.securityplatform.mapper.SysLogMapper;
import com.example.securityplatform.model.entity.SysUser;
import com.example.securityplatform.model.entity.SysLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class SysUserController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    // 🌟 注入 Request，用于抓取前端发来的真实账号
    @Autowired
    private HttpServletRequest request;

    @GetMapping("/list")
    public Result<List<SysUser>> list(@RequestParam(required = false) String username,
                                      @RequestParam(required = false) String realName) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (StringUtils.hasText(username)) qw.like("username", username);
        if (StringUtils.hasText(realName)) qw.like("real_name", realName);
        List<SysUser> list = userMapper.selectList(qw);
        list.forEach(u -> u.setPassword(null));
        return Result.success(list);
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody SysUser user) {
        try {
            QueryWrapper<SysUser> qw = new QueryWrapper<>();
            qw.eq("username", user.getUsername());
            if (userMapper.selectCount(qw) > 0) return Result.error(400, "添加失败：用户名已存在");

            user.setPassword(md5Hex(user.getPassword()));
            user.setCreateTime(new Date());
            userMapper.insert(user);

            recordLog("account", "IAM身份控制", "签发了新账号: " + user.getUsername(), "SysUserController.add", user.toString());
            return Result.success("新增用户成功");
        } catch (Exception e) {
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<String> update(@RequestBody SysUser user) {
        try {
            if (StringUtils.hasText(user.getPassword())) user.setPassword(md5Hex(user.getPassword()));
            else user.setPassword(null);
            userMapper.updateById(user);
            recordLog("account", "IAM身份控制", "更新了账户资料 (用户ID:" + user.getId() + ")", "SysUserController.update", "更新参数: " + user.toString());
            return Result.success("修改成功");
        } catch (Exception e) {
            return Result.error("修改失败: " + e.getMessage());
        }
    }

    @PutMapping("/changeStatus")
    public Result<String> changeStatus(@RequestBody Map<String, Object> params) {
        try {
            Integer id = (Integer) params.get("id");
            Integer status = (Integer) params.get("status");
            String reason = (String) params.get("reason");

            SysUser user = new SysUser();
            user.setId(id);
            user.setStatus(status);
            userMapper.updateById(user);

            String action = status == 1 ? "解除封禁" : "强制封禁账号";

            // 🌟 极简落库逻辑：有文字直接存文字，没文字存“无”
            String finalReason = StringUtils.hasText(reason) ? reason : "无";
            recordLog("account", "IAM访问安全", action, "SysUserController.changeStatus", finalReason);

            return Result.success("状态已更新");
        } catch (Exception e) {
            return Result.error("状态更新失败");
        }
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam Integer id, @RequestParam(required = false) String reason) {
        if (id != null && id == 1) return Result.error(400, "安全限制：超级管理员 admin 账号禁止删除！");
        SysUser user = userMapper.selectById(id);
        String uname = user != null ? user.getUsername() : String.valueOf(id);

        userMapper.deleteById(id);

        // 🌟 极简落库逻辑：有文字直接存文字，没文字存“无”
        String finalReason = StringUtils.hasText(reason) ? reason : "无";
        recordLog("account", "IAM身份控制", "永久注销了账号: " + uname, "SysUserController.delete", finalReason);

        return Result.success("删除成功");
    }

    // 🌟 核心修复1：完全自动抓取真正的操作人账号！
    private void recordLog(String type, String module, String action, String method, String params) {
        SysLog log = new SysLog();
        log.setLogType(type);

        try {
            // 从 Request Header 中提取前端传入的真实登录人
            String operator = request.getHeader("X-Operator");
            if (operator != null && !operator.isEmpty()) {
                log.setOperator(java.net.URLDecoder.decode(operator, "UTF-8"));
            } else {
                log.setOperator("SYSTEM");
            }
        } catch (Exception e) {
            log.setOperator("SYSTEM");
        }

        log.setModule(module);
        log.setAction(action);
        log.setMethod(method);
        log.setParams(params);
        log.setStatus(1);
        log.setCreateTime(new Date());
        sysLogMapper.insert(log);
    }

    private static String md5Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(s.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte value : b) {
            String hex = Integer.toHexString((value & 0xff));
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}