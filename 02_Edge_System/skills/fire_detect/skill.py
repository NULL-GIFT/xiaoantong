# skills/fire_detect/skill.py
import cv2
import numpy as np
import onnxruntime as ort
import os
import time  # 🌟 修复1：必须用标准库的 time，不能用 datetime 里的 time！

from skills.base_skill import BaseSkill
from edge_utils.img_utils import image_to_base64,compute_iou


class FireDetectSkill(BaseSkill):
    def __init__(self, conf_thres=0.45, iou_thres=0.45, model_name="model_v1.onnx"):
        # 动态获取当前脚本目录，加载模型
        current_dir = os.path.dirname(os.path.abspath(__file__))
        model_path = os.path.join(current_dir, model_name)

        try:
            # 🔥 释放全功率：优先使用显卡 CUDA，如果没有再退化到 CPU
            self.session = ort.InferenceSession(model_path, providers=['CUDAExecutionProvider', 'CPUExecutionProvider'])
            print(f"✅ [AI技能] 火焰检测模型成功装载 (已开启 GPU 加速): {model_path}")
        except Exception as e:
            print(f"❌ [AI技能] 模型加载失败: {e}")
            exit()

        # 🔥 【工业级新增】时序追踪与背景建模初始化
        self.frame_buffer = []  # 存最近15帧的灰度图
        self.tracker = None  # OpenCV 追踪器
        self.tracking_active = False

        # 👇 冷却池与动态盲区
        self.verifying_targets = {}
        self.suppression_zones = []

        self.input_name = self.session.get_inputs()[0].name
        self.net_w, self.net_h = 640, 640
        self.conf_thres = conf_thres
        self.iou_thres = iou_thres
        self.classes = {0: 'Fire', 1: 'Smoke'}
        self.colors = {0: (0, 0, 255), 1: (0, 255, 255)}

    def preprocess(self, frame):
        self.img_h, self.img_w = frame.shape[:2]
        img = cv2.resize(frame, (self.net_w, self.net_h))
        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img = img.astype(np.float32) / 255.0
        img = img.transpose(2, 0, 1)
        input_data = np.expand_dims(img, axis=0)

        # 动态探测 ONNX 躯壳需要什么精度的子弹
        expected_type = self.session.get_inputs()[0].type
        if 'float16' in expected_type or 'float16' == expected_type:
            input_data = input_data.astype(np.float16)

        return input_data

    def run(self, frame):
        # 1. 更新时序帧缓存
        gray_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        self.frame_buffer.append(gray_frame)
        if len(self.frame_buffer) > 15:
            self.frame_buffer.pop(0)

        input_data = self.preprocess(frame)
        outputs = self.session.run(None, {self.input_name: input_data})
        preds = np.squeeze(outputs[0])

        boxes, confs, ids = [], [], []
        x_ratio = self.img_w / self.net_w
        y_ratio = self.img_h / self.net_h

        # 解析 YOLO 预测
        for row in preds:
            obj_conf = row[4]
            if obj_conf > self.conf_thres:
                cls_scores = row[5:]
                cls_id = np.argmax(cls_scores)
                final_conf = obj_conf * cls_scores[cls_id]
                if final_conf > self.conf_thres:
                    cx, cy, rw, rh = row[0], row[1], row[2], row[3]
                    lx = max(0, int((cx - rw / 2) * x_ratio))
                    ly = max(0, int((cy - rh / 2) * y_ratio))
                    lw = int(rw * x_ratio)
                    lh = int(rh * y_ratio)
                    boxes.append([lx, ly, lw, lh])
                    confs.append(float(final_conf))
                    ids.append(int(cls_id))

        indices = cv2.dnn.NMSBoxes(boxes, confs, self.conf_thres, self.iou_thres)

        is_alarm = False
        show_frame = frame.copy()
        boxes_list, ids_list, confs_list, types_list = [], [], [], []

        best_yolo_conf = 0
        best_yolo_box = None

        # 🌟 预留 VLM 专属变量（确保组装 JSON 时不会未定义）
        vlm_crop_base64 = None
        current_track_id = None
        current_vlm_bbox = None

        # ==========================================
        # 核心逻辑一：处理 YOLO 检测框
        # ==========================================
        if len(indices) > 0:
            for i in indices.flatten():
                x, y, bw, bh = boxes[i]
                cid = ids[i]
                conf = confs[i]

                # 越界保护
                x, y = max(0, x), max(0, y)
                bw = min(bw, self.img_w - x)
                bh = min(bh, self.img_h - y)
                current_box = [x, y, bw, bh]

                # 🌟 1. 【盲区拦截】无论什么置信度，先查黑名单！
                is_suppressed = False
                for zone in self.suppression_zones:
                    if time.time() > zone["expire_at"]:
                        continue  # 盲区已过期
                    if compute_iou(current_box, zone["bbox"]) > 0.5:
                        is_suppressed = True
                        break

                if is_suppressed:
                    # 如果命中盲区，直接忽略这个框！不画框、不上报！
                    continue

                    # 记录最高分框，用于初始化追踪器
                if conf > best_yolo_conf and cid == 0:  # 假设 0 是火
                    best_yolo_conf = conf
                    best_yolo_box = current_box

                is_alarm = True  # 经过了盲区考验，确认为报警目标
                sample_type = "normal"

                # 🌟 2. 【大模型暧昧区处理：局部裁图送审】
                if 0.45 < conf <= 0.8 and cid == 0:
                    track_id = f"trk_{x}_{y}"
                    if track_id not in self.verifying_targets or time.time() > self.verifying_targets[track_id]:
                        self.verifying_targets[track_id] = time.time() + 10  # 冷却锁定 10 秒

                        print(f"📤 [边缘端] 发现暧昧目标 (conf={conf:.2f})，截取局部图...")
                        pad_x, pad_y = int(bw * 0.2), int(bh * 0.2)
                        y1 = max(0, y - pad_y)
                        y2 = min(self.img_h, y + bh + pad_y)
                        x1 = max(0, x - pad_x)
                        x2 = min(self.img_w, x + bw + pad_x)
                        crop_img = frame[y1:y2, x1:x2]

                        vlm_crop_base64 = image_to_base64(crop_img)
                        current_track_id = track_id
                        current_vlm_bbox = current_box

                # 【困难负样本挖掘 - 帧差静止检测】
                if conf > 0.8 and len(self.frame_buffer) == 15:
                    roi_old = self.frame_buffer[0][y:y + bh, x:x + bw]
                    roi_new = self.frame_buffer[-1][y:y + bh, x:x + bw]
                    if roi_old.size > 0 and roi_new.size > 0:
                        diff = cv2.absdiff(roi_old, roi_new)
                        if np.mean(diff) < 3.0:
                            sample_type = "hard_negative"
                            cv2.rectangle(show_frame, (x, y), (x + bw, y + bh), (255, 0, 0), 2)
                            cv2.putText(show_frame, "FALSE_ALARM", (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 0, 0), 2)
                            boxes_list.append(current_box)
                            ids_list.append(cid)
                            confs_list.append(conf)
                            types_list.append(sample_type)
                            continue

                            # 常规画框
                label = self.classes.get(cid, "Unknown")
                color = self.colors.get(cid, (0, 0, 255))

                if sample_type == "normal" and 0.45 < conf <= 0.7:
                    color = (0, 255, 255)  # 嫌疑犯降级为黄色
                    label = "Suspect"  # 标签改为嫌疑审核中

                cv2.rectangle(show_frame, (x, y), (x + bw, y + bh), color, 2)
                cv2.putText(show_frame, f"{label}", (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

                boxes_list.append(current_box)
                ids_list.append(cid)
                confs_list.append(conf)
                types_list.append(sample_type)

        # ==========================================
        # 核心逻辑二：处理追踪器 (如果 YOLO 丢帧)
        # ==========================================
        if best_yolo_conf > 0.85 and best_yolo_box is not None:
            try:
                self.tracker = cv2.TrackerCSRT_create()
            except:
                self.tracker = cv2.TrackerKCF_create()
            self.tracker.init(frame, tuple(best_yolo_box))
            self.tracking_active = True
        elif self.tracking_active:
            success, bbox = self.tracker.update(frame)
            if success:
                tx, ty, tw, th = [int(v) for v in bbox]
                current_box = [tx, ty, tw, th]

                # 🌟 1. 【追踪器盲区拦截】万一追踪的目标进入了云端刚刚下发的盲区？
                is_suppressed = False
                for zone in self.suppression_zones:
                    if time.time() > zone["expire_at"]:
                        continue
                    if compute_iou(current_box, zone["bbox"]) > 0.5:
                        is_suppressed = True
                        break

                if is_suppressed:
                    self.tracking_active = False  # 目标已被判死刑，立刻杀掉追踪器
                    print("🛑 [边缘端] 追踪目标误入 VLM 盲区，强行终止追踪！")
                elif best_yolo_conf < 0.6:
                    # 只有未命中盲区，才继续追踪画框
                    is_alarm = True
                    cv2.rectangle(show_frame, (tx, ty), (tx + tw, ty + th), (0, 255, 0), 2)
                    cv2.putText(show_frame, "TRACKED (Pseudo)", (tx, ty - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7,
                                (0, 255, 0), 2)
                    boxes_list.append(current_box)
                    ids_list.append(0)
                    confs_list.append(0.6)
                    types_list.append("pseudo_label")
            else:
                self.tracking_active = False

        # ==========================================
        # 核心逻辑三：组装发送给云端的数据
        # ==========================================
        alarm_data = None
        if is_alarm and len(boxes_list) > 0:
            alarm_data = {
                "type": "fire",
                "img_base64": image_to_base64(show_frame),  # 发全景图做记录
                "boxes": boxes_list,
                "ids": ids_list,
                "confs": confs_list,
                "types": types_list,
                # 👇 携带 VLM 大模型专用参数（如果没有暧昧目标，这几个值就是 None，Java 端能正常接收）
                "track_id": current_track_id,
                "vlm_crop_base64": vlm_crop_base64,
                "bbox": current_vlm_bbox
            }

        return is_alarm, alarm_data, show_frame