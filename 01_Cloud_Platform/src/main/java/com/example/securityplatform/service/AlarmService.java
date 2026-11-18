package com.example.securityplatform.service;

import com.example.securityplatform.model.dto.AlarmUploadDTO;

public interface AlarmService {
    /**
     * 保存报警记录
     * @param alarmDTO 报警数据
     */
    void saveAlarm(AlarmUploadDTO alarmDTO);
}
