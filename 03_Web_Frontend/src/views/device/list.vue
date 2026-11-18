<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="page-header">
      <h2>🖲️ 边缘感知通道与算力网关大盘</h2>
      <p class="sub-text">采用扁平化透明网关架构，直达端侧摄像通道，动态调度 AI 算力</p>
    </div>

    <a-row :gutter="24" class="kpi-row">
      <a-col :span="6">
        <div class="aero-card kpi-card">
          <div class="kpi-icon" style="background: #e0f2fe; color: #0ea5e9;"><appstore-outlined /></div>
          <div class="kpi-info">
            <div class="kpi-label">全网感知通道总数</div>
            <div class="kpi-value-box">
              <span class="kpi-val">{{ totalDevices }}</span> <span class="kpi-unit">路</span>
            </div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="aero-card kpi-card">
          <div class="kpi-icon" style="background: #ecfdf5; color: #10b981;"><link-outlined /></div>
          <div class="kpi-info">
            <div class="kpi-label">活跃算力网关数</div>
            <div class="kpi-value-box">
              <span class="kpi-val" style="color: #059669;">{{ uniqueGateways }}</span>
              <span class="kpi-trend trend-good">IP聚合</span>
            </div>
          </div>
        </div>
      </a-col>

      <a-col :span="6">
        <div class="aero-card kpi-card">
          <div class="kpi-icon" style="background: #f5f3ff; color: #8b5cf6;"><check-circle-outlined /></div>
          <div class="kpi-info">
            <div class="kpi-label">在线感知通道数</div>
            <div class="kpi-value-box">
              <span class="kpi-val" style="color: #7c3aed;">{{ onlineDevices }}</span>
              <span class="kpi-trend" style="background: #ede9fe; color: #7c3aed;">推流正常</span>
            </div>
          </div>
        </div>
      </a-col>

      

      <a-col :span="6">
        <div class="aero-card kpi-card">
          <div class="kpi-icon" style="background: #fef2f2; color: #ef4444;"><disconnect-outlined /></div>
          <div class="kpi-info">
            <div class="kpi-label">失联/故障通道数</div>
            <div class="kpi-value-box">
              <span class="kpi-val" :style="{ color: offlineDevices > 0 ? '#ef4444' : '#111827' }">{{ offlineDevices }}</span>
              <span v-if="offlineDevices > 0" class="kpi-trend trend-bad">需排查</span>
              <span v-else class="kpi-trend trend-good">全网通畅</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <div class="aero-card main-panel">
      
      <div class="filter-bar">
        <a-space wrap>
          <a-input v-model:value="queryParams.keyword" placeholder="输入通道名称或网关IP检索..." style="width: 240px" allowClear @change="applyFilter">
            <template #prefix><search-outlined style="color: #bfbfbf;"/></template>
          </a-input>
          
          <a-select v-model:value="queryParams.status" placeholder="AI算力状态" style="width: 140px" allowClear @change="applyFilter">
            <a-select-option :value="1">在线运行</a-select-option>
            <a-select-option :value="0">离线断连</a-select-option>
          </a-select>
          <a-button type="primary" @click="applyFilter">精准检索</a-button>
          <a-button @click="resetQuery">重置条件</a-button>
        </a-space>

        <a-space>
          <a-button type="primary" class="btn-install" @click="$router.push('/device/config')">
            <template #icon><plus-outlined /></template> 注册新通道
          </a-button>
        </a-space>
      </div>

      <a-table 
        :columns="columns" 
        :data-source="tableData" 
        :loading="loading" 
        row-key="id"
        :pagination="pagination"
        class="aero-table"
      >
        <template #bodyCell="{ column, record }">
          
          <template v-if="column.key === 'deviceName'">
            <div style="display: flex; align-items: center;">
              <div class="device-icon"><video-camera-outlined /></div>
              <div>
                <div style="font-weight: 600; color: #1e293b;">{{ record.deviceName || record.channelName || '未命名通道' }}</div>
                <div style="font-size: 12px; color: #94a3b8; font-family: monospace;">标识: {{ record.deviceId || record.device_id }}</div>
              </div>
            </div>
          </template>

          <template v-else-if="column.key === 'deviceIp'">
            <span style="color: #0f172a; font-weight: 600; font-family: monospace; background: #e0f2fe; padding: 4px 8px; border-radius: 4px; border: 1px solid #bae6fd;">
              {{ record.deviceIp || record.device_ip || '未分配' }}
            </span>
          </template>

          <template v-else-if="column.key === 'boxInfo'">
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <div style="color: #334155; font-weight: 600; font-family: monospace; display: flex; align-items: center;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #10b981; margin-right: 6px;"></span>
                盒子: <span style="margin-left: 4px;">{{ record.ipAddress || '独立运行' }}</span>
              </div>
              <div style="font-size: 12px; color: #64748b; padding-left: 12px; border-left: 2px solid #e2e8f0; margin-left: 2px;">
                └─ 接入: <a-tag color="purple" style="margin-left: 4px; border-radius: 4px;">物理通道 CH-{{ record.channel_no || record.channelNo || 1 }}</a-tag>
              </div>
            </div>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-badge 
              :status="(record.status === 1 || record.workStatus === '在线') ? 'success' : 'error'" 
              :text="(record.status === 1 || record.workStatus === '在线') ? '算力调度中' : '离线断连'" 
              style="font-weight: 500; color: #475569;"
            />
          </template>

          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" style="padding: 0; margin-right: 16px; font-weight: 600; color: #64748b;" @click="openDetailDrawer(record)">
              <profile-outlined /> 通道档案
            </a-button>
            <a-popconfirm
              title="确定要从云端卸载此感知通道吗？"
              ok-text="确认卸载"
              cancel-text="取消"
              @confirm="deleteDevice(record)"
            >
              <a-button type="link" danger size="small" style="padding: 0; font-weight: 600;">
                <delete-outlined /> 卸载通道
              </a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <a-drawer
      v-model:open="detailDrawerVisible"
      title="🖲️ 通道数字孪生档案 (Channel Profile)"
      placement="right"
      width="480"
    >
      <div v-if="currentDetail">
        <div class="drawer-header">
          <div class="d-icon"><video-camera-outlined /></div>
          <div class="d-title-group">
            <div class="d-name">{{ currentDetail.deviceName || currentDetail.channelName || '未命名通道' }}</div>
            <div class="d-code">
              <a-badge :status="(currentDetail.status === 1 || currentDetail.workStatus === '在线') ? 'success' : 'error'" />
              {{ (currentDetail.status === 1 || currentDetail.workStatus === '在线') ? '算力在线' : '设备离线' }}
            </div>
          </div>
        </div>

        <a-descriptions title="🌐 物理拓扑元数据" :column="1" bordered size="small" style="margin-top: 20px;">
          <a-descriptions-item label="通道唯一标识"><code>{{ currentDetail.deviceId || currentDetail.device_id }}</code></a-descriptions-item>
          <a-descriptions-item label="归属算力网关 IP">
            <strong>{{ currentDetail.ipAddress || currentDetail.deviceIp || currentDetail.device_ip || currentDetail.ipUrl || '未获取' }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="网关物理通道号" v-if="currentDetail.channel_no || currentDetail.channelNo">
            <a-tag color="purple">CH-{{ currentDetail.channel_no || currentDetail.channelNo }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="拉流寻址">
            <span style="font-size: 11px; color: #8b5cf6; word-break: break-all;">
              {{ (currentDetail.ipAddress || currentDetail.deviceIp || currentDetail.device_ip) ? `http://${currentDetail.ipAddress || currentDetail.deviceIp || currentDetail.device_ip}:5000/video_feed?device_id=${currentDetail.deviceId || currentDetail.device_id}` : 'IP未就绪' }}
            </span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-drawer>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, AppstoreOutlined, LinkOutlined, DisconnectOutlined,
  SearchOutlined, VideoCameraOutlined, ProfileOutlined, DeleteOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

const totalDevices = ref(0)
const onlineDevices = ref(0)
const offlineDevices = ref(0)
const uniqueGateways = ref(0) // 🌟 新增：活跃网关数

const queryParams = reactive({ keyword: '', status: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 路通道` })

const rawTableData = ref([]) 
const tableData = ref([])
const loading = ref(false)

const columns = [
  { title: '感知通道名称', key: 'deviceName', width: '20%' },
  // 💡 重点1：把摄像机自己的 IP 单独拿出来！
  { title: '摄像机本机 IP', key: 'deviceIp', width: '20%' },
  // 💡 重点2：专业叫法替换
  { title: '归属边缘盒子 & 物理通道', key: 'boxInfo', width: '25%' },
  { title: 'AI 算力状态', key: 'status', width: '15%' },
  { title: '通道管理操作', key: 'action', width: '20%', align: 'center' }
]

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/device/list', { params: { pageNum: 1, pageSize: 1000 } });
    if (res.code === 200 || res.code === 0) {
      const resultData = res.data || {};
      rawTableData.value = Array.isArray(resultData) ? resultData : (resultData.list || []);
      
      // 🌟 排序：让同一个 IP 的通道紧挨在一起显示
      rawTableData.value.sort((a, b) => {
        const ipA = a.ipAddress || a.deviceIp || a.device_ip || '';
        const ipB = b.ipAddress || b.deviceIp || b.device_ip || '';
        return ipA.localeCompare(ipB);
      });

      totalDevices.value = rawTableData.value.length;
      onlineDevices.value = rawTableData.value.filter(d => d.status === 1 || d.workStatus === '在线').length;
      offlineDevices.value = totalDevices.value - onlineDevices.value;
      
      // 🌟 核心逻辑：利用 Set 自动去重，计算有多少个不同的物理盒子 (网关)
      const ipSet = new Set();
      rawTableData.value.forEach(d => {
        const ip = d.ipAddress || d.deviceIp || d.device_ip || d.ipUrl;
        if (ip) ipSet.add(ip);
      });
      uniqueGateways.value = ipSet.size;
      
      applyFilter();
    } else {
      message.error(res.msg || '获取通道列表失败');
    }
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
}

const applyFilter = () => {
  let filtered = rawTableData.value.filter(item => {
    const kw = queryParams.keyword.toLowerCase();
    const nameMatch = (item.deviceName || item.channelName || '').toLowerCase().includes(kw);
    const ipMatch = (item.ipAddress || item.deviceIp || item.device_ip || item.ipUrl || '').toLowerCase().includes(kw);
    const matchKeyword = !kw || nameMatch || ipMatch;
    
    const isOnline = (item.status === 1 || item.workStatus === '在线') ? 1 : 0;
    const matchStatus = queryParams.status === undefined || isOnline === queryParams.status;
    
    return matchKeyword && matchStatus;
  });
  
  pagination.total = filtered.length;
  const start = (pagination.current - 1) * pagination.pageSize;
  const end = start + pagination.pageSize;
  tableData.value = filtered.slice(start, end);
}

const resetQuery = () => { queryParams.keyword = ''; queryParams.status = undefined; pagination.current = 1; applyFilter(); }

const detailDrawerVisible = ref(false);
const currentDetail = ref(null);

const openDetailDrawer = (record) => {
  currentDetail.value = record;
  detailDrawerVisible.value = true;
};

const deleteDevice = async (record) => {
  try {
    const deviceId = record.id || record.deviceId || record.device_id;
    const res = await request.delete(`/api/device/delete/${deviceId}`);
    if (res.code === 200 || res.code === 0) {
      message.success(`通道 [${record.deviceName || deviceId}] 已成功卸载`);
      fetchData(); 
    } else {
      message.error(res.msg || '卸载失败');
    }
  } catch (err) {
    message.error('网络请求异常，卸载失败');
  }
};

onMounted(() => fetchData())
</script>

<style scoped>
.aero-dashboard { position: relative; padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; color: #1e293b; overflow: hidden; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
.aero-bg-glow { position: absolute; top: -10%; left: -5%; width: 50%; height: 50%; background: radial-gradient(circle, rgba(14, 165, 233, 0.08) 0%, rgba(244, 247, 251, 0) 70%); pointer-events: none; z-index: 0; }

.page-header { margin-bottom: 24px; position: relative; z-index: 10;}
.page-header h2 { font-size: 22px; font-weight: 800; color: #0f172a; margin: 0 0 8px 0; }
.sub-text { font-size: 13px; color: #64748b; margin: 0; }

.kpi-row { margin-bottom: 24px; position: relative; z-index: 10;}
.aero-card { background: #ffffff; border-radius: 12px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02); border: 1px solid rgba(0,0,0,0.02); }
.kpi-card { padding: 24px; display: flex; align-items: center; }
.kpi-icon { width: 50px; height: 50px; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-size: 24px; margin-right: 16px; }
.kpi-info { flex: 1; }
.kpi-label { font-size: 13px; color: #6b7280; font-weight: 600; margin-bottom: 4px; }
.kpi-value-box { display: flex; align-items: baseline; gap: 8px; }
.kpi-val { font-size: 32px; font-weight: 800; color: #111827; line-height: 1; font-family: "DIN Alternate", Arial; }
.kpi-unit { font-size: 13px; color: #94a3b8; font-weight: bold;}
.kpi-trend { font-size: 12px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }
.trend-bad { color: #ef4444; background: #fef2f2; }
.trend-good { color: #10b981; background: #ecfdf5; }

.main-panel { padding: 20px; position: relative; z-index: 10;}
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.btn-install { background: #0ea5e9; border: none; font-weight: 600; box-shadow: 0 4px 6px rgba(14,165,233,0.2); }
.btn-install:hover { background: #0284c7; }

.device-icon { width: 36px; height: 36px; background: #f1f5f9; color: #475569; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 18px; margin-right: 12px;}

:deep(.aero-table .ant-table-thead > tr > th) { background-color: #f8fafc !important; font-weight: 600; color: #475569; border-bottom: 1px solid #e2e8f0 !important; padding: 16px; }
:deep(.aero-table .ant-table-tbody > tr > td) { border-bottom: 1px solid #f1f5f9 !important; padding: 16px; transition: background 0.2s;}
:deep(.aero-table .ant-table-tbody > tr:hover > td) { background-color: #f8fafc !important; }
:deep(.aero-table .ant-table-thead > tr > th::before) { display: none !important; }

.drawer-header { display: flex; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #f1f5f9; }
.d-icon { width: 50px; height: 50px; background: #0f172a; color: #fff; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-size: 24px; margin-right: 16px; }
.d-title-group { flex: 1; }
.d-name { font-size: 18px; font-weight: 800; color: #0f172a; margin-bottom: 6px;}
.d-code { font-size: 12px; color: #64748b; font-weight: 600; }
</style>