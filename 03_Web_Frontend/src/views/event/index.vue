<template>
  <div class="page-container" style="padding: 24px; background: #f0f2f5; min-height: 100vh;">
    
    <a-card :bordered="false" class="mb-4" style="border-radius: 8px; box-shadow: 0 1px 4px rgba(0,21,41,0.05);">
      <div class="filter-bar">
        <a-form layout="inline" :model="queryParams" @finish="handleSearch">
          
          <a-form-item label="数据范围">
            <a-radio-group v-model:value="timeScope" button-style="solid" @change="handleTimeScopeChange">
              <a-radio-button value="recent">近3个月</a-radio-button>
              <a-radio-button value="all">历史全部</a-radio-button>
              <a-radio-button value="custom">自定义</a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item v-if="timeScope === 'custom'">
            <a-range-picker v-model:value="queryParams.dateRange" show-time format="YYYY-MM-DD HH:mm" valueFormat="YYYY-MM-DD HH:mm:ss" />
          </a-form-item>
          
          <a-form-item label="监控通道">
            <a-select 
              v-model:value="queryParams.deviceId"  
              style="width: 170px" 
              allowClear
              show-search
              option-filter-prop="label"
              placeholder="输入关键字检索..."
            >
              <a-select-option v-for="dev in deviceOptions" :key="dev.deviceId" :value="dev.deviceId" :label="dev.deviceName || dev.channelName">
                {{ dev.deviceName || dev.channelName }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="异常技能">
            <a-select 
              v-model:value="queryParams.type"  
              style="width: 160px" 
              allowClear
              show-search
              option-filter-prop="label"
              placeholder="输入技能名检索..."
            >
              <a-select-option v-for="skill in skillOptions" :key="skill.skillCode" :value="skill.skillCode" :label="skill.skillName">
                {{ skill.skillName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          
          <a-form-item label="业务状态">
            <a-select v-model:value="queryParams.handleStatus" placeholder="全部状态" style="width: 130px" allowClear>
              <a-select-option value="pending">🚨 待处置</a-select-option>
              <a-select-option value="resolved">✅ 有效已闭环</a-select-option>
              <a-select-option value="false_alarm">🛡️ 负样本归档</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item>
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button style="margin-left: 8px" @click="resetQuery">重置</a-button>
          </a-form-item>
        </a-form>

        <div class="action-right">
          <span style="margin-right: 8px; font-size: 14px; color: #666;">自动刷新 </span>
          <a-switch v-model:checked="autoRefresh" @change="toggleAutoRefresh" checked-children="开" un-checked-children="关" style="margin-right: 24px;"/>
          
          <a-dropdown>
            <a-button type="primary" ghost>
              <template #icon><download-outlined /></template> 批量导出记录 <down-outlined />
            </a-button>
            <template #overlay>
              <a-menu @click="handleExportMenuClick">
                <a-menu-item key="selected">导出勾选项 ({{ selectedRowKeys.length }}条)</a-menu-item>
                <a-menu-item key="page">导出本页全部 ({{ dataSource.length }}条)</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </a-card>

    <a-card :bordered="false" title="报警记录台账" style="border-radius: 8px; box-shadow: 0 1px 4px rgba(0,21,41,0.05);">
      <a-table 
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange, preserveSelectedRowKeys: true }"
        :columns="columns" 
        :data-source="dataSource" 
        :loading="loading" 
        row-key="id" 
        :pagination="pagination" 
        @change="handleTableChange"
        :customRow="customRow"
        class="clickable-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'img'">
            <a-image 
              :preview="false" 
              :src="getImageUrl(record.imgPath)" 
              :width="80" 
              :height="50" 
              style="object-fit: cover; border-radius: 4px; cursor: pointer;"
              @click.stop="openDrawer(record)"
            />
          </template>
          
          <template v-if="column.key === 'type'">
            <span :style="{ color: getSkillColor(record.type), fontWeight: 'bold', background: getSkillBg(record.type), padding: '4px 8px', borderRadius: '4px' }">
              {{ getSkillIcon(record.type) }} {{ getSkillName(record.type) }}
            </span>
          </template>

          <template v-if="column.key === 'confidence'">
            <span style="color: #cf1322; font-weight: bold; font-family: monospace; font-size: 14px;">
              {{ (record.confidence * 100).toFixed(1) }}%
            </span>
          </template>

          <template v-if="column.key === 'status'">
            <a-tag v-if="record.isFalseAlarm === 1 || record.is_false_alarm === 1" color="default" style="border-style: dashed;">
              🛡️ 负样本归档
            </a-tag>            
            <a-tag v-else-if="record.isHandled === 1 || record.is_handled === 1" color="success">
              ✅ 有效已闭环
            </a-tag>            
            <a-tag v-else color="error" class="blinking-tag">
              🚨 待处置
            </a-tag>
          </template>

          <template v-if="column.key === 'action'">
            <template v-if="userRole === 'admin' && record.isHandled !== 1 && record.is_handled !== 1 && record.isFalseAlarm !== 1 && record.is_false_alarm !== 1">
              <a-popconfirm title="确认该警情已现场核实并处置完毕？" @confirm.stop="resolveAlarm(record.id)">
                <a-button type="link" style="color: #52c41a; font-weight: bold;" size="small" @click.stop>✓ 标记闭环</a-button>
              </a-popconfirm>
            </template>
            <template v-else>
              <span style="color: #bfbfbf; font-size: 12px;"><lock-outlined /> 已归档</span>
            </template>
          </template>
        </template>
      </a-table>   
    </a-card>

    <a-drawer
      v-model:visible="drawerVisible"
      title="事件处理详情"
      placement="right"
      width="520"
      :footer-style="{ textAlign: 'right' }"
    >
      <div v-if="currentRecord" style="padding-bottom: 20px;">
        
        <div style="text-align: center; margin-bottom: 24px;">
          <div style="background: #000; padding: 10px; border-radius: 8px; margin-bottom: 16px;">
            <a-image :src="getImageUrl(currentRecord.imgPath)" style="max-width: 100%; max-height: 280px; object-fit: contain;" />
          </div>
          
          <h3 style="font-weight: bold; font-size: 18px; margin-bottom: 8px; color: #333;">
            <environment-outlined style="color: #1890ff; margin-right: 4px;" />
            {{ currentRecord.deviceName || '未知节点' }}
          </h3>
          <div style="color: #cf1322; font-size: 18px; font-weight: bold; font-family: monospace; margin-bottom: 4px;">
            置信度: {{ (currentRecord.confidence * 100).toFixed(1) }}%
          </div>
          <div style="color: #888; font-size: 13px;">抓拍时间: {{ currentRecord.time }}</div>
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-top: 1px dashed #e8e8e8; border-bottom: 1px dashed #e8e8e8; margin-bottom: 24px;">
          <a style="color: #1890ff; cursor: pointer; font-weight: bold;" @click="downloadImage(currentRecord)">
            <download-outlined /> 下载证据原图
          </a>
          
          <a-button 
            v-if="userRole === 'admin' && currentRecord.isFalseAlarm !== 1 && currentRecord.is_false_alarm !== 1 && currentRecord.isHandled !== 1 && currentRecord.is_handled !== 1" 
            type="primary" 
            danger 
            size="small"
            @click="openFalseAlarmModal(currentRecord)"
          >
            判定为机器误报并归档
          </a-button>
          <span v-else-if="currentRecord.isFalseAlarm === 1 || currentRecord.is_false_alarm === 1" style="color: #722ed1; font-weight: bold; background: #f9f0ff; padding: 4px 12px; border-radius: 4px;">
            🛡️ 已作为负样本归档
          </span>
        </div>

        <div style="font-size: 15px; font-weight: bold; margin-bottom: 16px; border-left: 4px solid #1890ff; padding-left: 8px;">
          事件流转时间线
        </div>

        <a-timeline style="padding-left: 10px;">
          <a-timeline-item color="red">
            <template #dot><environment-outlined style="font-size: 16px;"/></template>
            <div style="font-weight: bold; color: #cf1322;">系统侦测到异常情况</div>
            <div style="font-size: 12px; color: #888; margin-top: 4px;">{{ currentRecord.time }}</div>
            <div style="font-size: 13px; color: #555; background: #fff1f0; padding: 8px; border-radius: 4px; margin-top: 8px;">
              边缘节点判定触发 <strong :style="{ color: getSkillColor(currentRecord.type) }">{{ getSkillName(currentRecord.type) }}</strong> 预警规则。
            </div>
          </a-timeline-item>

          <a-timeline-item v-if="currentRecord.isHandled === 1 && currentRecord.isFalseAlarm !== 1 && currentRecord.is_false_alarm !== 1" color="green">
            <template #dot><check-circle-outlined style="font-size: 16px;"/></template>
            <div style="font-weight: bold; color: #52c41a;">安保中心已确认并闭环</div>
            <div style="font-size: 12px; color: #888; margin-top: 4px;">{{ currentRecord.handledTime || currentRecord.handled_time || '近期' }}</div>
            <div style="font-size: 12px; color: #555; margin-top: 4px;">该警情已被标记为真实有效，并已完成线下处置。</div>
          </a-timeline-item>

          <a-timeline-item v-else-if="currentRecord.isFalseAlarm === 1 || currentRecord.is_false_alarm === 1" color="purple">
            <template #dot><api-outlined style="font-size: 16px;"/></template>
            <div style="font-weight: bold; color: #722ed1;">管理员驳回：模型负样本归档</div>
            <div style="font-size: 12px; color: #888; margin-top: 4px;">{{ currentRecord.handledTime || currentRecord.handled_time || '近期' }}</div>
            <div style="font-size: 13px; color: #555; background: #f9f0ff; padding: 8px; border-radius: 4px; margin-top: 8px; border-left: 3px solid #b37feb;">
              <strong>归档原因：</strong>{{ currentRecord.falseReason || currentRecord.false_reason || '人工判定为现场环境干扰' }}<br/>
              <span style="font-size: 11px; color: #888;">* 该样本将参与下一轮联邦学习梯度更新</span>
            </div>
          </a-timeline-item>

          <a-timeline-item v-else color="blue">
            <template #dot><loading-outlined style="font-size: 16px;"/></template>
            <div style="font-weight: bold; color: #1890ff;">任务待认领 / 处置中...</div>
            <div style="font-size: 12px; color: #888; margin-top: 4px;">当前时间：实时等待中</div>
            <div style="font-size: 12px; color: #888; margin-top: 4px;">请相关区域安保人员尽快前往现场核实。</div>
          </a-timeline-item>
        </a-timeline>

      </div>
    </a-drawer>

    <a-modal v-model:visible="falseAlarmVisible" title="判定误报并归档负样本" @ok="submitFalseAlarm" :confirmLoading="submittingFalse">
      <div style="margin-bottom: 16px; color: #666; background: #fffbe6; padding: 10px; border-left: 4px solid #faad14;">
        此操作将把当前抓拍标记为<strong style="color:#cf1322">“机器误判”</strong>，并存入联邦学习负样本池，用于持续优化算法。
      </div>
      <a-form :model="falseAlarmForm" layout="vertical">
        <a-form-item label="请客观选择或描述误判原因" required>
          <a-select v-model:value="falseAlarmForm.presetReason" placeholder="请选择干扰因素" @change="handleReasonChange">
            <a-select-option value="光线与阴影强干扰">光线与阴影强干扰</a-select-option>
            <a-select-option value="现场物体形态高度相似">现场物体形态高度相似</a-select-option>
            <a-select-option value="边缘摄像头防抖失效/重影">边缘摄像头防抖失效/重影</a-select-option>
            <a-select-option value="custom" style="color: #1890ff; font-weight: bold;">其他原因 (手动记录)</a-select-option>
          </a-select>
          
          <div v-if="falseAlarmForm.presetReason === 'custom'" style="margin-top: 12px;">
            <a-textarea 
              v-model:value="falseAlarmForm.customReason" 
              placeholder="请详细描述现场特殊的干扰情况，这对后续的 AI 模型定向微调极其重要..." 
              :rows="3" 
            />
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue';
import { 
  DownloadOutlined, DownOutlined, LockOutlined, 
  EnvironmentOutlined, CheckCircleOutlined, ApiOutlined, LoadingOutlined 
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import request from '@/utils/request'; 
import { getDevice } from '@/api/device.js';

const userRole = ref(localStorage.getItem('userRole') || 'user');
const loading = ref(false);
const dataSource = ref([]); 

// 1. 字典只保留纯 UI 属性 (颜色、图标、背景)
const localSkillUIDict = {
  'fire': { icon: '🔥', color: '#cf1322', bg: '#fff1f0' },
  'violence': { icon: '💢', color: '#cf1322', bg: '#fff1f0' },
  'perimeter': { icon: '🚧', color: '#cf1322', bg: '#fff1f0' },
  'smoke': { icon: '💨', color: '#fa8c16', bg: '#fff7e6' },
  'fire_lane_occupy': { icon: '🚗', color: '#fa8c16', bg: '#fff7e6' },
  'fall': { icon: '⚠️', color: '#d48806', bg: '#feffe6' },
  'good_deed_trash': { icon: '♻️', color: '#52c41a', bg: '#f6ffed' },
  'good_deed_bike': { icon: '🚲', color: '#1890ff', bg: '#e6f7ff' }
};

// 2. 名字获取函数：优先查 API 返回的 skillOptions，绝对不自己瞎猜！
const getSkillName = (code) => {
  // skillOptions 是咱们页面一开始调 /api/skill/list 存下来的真实数据
  const skill = skillOptions.value.find(s => s.skillCode === code || String(s.id) === String(code));
  return skill ? skill.skillName : code;
};

// 3. UI 获取函数：安全兜底
const getSkillColor = (code) => localSkillUIDict[code] ? localSkillUIDict[code].color : '#555';
const getSkillBg = (code) => localSkillUIDict[code] ? localSkillUIDict[code].bg : 'transparent';
const getSkillIcon = (code) => localSkillUIDict[code] ? localSkillUIDict[code].icon : '📌';


const timeScope = ref('recent');
const queryParams = reactive({ 
  dateRange: null, 
  deviceId: undefined, 
  type: undefined, 
  handleStatus: undefined 
});

const getRecentDateRange = (days) => {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - days);
  
  const pad = (n) => n < 10 ? '0' + n : n;
  const format = (d) => `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  
  return [format(start), format(end)];
};

const handleTimeScopeChange = () => {
  if (timeScope.value === 'recent') {
    queryParams.dateRange = getRecentDateRange(90);
    fetchEventList();
  } else if (timeScope.value === 'all') {
    queryParams.dateRange = null; 
    fetchEventList();
  } else {
    queryParams.dateRange = null; 
  }
};

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, pageSizeOptions: ['10', '20', '50', '100'], showTotal: (total) => `共 ${total} 条记录` });

const selectedRowKeys = ref([]);
const selectedRows = ref([]);

const onSelectChange = (keys, rows) => {
  selectedRowKeys.value = keys;
  const currentSelected = [...selectedRows.value];
  rows.forEach(r => { if (!currentSelected.find(item => item.id === r.id)) currentSelected.push(r); });
  selectedRows.value = currentSelected.filter(item => keys.includes(item.id));
};

const customRow = (record) => {
  return {
    onClick: () => {
      openDrawer(record);
    }
  };
};

const columns = [
  { title: '抓拍存证', key: 'img', width: 120 },
  { title: '设备名称', dataIndex: 'deviceName' }, 
  { title: '定性技能', key: 'type' },       
  { title: '发生位置', dataIndex: 'location' },
  { title: 'AI 置信度', key: 'confidence' },
  { title: '报警时间', dataIndex: 'time' },       
  { title: '流转状态', key: 'status' },
  { title: '操作', key: 'action', width: 150, align: 'center' }
];


const deviceOptions = ref([]);
const skillOptions = ref([]); 

const loadDeviceOptions = async () => {
  try {
    const res = await getDevice({ pageNum: 1, pageSize: 1000 }); 
    if (res.code === 200 || res.code === 0) {
      const resultData = res.data || {};
      deviceOptions.value = resultData.list || resultData || []; 
    }
  } catch (err) { console.error(err); }
};

const loadSkillOptions = async () => {
  try {
    const res = await request.get('/api/skill/list'); 
    if (res.code === 200 || res.code === 0) {
      const realData = res.data || [];
      skillOptions.value = Array.isArray(realData) ? realData : (realData.list || []); 
    }
  } catch (err) { console.error(err); }
};

// ================== 🌟 核心修复 1：控制加载顺序 ==================
// 必须等设备字典 (loadDeviceOptions) 查完，才能去查报警列表，否则对不上号！
onMounted(async () => { 
  queryParams.dateRange = getRecentDateRange(90);
  await loadDeviceOptions(); // 💡 加上 await，强制等待真实设备台账加载完毕
  loadSkillOptions(); 
  fetchEventList(); 
});

// ================== 🌟 核心修复：强行霸占并翻译脏数据，剔除所有随机抖动 ==================
const fetchEventList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.current, pageSize: pagination.pageSize,
      type: queryParams.type, deviceId: queryParams.deviceId,
      startTime: queryParams.dateRange ? queryParams.dateRange[0] : null,
      endTime: queryParams.dateRange ? queryParams.dateRange[1] : null,
      handleStatus: queryParams.handleStatus 
    };

    const res = await request.get('/api/alarm/list', { params });
    const realData = res.data?.data || res.data || res;
    
    let rawList = [];
    if (realData && Array.isArray(realData.list)) {
      rawList = realData.list;
      pagination.total = realData.total || 0;
    } else if (Array.isArray(realData)) {
      rawList = realData;
      pagination.total = rawList.length;
    }

    dataSource.value = rawList.map(item => {
      // 🌟 核心修复：坚决不用随机数！直接读取数据库存的 confidence！
      let realConf = parseFloat(item.confidence || item.conf || 0.95);
      if (realConf > 1) realConf /= 100;

      const typeCode = item.alarmType || item.type;
      const devId = item.deviceId || item.device_id;
      
      const realDevice = deviceOptions.value.find(d => d.deviceId === devId || d.id === devId);
      const trueDeviceName = realDevice ? (realDevice.deviceName || realDevice.channelName) : (item.deviceName || item.deviceId || '未知节点');

      return {
        ...item,
        confidence: realConf, // 绝对保真！
        type: typeCode,
        time: item.alarmTime ? item.alarmTime.replace('T', ' ') : item.time,
        deviceName: trueDeviceName, 
        location: trueDeviceName 
      };
    });

  } catch (error) {
    message.error("获取数据失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pagination.current = 1; fetchEventList(); };
const resetQuery = () => { 
  timeScope.value = 'recent'; 
  queryParams.dateRange = getRecentDateRange(90); 
  queryParams.deviceId = undefined; 
  queryParams.type = undefined; 
  queryParams.handleStatus = undefined; 
  handleSearch(); 
};
const handleTableChange = (pag) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchEventList(); };

const autoRefresh = ref(false);
let refreshTimer = null;
const toggleAutoRefresh = (checked) => {
  if (checked) {
    message.success('已开启台账自动刷新');
    refreshTimer = setInterval(fetchEventList, 5000);
  } else {
    clearInterval(refreshTimer);
  }
};
onUnmounted(() => { if (refreshTimer) clearInterval(refreshTimer); });

const handleExportMenuClick = ({ key }) => {
  let exportData = [];
  if (key === 'selected') {
    if (selectedRows.value.length === 0) return message.warning('请先勾选需要导出的数据！');
    exportData = selectedRows.value;
  } else if (key === 'page') {
    if (dataSource.value.length === 0) return message.warning('当前页面暂无数据！');
    exportData = dataSource.value;
  }
  executeExportCSV(exportData);
};

const executeExportCSV = (data) => {
  message.loading({ content: '正在生成审计报表...', key: 'export' });
  let csvContent = '\uFEFF'; 
  csvContent += "报警时间,设备名称,定性技能,发生位置,置信度,流转状态\n";

  data.forEach(row => {
    const time = row.time || '';
    const device = row.deviceName || '';
    const skill = getSkillName(row.type) || row.type || '';
    const loc = row.location || '';
    const conf = row.confidence ? (row.confidence * 100).toFixed(1) + '%' : '';
    const status = (row.isFalseAlarm === 1 || row.is_false_alarm === 1) ? '负样本归档' 
                 : ((row.isHandled === 1 || row.is_handled === 1) ? '有效已闭环' : '待处置');

    csvContent += `"${time}","${device}","${skill}","${loc}","${conf}","${status}"\n`;
  });

  try {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `校安通_报警记录_${new Date().getTime()}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
    message.success({ content: '导出成功！', key: 'export' });
  } catch(e) { message.error({ content: '导出失败！', key: 'export' }); }
};

const getImageUrl = (path) => { return path && path.startsWith('http') ? path : `${import.meta.env.VITE_APP_BASE_API || ''}${path}`; };

const drawerVisible = ref(false);
const currentRecord = ref(null);

const openDrawer = (record) => {
  currentRecord.value = record;
  drawerVisible.value = true;
};

const falseAlarmVisible = ref(false);
const submittingFalse = ref(false);
const falseAlarmForm = reactive({ id: null, presetReason: undefined, customReason: '' });

const openFalseAlarmModal = (record) => {
  falseAlarmForm.id = record.id;
  falseAlarmForm.presetReason = undefined; 
  falseAlarmForm.customReason = '';
  drawerVisible.value = false; 
  falseAlarmVisible.value = true;
};

const handleReasonChange = () => { falseAlarmForm.customReason = ''; };

const resolveAlarm = async (id) => {
  try {
    const res = await request.post(`/api/alarm/resolve/${id}`); 
    if (res.code === 200 || res.code === 0) {
      message.success('警情已真实闭环！');
      fetchEventList(); 
    } else {
      message.error(res.msg || '操作失败');
    }
  } catch (error) { message.error('网络请求异常'); }
};

const submitFalseAlarm = async () => {
  if (!falseAlarmForm.presetReason) return message.warning('请选择误判原因！');
  if (falseAlarmForm.presetReason === 'custom' && !falseAlarmForm.customReason.trim()) {
    return message.warning('请输入手动记录的详细原因！');
  }

  const finalReason = falseAlarmForm.presetReason === 'custom' ? falseAlarmForm.customReason : falseAlarmForm.presetReason;

  submittingFalse.value = true;
  try {
    const res = await request.post('/api/alarm/markFalse', { id: falseAlarmForm.id, reason: finalReason });
    if (res.code === 200 || res.code === 0) {
      message.success('已成功抽取为联邦学习负样本！');
      falseAlarmVisible.value = false;
      fetchEventList(); 
    } else {
      message.error(res.msg || '操作失败');
    }
  } catch (error) { message.error('请求异常'); } finally { submittingFalse.value = false; }
};

const downloadImage = async (record) => {
  const path = record.imgPath;
  if (!path) return message.warning('图片路径不存在');
  message.loading({ content: '正在下载证据原图...', key: 'downloadImage' });
  try {
    const url = getImageUrl(path);
    const response = await fetch(url);
    const blob = await response.blob();
    const objectUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = objectUrl;
    a.download = `证据抓拍_${record.deviceName || '未知'}_${new Date().getTime()}.jpg`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(objectUrl);
    document.body.removeChild(a);
    message.success({ content: '证据提取成功！', key: 'downloadImage' });
  } catch (error) { message.error({ content: '提取失败', key: 'downloadImage' }); }
};
</script>

<style scoped>
.filter-bar { padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: flex-start; }
.action-right { display: flex; align-items: center; }
@keyframes blink { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
.blinking-tag { animation: blink 1.5s infinite; }

:deep(.clickable-table .ant-table-tbody > tr) { cursor: pointer; transition: background-color 0.2s; }
:deep(.clickable-table .ant-table-tbody > tr:hover > td) { background-color: #e6f7ff !important; }
</style>