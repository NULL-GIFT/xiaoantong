# core/mqtt_agent.py
import threading
import time  # 🌟 修复1：绝对不能是 from datetime import time
import paho.mqtt.client as mqtt
import json
import config
import os
import base64
import cv2
import numpy as np
import shutil


class MqttAgent:
    def __init__(self, skill_manager=None):
        self.skill_manager = skill_manager
        self.client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=config.CLIENT_ID)
        will_payload = json.dumps({"device_id": config.DEVICE_ID, "status": 0})
        self.client.will_set(config.TOPIC_STATUS, will_payload, qos=1, retain=True)
        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message

        # 🌟 内存缓存池：用来暂存那些发给云端审核的“暧昧图片”
        self.pending_verifications = {}

    def on_connect(self, client, userdata, flags, rc, properties=None):
        client.subscribe("lwk/device/model_update")
        client.subscribe("lwk/device/false_alarm")
        client.subscribe("lwk/device/suppress")  # 云端盲区指令 (负样本)
        client.subscribe("lwk/device/confirm")  # 云端确认指令 (正样本)
        client.subscribe("lwk/device/vlm_feedback")  # 🌟 新增：监听大模型的最终判决
        if rc == 0:
            print(f"✅ [通信] 已成功连接 MQTT ({config.MQTT_BROKER_IP})")

    def on_message(self, client, userdata, msg):
        """收到 MQTT 消息时的回调"""
        payload = json.loads(msg.payload)

        if msg.topic == "lwk/device/model_update":
            download_url = payload.get("download_url")
            target_version = payload.get("target_version", "latest")
            threading.Thread(target=self.handle_model_update, args=(download_url, target_version)).start()

        elif msg.topic == "lwk/device/false_alarm":
            file_name = payload.get("record_file_name")
            if file_name and self.skill_manager:
                base_dir = self.skill_manager.fed_client.config['data']['local_data_dir']
                target_label = os.path.join(base_dir, "labels", f"{file_name}.txt")
                if os.path.exists(target_label):
                    open(target_label, 'w').close()
                    print(f"👮 [人工纠错] 已将 {file_name} 彻底打入负样本冷宫！")

        elif msg.topic == "lwk/device/suppress":
            track_id = payload.get("track_id")
            bbox = payload.get("bbox")
            ttl = payload.get("ttl", 300)

            if self.skill_manager and self.skill_manager.fire_skill:
                expire_at = time.time() + ttl
                self.skill_manager.fire_skill.suppression_zones.append({
                    "bbox": bbox,
                    "expire_at": expire_at
                })
                print(f"🛑 [云边协同] 收到 VLM 误报反馈，已在坐标 {bbox} 建立 5 分钟的动态盲区！")

                # 提取缓存图片生成空标签 (负样本)
                if track_id in self.pending_verifications:
                    self._save_dataset(track_id, self.pending_verifications[track_id], is_fire=False)
                    del self.pending_verifications[track_id]

        elif msg.topic == "lwk/device/confirm":
            track_id = payload.get("track_id")
            if track_id in self.pending_verifications:
                print(f"🚨 [云边协同] VLM 确认为真火！生成伪标签正样本...")
                self._save_dataset(track_id, self.pending_verifications[track_id], is_fire=True)
                del self.pending_verifications[track_id]







        # 👇 【新增：沙箱清道夫逻辑】处理大模型的最终判决
        elif msg.topic == "lwk/device/vlm_feedback":
            file_name = payload.get("record_file_name")
            is_real_fire = payload.get("is_real_fire")

            if file_name and self.skill_manager and self.skill_manager.fed_client:
                # 获取动态的绝对路径（随便换电脑都不会报错）
                base_dir = self.skill_manager.fed_client.config['data']['local_data_dir']

                # 沙箱路径
                temp_img_path = os.path.join(base_dir, "temp_cache", "images", f"{file_name}.jpg")
                temp_lbl_path = os.path.join(base_dir, "temp_cache", "labels", f"{file_name}.txt")

                # 正式图库路径
                formal_img_path = os.path.join(base_dir, "images", f"{file_name}.jpg")
                formal_lbl_path = os.path.join(base_dir, "labels", f"{file_name}.txt")

                # 只有文件在沙箱里存在，我们才操作
                if os.path.exists(temp_img_path) and os.path.exists(temp_lbl_path):
                    if is_real_fire:
                        # 👑 判决：真火 -> 转正！从沙箱移入正式图库
                        shutil.move(temp_img_path, formal_img_path)
                        shutil.move(temp_lbl_path, formal_lbl_path)
                        print(f"✅ [沙箱转正] 大模型确认真火，文件已移入联邦学习图库 -> {file_name}")

                        # 🌟 终审落锤：云端已自动推送大屏，边缘端只需本地拉响警报，并触发联邦冷却！
                        print(f"🚨🚨🚨 [终审落锤] 云端 VLM 判决为真火！最高级别警报生效！")
                        if self.skill_manager:
                            self.skill_manager.last_alarm_time = time.time()  # 触发 3 分钟联邦静默冷却
                    else:
                        # 🔪 判决：误报 -> 斩首！彻底删除脏数据
                        os.remove(temp_img_path)
                        os.remove(temp_lbl_path)
                        print(f"🗑️ [沙箱清理] 大模型判定误报，已彻底删除脏数据 -> {file_name}")
                else:
                    print(f"⚠️ [沙箱异常] 未找到对应的嫌疑文件，可能已被提前清理 -> {file_name}")










    # 🌟 修复2：将其从 on_message 内部移出来，恢复正确的缩进！
    def handle_model_update(self, download_url, target_version):
        """处理模型更新：下载新模型并热加载"""
        if self.skill_manager:
            self.skill_manager.update_fire_model(model_url=download_url, target_version=target_version)
        else:
            print("❌ 没有 skill_manager 引用，无法更新模型")

    def connect(self):
        try:
            self.client.connect(config.MQTT_BROKER_IP, config.PORT, 60)
            self.client.loop_start()
        except Exception as e:
            print(f"⚠️ 注意: MQTT 连接异常: {e}")

    def publish(self, topic, payload):
        try:
            # 准备发往云端时，如果是暧昧目标，先把全景图和坐标缓存下来
            if topic == config.TOPIC_ALARM and isinstance(payload, dict):
                track_id = payload.get("track_id")
                if track_id and payload.get("img_base64"):
                    self.pending_verifications[track_id] = {
                        "img_base64": payload["img_base64"],
                        "bbox": payload.get("bbox")
                    }
            self.client.publish(topic, json.dumps(payload), qos=1)
        except Exception as e:
            print(f"发送失败: {e}")

    # 将缓存的数据落盘为 YOLO 训练集
    def _save_dataset(self, track_id, cache_data, is_fire):
        if not self.skill_manager or not self.skill_manager.fed_client:
            return

        base_dir = self.skill_manager.fed_client.config['data']['local_data_dir']
        img_dir = os.path.join(base_dir, "images")
        lbl_dir = os.path.join(base_dir, "labels")
        os.makedirs(img_dir, exist_ok=True)
        os.makedirs(lbl_dir, exist_ok=True)

        img_path = os.path.join(img_dir, f"{track_id}.jpg")
        lbl_path = os.path.join(lbl_dir, f"{track_id}.txt")

        try:
            img_bytes = base64.b64decode(cache_data["img_base64"])
            nparr = np.frombuffer(img_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            cv2.imwrite(img_path, img)

            with open(lbl_path, "w") as f:
                if is_fire and cache_data.get("bbox"):
                    x, y, w, h = cache_data["bbox"]
                    h_img, w_img = img.shape[:2]
                    x_center = (x + w / 2.0) / w_img
                    y_center = (y + h / 2.0) / h_img
                    norm_w = w / float(w_img)
                    norm_h = h / float(h_img)
                    f.write(f"0 {x_center:.6f} {y_center:.6f} {norm_w:.6f} {norm_h:.6f}\n")
                else:
                    pass
            print(f"💾 [数据闭环] 自动标注完成！已落盘至数据集 -> ID: {track_id}")
        except Exception as e:
            print(f"❌ 数据集落盘失败: {e}")