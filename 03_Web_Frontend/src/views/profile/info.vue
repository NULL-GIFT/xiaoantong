<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>
    
    <a-row :gutter="24" class="relative-z">
      
      <a-col :span="8">
        <a-card title="👤 个人数字档案" :bordered="false" class="custom-card aero-card">
          <div class="avatar-section">
            <a-avatar 
              :size="100" 
              class="user-avatar"
              :style="{ backgroundColor: userInfo.role === 'admin' ? '#ef4444' : '#0ea5e9' }"
            >
              {{ avatarChar }}
            </a-avatar>
            <h2 class="real-name">{{ userInfo.realName }}</h2>
            <div class="role-badge">
              <a-tag :color="userInfo.role === 'admin' ? 'red' : 'blue'">
                {{ userInfo.role === 'admin' ? '👑 系统管理员' : '🛡️ 普通用户' }}
              </a-tag>
            </div>
          </div>
          
          <a-divider style="margin: 16px 0;" />
          
          <a-descriptions :column="1" size="small" class="real-info-list">
            <a-descriptions-item label="统一认证账号">
              <span class="fw-bold">{{ userInfo.username }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="IAM 授权状态">
              <a-badge :status="userInfo.status == '1' ? 'success' : 'error'" :text="userInfo.status == '1' ? '正常活跃' : '账号被限'" />
            </a-descriptions-item>
            <a-descriptions-item label="系统操作安全级别">
              <span style="color: #10b981; font-weight: bold;">高信任度</span>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :span="16">
        <a-card :bordered="false" class="custom-card aero-card p-0" :bodyStyle="{ padding: 0 }">
          <a-tabs v-model:activeKey="activeTab" size="large" style="padding: 10px 20px;">
            
            <a-tab-pane key="security" tab="🔒 密码与安全中心">
              <div class="pwd-wrapper">
                <a-alert 
                  message="工业级安全基线约束" 
                  description="身份凭证是系统安全的核心防线。新密码长度须不少于 6 位，必须包含字母与数字，且不能与近期使用的密码相同。" 
                  type="info" show-icon class="mb-4 aero-alert"
                />

                <a-form 
                  layout="vertical" :model="pwdForm" :rules="pwdRules" 
                  ref="pwdFormRef" @finish="handleUpdatePassword" class="pwd-form"
                >
                  <a-form-item label="当前审计原密码" name="oldPwd">
                    <a-input-password v-model:value="pwdForm.oldPwd" size="large" placeholder="请输入当前正在使用的密码" autocomplete="new-password" />
                  </a-form-item>
                  
                  <a-form-item label="签发新密码" name="newPwd">
                    <a-input-password v-model:value="pwdForm.newPwd" size="large" placeholder="请输入包含字母和数字的高强度密码" autocomplete="new-password" />
                  </a-form-item>
                  
                  <a-form-item label="新密码确认校验" name="confirmPwd">
                    <a-input-password v-model:value="pwdForm.confirmPwd" size="large" placeholder="请再次输入新密码以核对" autocomplete="new-password" />
                  </a-form-item>
                  
                  <a-form-item style="margin-top: 30px;">
                    <a-button type="primary" html-type="submit" size="large" :loading="btnLoading" class="aero-btn-primary submit-btn">
                      更新安全凭证并强制重新登录
                    </a-button>
                  </a-form-item>
                </a-form>
              </div>
            </a-tab-pane>

            <a-tab-pane key="profile" tab="📝 基础身份资料">
              <div class="pwd-wrapper">
                <a-form layout="vertical" :model="profileForm" @finish="handleUpdateProfile" class="pwd-form">
                  <a-form-item label="统一认证账号 (全局唯一标识不可修改)">
                    <a-input :value="userInfo.username" size="large" disabled />
                  </a-form-item>

                  <a-form-item label="操作员真实姓名 / 编号" name="realName" :rules="[{ required: true, message: '真实姓名不可为空' }]">
                    <a-input v-model:value="profileForm.realName" size="large" placeholder="如：张三 / 安保二队-05" />
                  </a-form-item>

                  <a-form-item style="margin-top: 30px;">
                    <a-button type="primary" html-type="submit" size="large" :loading="profileLoading" class="aero-btn-primary submit-btn">
                      同步资料变更至全域系统
                    </a-button>
                  </a-form-item>
                </a-form>
              </div>
            </a-tab-pane>

          </a-tabs>
        </a-card>
      </a-col>

    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import request from '@/utils/request'; 

const router = useRouter();
const activeTab = ref('security');

// 1. 用户资料状态
const userInfo = reactive({
  username: localStorage.getItem('username') || '未知账号',
  realName: localStorage.getItem('realName') || '操作员',
  role: localStorage.getItem('userRole') || 'user',
  status: localStorage.getItem('userStatus') || '1'
});

const avatarChar = computed(() => {
  return userInfo.realName && userInfo.realName.length > 0 ? userInfo.realName.charAt(0).toUpperCase() : 'U';
});

// 2. 资料更新逻辑
const profileLoading = ref(false);
const profileForm = reactive({ realName: userInfo.realName });

const handleUpdateProfile = async () => {
  profileLoading.value = true;
  try {
    const res = await request.post('/api/auth/updateProfile', {
      username: userInfo.username,
      realName: profileForm.realName
    });
    if (res.code === 200 || res.code === 0) {
      message.success('资料已同步至 IAM 系统中心');
      userInfo.realName = profileForm.realName;
      localStorage.setItem('realName', profileForm.realName); // 刷新本地缓存
    } else {
      message.error(res.msg || '资料更新受阻');
    }
  } catch (err) {
    message.error('网络请求异常');
  } finally {
    profileLoading.value = false;
  }
};

// 3. 密码表单逻辑
const btnLoading = ref(false);
const pwdFormRef = ref(null);
const pwdForm = reactive({ oldPwd: '', newPwd: '', confirmPwd: '' });

// 💡 前端企业级防呆拦截
const pwdRules = {
  oldPwd: [{ required: true, message: '请提供旧审计密码' }],
  newPwd: [
    { required: true, message: '新密码不可为空' },
    { min: 6, message: '新密码长度至少为 6 位' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{6,}$/, message: '新密码必须包含字母和数字' }
  ],
  confirmPwd: [
    { required: true, message: '请再次输入新密码' },
    { validator: async (rule, value) => {
        if (value !== pwdForm.newPwd) throw new Error('两次输入的密码无法吻合！');
      }
    }
  ]
};

const handleUpdatePassword = async () => {
  if (pwdForm.oldPwd === pwdForm.newPwd) return message.warning('安全合规要求：新密码不能与原密码相同！');
  
  btnLoading.value = true;
  try {
    const res = await request.post('/api/auth/updatePwd', {
      username: userInfo.username, oldPwd: pwdForm.oldPwd, newPwd: pwdForm.newPwd
    });

    if (res.code === 200 || res.code === 0) {
      message.success('✅ 凭证替换成功！系统将强制下线，请重新登录。');
      pwdFormRef.value.resetFields(); // 强制清理表单，防止二次提交
      
      setTimeout(() => {
        localStorage.clear();
        router.push('/login');
      }, 1200);
    } else {
      message.error(res.msg || '密码变更遭到拒绝');
    }
  } catch (error) {
    message.error("系统异常或服务器未响应");
  } finally {
    btnLoading.value = false;
  }
};
</script>

<style scoped>
.aero-dashboard { position: relative; padding: 24px; min-height: calc(100vh - 64px); background: #f4f7fb; color: #1e293b; overflow: hidden; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
.aero-bg-glow { position: absolute; top: -10%; left: -5%; width: 50%; height: 50%; background: radial-gradient(circle, rgba(14, 165, 233, 0.08) 0%, rgba(244, 247, 251, 0) 70%); pointer-events: none; z-index: 0; }
.relative-z { position: relative; z-index: 10; }

.aero-card { background: rgba(255,255,255,0.85); backdrop-filter: blur(12px); border-radius: 12px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02); border: 1px solid rgba(255,255,255,1); min-height: 540px; }

.avatar-section { text-align: center; padding: 20px 0; }
.user-avatar { font-size: 36px; font-weight: 900; box-shadow: 0 6px 16px rgba(0,0,0,0.1); font-family: 'Impact', sans-serif; }
.real-name { margin-top: 16px; margin-bottom: 8px; font-size: 22px; font-weight: 800; color: #0f172a; }
.role-badge { margin-bottom: 10px; font-weight: 600; }

.real-info-list { padding: 0 10px; }
.fw-bold { font-weight: 700; color: #1e293b; font-size: 15px; }

/* 右侧表单样式 */
.pwd-wrapper { padding: 20px 24px; }
.pwd-form { max-width: 460px; margin-top: 24px; }
.aero-btn-primary { background: #0f172a; border: none; font-weight: 600; box-shadow: 0 4px 12px rgba(15,23,42,0.15); }
.aero-btn-primary:hover { background: #1e293b; }
.submit-btn { width: 100%; }

.aero-alert { background: #f0f9ff; border: 1px dashed #bae6fd; color: #0369a1; }
:deep(.ant-tabs-nav) { margin-bottom: 0 !important; }
</style>