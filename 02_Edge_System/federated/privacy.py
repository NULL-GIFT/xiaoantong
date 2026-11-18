"""
差分隐私模块：提供向梯度添加噪声的函数，实现本地差分隐私保护。
支持高斯机制 (Gaussian) 和拉普拉斯机制 (Laplace)。
"""

import numpy as np
from typing import Union, List, Optional


def add_noise(
    gradients: Union[List[float], np.ndarray],   #要加噪的原始梯度
    epsilon: float,         #隐私预算
    sensitivity: float = 1.0,         #	敏感度
    mechanism: str = "gaussian",     #噪声机制
    seed: Optional[int] = None       #随机种子
) -> np.ndarray:
    """。

    Args:
        gradients: 原始梯度，可以是 Python 列表或 NumPy 数组（一维）。
        epsilon: 隐私预算，越小噪声越大。通常取值范围 0.1 ~ 10。
        sensitivity: 梯度的敏感度，通常设为 1.0（如果已经过裁剪）。
        mechanism: 噪声机制，可选 "gaussian" 或 "laplace"。
        seed: 随机种子，用于重现结果（可选）。

    Returns:
        添加噪声后的梯度，与输入类型相同的 NumPy 数组。

    Raises:
        ValueError: 当 epsilon 非正或 mechanism 不支持时抛出。
    """
    if epsilon <= 0:
        raise ValueError(f"epsilon must be positive, got {epsilon}")
    if mechanism not in {"gaussian", "laplace"}:
        raise ValueError(f"mechanism must be 'gaussian' or 'laplace', got {mechanism}")

    # 转换为NumPy一维数组
    grads = np.asarray(gradients).flatten()

    # 设置随机种子（可选）
    if seed is not None:
        np.random.seed(seed)


#----------------------加噪------------------------------

    # 根据机制生成噪声
    if mechanism == "gaussian":
        # 高斯机制：scale = sensitivity / epsilon
        scale = sensitivity / epsilon
        noise = np.random.normal(loc=0.0, scale=scale, size=grads.shape)
    else:  # laplace
        # 拉普拉斯机制：scale = sensitivity / epsilon
        scale = sensitivity / epsilon
        noise = np.random.laplace(loc=0.0, scale=scale, size=grads.shape)

    # 添加噪声
    noisy_grads = grads + noise

    return noisy_grads


if __name__ == "__main__":
    # 简单的测试示例
    sample_grads = [0.1, -0.2, 0.3, -0.4]
    print("原始梯度:", sample_grads)

    # 测试高斯机制
    noisy_g = add_noise(sample_grads, epsilon=1.0, mechanism="gaussian", seed=42)
    print("高斯加噪后:", noisy_g)

    # 测试拉普拉斯机制
    noisy_l = add_noise(sample_grads, epsilon=1.0, mechanism="laplace", seed=42)
    print("拉普拉斯加噪后:", noisy_l)