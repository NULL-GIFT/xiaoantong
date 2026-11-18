{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "device_id": "box_001",
      "device_ip": "192.168.137.2",
      "status": 1,                  // 1: 在线, 0: 离线
      "last_active_time": "2026-02-05 10:00:00",
      "firmware_version": "v1.0"
    },
    {
      "id": 2,
      "device_id": "box_002",
      "device_ip": "192.168.137.3",
      "status": 0,                  // 超过 30秒 没发心跳，后端自动判为离线
      "last_active_time": "2026-02-05 09:00:00",
      "firmware_version": "v1.0"
    }
  ]
}