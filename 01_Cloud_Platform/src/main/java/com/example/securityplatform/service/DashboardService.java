package com.example.securityplatform.service;

import com.example.securityplatform.model.vo.AlertStatsVo;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    AlertStatsVo getDashboardStats();
    List<Map<String, Object>> getRecentAlerts(Integer limit);
}