# demo.py
"""
联邦学习本地训练演示脚本。
用法：python demo.py --model fire
"""

import os
import sys
import argparse
import yaml
import json
import numpy as np


os.chdir(os.path.dirname(os.path.abspath(__file__)))
# 导入自定义模块
from federated.local_trainer import LocalTrainer

from federated.privacy import add_noise


def merge_config(base: dict, override: dict) -> dict:
    """
    递归合并两个配置字典，override 中的键会覆盖 base 中的对应键。
    用于合并 base.yaml 和模型特定配置。
    """
    result = base.copy()
    for key, value in override.items():
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = merge_config(result[key], value)
        else:
            result[key] = value
    return result


def load_config(model_name: str, config_dir: str = "configs") -> dict:
    """
    加载基础配置和模型特定配置，返回合并后的配置字典。

    Args:
        model_name: 模型名称，对应 configs/{model_name}.yaml
        config_dir: 配置文件夹路径（相对于当前脚本）

    Returns:
        完整的配置字典
    """
    base_path = os.path.join(config_dir, "base.yaml")
    model_path = os.path.join(config_dir, f"{model_name}.yaml")

    # 加载基础配置
    with open(base_path, 'r', encoding='utf-8') as f:
        base_cfg = yaml.safe_load(f)

    # 加载模型配置
    with open(model_path, 'r', encoding='utf-8') as f:
        model_cfg = yaml.safe_load(f)

    # 合并
    full_cfg = merge_config(base_cfg, model_cfg)
    return full_cfg


# demo.py（部分修改）
def main():
    parser = argparse.ArgumentParser(description="联邦学习本地训练演示")
    parser.add_argument('--model', type=str, default='fire',
                        help='模型名称（对应 configs 下的 YAML 文件，如 fire）')
    parser.add_argument('--config_dir', type=str, default='configs',
                        help='配置文件夹路径')
    args = parser.parse_args()

    print(f"Loading configuration for model: {args.model}")
    config = load_config(args.model, args.config_dir)


    # 打印关键配置信息（便于调试）
    print("Config loaded:")
    print(f"  Model path: {config['model']['path']}")
    print(f"  YOLOv5 path: {config['model']['yolov5_path']}")
    # 兼容两种数据集配置方式
    if 'yaml_path' in config['data']:
        print(f"  Dataset yaml: {config['data']['yaml_path']}")
    elif 'local_data_dir' in config['data']:
        print(f"  Local data dir: {config['data']['local_data_dir']}")
    else:
        print("  Warning: No dataset path specified in config.")
    print(f"  Training epochs: {config['training']['epochs']}")
    print(f"  Privacy epsilon: {config['privacy']['epsilon']}")

    # 实例化训练器
    trainer = LocalTrainer(config)

    # 执行本地训练
    trainer.train()

    # 提取梯度
    raw_grads = trainer.get_gradients()
    print(f"Raw gradients extracted, shape: {raw_grads.shape}")

    # 应用差分隐私
    privacy_cfg = config['privacy']
    noisy_grads = add_noise(
        raw_grads,
        epsilon=privacy_cfg['epsilon'],
        sensitivity=privacy_cfg['sensitivity'],
        mechanism=privacy_cfg['mechanism']
    )
    print("Noise added.")


    # 获取输出根目录（来自 base.yaml）
    output_root = config['output']['root_dir']
    # 模型专属子目录
    output_dir = os.path.join(output_root, args.model)
    os.makedirs(output_dir, exist_ok=True)

    # 生成带时间戳的文件名
    import datetime
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = os.path.join(output_dir, f"gradients_{timestamp}.json")

    # 准备要保存的数据
    save_data = {
        "model": args.model,
        "timestamp": timestamp,
        "epsilon": config['privacy']['epsilon'],
        "mechanism": config['privacy']['mechanism'],
        "gradients": noisy_grads.tolist()  # 使用加噪后的梯度
    }

    # 写入文件
    with open(output_file, 'w') as f:
        json.dump(save_data, f, indent=2)
    print(f"Gradients saved to {output_file}")

    print("Demo finished.")


if __name__ == '__main__':
    main()