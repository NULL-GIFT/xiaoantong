<template>
  <div class="market-page">
    <div class="page-header">
      <h2>🧩 边缘智能算法商城与技能调度</h2>
      <p class="sub-text">管理联邦学习模型资产，统一下发端侧 AI 算力技能</p>
    </div>

    <div class="glass-panel">
      <div class="filter-bar">
        <a-space wrap>
          <a-input v-model:value="queryParams.skillName" placeholder="输入技能名称检索..." style="width: 180px" allowClear @change="applyFilter">
            <template #prefix><search-outlined style="color: #bfbfbf;"/></template>
          </a-input>
          
          <a-input v-model:value="queryParams.version" placeholder="精确版本号 (如 v1.0)" style="width: 160px" allowClear @change="applyFilter">
            <template #prefix><tag-outlined style="color: #bfbfbf;"/></template>
          </a-input>

          <a-select v-model:value="queryParams.configStatus" placeholder="下发配置状态" style="width: 140px" allowClear @change="applyFilter">
            <a-select-option :value="1">已下发网关</a-select-option>
            <a-select-option :value="0">未配置</a-select-option>
          </a-select>
          
          <a-select v-model:value="queryParams.activationStatus" placeholder="授权激活状态" style="width: 140px" allowClear @change="applyFilter">
            <a-select-option :value="1">已激活</a-select-option>
            <a-select-option :value="0">未激活</a-select-option>
          </a-select>

          <a-button type="primary" @click="applyFilter">检索</a-button>
          <a-button @click="resetQuery">重置</a-button>
        </a-space>

        <a-space>
          <a-button type="primary" class="btn-install" @click="openInstallModal">
            <template #icon><cloud-upload-outlined /></template> 部署新模型
          </a-button>
          
          <a-dropdown>
            <a-button class="btn-activate" :loading="batchActivating">
              <template #icon><safety-certificate-outlined /></template>
              批量激活 <down-outlined />
            </a-button>
            <template #overlay>
              <a-menu @click="handleBatchMenuClick">
                <a-menu-item key="selected">激活选中项</a-menu-item>
                <a-menu-item key="all">激活全部未激活项</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </div>

      <a-table 
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        :dataSource="tableData" 
        :columns="columns" 
        :loading="loading" 
        rowKey="id"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 个算法模型` }"
        class="aero-table"
      >
        <template #bodyCell="{ column, record }">
          
          <template v-if="column.key === 'skillName'">
            <div style="display: flex; align-items: center;">
              <div class="skill-icon">{{ record.skillName.charAt(0) }}</div>
              <div>
                <div style="font-weight: 600; color: #1e293b;">{{ record.skillName }}</div>
                <div style="font-size: 12px; color: #94a3b8;">{{ record.skillCode }}</div>
              </div>
            </div>
          </template>

          <template v-else-if="column.key === 'version'">
            <a-tag color="blue" style="font-family: monospace; font-weight: bold;">{{ record.version }}</a-tag>
          </template>

          <template v-else-if="column.key === 'installTime'">
            <span style="color: #64748b; font-size: 13px;">{{ formatDate(record.createTime) }}</span>
          </template>

          <template v-else-if="column.key === 'validity'">
            <span v-if="record.validDays === -1" style="color: #10b981; font-weight: 600;"><check-circle-outlined /> 永久有效</span>
            <span v-else-if="record.validDays <= 7" style="color: #ef4444; font-weight: 600;">⚠️ 仅剩 {{ record.validDays }} 天</span>
            <span v-else style="color: #64748b;">剩余 {{ record.validDays }} 天</span>
          </template>

          <template v-else-if="column.key === 'configStatus'">
            <a-badge 
              :status="record.configStatus === 1 ? 'processing' : 'default'" 
              :text="record.configStatus === 1 ? '已配置网关' : '闲置中'" 
            />
          </template>

          <template v-else-if="column.key === 'activationStatus'">
            <a-tag :color="record.activationStatus === 1 ? 'success' : 'default'">
              <template #icon>
                <check-circle-outlined v-if="record.activationStatus === 1" />
                <minus-circle-outlined v-else />
              </template>
              {{ record.activationStatus === 1 ? '已激活' : '未激活' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" style="padding: 0; margin-right: 12px; font-weight: 600; color: #64748b;" @click="openDetailDrawer(record)">
              <profile-outlined /> 档案详情
            </a-button>
            <a-button type="link" size="small" style="padding: 0; margin-right: 12px; font-weight: 600;" @click="openConfigModal(record)">
              <send-outlined /> 网关部署
            </a-button>
            <a-button type="link" danger size="small" style="padding: 0;" @click="promptDelete(record)">移除</a-button>
          </template>
        </template>
      </a-table>

    <a-modal v-model:open="deleteModalVisible" title="🗑️ 从云端移除算法模型" @ok="submitDelete" :confirmLoading="deleteLoading" okType="danger" okText="确认移除">
      <div style="margin-bottom: 15px; font-size: 14px;">此操作将从云端剥离算法 <strong style="color:#ef4444;">{{ currentDeleteRecord?.skillName }}</strong>，所有挂载该算法的网关也将失效。</div>
      <a-textarea v-model:value="deleteReason" placeholder="必须填写移除原因（例如：精度不达标、版本过期替换等）" :rows="3" />
    </a-modal>
    </div>

    <a-drawer
      v-model:open="detailDrawerVisible"
      title="🤖 算法模型数字档案 (Model Profile)"
      placement="right"
      width="500"
      :footer-style="{ textAlign: 'right' }"
    >
      <div v-if="currentDetail">
        <div class="drawer-header" style="display: flex; align-items: center; margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid #f0f0f0;">
          <div class="d-icon" style="width: 48px; height: 48px; background: #e0f2fe; color: #0ea5e9; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 24px; font-weight: bold; margin-right: 15px;">
            {{ currentDetail.skillName.charAt(0) }}
          </div>
          <div class="d-title-group">
            <div class="d-name" style="font-size: 18px; font-weight: bold; color: #0f172a;">{{ currentDetail.skillName }} <a-tag color="blue">{{ currentDetail.version }}</a-tag></div>
            <div class="d-code" style="font-size: 13px; color: #64748b;">算法编码: {{ currentDetail.skillCode }}</div>
          </div>
        </div>

        <a-descriptions title="📦 算力网关分布 (Deployed Edge Boxes)" :column="1" bordered size="small">
          <a-descriptions-item label="当前搭载节点">
            <div v-if="deployedBoxesLoading" style="color: #1890ff;"><loading-outlined /> 正在检索网关拓扑...</div>
            <div v-else-if="deployedBoxes.length === 0" style="color: #94a3b8;">尚未下发至任何边缘网关</div>
            <div v-else style="display: flex; flex-wrap: wrap; gap: 8px;">
              <a-tag v-for="box in deployedBoxes" :key="box.id" color="cyan" style="padding: 4px 8px; border-radius: 4px; display: flex; align-items: center;">
                <hdd-outlined style="margin-right: 4px;"/> {{ box.boxName || '未命名网关' }} 
                <span style="color: #888; font-size: 12px; margin-left: 4px; margin-right: 6px;">({{ box.ipAddress }})</span>
                <close-circle-outlined 
                  style="color: #ff4d4f; cursor: pointer; font-size: 14px;" 
                  title="点击将此算法从该网关卸载"
                  @click.stop="promptRemoveFromBox(box, currentDetail)" 
                />
              </a-tag>
            </div>
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions title="📁 物理包元数据" :column="1" bordered size="small" style="margin-top: 20px;">
          <a-descriptions-item label="架构引擎">ONNX Runtime / TensorRT</a-descriptions-item>
          <a-descriptions-item label="权重包名"><code>weights_{{ currentDetail.skillCode }}_{{ currentDetail.version }}.tar.gz</code></a-descriptions-item>
          <a-descriptions-item label="评估大小">≈ {{ (Math.random() * 20 + 5).toFixed(1) }} MB (INT8 量化)</a-descriptions-item>
          <a-descriptions-item label="SHA256 校验">
            <span style="font-family: monospace; font-size: 11px; color: #888;">e3b0c44298fc1c149afbf4c8996fb924...</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions title="🛡️ 业务调度信息" :column="1" bordered size="small" style="margin-top: 20px;">
          <a-descriptions-item label="入库时间">{{ formatDate(currentDetail.createTime) }}</a-descriptions-item>
          <a-descriptions-item label="授权状态">
            <span :style="{ color: currentDetail.activationStatus === 1 ? '#10b981' : '#64748b' }">
              {{ currentDetail.activationStatus === 1 ? '已激活' : '未激活' }}
            </span>
          </a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px;">
          <div style="font-weight: bold; color: #1e293b; margin-bottom: 8px;">📝 架构与参数说明：</div>
          <div style="background: #f8fafc; border: 1px solid #e2e8f0; padding: 12px; border-radius: 6px; font-size: 13px; color: #475569; line-height: 1.6;">
            {{ currentDetail.description || '工程师暂未填写该模型架构的详细发布说明 (Release Notes)。' }}
          </div>
        </div>
      </div>
    </a-drawer>

    <a-modal v-model:open="installDialogVisible" title="部署新的算法模型" @ok="submitInstallSkill" :confirmLoading="installing" width="600px" destroyOnClose>
      <div class="modal-alert">
        💡 请确保上传的模型文件已完成轻量化压缩（支持 .onnx / .rknn / .tar.gz 格式），以适配边缘节点的 NPU 算力。
      </div>
      <a-form :model="installForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模型名称 (Display Name)" required>
              <a-input v-model:value="installForm.skillName" placeholder="如: 违规吸烟检测" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="模型编码 (Model Code)" required>
              <a-input v-model:value="installForm.skillCode" placeholder="如: smoke_detect" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="编译版本号" required>
              <a-input v-model:value="installForm.version" placeholder="如: v2.1.0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="授权周期 (License)" required>
              <a-select v-model:value="installForm.validDays">
                <a-select-option :value="30">30 天 (试用评估)</a-select-option>
                <a-select-option :value="90">90 天 (季度授权)</a-select-option>
                <a-select-option :value="365">365 天 (年度授权)</a-select-option>
                <a-select-option :value="-1">永久有效 (商业买断)</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="算法权重包上传 (Weights Package)" required>
          <a-upload-dragger
            v-model:fileList="installForm.fileList"
            name="file"
            :multiple="false"
            action="/fake-upload"
            :beforeUpload="handleBeforeUpload"
            @change="handleFileChange"
          >
            <p class="ant-upload-drag-icon"><inbox-outlined style="color: #0ea5e9;" /></p>
            <p class="ant-upload-text">点击或将 ONNX/权重包 拖拽到这里上传</p>
            <p class="ant-upload-hint">支持单次上传。严禁上传包含未脱敏隐私数据的预训练集。</p>
          </a-upload-dragger>
        </a-form-item>

        <a-form-item label="架构与参数说明 (Release Notes)">
          <a-textarea v-model:value="installForm.description" placeholder="请输入骨干网络描述、支持的类别及运行算力要求..." :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="configDialogVisible" title="算法模型 -> 算力网关调度下发" @ok="submitConfig" :confirmLoading="configuring" width="550px">
      <div class="modal-alert" style="background: #f0f9ff; border-color: #bae6fd; color: #0369a1;">
        当前选中下发算法：<strong style="font-family: monospace; font-size: 15px;">{{ currentSkill?.skillName }} ({{ currentSkill?.version }})</strong>
      </div>
      <a-form :model="configForm" layout="vertical">
        <a-form-item label="目标边缘算力盒子 (支持批量多选并行下发)" required>
          <a-select 
            v-model:value="configForm.boxIds" 
            mode="multiple" 
            placeholder="请搜索并勾选需要搭载此算法的边缘算力网关" 
            :loading="loadingBoxes"
            :max-tag-count="4"
            style="width: 100%"
          >
            <a-select-option v-for="box in boxList" :key="box.id" :value="box.id" :label="box.boxName || box.ipAddress">
              <span style="font-weight: 500;">{{ box.boxName || '未命名网关' }}</span> 
              <span style="color:#94a3b8; font-size: 12px; margin-left: 8px;">({{ box.ipAddress }})</span>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { 
  CloudUploadOutlined, DownOutlined, SearchOutlined, TagOutlined, ProfileOutlined,
  SafetyCertificateOutlined, InboxOutlined, SendOutlined, CheckCircleOutlined, MinusCircleOutlined,
  HddOutlined, LoadingOutlined, CloseCircleOutlined
} from '@ant-design/icons-vue'
import request from '@/utils/request'

// 检索参数
const queryParams = reactive({ skillName: '', version: '', configStatus: undefined, activationStatus: undefined })

const columns = [
  { title: '模型名称与标识', key: 'skillName', width: '22%' },
  { title: '版本', key: 'version', width: '10%' },
  { title: '入库时间', key: 'installTime', width: '18%' },
  { title: '剩余有效期', key: 'validity', width: '12%' },
  { title: '网关部署', key: 'configStatus', width: '12%' },
  { title: '激活状态', key: 'activationStatus', width: '10%' },
  { title: '业务操作', key: 'action', width: '18%', align: 'center' }
]

const rawTableData = ref([]) 
const tableData = ref([])    
const loading = ref(false)

const selectedRowKeys = ref([])
const onSelectChange = (keys) => { selectedRowKeys.value = keys }

// ========= 档案详情 & 部署盒子的状态管理 =========
const detailDrawerVisible = ref(false);
const currentDetail = ref(null);
const deployedBoxes = ref([]);
const deployedBoxesLoading = ref(false);

const openDetailDrawer = (record) => {
  currentDetail.value = record;
  detailDrawerVisible.value = true;
  loadDeployedBoxes(record.id);
};

// 提取出来的方法：刷新盒子列表
const loadDeployedBoxes = async (skillId) => {
  deployedBoxesLoading.value = true;
  try {
    const res = await request.get('/api/edgeBox/listBySkill', { params: { skillId } });
    if (res.code === 200 || res.code === 0) {
      deployedBoxes.value = res.data || [];
    } else { deployedBoxes.value = []; }
  } catch (error) { deployedBoxes.value = []; } finally { deployedBoxesLoading.value = false; }
};

// 🌟 新增：在详情里直接把算法从网关里卸载掉！
const promptRemoveFromBox = (box, skill) => {
  Modal.confirm({
    title: '⚠️ 网关算法卸载确认',
    content: `您确定要将【${skill.skillName}】从边缘算力网关【${box.boxName || box.ipAddress}】中卸载吗？此操作会导致该网关下属摄像头失去此检测能力。`,
    okText: '确认卸载',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await request.post('/api/skill/removeFromBox', { skillId: skill.id, boxId: box.id });
        if (res.code === 200 || res.code === 0) {
          message.success('已成功从该网关卸载算法！');
          loadDeployedBoxes(skill.id); // 刷新详情里的盒子列表
          fetchData(); // 刷新外层表格的“配置状态”
        } else {
          message.error(res.msg || '网关卸载受阻');
        }
      } catch (error) {
        message.error('网络请求异常');
      }
    }
  });
};

// ========= 数据拉取与过滤 =========
const fetchData = async () => { 
  loading.value = true
  try {
    const res = await request.get('/api/skill/list') 
    if (res.code === 200 || res.code === 0) {
      const realData = res.data || [];
      rawTableData.value = Array.isArray(realData) ? realData : (realData.list || []);
      applyFilter(); 
    } else { message.error(res.msg || '获取模型大盘失败') }
  } catch (error) { console.error('拉取异常:', error) } finally { loading.value = false }
}

const applyFilter = () => {
  tableData.value = rawTableData.value.filter(item => {
    const matchName = !queryParams.skillName || (item.skillName && item.skillName.toLowerCase().includes(queryParams.skillName.toLowerCase()));
    const matchVer = !queryParams.version || (item.version && item.version.toLowerCase().includes(queryParams.version.toLowerCase()));
    const matchConfig = queryParams.configStatus === undefined || item.configStatus === queryParams.configStatus;
    const matchActive = queryParams.activationStatus === undefined || item.activationStatus === queryParams.activationStatus;
    return matchName && matchVer && matchConfig && matchActive;
  });
}

const resetQuery = () => {
  queryParams.skillName = ''; queryParams.version = ''; queryParams.configStatus = undefined; queryParams.activationStatus = undefined;
  applyFilter();
}

const formatDate = (dateString) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleString('zh-CN', { hour12: false }); 
};

// ========= 模型安装逻辑 =========
const installDialogVisible = ref(false)
const installing = ref(false)
const installForm = reactive({ skillName: '', skillCode: '', version: '', validDays: -1, description: '', fileList: [] })
const openInstallModal = () => {
  installForm.skillName = ''; installForm.skillCode = ''; installForm.version = '';
  installForm.validDays = -1; installForm.description = ''; installForm.fileList = [];
  installDialogVisible.value = true;
}
const handleBeforeUpload = () => false; 
const handleFileChange = (info) => {
  let resFileList = [...info.fileList].slice(-1); 
  resFileList = resFileList.map(file => { if (file.status !== 'done') file.status = 'done'; return file; });
  installForm.fileList = resFileList;
}
const submitInstallSkill = async () => {
  if (!installForm.skillName || !installForm.version || !installForm.skillCode) return message.warning('请补全基础模型参数')
  if (installForm.fileList.length === 0) return message.warning('必须上传模型权重文件才能进行部署！')
  installing.value = true
  try {
    const res = await request.post('/api/skill/add', installForm)
    if (res.code === 200 || res.code === 0)  {
      message.success('🚀 模型解析并部署入库成功！')
      installDialogVisible.value = false; fetchData();
    } else { message.error(res.msg || '部署失败') }
  } catch (error) { console.error(error) } finally { installing.value = false }
}

// ========= 智能防呆批量激活 =========
const batchActivating = ref(false) 
const handleBatchMenuClick = ({ key }) => {
  let requestData = { isAll: false, ids: [] };
  if (key === 'selected') {
    if (selectedRowKeys.value.length === 0) return message.warning('请先在列表中勾选需要激活的模型！');
    const activeSkillIds = tableData.value.filter(item => selectedRowKeys.value.includes(item.id) && item.activationStatus === 1).map(item => item.id);
    if (activeSkillIds.length === selectedRowKeys.value.length) return message.info('😎 您勾选的模型已全部处于【已激活】状态。');
    requestData.ids = selectedRowKeys.value.filter(id => !activeSkillIds.includes(id));
  } else if (key === 'all') { requestData.isAll = true; }

  Modal.confirm({
    title: key === 'all' ? '全域模型授权激活' : '批量授权激活',
    content: `即将下发系统指令，激活算法模型。确认执行？`,
    okText: '确认激活',
    onOk: async () => {
      batchActivating.value = true;
      try {
        const res = await request.post('/api/skill/batchActivate', requestData);
        if (res.code === 200 || res.code === 0) {
          message.success('✨ 状态校验通过，节点激活指令已下发！');
          selectedRowKeys.value = []; fetchData(); 
        } else { message.error(res.msg || '激活失败'); }
      } catch (error) { message.error('请求异常'); } finally { batchActivating.value = false; }
    }
  });
}

// ========= 多节点批量下发配置 (核心修复) =========
const configDialogVisible = ref(false)
const configuring = ref(false)
const loadingBoxes = ref(false)
const boxList = ref([])
const currentSkill = ref(null)

const configForm = reactive({ skillId: null, boxIds: [] })

const openConfigModal = async (record) => {
  currentSkill.value = record;
  configForm.skillId = record.id;
  configForm.boxIds = [];
  configDialogVisible.value = true;
  
  loadingBoxes.value = true;
  try {
    // 去查边缘盒子的 API
    const res = await request.get('/api/edgeBox/list');
    if (res.code === 200 || res.code === 0) {
      boxList.value = res.data || [];
    }
  } catch (error) { message.error('获取底层边缘网关列表失败'); } finally { loadingBoxes.value = false; }
}

const submitConfig = async () => {
  if (configForm.boxIds.length === 0) return message.warning('必须至少选择一个目标边缘网关！');
  
  configuring.value = true;
  try {
    const promises = configForm.boxIds.map(bId => 
      request.post('/api/skill/deployToBox', { skillId: configForm.skillId, boxId: bId })
    );
    await Promise.all(promises);
    
    message.success(`🚀 模型权重包已打包，正在向 ${configForm.boxIds.length} 个网关下发...`);
    configDialogVisible.value = false;
    fetchData(); 
  } catch (error) { message.error('部分节点下发握手超时'); } finally { configuring.value = false; }
}

// ========= 真实的云端整个卸载逻辑 =========
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
  if (!deleteReason.value.trim()) return message.warning('必须填写卸载原因！');
  deleteLoading.value = true;
  try {
    const res = await request.delete(`/api/skill/delete/${currentDeleteRecord.value.id}?reason=${encodeURIComponent(deleteReason.value)}`);
    if (res.code === 200 || res.code === 0) {
      message.success('模型及其相关依赖已彻底卸载并审计');
      deleteModalVisible.value = false;
      fetchData(); 
    } else {
      message.error(res.msg || '卸载受阻');
    }
  } catch (error) { message.error('卸载请求异常'); } finally { deleteLoading.value = false; }
};

onMounted(() => { fetchData() })
</script>

<style scoped>
.market-page { padding: 24px; background: #f8fafc; min-height: 100vh; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;}
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 22px; font-weight: 800; color: #0f172a; margin: 0 0 8px 0; }
.sub-text { font-size: 13px; color: #64748b; margin: 0; }

.glass-panel { background: #ffffff; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.02), 0 2px 4px -2px rgba(0,0,0,0.02); border: 1px solid #f1f5f9; }

.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.btn-install { background: #0ea5e9; border: none; font-weight: 600; box-shadow: 0 4px 6px rgba(14,165,233,0.2); }
.btn-install:hover { background: #0284c7; }
.btn-activate { background: #10b981; border: none; color: #fff; font-weight: 600; }
.btn-activate:hover, .btn-activate:focus { background: #059669; color: #fff; }

.skill-icon { width: 40px; height: 40px; background: #f0f9ff; color: #0ea5e9; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 18px; font-weight: 900; margin-right: 12px; font-family: 'Impact', sans-serif;}

.modal-alert { margin-bottom: 20px; padding: 10px 16px; background: #fffbeb; border: 1px dashed #fcd34d; border-radius: 6px; color: #b45309; font-size: 13px; line-height: 1.6; }

/* 极致表格美化 */
:deep(.aero-table .ant-table-thead > tr > th) { background-color: #f8fafc !important; font-weight: 600; color: #475569; border-bottom: 1px solid #e2e8f0 !important; padding: 16px; }
:deep(.aero-table .ant-table-tbody > tr > td) { border-bottom: 1px solid #f1f5f9 !important; padding: 16px; transition: background 0.2s;}
:deep(.aero-table .ant-table-tbody > tr:hover > td) { background-color: #f8fafc !important; }
:deep(.aero-table .ant-table-thead > tr > th::before) { display: none !important; }

/* 拖拽上传覆盖 */
:deep(.ant-upload-drag) { background: #f8fafc; border-color: #cbd5e1; }
:deep(.ant-upload-drag:hover) { border-color: #0ea5e9; }
</style>