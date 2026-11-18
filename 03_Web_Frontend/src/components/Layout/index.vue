<template>
  <a-layout style="min-height: 100vh">
    <Sidebar />
    <a-layout>
      <Navbar />
      <a-layout-content style="margin: 16px">
        <!-- 这里会显示具体页面的内容 -->
        <router-view />
      </a-layout-content>
      <a-layout-footer style="text-align: center">
        校安通智能安防系统 ©2026 Created by Ant Design Vue
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'

import { onMounted } from 'vue';
import { initWebSocket } from '@/utils/websocket';

onMounted(() => {
  initWebSocket(import.meta.env.VITE_APP_WS_URL); 
});

</script>