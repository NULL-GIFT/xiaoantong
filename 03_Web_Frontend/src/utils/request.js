import axios from 'axios'
import { message } from 'ant-design-vue' 
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '', 
  timeout: 10000, 
})

// === 1. 请求拦截器 ===
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username'); // 🌟 拿到当前登录人账号
    
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`; 
    }
    if (username) {
      // 🌟 核心修复1：把操作人放在 Header 里传给 Java，用 URL 编码防止中文乱码
      config.headers['X-Operator'] = encodeURIComponent(username); 
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
)

// === 2. 响应拦截器 ===
service.interceptors.response.use(
  (response) => {
    return response.data || response;
  },
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        message.warning('登录凭证已失效，请重新登录！');
        localStorage.removeItem('token');
        localStorage.removeItem('userRole');
        if (router) { router.push('/login'); } 
        else { window.location.href = '/login'; }
      } else {
        message.error(error.response.data?.msg || `请求失败 (${error.response.status})`);
      }
    } else {
      message.error('网络连接断开或服务未启动，请检查网络！');
    }
    return Promise.reject(error);
  }
)

export default service