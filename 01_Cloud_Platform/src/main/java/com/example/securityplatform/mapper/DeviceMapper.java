package com.example.securityplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.securityplatform.model.entity.SysDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceMapper extends BaseMapper<SysDevice> {

    // 注意：由于继承了BaseMapper，基础CRUD方法已经自动拥有了
    // 不需要再手动定义 insert、update、delete、select等方法

    // 下面只定义特殊的统计查询方法

    /**
     * 统计在线设备数
     * @return 在线设备数量
     */
    Integer countOnlineDevices();

    /**
     * 统计总设备数
     * @return 总设备数量
     */
    Integer countTotalDevices();

    /**
     * 获取设备状态分布
     * @return List<Map<String, Object>> 包含status和count字段
     */
    List<Map<String, Object>> getDeviceStatusDistribution();
}