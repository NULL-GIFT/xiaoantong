import os
import json
import time
import requests
from federated.local_trainer import LocalTrainer
from federated.privacy import add_noise
import config

class FederatedClient:
    def __init__(self, config_dict, skill_manager=None):
        self.config = config_dict

        # 1. 建立物理锚点：锁定项目根目录（config.py 所在的绝对位置）
        import config
        self.project_root = os.path.dirname(os.path.abspath(config.__file__))

        # 2. 定义洗径器：如果路径是相对的，自动加上项目根目录
        def to_abs(path_str):
            if not path_str or os.path.isabs(path_str):
                return path_str
            return os.path.join(self.project_root, path_str)

        # 3. 把 YAML 里读出来的所有相对路径，全部洗成绝对路径
        self.config['data']['local_data_dir'] = to_abs(self.config['data'].get('local_data_dir'))
        if 'model' in self.config:
            self.config['model']['yolov5_path'] = to_abs(self.config['model'].get('yolov5_path'))
            self.config['model']['path'] = to_abs(self.config['model'].get('path'))

        # 此时，self.config 里的路径已经全是绝对路径了，去哪台电脑都不会错
        self.skill_manager = skill_manager
        self.data_dir = os.path.join(self.config['data']['local_data_dir'], "images")
        self.min_samples = config_dict.get('data', {}).get('num_samples', 10)
        self.cloud_url = f"http://{config.CLOUD_IP}:8080/api/fed/upload"
        self.round = 0

            
    """
    
    边缘端会定期检查本地新增图片数量，
    当图片数达到阈值就触发一次本地训练,
    不够就继续积累。
            
    """
    def check_and_train(self):
        """检查本地全局模型下载成功图片数量，如果足够则开始训练"""
        if not os.path.exists(self.data_dir):
            return
        files = [f for f in os.listdir(self.data_dir) if f.endswith('.jpg')]
        if len(files) >= self.min_samples:
            print(f"🧠 本地图片数量 ({len(files)}) 达到阈值，开始联邦学习...")
            self.run_training()
        else:
            print(f"⏳ 本地图片不足 {self.min_samples}，当前 {len(files)} 张，跳过训练")


        
        """
        LocalTrainer→ 加载模型、冻结骨干、本地训练

        get_gradients()→ 提取检测头梯度

        add_noise()→ 高斯/拉普拉斯加噪

        
        """

    def run_training(self):
        """执行训练并上传梯度"""
        # 1. 初始化训练器
        trainer = LocalTrainer(self.config)
        # 2. 训练（内部会使用 data_dir 中的图片，需要修改LocalTrainer以支持自定义数据路径）
        trainer.train()
        # 3. 获取原始梯度
        raw_grads = trainer.get_gradients()
        # 4. 加噪
        noisy_grads = add_noise(
            raw_grads,
            epsilon=self.config['privacy']['epsilon'],
            sensitivity=self.config['privacy']['sensitivity'],
            mechanism=self.config['privacy']['mechanism']
        )

        import datetime
        output_dir = os.path.join(self.project_root, "outputs", self.config.get('model', {}).get('name', 'fire'))
        os.makedirs(output_dir, exist_ok=True)
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")

        # 1. 将加噪后的梯度保存为本地临时 JSON 文件（不再放进内存大字典）
        output_file = os.path.join(output_dir, f"gradients_{timestamp}.json")
        save_data = {
            "weights": noisy_grads.tolist()  # 文件里只存纯粹的权重数组
        }
        with open(output_file, 'w') as f:
            json.dump(save_data, f)
        print(f"💾 梯度已落盘至临时文件 {output_file}，准备流式上传...")

        # 2. 准备多部分表单上传（Multipart Form-Data）
        upload_success = False
        try:
            self.round += 1

            # 从skill_manager获取基座版本号
            base_version = "V1.0"
            if self.skill_manager and hasattr(self.skill_manager, 'current_model_version'):
                base_version = self.skill_manager.current_model_version


            # 轻量级的表单数据，完全对齐云端的驼峰命名和扩展字段
            form_data = {
                "clientId": config.DEVICE_ID,  # 对齐: 对应设备的唯一标识
                "round": str(self.round),
                "skillCode": "fire",  # 新增: 明确告诉云端这是哪个技能的梯度
                "baseVersion": base_version  # 对齐: 血统版本号
            }

            # 以文件流的方式打开刚刚落盘的梯度文件
            with open(output_file, 'rb') as f:
                # 把字段名从 'file' 改为精准的 'weights'
                files = {'weights': (f"gradients_{timestamp}.json", f, 'application/json')}

                # 发送 POST 请求
                resp = requests.post(
                    f"http://{config.CLOUD_IP}:8080/api/fed/upload",
                    data=form_data,
                    files=files,
                    timeout=30
                )


            if resp.status_code == 200:
                print(f"✅ 梯度文件 (基于 {base_version}) 流式上传成功！")
                upload_success = True
            else:
                print(f"⚠️ 上传失败，云端拒绝接收。状态码: {resp.status_code}")
        except Exception as e:
            print(f"⚠️ 上传异常: {e}，本次联邦学习中止。")


            # 🔥 核心调整：先把后续的模型融合与导出做完
        rebuild_success = False
        if upload_success:
            json_save_path = os.path.join(self.project_root, "federated", "local_weights", "fire","global_weights.json")
            target_onnx_path = os.path.join(self.project_root, "skills", "fire_detect", "model_v1.onnx")

            if download_global_model(json_save_path):
                print("🔄 正在将下载的 JSON 全局权重融合进本地模型...")
                # 如果 rebuild_onnx 也返回 True，说明全链路真正无损打通！
                if self.rebuild_onnx(json_save_path, target_onnx_path):
                    rebuild_success = True
                    if self.skill_manager:
                        self.skill_manager.update_fire_model(local_path=target_onnx_path)

        # 🔥🔥🔥 【挪到最后的第4点：原子操作级的阅后即焚】 🔥🔥🔥
        # 只有在 上传成功 且 ONNX彻底重组成功 的双重保障下，才允许删图！
        if upload_success and rebuild_success:
            print("🧹 [垃圾回收] 全链路执行成功，准备清空已训练的本地图库...")
            import shutil
            img_dir = os.path.join(self.config['data']['local_data_dir'], "images")
            lbl_dir = os.path.join(self.config['data']['local_data_dir'], "labels")

            for d in [img_dir, lbl_dir]:
                if os.path.exists(d):
                    for filename in os.listdir(d):
                        file_path = os.path.join(d, filename)
                        try:
                            if os.path.isfile(file_path) and not filename.startswith("base_"):
                                os.unlink(file_path)
                        except Exception as e:
                            print(f"删除 {file_path} 失败: {e}")
            print("✨ 本地训练数据已全部清空，防爆盘机制执行完毕！")
        else:
            print("🛑 [安全中止] 发现链路中断或重组失败，保留本地训练数据以备下次重试！")


    def rebuild_onnx(self, json_path, output_onnx_path):
        """吃进 JSON 参数，吐出最新的 ONNX 模型，并【固化母本基因】"""
        try:
            import torch
            import sys
            import os

            # 0. 必须将 YOLOv5 源码路径临时加入系统环境，否则加载模型会报找不到 models 模块
            yolov5_path = self.config['model']['yolov5_path']
            if yolov5_path not in sys.path:
                sys.path.insert(0, yolov5_path)

            from models.experimental import attempt_load

            # 1. 找到母本 .pt 的绝对路径
            base_pt_path = self.config['model']['path']
            if not os.path.exists(base_pt_path):
                print(f"❌ 找不到母本模型: {base_pt_path}，无法重组！")
                return False

            print("   -> 1. 正在加载母本 .pt 模型...")
            device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
            # 使用 YOLO 专属的 attempt_load 加载
            model = attempt_load(base_pt_path, device=device)

            print("   -> 2. 读取 JSON 里的全局平均权重...")
            with open(json_path, 'r') as f:
                global_data = json.load(f)
            # 云端传回来的可能是完整权重数组，也可能是梯度增量
            global_weights_list = global_data if isinstance(global_data, list) else global_data.get('weights', [])

            print("   -> 3. 正在更新模型权重 (基因融合)...")
            # --- 工业级数据还原逻辑 ---
            # (这里假定你云端下发的是一维 Flatten 后的参数更新量，也就是和你 local_trainer 提取的格式对应)
            # 我们需要把 1D 的数组，根据模型参数原本的 shape 重新装填进去
            if len(global_weights_list) > 0:
                pointer = 0
                for param in model.parameters():
                    if param.requires_grad:  # 只有解冻参与了训练的参数才更新
                        num_param = param.numel()
                        # 从一维数组中截取对应长度，并 reshape 成原本的形状
                        update_val = global_weights_list[pointer: pointer + num_param]
                        update_tensor = torch.tensor(update_val, dtype=param.dtype, device=device).view_as(param)

                        # 将新权重赋予模型 (这里根据你们的聚合算法，有可能是 += 梯度，或者直接 = 新权重)
                        # 假定云端下发的是【更新后的直接权重】，我们直接覆盖：
                        with torch.no_grad():
                            param.copy_(update_tensor)
                        pointer += num_param
            # --------------------------

            # 🔥🔥🔥 【核心修复点：固化基因】🔥🔥🔥
            # 更新完内存里的 model 后，绝不能拍屁股走人！必须覆写回硬盘！
            print(f"   -> 4. 【核心动作】保存新基因覆盖母本文件: {base_pt_path}")
            # YOLOv5 标准保存格式是一个字典
            ckpt = {'model': model.float()}  # 工业级求稳：永远用全精度保存母本！
            torch.save(ckpt, base_pt_path)
            # 🔥🔥🔥 修复结束 🔥🔥🔥


            print("   -> 5. 正在导出全新的 ONNX 躯壳...")
            model.eval()

            # 🔥 匹配 GPU 的 16 位半精度输入
            model.float()
            dummy_input = torch.randn(1, 3, 640, 640).to(device)

                # 屏蔽内部的 Trace 警告，保持控制台干净
            import warnings
            with warnings.catch_warnings():
                warnings.filterwarnings("ignore", category=torch.jit.TracerWarning)
                # 使用标准算子导出 ONNX
                torch.onnx.export(
                    model,
                    dummy_input,
                    output_onnx_path,
                    opset_version=12,
                    input_names=['images'],
                    output_names=['output'],
                    dynamic_axes={'images': {0: 'batch'}, 'output': {0: 'batch'}}  # 支持动态解析分辨率
                )

            print("✅ [工业闭环] PT 母本已升级，ONNX 边缘重组大功告成！")
            return True

        except Exception as e:
            print(f"❌ 模型重组失败: {e}")
            import traceback
            traceback.print_exc()  # 打印详细报错方便查Bug
            return False





def download_global_model(save_path: str, version="latest"):
    """从云端下载最新全局模型，保存到 save_path"""
    os.makedirs(os.path.dirname(save_path), exist_ok=True)
    url = f"http://{config.CLOUD_IP}:8080/api/fed/download?version={version}"
    try:
        resp = requests.get(url, timeout=30)
        if resp.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(resp.content)
            print(f"✅ 全局模型下载成功: {save_path}")
            return True
        else:
            print(f"❌ 下载失败，状态码: {resp.status_code}")
            return False
    except Exception as e:
        print(f"❌ 下载异常: {e}")
        return False