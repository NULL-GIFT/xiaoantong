<template>
  <a-config-provider>
    <router-view />
  </a-config-provider>
  <!-- <router-view v-slot="{ Component }">
  <keep-alive>
    <component :is="Component" />
  </keep-alive>
</router-view> -->
</template>

<script setup>
// 这里不需要引入 Layout 相关的东西了
</script>

<style>
/* 全局样式放这里 */
body {
  margin: 0;
  padding: 0;
  background-color: #f0f2f5;
}

</style>