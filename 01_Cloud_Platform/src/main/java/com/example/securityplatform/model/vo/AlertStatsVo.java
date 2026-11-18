package com.example.securityplatform.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class AlertStatsVo {
    // 基础统计
    private Integer todayAlertCount;      // 今日报警总数
    private Integer onlineDeviceCount;    // 在线设备数
    private Integer totalDeviceCount;     // 总设备数
    private Integer unhandledAlertCount;  // 未处理报警数

    // 趋势数据
    private List<Integer> recent7DaysTrend;  // 近7天报警趋势
    private List<String> recent7DaysLabels;  // 对应日期标签

    // 报警类型分布
    private Integer fireAlertCount;       // 火灾报警数
    private Integer smokeAlertCount;      // 烟雾报警数
    private Integer fallAlertCount;       // 跌倒报警数

    // 设备状态分布
    private Integer normalDeviceCount;    // 正常设备数
    private Integer warningDeviceCount;   // 警告设备数
    private Integer errorDeviceCount;     // 故障设备数
}