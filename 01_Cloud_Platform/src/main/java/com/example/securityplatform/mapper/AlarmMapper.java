package com.example.securityplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.securityplatform.model.entity.SysAlarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AlarmMapper extends BaseMapper<SysAlarm> {

    // 注意：这里不需要再定义 insert 方法，因为 BaseMapper 已经提供了

    // 只定义自定义的统计方法
    Integer countTodayAlerts();
    Integer countUnhandledAlerts();
    List<Map<String, Object>> getLast7DaysTrend();
    List<Map<String, Object>> getAlertTypeDistribution();
    List<Map<String, Object>> getRecentAlerts(@Param("limit") Integer limit);
}