package com.example.securityplatform.service.impl;

import com.example.securityplatform.mapper.AlarmMapper;
import com.example.securityplatform.model.vo.AlertStatsVo;
import com.example.securityplatform.service.DashboardService;
import com.example.securityplatform.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final AlarmMapper alarmMapper;
    private final DeviceService deviceService;

    @Autowired
    public DashboardServiceImpl(AlarmMapper alarmMapper, DeviceService deviceService) {
        this.alarmMapper = alarmMapper;
        this.deviceService = deviceService;
    }

    @Override
    public AlertStatsVo getDashboardStats() {
        AlertStatsVo stats = new AlertStatsVo();

        // 1. 今日报警总数
        Integer todayCount = alarmMapper.countTodayAlerts();
        stats.setTodayAlertCount(todayCount != null ? todayCount : 0);

        // 2. 在线设备数 - 使用DeviceService
        Long onlineCount = deviceService.countOnlineDevices();
        stats.setOnlineDeviceCount(onlineCount != null ? onlineCount.intValue() : 0);

        // 3. 总设备数 - 使用DeviceService
        Integer totalCount = deviceService.countTotalDevices();
        stats.setTotalDeviceCount(totalCount != null ? totalCount : 0);

        // 4. 未处理报警数
        Integer unhandledCount = alarmMapper.countUnhandledAlerts();
        stats.setUnhandledAlertCount(unhandledCount != null ? unhandledCount : 0);

        // 5. 近7天报警趋势
        List<Map<String, Object>> trendData = alarmMapper.getLast7DaysTrend();
        List<Integer> trendValues = new ArrayList<>();
        List<String> trendLabels = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(formatter);
            trendLabels.add(dateStr);

            // 查找对应日期的数据
            long count = trendData.stream()
                    .filter(map -> dateStr.equals(map.get("date")))
                    .mapToLong(map -> {
                        Object countObj = map.get("count");
                        if (countObj instanceof Number) {
                            return ((Number)countObj).longValue();
                        }
                        return 0L;
                    })
                    .findFirst()
                    .orElse(0L);
            trendValues.add((int)count);
        }

        stats.setRecent7DaysTrend(trendValues);
        stats.setRecent7DaysLabels(trendLabels);

        // 6. 报警类型分布
        List<Map<String, Object>> typeDistribution = alarmMapper.getAlertTypeDistribution();

        // 初始化默认值
        int fireCount = 0;
        int smokeCount = 0;
        int fallCount = 0;

        if (typeDistribution != null) {
            for (Map<String, Object> map : typeDistribution) {
                String type = (String) map.get("type");
                Object countObj = map.get("count");
                int count = 0;

                if (countObj instanceof Number) {
                    count = ((Number)countObj).intValue();
                }

                if ("fire".equals(type)) {
                    fireCount = count;
                } else if ("smoke".equals(type)) {
                    smokeCount = count;
                } else if ("fall".equals(type)) {
                    fallCount = count;
                }
            }
        }

        stats.setFireAlertCount(fireCount);
        stats.setSmokeAlertCount(smokeCount);
        stats.setFallAlertCount(fallCount);

        // 7. 设备状态分布 - 使用DeviceService
        List<Map<String, Object>> deviceStatus = deviceService.getDeviceStatusDistribution();

        // 初始化默认值
        int normalCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        if (deviceStatus != null) {
            for (Map<String, Object> map : deviceStatus) {
                String status = (String) map.get("status");
                Object countObj = map.get("count");
                int count = 0;

                if (countObj instanceof Number) {
                    count = ((Number)countObj).intValue();
                }

                if ("normal".equals(status)) {
                    normalCount = count;
                } else if ("warning".equals(status)) {
                    warningCount = count;
                } else if ("error".equals(status)) {
                    errorCount = count;
                }
            }
        }

        stats.setNormalDeviceCount(normalCount);
        stats.setWarningDeviceCount(warningCount);
        stats.setErrorDeviceCount(errorCount);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getRecentAlerts(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return alarmMapper.getRecentAlerts(limit);
    }
}