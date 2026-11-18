package com.example.securityplatform.service;

import com.example.securityplatform.model.vo.AlertStatsVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void testGetDashboardStatistics() {
        // 1. 调用接口
        AlertStatsVo stats = dashboardService.getDashboardStats();

        // 2. 验证返回数据不为空
        assertNotNull(stats, "统计数据不应为空");

        // 3. 验证基础字段存在
        assertNotNull(stats.getTodayAlertCount(), "今日报警数不应为空");
        assertNotNull(stats.getOnlineDeviceCount(), "在线设备数不应为空");
        assertNotNull(stats.getTotalDeviceCount(), "总设备数不应为空");

        // 4. 验证数据逻辑正确性
        assertTrue(stats.getOnlineDeviceCount() <= stats.getTotalDeviceCount(),
                "在线设备数不应超过总设备数");

        // 5. 验证趋势数据
        assertNotNull(stats.getRecent7DaysTrend(), "7天趋势数据不应为空");
        assertNotNull(stats.getRecent7DaysLabels(), "7天标签数据不应为空");
        assertEquals(7, stats.getRecent7DaysTrend().size(), "应有7天趋势数据");
        assertEquals(7, stats.getRecent7DaysLabels().size(), "应有7天标签数据");

        System.out.println("今日报警数: " + stats.getTodayAlertCount());
        System.out.println("在线设备数: " + stats.getOnlineDeviceCount());
        System.out.println("7天趋势: " + stats.getRecent7DaysTrend());
    }

    @Test
    void testGetRecentAlerts() {
        // 测试获取最近报警
        var recentAlerts = dashboardService.getRecentAlerts(5);
        assertNotNull(recentAlerts, "最近报警列表不应为空");
        assertTrue(recentAlerts.size() <= 5, "返回的报警数量不应超过限制");
    }
}