import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout/index.vue'

const routes = [
  { path: '/login', component: () => import('@/views/login/index.vue') },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard', // 登录后默认跳转到总览大屏
    children: [
      // 🌟 1. 公共页面（所有人都能进）
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue') },
      { path: 'monitor', component: () => import('@/views/monitor/index.vue') },
      
      // 事件中心
      { path: 'event', component: () => import('@/views/event/index.vue') },
      { path: 'event/analysis', component: () => import('@/views/event/analysis.vue') },
      { path: 'event/redblack', component: () => import('@/views/event/redblack.vue') },
      
      // 个人中心
      { path: 'profile', component: () => import('@/views/profile/info.vue') },

      // 🌟 2. 管理员专属页面（普通用户进不来，带 requireAdmin 标记）
      
      // 🌟 技能管理 (修改这里，不再需要二级路由了)
      { path: 'skill', component: () => import('@/views/skill/market.vue'), meta: { requireAdmin: true } },
      
      // 设备管理
      { path: 'device/list', component: () => import('@/views/device/list.vue'), meta: { requireAdmin: true } },
      { path: 'device/config', component: () => import('@/views/device/config.vue'), meta: { requireAdmin: true } },
      
      // 联邦学习
      { path: 'federated/tasks', component: () => import('@/views/federated/tasks.vue'), meta: { requireAdmin: true } },
      { path: 'federated/models', component: () => import('@/views/federated/models.vue'), meta: { requireAdmin: true } },
      
      // 系统设置
      { path: 'system/user', component: () => import('@/views/system/user.vue'), meta: { requireAdmin: true } },
      // { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { requireAdmin: true } },
      { path: 'system/log', component: () => import('@/views/system/log.vue'), meta: { requireAdmin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 🌟 路由守卫：真正的权限拦截器
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('userRole'); // 'admin' 或 'user'

  // 1. 如果没登录，且要去的地方不是登录页，踢回登录页
  if (!token && to.path !== '/login') {
    return next('/login');
  }

  // 2. 如果是普通用户，却企图访问带有 requireAdmin 标记的页面，踢回总览页
  if (to.meta.requireAdmin && userRole !== 'admin') {
    console.warn("越权访问！已被拦截");
    return next('/dashboard'); 
  }

  // 3. 正常放行
  next();
})

export default router