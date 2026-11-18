# core/skill_manager.py
import cv2
import time
import config
from core.web_stream import update_frame
from skills.fire_detect.skill import FireDetectSkill
import os
from datetime import datetime
import threading
import time
import requests
from federated.client import FederatedClient
from edge_utils.img_utils import compute_iou,image_to_base64




class SkillManager:
    # def __init__(self, mqtt_agent):
    #     self.mqtt_agent = mqtt_agent
    #     self.fire_skill = FireDetectSkill()
    #     self.cap = cv2.VideoCapture(0)
    #     self.last_hb_time = 0
    #     self.last_alarm_time = 0
    #     # 👇【新增】嫌疑犯历史记录本，格式：{"ID或序号": {"box": [x,y,w,h], "first_seen": timestamp}}
    #     self.suspect_history = []
    #     # 0.45-0.6缓存区
    #     self.pending_labels = {}
    #     # 联邦配置
    #     self.fed_client = FederatedClient(self._load_fed_config(),skill_manager=self)  # 需要加载联邦配置
    #
    #     # 👇 【新增：阶段一】读取模型档案，获取当前 AI 模型版本
    #     self.current_model_version = self._load_model_manifest()
    #
    #     self.fed_thread = threading.Thread(target=self._fed_loop, daemon=True)
    #     self.fed_thread.start()

    def __init__(self, mqtt_agent):
        self.mqtt_agent = mqtt_agent

        # 👇【引导优化 1】：优先初始化联邦客户端（因为后面查档案需要用到它提供的绝对路径 project_root）
        self.fed_client = FederatedClient(self._load_fed_config(), skill_manager=self)

        # 👇【引导优化 2】：第二步立刻去查阅物理硬盘上的 manifest.json 档案！
        self.current_model_version = self._load_model_manifest()

        # 👇【引导优化 3】：动态拼装出真正存在于硬盘上的模型文件名
        # 如果档案里写着 V_1774529434，这里就会拼成 model_V_1774529434.onnx
        # 如果是首次启动没更新过，就会默认拼成 model_V1.0.onnx (请确保你的初始文件叫这个名字)
        dynamic_model_name = f"model_{self.current_model_version}.onnx"

        if self.current_model_version == "V1.0" and not os.path.exists(
                os.path.join(self.fed_client.project_root, "skills", "fire_detect", "model_V1.0.onnx")):
            # 为了兼容你现有的 model_v1.onnx 初始文件，做一个小小的容错
            dynamic_model_name = "model_v1.onnx"

        print(f"🔄 [动态引导] 系统冷启动，按图索骥加载模型: {dynamic_model_name}")

        # 👇【引导优化 4】：拿着真正存在的名字，去实例化 AI 大脑！
        self.fire_skill = FireDetectSkill(model_name=dynamic_model_name)

        # === 以下是常规的基础变量初始化 ===
        self.cap = cv2.VideoCapture(0)
        self.last_hb_time = 0
        self.last_alarm_time = 0
        # 嫌疑犯历史记录本
        self.suspect_history = []
        # 0.45-0.6缓存区
        self.pending_labels = {}

        # 启动后台监控线程
        self.fed_thread = threading.Thread(target=self._fed_loop, daemon=True)
        self.fed_thread.start()




    def _load_fed_config(self):
        """加载联邦学习配置（可以从configs/下的yaml读取）"""
        import yaml
        with open("federated/configs/fire.yaml", 'r', encoding='utf-8') as f:
            model_cfg = yaml.safe_load(f)
        with open("federated/configs/base.yaml", 'r', encoding='utf-8') as f:
            base_cfg = yaml.safe_load(f)
        # 合并配置（可复用demo.py中的merge_config函数）
        from federated.demo import merge_config
        return merge_config(base_cfg, model_cfg)




    def _load_model_manifest(self):
        """【新增：阶段一】读取或初始化模型版本档案"""
        import json
        from datetime import datetime

        # 使用物理根目录，告别相对路径报错
        manifest_path = os.path.join(self.fed_client.project_root, "skills", "fire_detect", "manifest.json")

        # 如果没有档案（首次部署），自动生成初始胎记
        if not os.path.exists(manifest_path):
            default_manifest = {
                "model_name": "fire_detect",
                "current_version": "V1.0",
                "update_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            os.makedirs(os.path.dirname(manifest_path), exist_ok=True)
            try:
                with open(manifest_path, 'w', encoding='utf-8') as f:
                    json.dump(default_manifest, f, indent=4)
                print(f"📄 [元数据] 首次运行，已自动生成初始版本档案: {manifest_path}")
            except Exception as e:
                print(f"⚠️ [元数据] 档案生成失败: {e}")
            return "V1.0"

        # 如果有档案，读取里面的版本号
        try:
            with open(manifest_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                version = data.get("current_version", "V1.0")
                print(f"📄 [元数据] 成功加载当前 AI 模型版本: {version}")
                return version
        except Exception as e:
            print(f"⚠️ [元数据] 读取 manifest.json 失败: {e}，默认降级使用 V1.0")
            return "V1.0"





    def _fed_loop(self):
        """后台线程：每6000秒检查一次是否可训练"""
        while True:
            time.sleep(300)  # 检查间隔可调
            current_time = time.time()

            # 👇 核心：距离上一次报警必须超过1800秒，确保 GPU 处于纯推理的低负载闲置状态
            if current_time - self.last_alarm_time > 1800:
                self.fed_client.check_and_train()
            else:
                print("[GPU 保护]刚发生过警情，暂缓联邦训练以防显存溢出...")






    def start(self):
        print(f"🚀 系统启动中 (请打开浏览器访问 http://127.0.0.1:5000/video_feed)...")
        while True:
            ret, frame = self.cap.read()
            if not ret: break

            # 1. 调用技能进行推理
            is_alarm, alarm_data, show_frame = self.fire_skill.run(frame)

            # 2. 将画面推给网页
            update_frame(show_frame)


            # 3. 报警逻辑
            current_time = time.time()
            if is_alarm and alarm_data and (current_time - self.last_alarm_time >= 5):
                base_dir = self.fed_client.config['data']['local_data_dir']
                img_save_dir = os.path.join(base_dir, "images")
                lbl_save_dir = os.path.join(base_dir, "labels")

                # 👇 【新增：沙箱隔离区目录】
                temp_img_dir = os.path.join(base_dir, "temp_cache", "images")
                temp_lbl_dir = os.path.join(base_dir, "temp_cache", "labels")
                # 一次性建好 4 个文件夹
                for d in [img_save_dir, lbl_save_dir, temp_img_dir, temp_lbl_dir]:
                    os.makedirs(d, exist_ok=True)

                os.makedirs(img_save_dir, exist_ok=True)
                os.makedirs(lbl_save_dir, exist_ok=True)

                # 生成专属“身份证”
                timestamp_str = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]
                file_name = f"fire_{timestamp_str}"


                boxes = alarm_data.get('boxes', [])
                ids = alarm_data.get('ids', [])
                confs = alarm_data.get('confs', [])
                types = alarm_data.get('types', [])  # 获取工业标签类型




                img_h, img_w = show_frame.shape[:2]
                write_flag = False
                send_alarm_flag = False  # 🤫 新增：是否需要惊动保安的开关
                request_vlm_flag = False  # 🌟 新增：是否需要悄悄请示云端
                label_content = []
                # 👇 【新增：判断这张图是否需要进沙箱隔离】
                is_suspect_for_sandbox = False

                # 🔥 【核心分类逻辑】
                for i in range(len(boxes)):
                    b_type = types[i]
                    conf = confs[i]
                    x, y, w, h = boxes[i]
                    cls_id = ids[i]

                    x_center, y_center = (x + w / 2) / img_w, (y + h / 2) / img_h
                    n_w, n_h = w / img_w, h / img_h
                    line = f"{cls_id} {x_center:.6f} {y_center:.6f} {n_w:.6f} {n_h:.6f}\n"

                    if b_type == "hard_negative":
                        write_flag = True
                        label_content = []
                        send_alarm_flag = False  # 默默消化
                        print("🚫 [负样本挖掘] 检测到静止误报光斑，生成空白错题本！")
                        break

                    elif b_type == "pseudo_label":
                        write_flag = True
                        label_content.append(line)
                        send_alarm_flag = True  # 报警
                        is_suspect_for_sandbox = True  # 👈 【新增】伪标签必须进沙箱！
                        print("🎯 [伪标签生成] 捕获复杂形变火焰！")

                    elif b_type == "normal":
                        if 0.45 <= conf <= 0.8:
                            current_box = boxes[i]
                            is_static_dead_object = False

                            self.suspect_history = [s for s in self.suspect_history if
                                                    current_time - s["first_seen"] < 3.0]

                            matched_old_suspect = False
                            for suspect in self.suspect_history:
                                iou = compute_iou(current_box, suspect["box"])
                                if iou > 0.85:
                                    matched_old_suspect = True
                                    if current_time - suspect["first_seen"] > 1.5:
                                        is_static_dead_object = True
                                    break

                            if is_static_dead_object:
                                write_flag = True
                                label_content = []
                                send_alarm_flag = False  # 🤫 核心：默默消化死物，绝不打扰保安
                                print(f"🚫 [时序过滤] 发现静止死物 (置信度 {conf:.2f})！已生成空标签默默消化。")
                                break
                            else:
                                if not matched_old_suspect:
                                    self.suspect_history.append({"box": current_box, "first_seen": current_time})

                                # 👇 【极其清爽的 Opt-out 逻辑】
                                write_flag = True
                                label_content.append(line)  # 统统当真火存下
                                send_alarm_flag = False  # 🤫 没收报警权！绝不惊动本地和冷却机制
                                request_vlm_flag = True  # 🌟 赋予提审权：悄悄发给大模型
                                is_suspect_for_sandbox = True  # 👈 【新增】暧昧目标必须进沙箱！
                                print(f"🕵️ [静默提审] 捕获嫌疑目标 (置信度 {conf:.2f})，不拉警报，关入沙箱提请终审！")
                        else:
                            write_flag = True
                            label_content.append(line)
                            send_alarm_flag = True  # 学霸题必须报警
                            print(f"⏩ [学霸题] 置信度 {conf:.2f} 极高，保存正样本并报警！")

                # 👇 落盘与报警分离逻辑
                # 👇 动态路由落盘与报警分离逻辑
                if write_flag:
                    # 判决分流：是去正式服，还是去沙箱服？
                    if is_suspect_for_sandbox:
                        final_img_path = os.path.join(temp_img_dir, f"{file_name}.jpg")
                        final_lbl_path = os.path.join(temp_lbl_dir, f"{file_name}.txt")
                        print(f"📦 [沙箱隔离] 嫌疑图片已暂存至 temp_cache 等待云端判决 -> {file_name}")
                    else:
                        final_img_path = os.path.join(img_save_dir, f"{file_name}.jpg")
                        final_lbl_path = os.path.join(lbl_save_dir, f"{file_name}.txt")
                        print(f"💾 [数据闭环] 确认为学霸题/负样本，直接落盘至正式图库 -> {file_name}")

                    # 写入对应的文件夹
                    cv2.imwrite(final_img_path, show_frame)
                    with open(final_lbl_path, 'w') as f:
                        f.writelines(label_content)

                    # 🔥 确定的真火报警，或者 暧昧目标的静默提审，都需要上报云端！
                    if send_alarm_flag or request_vlm_flag:
                        alarm_payload = {
                            "device_id": config.DEVICE_ID,
                            "type": "fire",
                            "timestamp": int(current_time * 1000),
                            "track_id": file_name,
                            "img_base64": image_to_base64(show_frame),
                            "confs": confs,
                            "types": types,
                            "bbox": boxes[0] if len(boxes) > 0 else [],
                            "event": alarm_data,
                            "record_file_name": file_name
                        }
                        self.mqtt_agent.publish(config.TOPIC_ALARM, alarm_payload)

                        if send_alarm_flag:
                            # 只有真正的学霸题真火，才触发本地的 3 分钟 GPU 联邦冷却
                            self.last_alarm_time = current_time


            # 4. 心跳逻辑
            if current_time - self.last_hb_time >= 90:
                hb_msg = {
                    "device_id": config.DEVICE_ID,
                    "device_ip": "192.168.192.195",
                    "status": 1,
                    "version": config.VERSION,
                    "current_model_version": self.current_model_version,  # 👇【新增：阶段一】AI模型版本
                    "timestamp": int(current_time * 1000)
                }
                self.mqtt_agent.publish(config.TOPIC_HEARTBEAT, hb_msg)
                self.last_hb_time = current_time

            # 5. 本地预览 (可选，可注释)
            cv2.imshow("Campus Safety Edge", show_frame)
            if cv2.waitKey(1) & 0xFF == ord('q'): break

        self.cap.release()
        cv2.destroyAllWindows()

    def update_fire_model(self, model_url=None, local_path=None, target_version="latest"):
        """【阶段三：工业级双缓冲无损热加载】从指定URL下载最新模型并瞬间切换大脑"""
        import shutil
        import json

        try:
            skill_dir = os.path.join(self.fed_client.project_root, "skills", "fire_detect")

            # 👇 1. 明确新旧文件的身份
            # 旧文件通常是 model_v1.onnx，新文件我们给它加上版本号名字，比如 model_V1.2.onnx
            if target_version == "latest":
                target_version = f"V_{int(time.time())}"  # 如果云端没给名字，就用时间戳防重复

            new_model_filename = f"model_{target_version}.onnx"
            new_model_path = os.path.join(skill_dir, new_model_filename)

            # 👇 2. 获取新模型文件 (从本地复制，或者从云端下载)
            if local_path is not None:
                if not os.path.exists(local_path):
                    print(f"❌ [热更新] 本地生成的模型不存在: {local_path}")
                    return
                print(f"📁 [热更新] 准备应用本地融合的新模型: {local_path}")
                # 复制为带版本号的新文件
                shutil.copy2(local_path, new_model_path)

            elif model_url is not None:
                print(f"⬇️ [热更新] 开始从云端下载新模型: {model_url}")
                r = requests.get(model_url, timeout=60)
                if r.status_code == 200:
                    with open(new_model_path, 'wb') as f:
                        f.write(r.content)
                    print(f"✅ [热更新] 模型下载成功: {new_model_filename}")
                else:
                    print(f"❌ [热更新] 下载失败，状态码：{r.status_code}")
                    return
            else:
                print("❌ [热更新] 既没有提供 local_path 也没有提供 model_url")
                return

            # 👇 3. 【核心：旁路实例化】绝对不要动当前正在运行的 fire_skill！
            print(f"🧠 [热更新] 正在后台秘密孵化新大脑 ({target_version})...")
            # 临时修改类变量指向新文件，或者直接实例化
            # 为了防止干扰旧实例，我们用一套干净的环境测试它
            try:
                # 假设 FireDetectSkill 内部会去读它目录下的文件，我们需要稍微改一下 FireDetectSkill 让它支持传参
                # 但如果不改 skill.py，最稳妥的办法是：把新文件重命名为一个约定好的 "次要替身名字"，测试完再切。
                # 既然是双缓冲，我们直接实例化！

                # 【前置条件】：你需要确保 skills/fire_detect/skill.py 里的 __init__ 能接收指定的模型路径。
                # （这里假设你稍微改了下 skill.py，允许传入 model_path 参数）
                new_skill_instance = FireDetectSkill(model_name=new_model_filename)

                # 随便喂一张黑图给它测试一下，看看 ONNX 会不会报错
                import numpy as np
                dummy_frame = np.zeros((480, 640, 3), dtype=np.uint8)
                new_skill_instance.run(dummy_frame)
                print("✅ [热更新] 新大脑测试推理成功，证明算子兼容！")

            except Exception as e:
                print(f"🛑 [安全熔断] 新大脑 ({target_version}) 孵化失败或崩溃: {e}")
                print("🛡️ [安全熔断] 已放弃更新，继续使用原稳定版本运行！")
                if os.path.exists(new_model_path):
                    os.remove(new_model_path)  # 删掉报错的垃圾文件
                return

            # 👇 4. 【原子级指针切换】
            print(f"⚡ [热更新] 准备进行瞬间脑机接口切换...")
            # 因为 Python 的 GIL 特性，引用赋值是原子的，这一瞬间画面绝对不会卡！
            old_skill_instance = self.fire_skill
            self.fire_skill = new_skill_instance

            # 👇 5. 【术后清理与档案刷新】
            # 更新内存里的版本号
            self.current_model_version = target_version

            # 覆写 manifest.json
            manifest_path = os.path.join(skill_dir, "manifest.json")
            try:
                with open(manifest_path, 'r', encoding='utf-8') as f:
                    manifest_data = json.load(f)
            except:
                manifest_data = {}

            manifest_data['current_version'] = target_version
            from datetime import datetime
            manifest_data['update_time'] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            with open(manifest_path, 'w', encoding='utf-8') as f:
                json.dump(manifest_data, f, indent=4)

            print(f"🎉 [工业闭环] 档案已刷新，当前系统运行在 {target_version} 版本！")

            # 尝试删除旧的模型文件释放空间 (只保留最新的)
            # 注意：在 Windows 上，ONNX 即使不再使用，也可能有一段时间被进程锁定
            # 所以我们用 try-except 包围，删不掉就算了，不影响主流程。
            print("🧹 [热更新] 尝试清理旧版模型文件...")
            for f in os.listdir(skill_dir):
                if f.endswith('.onnx') and f != new_model_filename:
                    try:
                        os.remove(os.path.join(skill_dir, f))
                        print(f"   -> 已清理旧文件: {f}")
                    except:
                        pass  # 删不掉就留着下次删

        except Exception as e:
            print(f"❌ [热更新] 发生严重异常，热替换中止: {e}")