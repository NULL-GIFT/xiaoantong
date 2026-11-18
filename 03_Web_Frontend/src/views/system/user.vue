<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="page-header">
      <div class="title-group">
        <h2>👥 IAM 身份与访问控制中心</h2>
        <p class="sub-text">统一纳管全域安防操作员与系统管理员权限</p>
      </div>
    </div>

    <a-row :gutter="24" class="kpi-row">
      <a-col :span="8"><div class="aero-card kpi-card"><div class="kpi-info"><div class="kpi-label">系统在籍账号总数</div><div class="kpi-value-box"><span class="kpi-val">{{ globalTotal }}</span> <span class="kpi-unit">人</span></div></div></div></a-col>
      <a-col :span="8"><div class="aero-card kpi-card"><div class="kpi-info"><div class="kpi-label">高权限管理员组</div><div class="kpi-value-box"><span class="kpi-val text-danger">{{ adminCount }}</span><span class="kpi-trend trend-bad">管理员</span></div></div></div></a-col>
      <a-col :span="8"><div class="aero-card kpi-card"><div class="kpi-info"><div class="kpi-label">已停用/封禁账号</div><div class="kpi-value-box"><span class="kpi-val" style="color: #64748b;">{{ disabledCount }}</span><span class="kpi-trend" style="background: #e2e8f0; color: #475569;">限制访问</span></div></div></div></a-col>
    </a-row>

    <div class="aero-card main-panel">
      <div class="filter-bar">
        <a-space wrap align="center">
          <span class="filter-label">登录账号：</span>
          <a-input v-model:value="queryParams.username" placeholder="请输入账号" style="width: 180px" allowClear @input="handleAutoSearch" />
          <span class="filter-label" style="margin-left: 16px;">真实姓名 / ID：</span>
          <a-input v-model:value="queryParams.realName" placeholder="请输入姓名或ID" style="width: 220px" allowClear @input="handleAutoSearch" />
          <a-button type="primary" @click="applySearch" style="margin-left: 8px;">查询</a-button>
          <a-button @click="resetQuery">重置</a-button>
        </a-space>
        <a-space>
          <a-button type="primary" class="btn-install" @click="handleAdd">签发新账号</a-button>
        </a-space>
      </div>

      <a-table :columns="columns" :data-source="tableData" :loading="loading" row-key="id" :pagination="pagination" @change="handleTableChange" class="aero-table">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'"><span style="font-weight: 600; color: #475569;">{{ record.id }}</span></template>
          <template v-if="column.key === 'username'">
            <div style="display: flex; align-items: center;">
              <div class="avatar-icon" :class="record.role === 'admin' ? 'avatar-admin' : 'avatar-user'">{{ record.username.charAt(0).toUpperCase() }}</div>
              <div>
                <span style="font-weight: 600; color: #1e293b;">{{ record.username }}</span>
                <a-tag v-if="record.username === currentLoginUser" color="green" size="small" style="margin-left: 8px;">我自己</a-tag>
              </div>
            </div>
          </template>
          <template v-if="column.key === 'realName'"><span>{{ record.realName || '未填写' }}</span></template>
          <template v-if="column.key === 'role'">
            <a-tag :color="record.role === 'admin' ? 'red' : 'blue'" style="font-weight: 600;">{{ record.role === 'admin' ? '👑 管理员' : '🛡️ 普通用户' }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tooltip :title="!canDisableOrDelete(record) ? '禁止操作该权限' : '点击切换管控状态'">
              <a-switch :checked="record.status === 1" :disabled="!canDisableOrDelete(record)" @click="(checked, e) => handleStatusClick(checked, record, e)" />
            </a-tooltip>
            <span style="margin-left: 8px; font-size: 12px;" :style="{ color: record.status === 1 ? '#10b981' : '#ef4444' }">{{ record.status === 1 ? '正常活跃' : '已封禁' }}</span>
          </template>
          <template v-if="column.key === 'createTime'"><span>{{ formatDate(record.createTime) }}</span></template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" style="font-weight: 600;" :disabled="!canEdit(record)" @click="handleEdit(record)">资料更新</a-button>
            <a-divider type="vertical" />
            <a-button type="link" danger size="small" :disabled="!canDisableOrDelete(record)" @click="promptDelete(record)">注销账户</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="deleteModalVisible" title="🗑️ 账号永久注销" @ok="submitDelete" :confirmLoading="deleteLoading" okType="danger" okText="确认永久注销">
      <div style="margin-bottom: 15px; font-size: 14px;">您正在销毁操作员 <strong style="color:#ef4444;">{{ currentDeleteRecord?.username }}</strong> 的所有访问令牌。</div>
      <a-textarea v-model:value="deleteReason" placeholder="必须填写注销原因（例如：员工离职、调岗交接等）" :rows="3" />
    </a-modal>

    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleSave" :confirmLoading="saveLoading" width="500px" destroyOnClose>
      <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="登录账号" name="username"><a-input v-model:value="formData.username" :disabled="!!formData.id" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="真实姓名 / ID" name="realName"><a-input v-model:value="formData.realName" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="初始审计密码" name="password" v-if="!formData.id">
          <a-input-group compact style="display: flex;">
            <a-input-password v-model:value="formData.password" style="flex: 1;" />
            <a-button @click="generatePwd">随机生成</a-button>
          </a-input-group>
        </a-form-item>
        <a-form-item label="系统安全角色" name="role">
          <a-select v-model:value="formData.role">
            <a-select-option value="admin">系统管理员</a-select-option>
            <a-select-option value="user">普通用户</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="banModalVisible" title="🛑 执行账号封禁管控" @ok="submitBan" :confirmLoading="banLoading" okType="danger" okText="确认封禁">
      <div style="margin-bottom: 15px; font-size: 14px;">您正在对操作员 <strong style="color:#ef4444;">{{ currentBanRecord?.username }}</strong> 实施停用管控。</div>
      <a-textarea v-model:value="banReason" placeholder="必须填写封禁原因（例如：恶意删除数据、离职交接等），该记录将永久记入安全审计日志。" :rows="4" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import request from '@/utils/request';

const loading = ref(false);
const tableData = ref([]);
const globalTotal = ref(0);
const adminCount = ref(0);
const disabledCount = ref(0);
const currentLoginUser = localStorage.getItem('username') || '';

const queryParams = reactive({ realName: '', username: '' });
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true });

const columns = [
  { title: 'ID', key: 'id', width: '8%' },
  { title: '登录账号', key: 'username', width: '18%' },
  { title: '真实姓名', key: 'realName', width: '15%' },
  { title: '系统角色', key: 'role', width: '14%' },
  { title: '管控状态', key: 'status', width: '14%' },
  { title: '签发时间', key: 'createTime', width: '15%' },
  { title: '账户操作', key: 'action', width: '16%', align: 'center' }
];

const canDisableOrDelete = (r) => r.username !== currentLoginUser && r.role !== 'admin';
const canEdit = (r) => r.username === currentLoginUser || r.role !== 'admin';
const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN', { hour12: false }) : '-';

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get('/api/user/list', { params: { pageNum: pagination.current, pageSize: pagination.pageSize, ...queryParams } });
    if (res.code === 200 || res.code === 0) {
      tableData.value = res.data?.list || res.data || [];
      pagination.total = res.data?.total || tableData.value.length;
      if (!queryParams.username && !queryParams.realName) {
        globalTotal.value = res.data?.total || tableData.value.length;
        adminCount.value = tableData.value.filter(u => u.role === 'admin').length;
        disabledCount.value = tableData.value.filter(u => u.status === 0).length;
      }
    }
  } finally { loading.value = false; }
};

const applySearch = () => { pagination.current = 1; fetchData(); };
let searchTimeout = null;
const handleAutoSearch = () => { if(searchTimeout) clearTimeout(searchTimeout); searchTimeout = setTimeout(applySearch, 500); };
const resetQuery = () => { queryParams.realName = ''; queryParams.username = ''; applySearch(); };
const handleTableChange = (pag) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; fetchData(); };

// ==================== 🌟 真实的封禁原因控制逻辑 ====================
const banModalVisible = ref(false);
const banReason = ref('');
const banLoading = ref(false);
const currentBanRecord = ref(null);

const handleStatusClick = (checked, record, e) => {
  // checked 代表希望变成的状态：true为恢复，false为封禁
  if (!checked) {
    // 如果要封禁，拦截并弹窗
    currentBanRecord.value = record;
    banReason.value = '';
    banModalVisible.value = true;
  } else {
    // 如果要恢复，直接发送请求并携带恢复原因
    executeStatusChange(record.id, 1, "管理员解除封禁");
  }
};

const submitBan = () => {
  if (!banReason.value.trim()) {
    message.warning("工业级管控要求：必须填写封禁原因！");
    return;
  }
  banLoading.value = true;
  executeStatusChange(currentBanRecord.value.id, 0, banReason.value).finally(() => {
    banLoading.value = false;
    banModalVisible.value = false;
  });
};

const executeStatusChange = async (id, targetStatus, reason) => {
  try {
    // 🌟 调用新的带有 reason 的接口
    const res = await request.put('/api/user/changeStatus', { id, status: targetStatus, reason });
    if (res.code === 200 || res.code === 0) {
      message.success(`安全审计记录已归档！`);
      fetchData(); 
    } else {
      message.error(res.msg || "状态变更被服务器拒绝");
    }
  } catch (error) {
    message.error("网络阻断，变更未生效");
  }
};
// ==================== 🌟 真实的注销原因控制逻辑 ====================
const deleteModalVisible = ref(false);
const deleteReason = ref('');
const deleteLoading = ref(false);
const currentDeleteRecord = ref(null);

const promptDelete = (record) => {
  currentDeleteRecord.value = record;
  deleteReason.value = '';
  deleteModalVisible.value = true;
};

const submitDelete = async () => {
  if (!deleteReason.value.trim()) {
    return message.warning("工业级管控要求：必须填写注销原因！");
  }
  deleteLoading.value = true;
  try {
    // 将原因拼接到 URL 传给后端
    const res = await request.delete(`/api/user/delete?id=${currentDeleteRecord.value.id}&reason=${encodeURIComponent(deleteReason.value)}`);
    if (res.code === 200 || res.code === 0) {
      message.success("🗑️ 账户已注销，审计已归档");
      deleteModalVisible.value = false;
      fetchData();
    } else {
      message.error(res.msg || "注销失败");
    }
  } catch (error) {
    message.error("注销请求异常");
  } finally {
    deleteLoading.value = false;
  }
};
// ================================================================

const modalVisible = ref(false);
const modalTitle = ref('');
const saveLoading = ref(false);
const formRef = ref(null);
const formData = reactive({ id: undefined, username: '', password: '', realName: '', role: 'user', status: 1 });
const formRules = {
  username: [{ required: true, message: '必填' }, { min: 4, message: '最短4位' }],
  password: [{ required: true, message: '必填' }, { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,}$/, message: '需含字母和数字' }],
  realName: [{ required: true, message: '必填' }], role: [{ required: true }]
};

const handleAdd = () => { modalTitle.value = '签发新账号'; Object.assign(formData, { id: undefined, username: '', password: '', realName: '', role: 'user' }); modalVisible.value = true; };
const handleEdit = (r) => { modalTitle.value = '更新资料'; Object.assign(formData, { id: r.id, username: r.username, realName: r.realName, role: r.role }); modalVisible.value = true; };
const generatePwd = () => { let pwd = 'Xat@'; for(let i=0;i<4;i++) pwd += Math.floor(Math.random()*10); formData.password = pwd; };
const handleSave = () => {
  formRef.value.validate().then(async () => {
    saveLoading.value = true;
    try {
      const api = formData.id ? '/api/user/update' : '/api/user/add';
      const res = await request[formData.id ? 'put' : 'post'](api, formData);
      if (res.code === 200 || res.code === 0) { message.success("操作成功"); modalVisible.value = false; fetchData(); }
    } finally { saveLoading.value = false; }
  });
};

onMounted(() => fetchData());
</script>

<style scoped>
.aero-dashboard { padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; }
.aero-card { background: rgba(255,255,255,0.85); border-radius: 12px; border: 1px solid #fff; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.kpi-row { margin-bottom: 24px; }
.kpi-card { padding: 20px 24px; display: flex; align-items: center; }
.kpi-icon { width: 50px; height: 50px; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-size: 22px; margin-right: 16px; }
.kpi-val { font-size: 28px; font-weight: 900; }
.main-panel { padding: 20px; }
.filter-bar { display: flex; justify-content: space-between; margin-bottom: 20px; }
.filter-label { font-size: 14px; font-weight: 600; color: #475569; }
.avatar-icon { width: 38px; height: 38px; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 900; margin-right: 12px; }
.avatar-admin { background: #fef2f2; color: #ef4444; }
.avatar-user { background: #f0f9ff; color: #0ea5e9; }
</style>