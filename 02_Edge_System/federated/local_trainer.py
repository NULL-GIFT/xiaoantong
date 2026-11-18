"""
本地训练模块：实现联邦学习中的客户端本地微调。
加载 YOLOv5 模型，冻结骨干网络，使用本地少量数据训练，提取梯度。
"""

import sys
import os
import yaml
import torch
import numpy as np
from torch.utils.data import DataLoader

# 将 YOLOv5 源码路径加入系统路径的函数
def add_yolov5_path(yolov5_path):
    abs_path = os.path.abspath(yolov5_path)
    print(f"YOLO 绝对路径被解析成了: {abs_path}")
    if not os.path.exists(abs_path):
        print("❌ [致命错误] 这个路径根本不存在！找错地方了！")
    if abs_path not in sys.path:
        sys.path.insert(0, abs_path)


class LocalTrainer:
    def __init__(self, config: dict):
        """
        初始化训练器。

        Args:
            config: 合并后的配置字典（base.yaml + fire.yaml）
        """
        self.config = config
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        print(f"Using device: {self.device}")

        # 添加YOLOv5源码路径
        yolov5_path = config['model']['yolov5_path']
        add_yolov5_path(yolov5_path)

        # 在方法内导入导入YOLOv5模块
        self._import_yolov5_modules()

        # 加载模型
        self.model = self._load_model()
        self.model.to(self.device)

        # 冻结骨干网络
        if config['training']['freeze_backbone']:
            self._freeze_backbone()

        # 准备数据加载器
        self.dataloader = self._prepare_dataloader()

        # 设置优化器
        self.optimizer = self._create_optimizer()

        # 初始化损失计算器
        self.compute_loss = self.ComputeLoss(self.model)  # 使用保存的类


    def _import_yolov5_modules(self):
        """动态导入 YOLOv5 所需的模块，并保存为实例变量"""
        from models.experimental import attempt_load
        from utils.dataloaders import LoadImagesAndLabels
        from utils.general import check_img_size, check_dataset  # 如果 demo 用到了就加在这里
        from utils.loss import ComputeLoss

        # 加载.pt模型
        self.attempt_load = attempt_load
        # YOLOv5加载图片+标签的数据集类
        self.LoadImagesAndLabels = LoadImagesAndLabels
        # 损失函数（box+cls+obj三部分）
        # self.select_device = select_device
        self.ComputeLoss = ComputeLoss  # 保存为实例变量
        

        """
        加载模型：边缘端加载的是本地保存的best.pt权重文件，
        通过YOLOv5官方的attempt_load加载，
        之后在这个基础上做本地微调。

        """
    def _load_model(self):
        """加载 YOLOv5 模型权重（.pt）"""
        model_path = self.config['model']['path']
        print(f"Loading model from {model_path}...")
        model = self.attempt_load(model_path)
        return model

    def _freeze_backbone(self):
        """
        冻结骨干网络，只训练检测头。
        需要导入Detect类，所以放在方法内部导入。
        """
        from models.yolo import Detect

        # 先全部冻结
        for param in self.model.parameters():
            param.requires_grad = False

        # 解冻检测头（Detect 模块）
        for module in self.model.modules():
            if isinstance(module, Detect):
                for param in module.parameters():
                    param.requires_grad = True
                print("Detect module found, unfreezed its parameters.")
                
    
    

    def _prepare_dataloader(self):
        """
        从data.yaml中读取数据集信息，构建 DataLoader。
        使用YOLOv5的LoadImagesAndLabels加载数据。
        """
        data_cfg = self.config['data']

        # ---------- 使用本地数据目录 ----------
        if 'local_data_dir' in data_cfg and data_cfg['local_data_dir']:
            local_dir = data_cfg['local_data_dir']
            # 确保 local_dir 是绝对路径（相对于工作目录）
            if not os.path.isabs(local_dir):
                local_dir = os.path.join(os.getcwd(), local_dir)
            img_dir = os.path.join(local_dir, 'images')
            label_dir = os.path.join(local_dir, 'labels')

            # 检查必要的目录是否存在
            if not os.path.isdir(img_dir):
                raise NotADirectoryError(f"Images directory not found: {img_dir}")
            if not os.path.isdir(label_dir):
                raise NotADirectoryError(f"Labels directory not found: {label_dir}")

            print(f"Loading local dataset from {img_dir} (labels in {label_dir})")

            # 使用 LoadImagesAndLabels 加载数据，传入图片目录即可，它会自动关联标签目录
            dataset = self.LoadImagesAndLabels(
                img_dir,  # 图片目录
                img_size=data_cfg['img_size'],
                batch_size=data_cfg['batch_size'],
                augment=False,
                hyp={'mosaic': 0.0},  # 禁用 mosaic 增强
                rect=False,
                cache_images=False,
                single_cls=False,
                stride=int(self.model.stride.max()),
                pad=0.0,
                prefix=''
            )

            # 限制样本数量（可选）
            num_samples = data_cfg.get('num_samples', 0)
            if num_samples > 0 and num_samples < len(dataset):
                indices = list(range(num_samples))
                dataset = torch.utils.data.Subset(dataset, indices)

            # 定义 collate_fn
            def collate_fn(batch):
                img, label, path, shapes = zip(*batch)
                img = [im.float() / 255.0 for im in img]
                for i, l in enumerate(label):
                    l[:, 0] = i
                return torch.stack(img, 0), torch.cat(label, 0), path, shapes

            dataloader = DataLoader(
                dataset,
                batch_size=data_cfg['batch_size'],
                shuffle=True,
                num_workers=0,
                collate_fn=collate_fn,
                pin_memory=self.device.type != 'cpu'
            )
            return dataloader





        yaml_path = data_cfg['yaml_path']

        # 检查并加载 data.yaml
        with open(yaml_path, 'r') as f:
            data_info = yaml.safe_load(f)

        # 获取训练图片路径（相对路径，需要基于 data.yaml 所在目录解析）
        data_dir = os.path.dirname(yaml_path)
        train_path = os.path.join(data_dir, data_info['train'])
        img_size = data_cfg['img_size']
        batch_size = data_cfg['batch_size']
        num_samples = data_cfg['num_samples']

        # 使用 LoadImagesAndLabels 加载数据集，并限制样本数
        dataset = self.LoadImagesAndLabels(
            train_path,
            img_size=img_size,
            batch_size=batch_size,
            augment=False,       # 本地微调不需要数据增强
            hyp={'mosaic': 0.0}, # 禁用 mosaic 增强
            rect=False,
            cache_images=False,
            single_cls=False,
            stride=int(self.model.stride.max()),
            pad=0.0,
            prefix=''
        )

        # 限制使用的样本数量（取前 num_samples 个）
        if num_samples and num_samples < len(dataset):
            indices = list(range(num_samples))
            dataset = torch.utils.data.Subset(dataset, indices)

        # 自定义 collate_fn，因为 LoadImagesAndLabels 返回的 batch 格式特殊
        def collate_fn(batch):
            img, label, path, shapes = zip(*batch)
            # 将图像转换为 float32 并归一化到 [0,1]
            img = [im.float() / 255.0 for im in img]
            for i, l in enumerate(label):
                l[:, 0] = i
            return torch.stack(img, 0), torch.cat(label, 0), path, shapes

        dataloader = DataLoader(
            dataset,
            batch_size=batch_size,
            shuffle=True,
            num_workers=0,
            collate_fn=collate_fn,
            pin_memory=self.device.type != 'cpu'
        )
        return dataloader

    def _create_optimizer(self):
        """创建优化器，只优化 requires_grad=True 的参数"""
        train_params = [p for p in self.model.parameters() if p.requires_grad]
        if not train_params:
            raise RuntimeError("No trainable parameters! Check freeze_backbone settings.")

        optim_cfg = self.config['training']
        if optim_cfg['optimizer'].lower() == 'adam':
            optimizer = torch.optim.Adam(train_params, lr=optim_cfg['learning_rate'])
        else:
            optimizer = torch.optim.SGD(train_params, lr=optim_cfg['learning_rate'], momentum=0.937)
        return optimizer
    
    
    
        """
        PyTorch 训练四步:前向传播，得到预测、
                        算损失、反向传播，
                        算出每个参数的梯度，存进param.grad
                        用梯度更新参数
        """ 
    def train(self):
        epochs = self.config['training']['epochs']
        self.model.train()
        print(f"Starting local training for {epochs} epochs...")

        for epoch in range(epochs):
            total_loss = 0.0
            for batch_idx, (imgs, targets, paths, shapes) in enumerate(self.dataloader):
                imgs = imgs.to(self.device)
                targets = targets.to(self.device)

                # 前向传播：只传入图像，得到预测结果
                pred = self.model(imgs)

                # 计算损失
                loss, loss_items = self.compute_loss(pred, targets)

                # 反向传播
                self.optimizer.zero_grad()
                loss.backward()
                self.optimizer.step()

                total_loss += loss.item()
            avg_loss = total_loss / len(self.dataloader)
            print(f"Epoch {epoch + 1}/{epochs} completed. Average loss: {avg_loss:.4f}")
        print("Local training finished.")
        
        
        """
        遍历模型所有参数

        只挑 requires_grad=True（即检测头）且param.grad非空的参数

        把每个参数的梯度转成numpy、展平成一维向量

        把所有一维向量 `concatenate` 拼成一个大一维数组

        返回
    
        """

    def get_gradients(self) -> np.ndarray:
        """
        提取所有可训练参数的梯度，并展平为一维数组。

        Returns:
            gradients: 一维 NumPy 数组，包含所有可训练参数的梯度。
        """
        grads = []
        for param in self.model.parameters():
            if param.requires_grad and param.grad is not None:
                grads.append(param.grad.cpu().numpy().flatten())
        if not grads:
            raise RuntimeError("No gradients available. Did you run train()?")
        return np.concatenate(grads)