import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

// ===== 只保留 Ant Design Vue =====
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import * as AntdIcons from '@ant-design/icons-vue';

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd) // 🌟 只有 Ant Design

// 注册 Ant Design 图标
for (const i in AntdIcons) {
  app.component(i, AntdIcons[i]);
}

app.mount('#app')