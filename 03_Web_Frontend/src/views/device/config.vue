<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="page-header">
      <div class="header-left">
        <a-button type="link" class="back-btn" @click="$router.push('/device/list')">
          <left-outlined /> 返回设备列表
        </a-button>
        <div class="title-group">
          <h2>⚡ 边缘计算与感知终端接入 (Device Onboarding)</h2>
          <p class="sub-text">标准化的云边端资产纳管流程，支持独立网关注册与通道自动寻址</p>
        </div>
      </div>
    </div>

    <a-row :gutter="24" class="main-row">
      
      <a-col :span="14">
        <div class="aero-card form-card">
          <div class="card-header" style="display: flex; justify-content: space-between; align-items: center;">
            <span class="card-title"><appstore-add-outlined /> 资产元数据录入</span>
            <a-radio-group v-model:value="registerMode" button-style="solid" size="large">
              <a-radio-button value="gateway">📦 注册边缘算力盒子 (网关)</a-radio-button>
              <a-radio-button value="channel">📷 接入新的感知通道 (摄像头)</a-radio-button>
            </a-radio-group>
          </div>
          
          <div class="card-body">
            
            <a-form v-if="registerMode === 'gateway'" ref="gatewayFormRef" :model="gatewayForm" :rules="gatewayRules" layout="vertical" class="animated-form">
              <div class="mode-alert">
                <info-circle-outlined /> 当前模式：将一台全新的物理边缘计算盒（如树莓派、Jetson）登记到云端台账，等待后续线下开机与挂载摄像头。
              </div>

              <a-form-item label="边缘盒子名称 (Gateway Name)" name="deviceName">
                <a-input v-model:value="gatewayForm.deviceName" placeholder="例如：主教学楼总算力网关" size="large" />
              </a-form-item>

              <a-form-item label="盒子出厂 SN 码 (网关唯一标识)" name="deviceId">
                <a-input-group compact style="display: flex;">
                  <a-input v-model:value="gatewayForm.deviceId" placeholder="输入 MAC 地址或唯一标识 (如 BOX-1001)" size="large" style="flex: 1;" />
                  <a-button size="large" @click="generateSN('BOX')"><sync-outlined /> 自动生成</a-button>
                </a-input-group>
              </a-form-item>

              <a-form-item label="网关局域网 IP (用于拉流和通信)" name="ipAddress">
                <a-input v-model:value="gatewayForm.ipAddress" placeholder="例如：192.168.1.100" size="large" />
                <!-- <div class="form-hint">云端将通过此预设的 IP 与物理盒子建立通信及拉流。</div> -->
              </a-form-item>
            </a-form>

            <a-form v-if="registerMode === 'channel'" ref="channelFormRef" :model="channelForm" :rules="channelRules" layout="vertical" class="animated-form">
              <div class="mode-alert mode-alert-channel">
                <video-camera-outlined /> 当前模式：将一路摄像头视频流，分配挂载到云端已登记的某个边缘盒子上。
              </div>

              <a-form-item label="感知通道名称 (位置)" name="deviceName">
                <a-input v-model:value="channelForm.deviceName" placeholder="例如：南大门入口人脸识别终端" size="large" />
              </a-form-item>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="通道唯一标识 (如 CAM-1001)" name="deviceId">
                    <a-input-group compact style="display: flex;">
                      <a-input v-model:value="channelForm.deviceId" placeholder="输入标识" size="large" style="flex: 1;" />
                      <a-button size="large" @click="generateSN('CAM')"><sync-outlined /></a-button>
                    </a-input-group>
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="摄像机自身 IP (推流源)" name="deviceIp">
                    <a-input v-model:value="channelForm.deviceIp" placeholder="如 10.0.0.102" size="large" />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="16">
                <a-col :span="14">
                  <a-form-item label="挂载至边缘盒子 (算力底座)" name="ipAddress">
                    <a-select
                      v-model:value="channelForm.ipAddress"
                      :options="existingGateways"
                      placeholder="请选择要挂载的边缘盒子"
                      size="large"
                      show-search
                      option-filter-prop="label"
                    >
                      <template #notFoundContent>暂无可用盒子，请先在上方注册盒子</template>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="10">
                  <a-form-item label="分配物理通道 (自动寻址)">
                    <div class="auto-channel-box">
                      <span v-if="!channelForm.ipAddress" class="empty-hint">请先选择盒子...</span>
                      <span v-else class="ch-badge">
                        CH-{{ nextChannelNo }} <check-circle-outlined style="margin-left:4px; color:#10b981;"/>
                      </span>
                    </div>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>

          </div>
          
          <div class="card-footer">
            <a-button size="large" @click="$router.push('/device/list')" style="margin-right: 12px;">取消并返回</a-button>
            <a-button type="primary" class="aero-btn-primary" size="large" :loading="submitting" @click="handleSubmit">
              <template #icon><cloud-upload-outlined /></template> 
              确认注册 {{ registerMode === 'gateway' ? '算力网关' : '感知通道' }}
            </a-button>
          </div>
        </div>
      </a-col>

      <a-col :span="10">
        <div class="aero-card guide-card">
          <div class="card-header" style="border-bottom: 1px solid #e2e8f0; padding-bottom: 16px;">
            <span class="card-title" style="font-size: 15px; color: #0f172a;"><rocket-outlined /> 资产纳管工作流</span>
          </div>
          <div class="guide-body">
            <a-steps direction="vertical" size="small" :current="registerMode === 'gateway' ? 0 : 2" class="aero-steps">
              <a-step title="第一步: 云端资产预登记 (网关)">
                <template #description>
                  <div class="step-desc">录入物理计算盒的 IP 等信息，完成云端台账的建立。</div>
                </template>
              </a-step>
              <a-step title="第二步: 物理部署与激活">
                <template #description>
                  <div class="step-desc">线下通电后，在设备端执行初始化脚本，建立与云平台的 MQTT 长连接。</div>
                </template>
              </a-step>
              <a-step title="第三步: 接入感知通道 (摄像头)">
                <template #description>
                  <div class="step-desc">
                    将摄像头分配给指定的边缘盒子。<strong style="color:#0ea5e9;">系统将自动检测盒子当前的负载，并为其分配下一个空闲的 CH 端口。</strong>
                  </div>
                </template>
              </a-step>
            </a-steps>

            <div class="code-block" style="margin-top: 30px;">
              <div class="code-header">
                <span>边缘端激活脚本</span>
                <copy-outlined class="copy-icon" @click="copyCode" />
              </div>
              <div class="code-content">
                curl -X GET "http://{{ VITE_APP_BASE_API }}/api/device/sync" | python3 init.py
              </div>
            </div>
          </div>
        </div>
      </a-col>

    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { 
  LeftOutlined, AppstoreAddOutlined, CloudUploadOutlined, InfoCircleOutlined,
  RocketOutlined, CopyOutlined, SyncOutlined, VideoCameraOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const submitting = ref(false)

const VITE_APP_BASE_API = import.meta.env.VITE_APP_BASE_API || '127.0.0.1:8080'

// 核心状态
const registerMode = ref('gateway')

// =================== 网关表单逻辑 ===================
const gatewayFormRef = ref(null)
const gatewayForm = reactive({ deviceName: '', deviceId: '', ipAddress: '' })
const gatewayRules = {
  deviceName: [{ required: true, message: '请命名边缘盒子', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请输入盒子唯一标识', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '请输入盒子的预设局域网 IP', trigger: 'blur' }]
}

// =================== 通道表单逻辑 ===================
const channelFormRef = ref(null)
const channelForm = reactive({ deviceName: '', deviceId: '', deviceIp: '', ipAddress: '' })
const channelRules = {
  deviceName: [{ required: true, message: '请命名摄像通道', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请输入通道唯一标识', trigger: 'blur' }],
  deviceIp: [{ required: true, message: '请输入摄像机的局域网 IP', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '必须选择一个归属边缘盒子', trigger: 'change' }]
}

// 全局列表
const allDevices = ref([]) 
const existingGateways = ref([]) 

const fetchData = async () => {
  try {
    const boxRes = await request.get('/api/edgeBox/list')
    if (boxRes.code === 200 || boxRes.code === 0) {
      const boxes = Array.isArray(boxRes.data) ? boxRes.data : []
      existingGateways.value = boxes.map(box => ({
        value: box.ipAddress,
        label: `${box.boxName} (${box.ipAddress})` 
      }))
    }

    const devRes = await request.get('/api/device/list', { params: { pageNum: 1, pageSize: 1000 } })
    if (devRes.code === 200 || devRes.code === 0) {
      allDevices.value = Array.isArray(devRes.data) ? devRes.data : (devRes.data.list || [])
    }
  } catch (error) { console.error('拉取基础数据失败', error) }
}
onMounted(() => fetchData())

// 自动推算物理通道号
const nextChannelNo = computed(() => {
  if (!channelForm.ipAddress) return 1;
  const devicesOnBox = allDevices.value.filter(d => d.ipAddress === channelForm.ipAddress);
  let maxCh = 0;
  devicesOnBox.forEach(d => {
    const ch = parseInt(d.channel_no || d.channelNo || 0);
    if (ch > maxCh) maxCh = ch;
  });
  return maxCh + 1;
});

const generateSN = (prefix) => {
  const sn = prefix + '-' + Math.random().toString(36).substring(2, 8).toUpperCase();
  if (prefix === 'BOX') gatewayForm.deviceId = sn;
  else channelForm.deviceId = sn;
}

// =================== 统一提交逻辑 ===================
const handleSubmit = () => {
  const formRef = registerMode.value === 'gateway' ? gatewayFormRef : channelFormRef;
  
  formRef.value.validate().then(async () => {
    submitting.value = true;
    try {
      let res;
      if (registerMode.value === 'gateway') {
        const payload = {
          boxName: gatewayForm.deviceName,
          boxSn: gatewayForm.deviceId,
          ipAddress: gatewayForm.ipAddress,
          status: 0 // 💡 预登记的设备状态默认给 0(离线)，等心跳上来自动变在线
        };
        res = await request.post('/api/edgeBox/add', payload);
      } else {
        const payload = {
          ...channelForm,
          channel_no: nextChannelNo.value, 
          deviceType: '网络摄像头',
          serviceMode: '视频模式',
          status: 1 
        };
        res = await request.post('/api/device/add', payload);
      }
      
      if (res.code === 200 || res.code === 0) {
        message.success(`🎉 ${registerMode.value === 'gateway' ? '算力盒子' : '感知通道'} 注册成功！`);
        router.push('/device/list');
      } else {
        message.error(res.msg || '注册失败，可能有信息冲突');
      }
    } catch (error) { message.error('网络请求异常'); } finally { submitting.value = false; }
  }).catch(() => { message.warning('请检查必填配置项是否完整'); });
}

const copyCode = () => {
  const code = `curl -X GET "http://${VITE_APP_BASE_API}/api/device/sync" | python3 init.py`;
  navigator.clipboard.writeText(code).then(() => { message.success('执行脚本已复制'); });
}
</script>

<style scoped>
.aero-dashboard { position: relative; padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; color: #1e293b; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; }
.aero-bg-glow { position: absolute; top: -10%; left: -5%; width: 50%; height: 50%; background: radial-gradient(circle, rgba(14, 165, 233, 0.08) 0%, rgba(244, 247, 251, 0) 70%); pointer-events: none; z-index: 0; }

.page-header { margin-bottom: 24px; position: relative; z-index: 10; display: flex; justify-content: space-between; align-items: flex-end;}
.header-left { display: flex; flex-direction: column; align-items: flex-start; }
.back-btn { padding: 0; color: #64748b; font-weight: 600; margin-bottom: 12px; }
.back-btn:hover { color: #0ea5e9; }
.title-group h2 { font-size: 24px; font-weight: 800; color: #0f172a; margin: 0 0 6px 0; }
.sub-text { font-size: 13px; color: #64748b; margin: 0; }

.main-row { position: relative; z-index: 10; }

.aero-card { background: #ffffff; border-radius: 12px; border: 1px solid rgba(0,0,0,0.04); box-shadow: 0 10px 30px rgba(15,23,42,0.03); display: flex; flex-direction: column;}
.card-header { padding: 20px 24px 20px 24px; border-bottom: 1px dashed #e2e8f0;}
.card-title { font-size: 16px; font-weight: 700; color: #1e293b; display: flex; align-items: center; gap: 8px;}
.card-body { padding: 24px; flex: 1; }

.mode-alert { padding: 12px 16px; background: #f0fdf4; border: 1px dashed #6ee7b7; border-radius: 8px; color: #059669; font-size: 13px; margin-bottom: 24px; display: flex; align-items: center; gap: 8px;}
.mode-alert-channel { background: #eff6ff; border-color: #7dd3fc; color: #0284c7; }

:deep(.ant-form-item-label > label) { font-weight: 600; color: #475569; font-size: 13px; }
:deep(.ant-input), :deep(.ant-select-selector), :deep(.ant-input-number) { border-radius: 6px; border-color: #e2e8f0; box-shadow: none !important; }
:deep(.ant-input:focus), :deep(.ant-input-focused) { border-color: #0ea5e9; }

.auto-channel-box { height: 40px; display: flex; align-items: center; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 6px; padding: 0 12px; }
.empty-hint { color: #94a3b8; font-size: 13px; }
.ch-badge { font-weight: bold; color: #0f172a; font-size: 16px; font-family: monospace; display: flex; align-items: center;}

.card-footer { padding: 16px 24px; background: #f8fafc; border-radius: 0 0 12px 12px; border-top: 1px dashed #e2e8f0; display: flex; justify-content: flex-end; }
.aero-btn-primary { background: #0f172a; border: none; font-weight: 600; box-shadow: 0 4px 12px rgba(15,23,42,0.15); }
.aero-btn-primary:hover { background: #1e293b; }

.guide-card { background: #f8fafc; border: 1px solid #e2e8f0; box-shadow: none; }
.guide-body { padding: 0 24px 24px 24px; }
:deep(.aero-steps .ant-steps-item-title) { font-weight: 600; color: #334155 !important; font-size: 14px; transition: color 0.3s;}
.step-desc { font-size: 13px; color: #64748b; line-height: 1.6; margin-top: 4px; }

.code-block { background: #0f172a; border-radius: 8px; margin-top: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.code-header { display: flex; justify-content: space-between; align-items: center; padding: 6px 12px; background: #1e293b; border-bottom: 1px solid #334155; }
.code-header span { color: #94a3b8; font-size: 12px; font-weight: 600; font-family: monospace; }
.copy-icon { color: #94a3b8; cursor: pointer; transition: color 0.2s; }
.copy-icon:hover { color: #fff; }
.code-content { padding: 12px; color: #a5b4fc; font-family: 'Fira Code', monospace; font-size: 12px; line-height: 1.5; word-break: break-all; }
</style>