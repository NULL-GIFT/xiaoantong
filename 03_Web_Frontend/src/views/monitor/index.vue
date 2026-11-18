<template>
  <div class="monitor-container">
    <div class="main-section">
      <div class="video-grid-area">
        
        <div class="grid-toolbar">
          <div class="toolbar-left">
            <span class="page-title time-display">
              <clock-circle-outlined style="margin-right: 8px; color: #1890ff;" />
              {{ currentTime }}
            </span>
            <span style="color: #fff; font-weight: bold; font-size: 15px; margin-left: 12px; letter-spacing: 1px;">
              实时预览
            </span>
          </div>

          <div class="toolbar-right">
            <a-select
              v-model:value="currentDeviceSelect"
              show-search
              option-filter-prop="label"
              placeholder="🔍 输入设备名称或位置搜索..."
              style="width: 240px; margin-right: 16px;"
              :options="deviceOptions"
              allowClear
            >
              <template #notFoundContent>未找到匹配的设备</template>
            </a-select>

            <a-button-group>
              <a-tooltip title="单屏模式">
                <a-button @click="changeLayout(1)" :type="layoutMode === 1 ? 'primary' : 'default'">
                  <border-outer-outlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="4 分屏 (2x2)">
                <a-button @click="changeLayout(4)" :type="layoutMode === 4 ? 'primary' : 'default'">
                  <appstore-outlined />
                </a-button>
              </a-tooltip>
              <a-tooltip title="9 分屏 (3x3)">
                <a-button @click="changeLayout(9)" :type="layoutMode === 9 ? 'primary' : 'default'">
                  <layout-outlined />
                </a-button>
              </a-tooltip>
               <a-tooltip title="16 分屏 (4x4)">
                <a-button @click="changeLayout(16)" :type="layoutMode === 16 ? 'primary' : 'default'">
                  <table-outlined />
                </a-button>
              </a-tooltip>
            </a-button-group>
          </div>
        </div>

        <a-spin :spinning="loadingDevices" tip="正在接入监控通道..." wrapperClassName="full-height-spin">
          <div class="video-grid-wrapper" :class="'grid-' + layoutMode">
            <div v-for="index in layoutMode" 
            :key="index" 
            class="video-slot"
            :class="{ 
              'active-slot': currentSelectIndex === index,
              'maximized-slot': maximizedSlot === index, 
              'hidden-slot': maximizedSlot !== null && maximizedSlot !== index 
            }"
            @click="currentSelectIndex = index"
            @dblclick="toggleMaximize(index)"
          >
            <div v-if="slotAssignments[index]" class="player-container">
              <div class="player-header">
                <span class="camera-name">
                  <environment-outlined style="margin-right: 4px; color: #1890ff;"/>
                  {{ slotAssignments[index].channelName || slotAssignments[index].deviceName }}
                </span>
                <div style="display: flex; align-items: center;">
                  
                  <a-tag :color="(slotAssignments[index].status === 1 || slotAssignments[index].workStatus === '在线') ? 'green' : 'red'">
                    {{ (slotAssignments[index].status === 1 || slotAssignments[index].workStatus === '在线') ? '在线' : '离线' }}
                  </a-tag>

                  <div class="maximize-btn" @click.stop="toggleMaximize(index)" title="双击画面也可放大/缩小">
                    <fullscreen-exit-outlined v-if="maximizedSlot === index" />
                    <fullscreen-outlined v-else />
                  </div>
                </div>
              </div>

              <div class="player-content">
                <img 
                  :src="getStreamUrl(index)"
                  alt="监控画面加载中..." 
                  class="real-video-stream"
                />
              </div>
            </div>

            <div v-else class="empty-slot-placeholder">
              <video-camera-outlined style="font-size: 32px; color: #555; margin-bottom: 8px;" />
              <div class="text">通道 {{ index }}</div>
              <div class="sub-text">点击选中，在右上角搜索分配</div>
            </div>
          </div>
          </div>
        </a-spin>
        
      </div>
    

      <div class="sidebar-area">
        <div class="sidebar-header">
          <div>
            <span style="color: white; font-weight: bold;">实时报警动态</span>
            <a-tag color="red" style="margin-left: 8px;">监测中</a-tag>
          </div>
          <a-button type="link" danger size="small" @click="clearAlarms">
            <template #icon><delete-outlined /></template>清除
          </a-button>
        </div>
        
        <div class="alarm-list">
          <div v-for="item in formattedAlarmList" :key="item.id" class="alarm-item" @click="openAlarmDetail(item)" style="cursor: pointer; transition: all 0.2s;" onmouseover="this.style.background='rgba(24, 144, 255, 0.1)'" onmouseout="this.style.background='transparent'">
            <img :src="item.imgSrc" class="alarm-img" alt="抓拍" />
            
            <div class="alarm-info">
              <div class="alarm-title" style="font-weight: bold;" :style="{ color: item.typeName.includes('火') ? '#ff4d4f' : '#1890ff' }">
                {{ item.typeName.includes('火') ? '🔥' : '🚨' }}
                {{ item.typeName }}
              </div>
              
              <div class="alarm-desc" style="font-size: 11px; margin-bottom: 2px; color: #aaa;">
                <span>设备: {{ item.devName }}</span> | 
                <span style="color: #1890ff;">位置: {{ item.location }}</span>
              </div>
              
              <div class="alarm-time" style="color: #888; font-size: 12px;">
                时间: {{ item.time }}
              </div>

              <div class="alarm-time" style="color: #888; font-size: 12px; display: flex; justify-content: space-between; align-items: center;">
                <span>时间: {{ item.time }}</span>
                <span style="color: #cf1322; font-weight: bold; font-family: monospace; font-size: 14px;">
                  {{ (item.confidence * 100).toFixed(1) }}%
                </span>
              </div>
              
              
            </div>
          </div>
          <a-empty v-if="formattedAlarmList.length === 0" description="暂无异常报警" style="margin-top: 50px;" />
        </div>
      </div>
    </div>
  </div>
  <a-modal 
      v-model:open="alarmModalVisible" 
      title="🚨 实时报警事件详情" 
      :footer="null" 
      width="650px" 
      centered
      destroyOnClose
    >
      <div v-if="currentAlarmDetail" class="alarm-detail-container">
        <div style="width: 100%; height: 350px; background: #000; border-radius: 8px; overflow: hidden; display: flex; justify-content: center; align-items: center; margin-bottom: 20px;">
          <img 
            :src="currentAlarmDetail.imgSrc" 
            alt="报警抓拍大图" 
            style="max-width: 100%; max-height: 100%; object-fit: contain;" 
          />
        </div>
        
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="报警类型" :span="2">
            <span style="font-size: 16px; font-weight: bold;" :style="{ color: currentAlarmDetail.typeName.includes('火') ? '#ff4d4f' : '#1890ff' }">
              {{ currentAlarmDetail.typeName }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="抓拍时间" :span="2">
            <span style="font-family: monospace; font-size: 14px;">{{ currentAlarmDetail.time }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="设备节点">
            <a-tag color="blue">{{ currentAlarmDetail.devName }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发生位置">
            <strong>{{ currentAlarmDetail.location }}</strong>
          </a-descriptions-item>
          <a-descriptions-item label="置信度" v-if="currentAlarmDetail.confidence">
            <a-progress 
              :percent="parseFloat((currentAlarmDetail.confidence * 100).toFixed(1))" 
              size="small" 
              status="active" 
              :stroke-color="currentAlarmDetail.typeName.includes('火') ? '#ff4d4f' : '#1890ff'"
            />
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useAlarmStore } from '@/stores/alarmStore'; 
import { storeToRefs } from 'pinia';
import { initWebSocket } from '@/utils/websocket'; 
import request from '@/utils/request';
import { getDevice } from '@/api/device.js';
import { 
  BorderOuterOutlined, AppstoreOutlined, LayoutOutlined, TableOutlined, 
  EnvironmentOutlined, VideoCameraOutlined, DeleteOutlined,
  ClockCircleOutlined, FullscreenOutlined, FullscreenExitOutlined 
} from '@ant-design/icons-vue';

// ================= 🌟 时钟管理 =================
const currentTime = ref('');
let clockTimer = null;

const padZero = (num) => (num < 10 ? '0' + num : num);
const updateClock = () => {
  const now = new Date();
  const dateStr = `${now.getFullYear()}-${padZero(now.getMonth() + 1)}-${padZero(now.getDate())}`;
  const timeStr = `${padZero(now.getHours())}:${padZero(now.getMinutes())}:${padZero(now.getSeconds())}`;
  currentTime.value = `${dateStr} ${timeStr}`;
};

const layoutMode = ref(Number(localStorage.getItem('monitor_layout')) || 4); 
const currentSelectIndex = ref(1); 
const slotAssignments = ref({});

const deviceList = ref([]); 
const loadingDevices = ref(false);

const deviceOptions = computed(() => {
  return deviceList.value.map(dev => ({
    value: dev.id || dev.deviceId, 
    label: dev.channelName || dev.deviceName || '未知地点'
  }));
});

const currentDeviceSelect = computed({
  get: () => {
    const device = slotAssignments.value[currentSelectIndex.value];
    return device ? (device.id || device.deviceId) : undefined;
  },
  set: (newVal) => {
    if (!newVal) {
      delete slotAssignments.value[currentSelectIndex.value];
    } else {
      const device = deviceList.value.find(d => d.id === newVal || d.deviceId === newVal);
      if (device) slotAssignments.value[currentSelectIndex.value] = device;
    }
  }
});

// ================= 🌟 报警详情弹窗逻辑 =================
const alarmModalVisible = ref(false);
const currentAlarmDetail = ref(null);

const openAlarmDetail = (item) => {
  currentAlarmDetail.value = item;
  alarmModalVisible.value = true;
};

// ================= 🌟 单路画面放大逻辑 =================
const maximizedSlot = ref(null); 

const toggleMaximize = (index) => {
  if (maximizedSlot.value === index) {
    maximizedSlot.value = null; 
  } else {
    maximizedSlot.value = index; 
    currentSelectIndex.value = index; 
  }
};

const changeLayout = (mode) => {
  maximizedSlot.value = null; 
  layoutMode.value = mode;
  if (currentSelectIndex.value > mode) {
    currentSelectIndex.value = 1;
  }
  // 💡 修复 4：每次用户切换分屏模式，都永久保存在浏览器里
  localStorage.setItem('monitor_layout', mode);
};

// ================= 🌟 真实视频流接入 (中心化安全版) =================
const getStreamUrl = (slotIndex) => {
  const device = slotAssignments.value[slotIndex];
  
  // 1. 如果这个槽位是空的，直接返回空
  if (!device) return '';

  // 2. 如果后端数据库里已经配置了这台设备的专属流地址，优先拉取
  if (device.streamUrl || device.stream_url) {
    return `${device.streamUrl || device.stream_url}?t=${new Date().getTime()}`;
  }

  // 3. 核心修复：直接读取你在 .env 里配好的 Python 服务器地址！
  // 此时 baseUrl 的值就是: http://192.168.192.195:5000/video_feed
  const baseUrl = import.meta.env.VITE_APP_VIDEO_URL; 
  
  const deviceId = device.deviceId || device.device_id || `cam_${slotIndex}`;

  // 💡 绝招：用 ?device_id=xxx 的方式传参，而不是 /xxx。
  // 这样无论你的 Python 怎么写，只要它开放了 /video_feed，就绝对能出画面！
  return `${baseUrl}?device_id=${deviceId}&t=${new Date().getTime()}`;
};

const fetchRealDevices = async () => {
  loadingDevices.value = true;
  try {
    const res = await getDevice({ pageNum: 1, pageSize: 1000 }); 
    if (res.code === 200 || res.code === 0) {
      const realData = res.data || {};
      deviceList.value = Array.isArray(realData) ? realData : (realData.list || []);
      
      deviceList.value.forEach((dev, idx) => {
        // 💡 修改这里：只把拉取到的第一个设备自动分配给“通道1”
        // 这样其他格子就会显示“等待分配”，不会重复请求同一个视频流了
        if (idx === 0) slotAssignments.value[1] = dev;
      });
    }
  } catch (error) {
    console.error("加载真实摄像头列表失败", error);
  } finally {
    loadingDevices.value = false;
  }
};

// ================= 🌟 报警处理 =================
// ================= 🌟 报警处理 =================
const alarmStore = useAlarmStore();
const { alarmList } = storeToRefs(alarmStore);

// 💡 修复 1：清除时，在浏览器会话里打个标记
const clearAlarms = () => {
  alarmList.value = [];
  sessionStorage.setItem('isAlarmCleared', 'true'); // 记住：用户已经清空过了
};

const loadInitialAlarms = async () => {
  if (sessionStorage.getItem('isAlarmCleared') === 'true') return; 

  try {
    const params = { pageNum: 1, pageSize: 15 };
    const res = await request.get('/api/alarm/list', { params });
    const realData = res.data?.data || res.data || res;
    let rawList = Array.isArray(realData.list) ? realData.list : (Array.isArray(realData) ? realData : []);

    if (rawList.length > 0) {
      const formattedAlarms = rawList.map(item => {
        // 🌟 核心修复：坚决不使用随机数！信任后端传来的原始置信度！
        let realConf = parseFloat(item.confidence || item.conf || item.score || 0.95);
        if (realConf > 1) realConf /= 100; // 如果后端传的是 95，就转成 0.95

        const typeCode = item.alarmType || item.alarm_type || item.type || '';
        const devId = item.deviceId || item.device_id || item.deviceName || item.device_name || '未知设备';
        const realDevName = getDeviceDisplayName({ deviceId: devId });

        return {
          id: item.id || Math.random(),
          imgPath: item.imgPath || item.img_path,
          type: typeCode,
          devName: realDevName, 
          location: realDevName, 
          confidence: realConf, // 统一下发转换好的置信度
          time: item.alarmTime ? item.alarmTime.replace('T', ' ') : (item.time || '')
        };
      });
      alarmStore.initAlarms(formattedAlarms);
    }
  } catch (error) { console.error('获取报警记录失败:', error); }
};

// 🚨 终极防御：自动清洗并解析 WebSocket 推送过来的各种异形数据
const formattedAlarmList = computed(() => {
  return alarmList.value.map(rawItem => {
    let item = rawItem;
    if (typeof rawItem === 'string') { try { item = JSON.parse(rawItem); } catch (e) {} }
    if (item && typeof item === 'object' && !item.type && !item.alarmType && !item.img_base64 && !item.imgPath) {
      item = item.data || item.msg || item.payload || item;
      if (typeof item === 'string') item = JSON.parse(item);
    }

    // 🕵️‍♂️ 破案关键：按 F12 打开浏览器控制台，看看后端推过来的到底是个什么神仙结构
    console.log('【实时监控 - WebSocket 原始数据】:', item);

    const eventObj = item.event || item;

    const img = eventObj.img_base64 || eventObj.imgPath || item.imgPath || item.img_path || '';
    const typeCode = eventObj.type || eventObj.alarmType || item.alarmType || item.alarm_type || 'unknown';
    const devId = item.deviceId || item.device_id || item.deviceName || item.device_name || '未知设备';
    const timeVal = item.time || item.alarmTime || item.alarm_time || item.timestamp || new Date().getTime();

    // 🌟 核心修改：完全照搬报警列表（事件中心）的取值逻辑，一字不差！
    // 同时兼顾 WebSocket 可能存在的 eventObj 嵌套层级
    let rawConf = item.confidence || item.conf || eventObj.confidence || eventObj.conf;
    
    // 如果提取不到，走 0.95 保底（和 index.vue 逻辑 100% 对齐）
    let realConf = parseFloat(rawConf || 0.95);
    if (realConf > 1) realConf /= 100;

    const realDevName = getDeviceDisplayName({ deviceId: devId });

    return {
      id: item.id || Math.random(),
      imgSrc: getImageUrl(img), 
      typeName: getAlarmName(typeCode), 
      devName: realDevName, 
      location: realDevName, 
      time: typeof timeVal === 'number' ? new Date(timeVal).toLocaleString('zh-CN', { hour12: false }) : (typeof timeVal === 'string' ? timeVal.replace('T', ' ') : timeVal),
      confidence: realConf 
    };
  });
});
// 🌟 1. 强化图片解析函数 (完美兼容 HTTP路径、标准Base64 和 Python发来的纯Base64)
const getImageUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http')) return path;
  if (path.startsWith('data:image')) return path;
  
  // 核心修复：如果是纯 Base64 字符串（通常很长，或者以 /9j/ 开头），手动补全头部
  if (path.length > 500 || path.startsWith('/9j/')) {
    return `data:image/jpeg;base64,${path}`;
  }
  
  // 否则认为是相对路径，拼接 baseUrl
  const baseUrl = import.meta.env.VITE_APP_BASE_API || '';
  return `${baseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
};



// 🌟 3. 新增：设备 SN 码翻译官 (利用已拉取的 deviceList 字典进行跨表匹配)
const getDeviceDisplayName = (item) => {
  const id = item.deviceId || item.device_id || item.deviceName || item.device_name;
  // 去全局设备列表里找这个 SN 码对应的真实名称
  const dev = deviceList.value.find(d => d.deviceId === id || d.id === id);
  return dev ? (dev.deviceName || dev.channelName || id) : (id || '未知设备');
};



// 🌟 新增：强化版警报类型翻译官 (防弹处理)
const getAlarmName = (code) => {
  // 强制转为小写字符串，防止后端传来的是数字 ID
  const lowerCode = String(code || '').toLowerCase();
  
  if (lowerCode.includes('fire') || lowerCode.includes('火')) return '烟火检测';
  if (lowerCode.includes('smoke') || lowerCode.includes('烟')) return '违规吸烟';
  if (lowerCode.includes('fall') || lowerCode.includes('跌倒')) return '人员跌倒';
  if (lowerCode.includes('occupy') || lowerCode.includes('占')) return '消防通道占用';
  
  // 兜底：如果后端传的是数据库的 ID (如 1, 2)
  if (lowerCode === '1') return '烟火检测';
  if (lowerCode === '2') return '人员跌倒';
  
  return code || '异常报警';
};


// 生命周期钩子
onMounted(async () => {
  updateClock();
  clockTimer = setInterval(updateClock, 1000); 
  
  // 🌟 核心修复：加上 await，强制程序停在这里等！
  // 必须等这本“设备字典”完完整整地从后端下载下来，再往下走！
  await fetchRealDevices(); 
  
  // 此时字典已经拿到了，再去解析报警记录，翻译绝对 100% 成功！
  loadInitialAlarms();
  initWebSocket(import.meta.env.VITE_APP_WS_URL);
});

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer); 
});

</script>

<style scoped>
.monitor-container {
  height: calc(100vh - 64px);
  padding: 16px;
  background: #141414;
  color: rgba(255,255,255,0.85);
  overflow: hidden;
}

.main-section {
  display: flex;
  height: 100%;
  gap: 16px;
}

.video-grid-area {
  flex: 1;
  background: #000;
  border-radius: 8px;
  border: 1px solid #333;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.grid-toolbar {
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid #333;
  background: rgba(25, 25, 25, 0.8);
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 10;
}
.toolbar-left {
  display: flex;
  align-items: center;
}
.page-title {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
  letter-spacing: 1px;
}
.time-display {
  font-family: 'Courier New', Courier, monospace;
  background: rgba(0, 0, 0, 0.5);
  padding: 4px 12px;
  border-radius: 4px;
  border: 1px solid #333;
}
.toolbar-right {
  display: flex;
  align-items: center;
}
:deep(.full-height-spin) {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
:deep(.full-height-spin .ant-spin-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}
/* 🌟 下拉框搜索优化样式 */
:deep(.ant-select-selector) {
  background-color: rgba(0, 0, 0, 0.4) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
  border-radius: 4px !important;
}
:deep(.ant-select-arrow), :deep(.ant-select-clear) {
  color: rgba(255, 255, 255, 0.5);
}
:deep(.ant-select-selection-search-input) {
  color: #fff !important;
}

.video-grid-wrapper {
  flex: 1;
  display: grid;
  gap: 1px;
  background: #2a2a2a;
  padding: 1px;
  position: relative; 
}

.video-grid-wrapper.grid-1 { grid-template-columns: 1fr; grid-template-rows: 1fr; }
.video-grid-wrapper.grid-4 { grid-template-columns: repeat(2, 1fr); grid-template-rows: repeat(2, 1fr); }
.video-grid-wrapper.grid-9 { grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr); }
.video-grid-wrapper.grid-16 { grid-template-columns: repeat(4, 1fr); grid-template-rows: repeat(4, 1fr); }

.video-slot {
  background: #000;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent; 
}

.video-slot:hover { border-color: rgba(24, 144, 255, 0.4); }
.video-slot.active-slot {
  border-color: #1890ff;
  box-shadow: 0 0 15px rgba(24, 144, 255, 0.3) inset;
}

.video-slot.maximized-slot {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 50;
  border: none;
}
.video-slot.hidden-slot {
  display: none;
}

.empty-slot-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #444;
}
.empty-slot-placeholder .text { font-size: 13px; font-weight: bold; margin-bottom: 4px; }
.empty-slot-placeholder .sub-text { font-size: 11px; color: #555; }

.player-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.player-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 6px 12px;
  background: linear-gradient(to bottom, rgba(0,0,0,0.8), transparent);
  z-index: 5;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.camera-name { font-weight: bold; }

.maximize-btn {
  margin-left: 12px;
  cursor: pointer;
  font-size: 14px;
  color: #aaa;
  transition: color 0.2s;
}
.maximize-btn:hover { color: #fff; }

.player-content {
  flex: 1;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #050505;
}

.real-video-stream {
  /* 强制宽和高都撑满父容器 */
  width: 100%;
  height: 100%;
  
  /* 💡 关键属性：
   fill: 强行拉伸填满整个黑框（画面可能会有一点点变形，但监控最常用）
   cover: 等比例放大填满整个黑框（画面不会变形，但边缘可能会被裁掉一点）
   你可以把 fill 改成 cover 对比一下哪个效果更好！
  */
  object-fit: fill; 
}

/* 侧边栏样式 */
.sidebar-area {
  width: 350px;
  background: #1f1f1f;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  border: 1px solid #333;
}
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #333;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.alarm-list { flex: 1; overflow-y: auto; padding: 12px; }
.alarm-item {
  display: flex; margin-bottom: 12px; background: #2a2a2a;
  padding: 10px; border-radius: 6px; border-left: 4px solid #ff4d4f;
}
.alarm-img { width: 100px; height: 70px; object-fit: cover; border-radius: 4px; margin-right: 12px; }
.alarm-info { display: flex; flex-direction: column; justify-content: center; }
</style>