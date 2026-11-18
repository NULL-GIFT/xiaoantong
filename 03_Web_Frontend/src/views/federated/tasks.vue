<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="page-header">
      <div class="title-group">
        <h2>📈 联邦学习模型演进与任务大盘</h2>
        <p class="sub-text">监控边缘节点参与的聚合任务，分析模型精度演进趋势</p>
      </div>
    </div>

    <div class="aero-card" style="margin-bottom: 20px; padding: 20px;">
      <a-form layout="inline" :model="queryParams">
        <a-form-item label="模型版本号">
          <a-input v-model:value="queryParams.version" placeholder="例如 v1.2" allowClear style="width: 200px" @pressEnter="handleSearch" />
        </a-form-item>
        
        <a-form-item label="技能算法">
          <a-select v-model:value="queryParams.skillCode" placeholder="请选择算法类型" style="width: 180px" allowClear @change="handleSearch">
            <a-select-option v-for="skill in skillList" :key="skill.skillCode" :value="skill.skillCode">
              {{ skill.skillName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" @click="handleSearch" class="aero-btn-primary">
            <template #icon><search-outlined /></template> 查询演进记录
          </a-button>
          <a-button style="margin-left: 10px" @click="resetQuery">重置</a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-row :gutter="24">
      <a-col :span="24" :lg="8" style="margin-bottom: 24px;">
        <div class="aero-card chart-container">
          <div class="card-title">模型识别精度演进 (Accuracy Trend)</div>
          <div ref="chartRef" style="width: 100%; height: 320px;"></div>
        </div>
      </a-col>

      <a-col :span="24" :lg="16">
        <div class="aero-card">
          <div class="card-title" style="margin-bottom: 16px;">全局模型聚合流水表</div>
          <a-table 
            :columns="modelColumns" 
            :data-source="modelList" 
            :loading="loading" 
            row-key="id"
            class="aero-table"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'skillCode'">
                <a-tag color="blue">{{ getSkillName(record.skillCode) }}</a-tag>
              </template>

              <template v-else-if="column.key === 'version'">
                <span style="font-family: monospace; font-weight: bold; font-size: 14px; color: #0ea5e9;">
                  {{ record.version }}
                </span>
              </template>

              <template v-else-if="column.key === 'accuracy'">
                <div style="display: flex; align-items: center;">
                  <span style="width: 45px; font-weight: bold;">{{ record.accuracy }}%</span>
                  <a-progress :percent="Number(record.accuracy)" :show-info="false" stroke-color="#10b981" style="width: 80px; margin-left: 8px;" />
                </div>
              </template>

              <template v-else-if="column.key === 'status'">
                <a-tag v-if="record.status === 'active'" color="success">
                  <template #icon><check-circle-outlined /></template> 现役生效
                </a-tag>
                <a-tag v-else-if="record.status === 'training'" color="processing">
                  <template #icon><sync-outlined spin /></template> 云端聚合中
                </a-tag>
                <a-tag v-else-if="record.status === 'archived'" color="default">
                  历史归档
                </a-tag>
                <a-tag v-else color="default">{{ record.status || '未知状态' }}</a-tag>
              </template>

              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="downloadModel(record)" :disabled="record.status === 'training'">
                  下载权重
                </a-button>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { SearchOutlined, CheckCircleOutlined, SyncOutlined } from '@ant-design/icons-vue';
import * as echarts from 'echarts';
import request from '@/utils/request';

// 查询参数与表格状态
const queryParams = ref({ version: '', skillCode: undefined });
const loading = ref(false);
const modelList = ref([]);
const chartRef = ref(null);
let echartInstance = null;
// 🌟 新增：存放从后端拉取的真实技能字典
const skillList = ref([]);
// 🌟 动态获取真实中文名称
const getSkillName = (code) => {
  const skill = skillList.value.find(s => s.skillCode === code);
  return skill ? skill.skillName : code;
};
// 🌟 新增：获取全量技能字典
const fetchSkills = async () => {
  try {
    const res = await request.get('/api/skill/list');
    skillList.value = res.data?.records || res.data?.list || res.data || [];
  } catch (err) {
    console.error('获取技能字典失败');
  }
};
// 表格列定义 (完全对齐 SysFedModel 实体的驼峰字段名)
const modelColumns = [
  { title: '模型版本号', dataIndex: 'version', key: 'version' },
  { title: '技能算法', dataIndex: 'skillCode', key: 'skillCode' },
  { title: '迭代轮次', dataIndex: 'round', key: 'round', align: 'center' },
  { title: '模型精度', dataIndex: 'accuracy', key: 'accuracy' },
  { title: '参与节点数', dataIndex: 'nodes', key: 'nodes', align: 'center' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '生成时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', align: 'center' }
];


// ================= 核心：真实后端接口拉取 =================
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/fed/list', { params: queryParams.value });
    const rawData = res.data?.records || res.data?.list || res.data || [];
    modelList.value = rawData;
    renderChart(rawData);
  } catch (err) {
    console.error(err);
    message.error('数据拉取失败，请检查网络或后端接口是否正常');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { fetchData(); };
const resetQuery = () => { queryParams.value = { version: '', skillCode: undefined }; fetchData(); };

// ================= 核心：基于真实数据的图表渲染 =================
const renderChart = (data) => {
  if (!chartRef.value) return;
  if (!echartInstance) echartInstance = echarts.init(chartRef.value);

  // 如果没有数据，清空图表并提示
  if (!data || data.length === 0) {
    echartInstance.clear();
    return;
  }

  // 对数据按照 round (轮次) 进行升序排序，保证曲线是向右推进的
  const sortedData = [...data].sort((a, b) => a.round - b.round);
  
  // 提取 X 轴 (版本号/轮次) 和 Y 轴 (精度) 数据
  const xAxisData = sortedData.map(item => item.version);
  const accuracies = sortedData.map(item => item.accuracy);

  const option = {
    tooltip: { 
      trigger: 'axis',
      formatter: '{b} <br/>模型精度: <b>{c}%</b>'
    },
    grid: { left: '12%', right: '5%', bottom: '15%', top: '15%' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      min: 50, // Y轴起点为50
      max: 100,
      axisLabel: { formatter: '{value}%', color: '#64748b' },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
    },
    series: [
      {
        name: '模型精度',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#0ea5e9', borderColor: '#fff', borderWidth: 2 },
        lineStyle: { width: 3, shadowColor: 'rgba(14,165,233,0.4)', shadowBlur: 8 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14, 165, 233, 0.4)' },
            { offset: 1, color: 'rgba(14, 165, 233, 0.0)' }
          ])
        },
        data: accuracies
      }
    ]
  };
  echartInstance.setOption(option, true);
};

// 模型下载
const downloadModel = (record) => {
  if (!record.filePath) {
    return message.warning('模型物理路径缺失，无法下载');
  }
  // 构建真实下载链接
  message.success('正在建立下载链接...');
  window.open(`${import.meta.env.VITE_APP_BASE_API}/api/fed/download?version=${record.version}`, '_blank');
};

onMounted(async () => {
  await fetchSkills();
  fetchData();
  window.addEventListener('resize', () => { if (echartInstance) echartInstance.resize(); });
});
</script>

<style scoped>
.aero-dashboard { padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; }
.aero-card { background: rgba(255,255,255,0.9); border-radius: 12px; border: 1px solid #fff; box-shadow: 0 4px 20px rgba(0,0,0,0.03); padding: 20px; }
.page-header { margin-bottom: 24px; }
.title-group h2 { font-size: 24px; font-weight: 800; color: #0f172a; margin-bottom: 6px; }
.sub-text { font-size: 13px; color: #64748b; }
.card-title { font-size: 16px; font-weight: 700; color: #1e293b; }
.chart-container { height: 100%; display: flex; flex-direction: column; }
.aero-btn-primary { background: #0f172a; border: none; }
.aero-btn-primary:hover { background: #1e293b; }

/* 极致表格美化 */
:deep(.aero-table .ant-table) { background: transparent; }
:deep(.aero-table .ant-table-thead > tr > th) { background: #f8fafc; color: #475569; font-weight: 600; border-bottom: 1px solid #e2e8f0; }
</style>