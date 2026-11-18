import os

# 切换工作目录到脚本所在目录
os.chdir(os.path.dirname(os.path.abspath(__file__)))
print("Current working directory:", os.getcwd())

# 测试模型路径
model_rel = "../../4_delivery_models/Fire_SmokeDetect/best.pt"
model_abs = os.path.abspath(model_rel)
print("Relative model path:", model_rel)
print("Absolute model path:", model_abs)
print("File exists:", os.path.exists(model_abs))

# 测试数据集 yaml 路径
yaml_rel = "../../1_datasets/Fire_SmokeDetect/data.yaml"
yaml_abs = os.path.abspath(yaml_rel)
print("\nRelative yaml path:", yaml_rel)
print("Absolute yaml path:", yaml_abs)
print("File exists:", os.path.exists(yaml_abs))

# 列出目标目录内容以便确认
target_dir = os.path.dirname(model_abs)
if os.path.exists(target_dir):
    print("\nContents of", target_dir)
    for f in os.listdir(target_dir):
        print("  ", f)
else:
    print("\nTarget directory does not exist:", target_dir)