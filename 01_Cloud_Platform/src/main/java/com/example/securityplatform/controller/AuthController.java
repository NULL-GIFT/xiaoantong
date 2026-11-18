package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.mapper.SysUserMapper;
import com.example.securityplatform.model.entity.SysUser;
import com.example.securityplatform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        try {
            if (req == null || req.username == null || req.password == null) {
                return Result.error(400, "缺少用户名或密码");
            }
            QueryWrapper<SysUser> qw = new QueryWrapper<>();
            qw.eq("username", req.username);
            SysUser user = userMapper.selectOne(qw);
            if (user == null || user.getStatus() != 1) {
                return Result.error(401, "用户不存在或已禁用");
            }
            // 🌟🌟🌟 加入照妖镜代码，打印密码明细 🌟🌟🌟
            System.out.println("==== 登录验证开始 ====");
            System.out.println("1. 前端传来的明文密码: [" + req.password + "]");
            String md5 = md5Hex(req.password);
            System.out.println("2. 将明文加密后的MD5: [" + md5 + "]");
            System.out.println("3. 数据库里存的MD5: [" + user.getPassword() + "]");
            System.out.println("4. 密码是否匹配: " + md5.equalsIgnoreCase(user.getPassword()));
            System.out.println("======================");

            if (!md5.equalsIgnoreCase(user.getPassword())) {
                return Result.error(401, "用户名或密码错误");
            }
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", user.getUsername());
            claims.put("real_name", user.getRealName());
            claims.put("role", user.getRole());
            String token = jwtUtil.generateToken(claims);
            Map<String, Object> resp = new HashMap<>();
            resp.put("token", token);
            resp.put("username", user.getUsername());
            resp.put("real_name", user.getRealName());
            resp.put("role", user.getRole());
            resp.put("status", user.getStatus());
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("ok");
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
    /**
     * 🌟 真实且安全的修改密码接口 (自带 MD5 和新旧密码防重复校验)
     */
    @PostMapping("/updatePwd")
    public Result<?> updatePassword(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String oldPwd = params.get("oldPwd");
            String newPwd = params.get("newPwd");

            if (username == null || oldPwd == null || newPwd == null) {
                return Result.error(400, "参数缺失");
            }

            // 🌟 1. 新增业务判断：新密码不能与原密码相同
            if (oldPwd.equals(newPwd)) {
                return Result.error(400, "新密码不能与原密码相同！");
            }

            // 2. 根据前端传来的账号找用户
            QueryWrapper<SysUser> qw = new QueryWrapper<>();
            qw.eq("username", username);
            SysUser user = userMapper.selectOne(qw); // 使用你代码里已有的 userMapper

            if (user == null) {
                return Result.error(404, "用户不存在！");
            }

            // 3. 校验原密码 (直接复用你 AuthController 里的 md5Hex 方法)
            String oldPwdMd5 = md5Hex(oldPwd);
            if (!oldPwdMd5.equalsIgnoreCase(user.getPassword())) {
                return Result.error(400, "原密码输入错误，请重试！");
            }

            // 4. 将新密码 MD5 加密后，更新到数据库
            String newPwdMd5 = md5Hex(newPwd);
            user.setPassword(newPwdMd5);

            // 🌟 核心修复：确保真正执行 UPDATE SQL 语句
            int rows = userMapper.updateById(user);

            if (rows > 0) {
                return Result.success("密码修改成功");
            } else {
                return Result.error(500, "数据库更新失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "修改密码异常: " + e.getMessage());
        }
    }
    /**
     * 🌟 新增：普通用户个人中心 - 更新自己的基础资料 (目前主要是真实姓名)
     */
    @PostMapping("/updateProfile")
    public Result<?> updateProfile(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String realName = params.get("realName");

            if (username == null || realName == null) {
                return Result.error(400, "参数缺失，无法更新");
            }

            // 1. 找到当前用户
            QueryWrapper<SysUser> qw = new QueryWrapper<>();
            qw.eq("username", username);
            SysUser user = userMapper.selectOne(qw);

            if (user == null) {
                return Result.error(404, "当前操作员账户不存在！");
            }

            // 2. 只更新被允许更新的字段
            user.setRealName(realName);
            int rows = userMapper.updateById(user);

            if (rows > 0) {
                return Result.success("个人基础资料更新成功");
            } else {
                return Result.error(500, "数据库同步失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "资料更新异常: " + e.getMessage());
        }
    }
}
