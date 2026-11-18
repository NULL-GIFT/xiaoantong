<template>
  <a-layout-header class="header">
    <div class="header-left">
      </div>
    <div class="header-right">
      <a-tag color="blue" style="margin-right: 16px;">系统状态：正常</a-tag>
      
      <a-dropdown placement="bottomRight">
        <a class="ant-dropdown-link" @click.prevent style="color: #333; font-size: 15px; cursor: pointer;">
          <UserOutlined style="margin-right: 6px;" />
          {{ realName || '未登录' }}
          <DownOutlined style="margin-left: 4px; font-size: 12px;" />
        </a>
        <template #overlay>
          <a-menu>
            <a-menu-item key="role">
              <a-tag :color="userRole === 'admin' ? 'red' : 'blue'">
                {{ userRole === 'admin' ? '👑 管理员' : '🛡️ 普通用户' }}
              </a-tag>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout" @click="handleLogout">
              <span style="color: #ff4d4f;"><LogoutOutlined /> 退出登录</span>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </a-layout-header>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
// 🌟 引入图标
import { UserOutlined, DownOutlined, LogoutOutlined } from '@ant-design/icons-vue';

const router = useRouter();

// 响应式变量，用于显示名字和判断角色
const realName = ref('');
const userRole = ref('');

onMounted(() => {
  // 页面一加载，就把登录时存进 localStorage 的身份拿出来显示
  realName.value = localStorage.getItem('realName') || '游客';
  userRole.value = localStorage.getItem('userRole') || 'guest';
});

// 🌟 退出登录功能
const handleLogout = () => {
  // 1. 无情地清空所有本地存储的身份信息
  localStorage.removeItem('token');
  localStorage.removeItem('realName');
  localStorage.removeItem('userRole');
  
  // 2. 强行踢回登录页
  router.push('/login');
};
</script>

<style scoped>
.header {
  background: #fff !important;
  padding: 0 24px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}

.header-right {
  display: flex;
  align-items: center;
}
</style>