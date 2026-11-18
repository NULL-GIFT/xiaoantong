# 校安通 · 校园安全云边协同平台

由 Java 云端、Python 边缘端与 Vue 管理前端组成的校园安全项目。代码包含设备管理、报警事件、视频监控、检测技能管理与联邦学习相关模块。

本仓库是源码整理版本：不包含本地报警照片、采集数据、训练输出、模型权重和个人配置。尚未完成全新环境的端到端验证。

## 项目结构

| 目录 | 职责 | 技术 |
| --- | --- | --- |
| `01_Cloud_Platform` | 云端 API、设备/报警/技能/联邦学习管理 | Java 11、Spring Boot 2.7、MySQL、MQTT |
| `02_Edge_System` | 视频流、检测技能、MQTT 通信、本地训练 | Python、Flask、OpenCV、PyTorch |
| `03_Web_Frontend` | 管理界面、报警展示、监控页面 | Vue 3、Vite、Pinia、ECharts |

## 启动准备

需要 JDK 11、MySQL、MQTT Broker、Python 以及 Node.js（`^20.19.0` 或 `>=22.12.0`）。Python/PyTorch 版本需按设备与 CUDA 环境选择；现有 requirements 使用版本范围，不代表任意版本组合都兼容。

### 1. 云端

在 MySQL 中执行 `01_Cloud_Platform/src/main/resources/sql/init_schema.sql`。该脚本来自现有工程，是否覆盖所有后续新增表仍需核对；不要直接导入包含真实人员或报警信息的数据库备份。

在 PowerShell 中设置环境变量（将示例值替换为自己的值）：

```powershell
$env:DB_PASSWORD = "your-local-db-password"
$env:JWT_SECRET = "your-own-long-random-secret"
# 以下按需设置，默认数据库 localhost:3306/security_db
$env:DB_USERNAME = "root"
$env:MQTT_URL = "tcp://127.0.0.1:1883"
$env:MQTT_USERNAME = "your-mqtt-user"
$env:MQTT_PASSWORD = "your-mqtt-password"
# 使用视觉大模型复核时设置；留空无法完成真实模型调用
$env:DASHSCOPE_API_KEY = "your-own-api-key"
cd 01_Cloud_Platform
.\mvnw.cmd spring-boot:run
```

可用 `DB_URL` 覆盖完整 JDBC 地址。API Key 和本地密码不应写入提交文件。

### 2. 边缘端

```powershell
cd 02_Edge_System
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

运行前检查 `config.py` 中的云端和 MQTT 地址，及云端订阅主题与边缘端发布主题是否对应；设备 ID 可通过 `DEVICE_ID` 环境变量配置。

检测需要自备与代码匹配的 ONNX 权重；`skills/fire_detect/manifest.json` 保留了原开发版本信息，须与实际模型文件一致。联邦训练配置见 `federated/configs/`，需要自行准备 `federated/local_weights/fire/best.pt` 和 `data/capture/fire` 下的训练数据。

联邦训练使用 YOLOv5 源码，原开发目录为 `yolo/yolov5-master`。此第三方目录未收录；请从 [Ultralytics YOLOv5](https://github.com/ultralytics/yolov5) 获取与模型兼容的版本并放到该位置，按其说明安装额外依赖，并遵守对应版本许可证。当前没有记录原始源码的精确提交，不保证最新版本直接兼容。

准备好检测模型与相机后，在边缘端目录执行：

```powershell
.\.venv\Scripts\python.exe boot.py
```

视频流默认位于 `http://localhost:5000/video_feed`。

### 3. 前端

```powershell
cd 03_Web_Frontend
Copy-Item .env.example .env
npm ci
npm run dev
```

多机部署时修改 `.env` 的三个服务地址。构建执行 `npm run build`。

## 发布说明

- 模型、数据与运行输出未随代码发布，不能仅靠克隆仓库完成全部功能演示。
- 本项目为开发原型，公开部署前仍需检查身份验证、调试接口和日志中的敏感信息。
- 本次整理未为原创代码指定开源许可证；公开仓库也不自动授予第三方使用许可。第三方依赖遵循各自许可证。
