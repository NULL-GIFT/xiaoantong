package com.example.securityplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.securityplatform.common.Result;
import com.example.securityplatform.mapper.SysLogMapper;
import com.example.securityplatform.model.entity.SysLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/log")
public class SysLogController {

    @Autowired
    private SysLogMapper sysLogMapper;

    /**
     * 解决前端 404 的真实日志查询接口
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getLogList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "15") Integer pageSize,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) Integer status) {

        try {
            Page<SysLog> page = new Page<>(pageNum, pageSize);
            QueryWrapper<SysLog> qw = new QueryWrapper<>();

            // 1. 根据顶部 Tab 切换 (账号日志 / 业务日志)
            if (StringUtils.hasText(logType)) {
                qw.eq("log_type", logType);
            }

            // 2. 根据操作人精准搜索
            if (StringUtils.hasText(operator)) {
                // 如果输入带有 @，先去掉 @ 再查
                String cleanOperator = operator.replace("@", "");
                qw.like("operator", cleanOperator);
            }

            // 3. 根据执行状态筛选 (成功/失败)
            if (status != null) {
                qw.eq("status", status);
            }

            // 4. 按操作时间倒序排列 (最新的日志在最上面)
            qw.orderByDesc("create_time");

            Page<SysLog> resultPage = sysLogMapper.selectPage(page, qw);

            // 封装给前端的数据格式
            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("list", resultPage.getRecords());

            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取日志记录失败: " + e.getMessage());
        }
    }
}