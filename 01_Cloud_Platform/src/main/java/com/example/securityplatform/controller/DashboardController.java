package com.example.securityplatform.controller;

import com.example.securityplatform.common.Result;
import com.example.securityplatform.model.vo.AlertStatsVo;
import com.example.securityplatform.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取大屏统计数据
     */
    @GetMapping("/statistics")
    public Result<AlertStatsVo> getDashboardStatistics() {
        AlertStatsVo stats = dashboardService.getDashboardStats();
        return Result.success(stats);
    }

    /**
     * 获取实时报警（最近10条）
     */
    @GetMapping("/recent-alerts")
    public Result<?> getRecentAlerts() {
        return Result.success(dashboardService.getRecentAlerts(10));
    }
}