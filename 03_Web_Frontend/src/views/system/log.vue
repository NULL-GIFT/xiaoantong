<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="page-header">
      <div class="title-group">
        <h2>🛡️ 系统安全审计追踪 (Audit Trail)</h2>
        <p class="sub-text">实时监控并记录全域操作员的敏感业务行为与权限变更</p>
      </div>
    </div>

    <div class="aero-card tabs-container">
      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange" size="large">
        <a-tab-pane key="account" tab="👥 账号与权限审计 (IAM Logs)"></a-tab-pane>
        <a-tab-pane key="business" tab="📊 业务与数据管控 (Data Logs)"></a-tab-pane>
      </a-tabs>

      <div class="filter-bar">
        <a-space wrap align="center">
          <span class="filter-label">操作人：</span>
          <a-input v-model:value="queryParams.operator" placeholder="请输入操作人账号" style="width: 180px" allowClear @input="handleAutoSearch" />
          <span class="filter-label" style="margin-left: 16px;">执行状态：</span>
          <a-select v-model:value="queryParams.status" style="width: 120px" allowClear placeholder="全选" @change="applySearch">
            <a-select-option value="1">✅ 成功</a-select-option>
            <a-select-option value="0">❌ 失败</a-select-option>
          </a-select>
          <a-button type="primary" @click="applySearch" style="margin-left: 8px;">查询记录</a-button>
        </a-space>

        <a-button class="btn-export" @click="handleRealExport" :loading="exportLoading">
          <template #icon><download-outlined /></template> 导出真实审计报表 (CSV)
        </a-button>
      </div>

      <a-table :columns="columns" :data-source="tableData" :loading="loading" row-key="id" :pagination="pagination" @change="handleTableChange" class="aero-table">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'operator'"><div style="font-weight: 600; color: #1e293b;">@{{ record.operator }}</div></template>
          <template v-if="column.key === 'module'"><a-tag color="blue" style="font-weight: bold;">{{ record.module }}</a-tag></template>
          <template v-if="column.key === 'action'"><span style="color: #334155; font-weight: 500;">{{ record.action }}</span></template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '执行成功' : '被拦截/失败' }}</a-tag>
          </template>
          <template v-if="column.key === 'createTime'"><span style="color: #64748b; font-size: 13px; font-family: monospace;">{{ formatDate(record.createTime) }}</span></template>
          <template v-if="column.key === 'details'">
            <a-button type="link" size="small" @click="showDetails(record)">查看详情</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="detailsVisible" title="📄 审计详细信息" :footer="null" width="500px" destroyOnClose>
      <div class="log-detail-box">
        <div class="detail-row">
          <span class="d-label">操作事由 / 详情：</span>
          <div class="reason-highlight">{{ parseReason(currentRecord?.params) }}</div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { DownloadOutlined } from '@ant-design/icons-vue';
import request from '@/utils/request';

const activeTab = ref('account');
const loading = ref(false);
const exportLoading = ref(false);
const tableData = ref([]);

const queryParams = reactive({ operator: '', status: undefined });
const pagination = reactive({ current: 1, pageSize: 15, total: 0, showSizeChanger: true });

const columns = [
  { title: '操作人', key: 'operator', width: '16%' },
  { title: '业务模块', key: 'module', width: '16%' },
  { title: '动作定性', key: 'action', width: '26%' },
  { title: '执行状态', key: 'status', width: '12%' },
  { title: '时间', key: 'createTime', width: '18%' },
  { title: '补充详情', key: 'details', width: '12%', align: 'center' }
];

const formatDate = (dateString) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'); 
};

// 🌟 核心提取函数：把乱七八糟的旧数据全部清洗成纯净文字
const parseReason = (text) => {
  if (!text || text.trim() === '' || text === 'null') return '无';
  
  // 1. 兼容旧版本带有 JSON 格式的数据
  if (text.includes('{') && text.includes('}')) {
    try {
      const obj = JSON.parse(text);
      return obj['操作原因'] || obj['注销原因'] || obj['卸载原因'] || '无';
    } catch(e) {}
  }

  // 2. 剥离之前自带的前缀 (如 "操作原因: xxx" 变成 "xxx")
  let cleanText = text.replace(/^(操作原因:\s*|注销原因:\s*|卸载原因:\s*)/, '').trim();
  
  // 3. 过滤掉无意义的系统参数 (比如 "更新参数: id=2")
  if (cleanText.startsWith('id=') || cleanText.startsWith('更新参数')) {
    return '无';
  }

  return cleanText || '无';
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params = { pageNum: pagination.current, pageSize: pagination.pageSize, logType: activeTab.value, operator: queryParams.operator || undefined, status: queryParams.status !== undefined ? queryParams.status : undefined };
    const res = await request.get('/api/log/list', { params });
    if (res.code === 200 || res.code === 0) {
      tableData.value = res.data?.list || res.data || [];
      pagination.total = res.data?.total || tableData.value.length;
    }
  } finally { loading.value = false; }
};

const handleTabChange = () => { pagination.current = 1; queryParams.operator = ''; queryParams.status = undefined; fetchData(); };
const applySearch = () => { pagination.current = 1; fetchData(); };
let searchTimeout = null;
const handleAutoSearch = () => { if (searchTimeout) clearTimeout(searchTimeout); searchTimeout = setTimeout(applySearch, 500); };
const handleTableChange = (pag) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchData(); };

const detailsVisible = ref(false);
const currentRecord = ref(null);

const showDetails = (record) => {
  currentRecord.value = record;
  detailsVisible.value = true;
};

const handleRealExport = () => {
  if(tableData.value.length === 0) return message.warning("无数据可导出");
  exportLoading.value = true;
  let csvContent = "data:text/csv;charset=utf-8,\uFEFF";
  csvContent += "操作人,业务模块,动作定性,执行状态,发生时间,事由与详情\n";
  tableData.value.forEach(row => {
    // 💡 导出报表时，同样调用清洗函数，让 Excel 看起来非常专业
    const pureReason = parseReason(row.params).replace(/"/g, '""'); 
    const statusStr = row.status === 1 ? '成功' : '失败';
    csvContent += `${row.operator},${row.module},${row.action},${statusStr},${formatDate(row.createTime)},"${pureReason}"\n`;
  });
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement("a");
  link.setAttribute("href", encodedUri);
  link.setAttribute("download", `XAT_审计流水_${activeTab.value}.csv`);
  document.body.appendChild(link);
  link.click(); document.body.removeChild(link);
  message.success("✅ 报表已生成"); exportLoading.value = false;
};

onMounted(() => fetchData());
</script>

<style scoped>
.aero-dashboard { padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; }
.aero-card { background: rgba(255,255,255,0.85); border-radius: 12px; border: 1px solid #fff; }
.tabs-container { padding: 0 20px 20px 20px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding: 10px 0;}
.filter-label { font-size: 14px; font-weight: 600; color: #475569; }
.btn-export { background: #fff; border: 1px solid #0f172a; color: #0f172a; font-weight: 600;}
.btn-export:hover { background: #0f172a; color: #fff; }

.log-detail-box { padding: 10px; }
.detail-row { margin-bottom: 16px; display: flex; flex-direction: column; gap: 8px;}
.d-label { font-weight: 600; color: #475569; font-size: 13px;}
.reason-highlight { font-size: 14px; color: #0f172a; background: #f8fafc; padding: 16px; border-radius: 8px; border-left: 4px solid #3b82f6; line-height: 1.6; word-break: break-all;}

:deep(.aero-table .ant-table-thead > tr > th) { background-color: #f8fafc !important; font-weight: 600; color: #475569; border-bottom: 1px solid #e2e8f0 !important; padding: 16px; }
:deep(.aero-table .ant-table-tbody > tr > td) { border-bottom: 1px solid #f1f5f9 !important; padding: 16px;}
</style>