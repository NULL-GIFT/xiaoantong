<template>
  <div class="permission-container">
    <a-result
      status="403"
      title="403"
      sub-title="抱歉，您没有权限访问此页面。"
    >
      <template #extra>
        <a-button type="primary" @click="goHome">返回首页</a-button>
        <a-button @click="contactAdmin">联系管理员</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const contactAdmin = () => {
  // 这里可以弹出联系方式或跳转到联系页面
  alert('请联系系统管理员申请权限')
}
</script>