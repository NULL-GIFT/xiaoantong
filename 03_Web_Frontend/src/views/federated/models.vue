<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="page-header">
      <div class="title-group">
        <h2>📦 模型资产与版本发布库 (Model Assets)</h2>
        <p class="sub-text">管理经过联邦聚合生成的全局模型资产，统筹端侧算法的版本下发与回滚</p>
      </div>
    </div>

    <div class="aero-card" style="padding: 20px;">
      <div class="toolbar" style="margin-bottom: 20px; display: flex; justify-content: space-between;">
        <div class="left-filters">
          <a-select v-model:value="queryParams.skillCode" placeholder="筛选算法类型" style="width: 180px" allowClear @change="fetchData">
            <a-select-option v-for="skill in skillList" :key="skill.skillCode" :value="skill.skillCode">
              {{ skill.skillName }}
            </a-select-option>
          </a-select>
        </div>
        <div class="right-actions">
          <a-button @click="fetchData"><sync-outlined /> 刷新资产库</a-button>
        </div>
      </div>

      <a-table 
        :columns="columns" 
        :data-source="modelList" 
        :loading="loading" 
        row-key="id"
        class="aero-table"
      >
        <template #bodyCell="{ column, record }">
          
          <template v-if="column.key === 'skillCode'">
            <span class="skill-tag">{{ getSkillName(record.skillCode) }}</span>
          </template>

          <template v-else-if="column.key === 'version'">
            <div class="version-badge">
              <span class="v-icon">V</span>
              <span class="v-text">{{ record.version }}</span>
            </div>
          </template>

          <template v-else-if="column.key === 'accuracy'">
            <div class="acc-box">
              <span class="acc-num">{{ Number(record.accuracy).toFixed(1) }}%</span>
              <a-progress :percent="Number(record.accuracy)" :show-info="false" stroke-color="#8b5cf6" style="width: 60px; margin-left: 8px;" />
            </div>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag v-if="record.status === 'active'" color="success" class="status-tag">
              <check-circle-outlined /> 现役版本 (Active)
            </a-tag>
            <a-tag v-else-if="record.status === 'archived'" color="default" class="status-tag">
              历史归档 (Archived)
            </a-tag>
            <a-tag v-else color="processing" class="status-tag">
              <sync-outlined spin /> {{ record.status || '聚合中' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space size="small">
              <a-popconfirm
                title="确定要将此版本通过 MQTT 下发至全网边缘节点吗？"
                ok-text="确认下发"
                cancel-text="取消"
                @confirm="dispatchModel(record)"
              >
                <a-button type="primary" size="small" class="dispatch-btn" :disabled="record.status === 'active' || record.status === 'training'">
                  <template #icon><cloud-upload-outlined /></template> 
                  {{ record.status === 'archived' ? '版本回滚' : '全网下发' }}
                </a-button>
              </a-popconfirm>

              <a-button type="link" size="small" @click="downloadModel(record)">
                <template #icon><download-outlined /></template>
              </a-button>

              <a-popconfirm
                title="确定要永久删除该模型文件吗？"
                ok-text="删除"
                ok-type="danger"
                cancel-text="取消"
                @confirm="deleteModel(record.id)"
              >
                <a-button type="text" danger size="small" :disabled="record.status === 'active'">
                  <template #icon><delete-outlined /></template>
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>

        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { 
  SyncOutlined, CheckCircleOutlined, CloudUploadOutlined, 
  DownloadOutlined, DeleteOutlined 
} from '@ant-design/icons-vue';
import request from '@/utils/request';
const loading = ref(false);
const modelList = ref([]);
const queryParams = ref({ skillCode: undefined });

// 🌟 新增：存放从后端拉取的真实技能字典
const skillList = ref([]);

const columns = [
  { title: '技能算法', dataIndex: 'skillCode', key: 'skillCode', width: '15%' },
  { title: '模型版本', dataIndex: 'version', key: 'version', width: '15%' },
  { title: '全局识别精度', dataIndex: 'accuracy', key: 'accuracy', width: '20%' },
  { title: '生成时间', dataIndex: 'createTime', key: 'createTime', width: '20%' },
  { title: '资产状态', dataIndex: 'status', key: 'status', width: '15%' },
  { title: '版本操作', key: 'action', width: '15%', align: 'center' }
];

// 🌟 动态获取真实中文名称，告别写死！
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


// 1. 获取列表
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/fed/list', { params: queryParams.value });
    modelList.value = res.data?.records || res.data?.list || res.data || [];
  } catch (err) {
    message.error('无法获取模型资产列表');
  } finally {
    loading.value = false;
  }
};

// 2. 下发模型指令 (OTA升级)
const dispatchModel = async (record) => {
  const hide = message.loading('正在通过 MQTT 广播下发模型指令...', 0);
  try {
    const res = await request.post('/api/fed/dispatch', { modelId: record.id });
    if (res.code === 200 || res.code === 0) {
      hide();
      message.success(`版本 ${record.version} 下发成功，边缘节点即将自动拉取更新！`);
      fetchData(); // 刷新列表，后端的 service 应该把这个模型的状态改为 active，其他的改为 archived
    } else {
      hide();
      message.error(res.msg || '下发失败');
    }
  } catch (err) {
    hide();
    message.error('下发请求异常');
  }
};

// 3. 删除模型
const deleteModel = async (id) => {
  try {
    await request.delete(`/api/fed/delete/${id}`);
    message.success('模型已从资产库中移除');
    fetchData();
  } catch (err) {
    message.error('删除失败');
  }
};

// 4. 下载模型
const downloadModel = (record) => {
  window.open(`${import.meta.env.VITE_APP_BASE_API}/api/fed/download?version=${record.version}`, '_blank');
};

// 🌟 修改挂载顺序：先拉字典，再拉数据
onMounted(async () => {
  await fetchSkills();
  fetchData();
});
</script>

<style scoped>
.aero-dashboard { padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; }
.aero-card { background: rgba(255,255,255,0.9); border-radius: 12px; border: 1px solid #fff; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
.page-header { margin-bottom: 24px; }
.title-group h2 { font-size: 24px; font-weight: 800; color: #0f172a; margin-bottom: 6px; }
.sub-text { font-size: 13px; color: #64748b; }

/* 极客风的版本徽章 */
.version-badge { display: inline-flex; align-items: center; background: #f1f5f9; border-radius: 6px; padding: 2px 8px 2px 2px; border: 1px solid #e2e8f0; }
.v-icon { background: #0f172a; color: #fff; width: 22px; height: 22px; border-radius: 4px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-family: monospace; font-size: 12px; margin-right: 6px; }
.v-text { font-family: monospace; font-weight: bold; font-size: 14px; color: #334155; }

.skill-tag { font-weight: 600; color: #0369a1; }
.acc-box { display: flex; align-items: center; }
.acc-num { font-size: 15px; font-weight: 800; color: #8b5cf6; font-family: "DIN Alternate", Arial; width: 45px;}
.status-tag { padding: 2px 10px; border-radius: 20px; font-weight: 600; }

.dispatch-btn { background: #0ea5e9; border: none; font-weight: 600; }
.dispatch-btn:hover { background: #0284c7; }

:deep(.aero-table .ant-table) { background: transparent; }
:deep(.aero-table .ant-table-thead > tr > th) { background: #f8fafc; color: #475569; font-weight: 600; border-bottom: 1px solid #e2e8f0; }
</style>
