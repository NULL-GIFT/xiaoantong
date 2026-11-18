# config.py
# --- IP与网络配置 ---
# MQTT服务
# MQTT_BROKER_IP = "broker.emqx.io"
# MQTT_BROKER_IP = "192.168.43.20"
MQTT_BROKER_IP = "127.0.0.1"
#云端IP
CLOUD_IP = "127.0.0.1"
PORT = 1883




# --- 设备基础信息 ---


# config.py
import os

def _get_device_id():
    # 优先从环境变量读取
    env_id = os.getenv("DEVICE_ID")
    if env_id:
        return env_id
    # 其次尝试从本地文件读取
    try:
        with open("device_id.txt", "r") as f:
            return f.read().strip()
    except:
        # 最后 fallback 到默认值（也可以基于 MAC 生成）
        return "box_001"

DEVICE_ID = _get_device_id()
CLIENT_ID = f"edge-{DEVICE_ID}"
VERSION = "v1.1"
LOCATION = "Laboratory_A"

# --- MQTT 主题配置 ---
TOPIC_ALARM = "lwk/device/alarm"
TOPIC_HEARTBEAT = "lwk/device/heartbeat"
TOPIC_STATUS = "lwk/device/status"




