# boot.py
import threading
from core.mqtt_agent import MqttAgent
from core.web_stream import start_flask
from core.skill_manager import SkillManager

if __name__ == '__main__':
    # 1. 启动 Web 视频流服务
    threading.Thread(target=start_flask, daemon=True).start()

    # 2. 先创建 MQTT 代理（此时 skill_manager 为 None）
    mqtt_agent = MqttAgent()          # 因为 __init__ 已添加默认参数 skill_manager=None
    mqtt_agent.connect()

    # 3. 创建技能管理器（需要传入 mqtt_agent，用于发送报警、心跳等）
    manager = SkillManager(mqtt_agent)

    # 4. 将技能管理器设置给 MQTT 代理，使 MQTT 能调用 update_fire_model
    mqtt_agent.skill_manager = manager

    # 5. 启动主循环
    manager.start()