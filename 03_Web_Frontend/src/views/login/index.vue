<template>
  <div class="aurora-login-container">
    <div class="aurora-bg">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="noise-overlay"></div>
    </div>

    <div class="cyber-terminal">
      <div class="terminal-header">
        <div class="logo-shield">
          <safety-certificate-outlined />
        </div>
        <h1 class="sys-title">校安通</h1>
        <p class="sys-subtitle">边缘人工智能与联邦学习平台</p>
      </div>

      <a-form
        :model="loginForm"
        :rules="rules"
        @finish="handleLogin"
        class="terminal-form"
      >
        <div class="input-group">
          <label>账号名称</label>
          <a-form-item name="username">
            <a-input
              v-model:value="loginForm.username"
              placeholder="请输入您的管理员或用户账号"
              class="cyber-input"
            >
              <template #prefix><user-outlined class="input-icon" /></template>
            </a-input>
          </a-form-item>
        </div>

        <div class="input-group password-group">
          <label>登录密码</label>
          <a-form-item name="password">
            <a-input-password
              v-model:value="loginForm.password"
              placeholder="请输入密码"
              class="cyber-input cyber-password-input"
            >
              <template #prefix><lock-outlined class="input-icon" /></template>
              </a-input-password>
          </a-form-item>
        </div>

        <a-form-item style="margin-top: 50px; margin-bottom: 0;">
          <a-button
            html-type="submit"
            block
            :loading="loading"
            class="cyber-btn"
          >
            <span>安全登录</span>
            <arrow-right-outlined class="btn-arrow" />
          </a-button>
        </a-form-item>
      </a-form>
    </div>
    
    <div class="terminal-footer">
      安全连接已建立 © 2026 SMART CAMPUS. 版权所有
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { 
  UserOutlined, LockOutlined, SafetyCertificateOutlined, ArrowRightOutlined
  // 如果需要自定义眼睛图标，可以导入这两个：
  // EyeOutlined, EyeInvisibleOutlined 
} from '@ant-design/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const loading = ref(false);

const loginForm = reactive({ username: '', password: '' });

// 规则提示语已改为中文
const rules = {
  username: [{ required: true, message: '请输入您的账号名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入您的登录密码', trigger: 'blur' }]
};

const handleLogin = async () => {
  loading.value = true;
  try {
    const res = await request.post('/api/auth/login', loginForm);
    
    if (res.code === 200 || res.code === 0) {
      const payload = res.data?.data || res.data || res; 
      const token = payload.token || payload.access_token;

      if (!token) return message.error("登录失败：凭证缺失 [AUTH-NO-TOKEN]");

      const role = payload.role || payload.role_key || 'user'; 
      const realName = payload.real_name || payload.realName || payload.username || loginForm.username; 

      localStorage.setItem('token', token);
      localStorage.setItem('username', loginForm.username); 
      localStorage.setItem('realName', realName);
      localStorage.setItem('userRole', role);

      message.success(`连接成功：欢迎回来，${realName}`);
      router.push('/dashboard'); 
    } else {
      message.error(res.msg || "登录失败：凭证无效，请检查账号密码");
    }
  } catch (error) {
    console.error(error);
    message.error("网络连接异常，请重试");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 深空背景底座 */
.aurora-login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: #020617; /* 极深蓝黑 */
  position: relative;
  overflow: hidden;
  /* 增加了中文字体的优先级 */
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "DIN Alternate", "Segoe UI", Roboto, sans-serif;
}

/* 纯 CSS 极致极光渲染 */
.aurora-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; overflow: hidden; z-index: 0; }
.orb { position: absolute; border-radius: 50%; filter: blur(90px); opacity: 0.5; animation: float 20s infinite ease-in-out alternate; mix-blend-mode: screen; }
.orb-1 { width: 600px; height: 600px; background: #0ea5e9; top: -200px; left: -100px; animation-delay: 0s; }
.orb-2 { width: 500px; height: 500px; background: #8b5cf6; bottom: -150px; right: -100px; animation-delay: -5s; }
.orb-3 { width: 400px; height: 400px; background: #10b981; top: 40%; left: 50%; transform: translate(-50%, -50%); animation-delay: -10s; opacity: 0.3;}
.noise-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background-image: url('data:image/svg+xml,%3Csvg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg"%3E%3Cfilter id="noiseFilter"%3E%3CfeTurbulence type="fractalNoise" baseFrequency="0.65" numOctaves="3" stitchTiles="stitch"/%3E%3C/filter%3E%3Crect width="100%25" height="100%25" filter="url(%23noiseFilter)"/%3E%3C/svg%3E'); opacity: 0.04; z-index: 1; pointer-events: none;}

@keyframes float {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(50px, 30px) scale(1.1); }
  100% { transform: translate(-30px, 50px) scale(0.9); }
}

/* 硬核暗黑悬浮终端 */
.cyber-terminal {
  /* 稍微加宽一点，给内容更多空间，增加高级感 */
  width: 460px;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 24px; /* 增加圆角，更圆润 */
  padding: 60px 50px; /* 增加内边距，更有呼吸感 */
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255,255,255,0.1);
  position: relative;
  z-index: 10;
  transition: box-shadow 0.3s ease;
}
.cyber-terminal:hover {
  box-shadow: 0 40px 90px rgba(14, 165, 233, 0.15), inset 0 1px 0 rgba(255,255,255,0.1);
}

/* 终端头部 */
.terminal-header { text-align: center; margin-bottom: 50px; }
.logo-shield { width: 60px; height: 60px; margin: 0 auto 18px; display: flex; justify-content: center; align-items: center; font-size: 32px; color: #38bdf8; background: rgba(56, 189, 248, 0.1); border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 16px; box-shadow: 0 0 24px rgba(56, 189, 248, 0.2); }
.sys-title { color: #ffffff; font-size: 32px; font-weight: 800; letter-spacing: 2.5px; margin: 0 0 8px; }
.sys-subtitle { color: #94a3b8; font-size: 13px; font-weight: 500; letter-spacing: 1.8px; font-family: monospace; }

/* 极简输入组 */
.input-group { margin-bottom: 28px; }
.input-group label { display: block; font-size: 13px; color: #cbd5e1; font-weight: 600; letter-spacing: 1.2px; margin-bottom: 10px; font-family: monospace; }
.input-icon { color: #64748b; font-size: 18px; transition: color 0.3s; margin-right: 8px;}

/* 优化输入框样式 */
:deep(.cyber-input.ant-input-affix-wrapper) {
  background: rgba(0, 0, 0, 0.3) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px; /* 圆角加大 */
  padding: 14px 18px; /* 增加高度 */
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
:deep(.cyber-input.ant-input-affix-wrapper:hover) { border-color: rgba(56, 189, 248, 0.4) !important; }
:deep(.cyber-input.ant-input-affix-wrapper-focused) {
  background: rgba(0, 0, 0, 0.5) !important;
  border-color: #38bdf8 !important;
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.18), inset 0 2px 4px rgba(0,0,0,0.2) !important;
}
:deep(.cyber-input.ant-input-affix-wrapper-focused .input-icon) { color: #38bdf8; }

/* 核心：密码框内部样式修正 (眼睛图标) */
:deep(.cyber-password-input .ant-input) {
  font-size: 16px; /* 放大输入文字的字体 */
}
:deep(.cyber-password-input .ant-input-suffix) {
  margin-left: 12px;
}
:deep(.cyber-password-input .ant-input-suffix .ant-input-password-icon) {
  color: #64748b; /* 初始眼睛图标颜色 */
  font-size: 18px; /* 眼睛图标大小放大 */
  cursor: pointer;
  transition: color 0.3s;
}
:deep(.cyber-password-input .ant-input-suffix .ant-input-password-icon:hover) {
  color: #38bdf8; /* 鼠标悬停变色 */
}

/* 普通输入框文字样式 */
:deep(.cyber-input .ant-input) { background: transparent !important; color: #f8fafc; font-size: 16px; font-weight: 500; letter-spacing: 0.8px;}
:deep(.cyber-input .ant-input::placeholder) { color: #475569; font-weight: 400; font-size: 14px; }

/* 发光连接按钮 */
.cyber-btn {
  height: 56px; /* 按钮也更高 */
  background: #f8fafc;
  color: #0f172a;
  border: none;
  border-radius: 12px;
  font-weight: 700;
  font-size: 16px; /* 按钮字体放大 */
  letter-spacing: 2px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
}
.cyber-btn:hover {
  background: #38bdf8;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(56, 189, 248, 0.4);
}
.btn-arrow { transition: transform 0.3s ease; font-size: 16px; }
.cyber-btn:hover .btn-arrow { transform: translateX(5px); }

/* 底部声明 */
.terminal-footer { position: relative; z-index: 10; margin-top: 40px; font-size: 11px; color: #475569; font-family: monospace; letter-spacing: 1.2px; text-align: center; }
</style>