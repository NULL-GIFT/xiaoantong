<template>
  <a-layout-sider v-model:collapsed="collapsed" collapsible theme="dark">
    <div class="logo">
      <span v-if="!collapsed">校安通智能安防</span>
      <span v-else>安防</span>
    </div>
    
    <a-menu
      v-model:selectedKeys="selectedKeys"
      v-model:openKeys="openKeys"
      theme="dark"
      mode="inline"
      @select="handleSelect"
      @openChange="handleOpenChange"
    >
      <a-menu-item key="/dashboard">
        <template #icon><dashboard-outlined /></template>
        <span>数据大屏</span>
      </a-menu-item>
      
      <a-menu-item key="/monitor">
        <template #icon><video-camera-outlined /></template>
        <span>实时监控</span>
      </a-menu-item>
      
      <a-sub-menu key="/event">
        <template #icon><alert-outlined /></template>
        <template #title>事件中心</template>
        <a-menu-item key="/event/index">报警列表</a-menu-item>
        <a-menu-item key="/event/redblack">红黑榜</a-menu-item>
        <a-menu-item key="/event/analysis">智能分析报告</a-menu-item>
      </a-sub-menu>
      
      <a-menu-item key="/skill" v-if="userRole === 'admin'">
        <template #icon><tool-outlined /></template>
        <span>技能管理</span>
      </a-menu-item>
      
      <a-sub-menu key="/device" v-if="userRole === 'admin'">
        <template #icon><setting-outlined /></template>
        <template #title>设备管理</template>
        <a-menu-item key="/device/list">设备列表</a-menu-item>
        <a-menu-item key="/device/config">设备配置</a-menu-item>
      </a-sub-menu>
      
      <a-sub-menu key="/federated" v-if="userRole === 'admin'">
        <template #icon><experiment-outlined /></template>
        <template #title>联邦学习</template>
        <a-menu-item key="/federated/tasks">训练任务列表</a-menu-item>
        <a-menu-item key="/federated/models">模型版本管理</a-menu-item>
      </a-sub-menu>
      
      <a-sub-menu key="/system" v-if="userRole === 'admin'">
        <template #icon><setting-outlined /></template>
        <template #title>系统设置</template>
        <a-menu-item key="/system/user">用户管理</a-menu-item>
        <!-- <a-menu-item key="/system/role">角色权限</a-menu-item> -->
        <a-menu-item key="/system/log">操作日志</a-menu-item>
      </a-sub-menu>

      <a-menu-item key="/profile">
        <template #icon><user-outlined /></template>
        <span>个人中心</span>
      </a-menu-item>

    </a-menu>
  </a-layout-sider>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  DashboardOutlined,
  VideoCameraOutlined,
  AlertOutlined,
  ToolOutlined,
  SettingOutlined,
  ExperimentOutlined,
  UserOutlined // 🌟 别忘了引入个人中心的图标
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const collapsed = ref(false)
const selectedKeys = ref([route.path])
const openKeys = ref([])

// 🌟 获取当前登录用户的角色，确保能正确显示/隐藏菜单
const userRole = ref('user');

onMounted(() => {
  userRole.value = localStorage.getItem('userRole') || 'user';
});

// 菜单选择
const handleSelect = ({ key }) => {
  // 修正路由跳转路径（去掉多余的 /index，保持和 router.js 一致）
  const targetPath = key.endsWith('/index') ? key.replace('/index', '') : key;
  router.push(targetPath);
}

// 子菜单展开/收起
const handleOpenChange = (keys) => {
  openKeys.value = keys
}

// 监听路由变化，更新选中状态
watch(() => route.path, (newPath) => {
  // 如果当前在 /event，让菜单高亮匹配上 /event/index
  const activeKey = newPath === '/event' ? '/event/index' : newPath;
  selectedKeys.value = [activeKey]
  
  // 自动展开父级菜单
  if (newPath.startsWith('/event')) {
    openKeys.value = ['/event']
  } else if (newPath.startsWith('/device')) {
    openKeys.value = ['/device']
  } else if (newPath.startsWith('/federated')) {
    openKeys.value = ['/federated']
  } else if (newPath.startsWith('/system')) {
    openKeys.value = ['/system']
  }
}, { immediate: true })
</script>

<style scoped>
.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
  text-align: center;
  line-height: 32px;
  color: #fff;
  overflow: hidden;
  white-space: nowrap;
  transition: all 0.3s;
}
</style>