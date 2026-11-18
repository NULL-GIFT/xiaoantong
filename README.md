# 校安通 · 校园安全云边协同平台

校园安全项目，由 Java 云端、Python 边缘端和 Vue 管理前端组成，包含设备管理、报警事件、视频监控、检测技能管理与联邦学习相关模块。

| 目录 | 职责 | 技术 |
| --- | --- | --- |
| `01_Cloud_Platform` | 云端 API、设备、报警、技能与联邦学习管理 | Spring Boot、MySQL、MQTT |
| `02_Edge_System` | 视频流、检测、MQTT 通信与本地训练 | Flask、OpenCV、PyTorch、ONNX Runtime |
| `03_Web_Frontend` | 管理界面、报警展示、监控页面 | Vue 3、Vite、Pinia、ECharts |

## 本地文件与 GitHub 文件

完整开发目录可包含模型、训练数据、照片、Python 环境、前端依赖和本地配置；Git 只管理源码及适合发布的文件。被忽略的文件不会因为 `git add .` 自动上传。

- Java 的 `src/main/resources/application.yml` 保留在本地，可直接填写 Key、密码与地址；GitHub 提供 `application.example.yml`。
- 前端 `.env` 保留在本地，GitHub 提供 `.env.example`。
- 模型、数据、YOLOv5 源码、依赖目录、训练输出和报警照片不上传。
- 本项目不要求使用环境变量保存 Key。请勿强制添加含密钥的本地配置。

## 首次从 GitHub 获取代码

从 GitHub 克隆的代码不包含本机完整运行资源。需要自行准备 MySQL、MQTT、模型、数据和 Python 环境。

### 云端

使用 JDK 11 或与现有项目兼容的 JDK，配置 MySQL 和 MQTT Broker。数据库脚本为 `01_Cloud_Platform/src/main/resources/sql/init_schema.sql`，是否覆盖全部后续新增表需按实际数据库核对。

复制 `src/main/resources/application.example.yml` 为同目录的 `application.yml`，填写本地数据库、MQTT、JWT 和百炼 API Key。然后在云端目录通过 IDE 启动 `SecurityPlatformApplication`，或执行 `mvnw.cmd spring-boot:run`。

### 边缘端

使用与模型兼容的 Python/PyTorch 环境，安装 `requirements.txt` 所列依赖；火灾检测还使用 `onnxruntime`。版本组合应以现有运行环境为准。

检查 `config.py` 中的地址和 MQTT 主题。检测模型位于 `skills/fire_detect/`，模型名称与 `manifest.json` 一致；本地训练配置在 `federated/configs/`，还需要对应模型权重和训练数据。

联邦训练依赖 `yolo/yolov5-master` 下的 YOLOv5 源码，请自行准备与模型匹配的版本，并遵守对应版本许可证。原开发环境的精确源码提交未记录。

工作目录设为 `02_Edge_System`，使用已配置好的解释器运行 `boot.py`。视频流默认地址为 `http://localhost:5000/video_feed`。

### 前端

需要 Node.js `^20.19.0` 或 `>=22.12.0`。在前端目录复制 `.env.example` 为 `.env`，填写服务地址，然后执行 `npm ci`、`npm run dev`。已有 `node_modules` 的本地完整项目可直接使用原安装环境。构建执行 `npm run build`。

## 日常提交

直接在这个仓库里的三个模块中修改代码，然后在仓库根目录执行：

```powershell
git status
git add .
git commit -m "说明这次修改"
git push
```

每次提交前检查文件列表。本地 Key、模型和数据的修改不需要上传。

## 当前说明

项目为开发原型；尚未完成迁移后的端到端联调。公开部署前需检查调试接口、权限控制和日志。当前未为原创代码指定开源许可证，第三方依赖遵循各自许可证。
