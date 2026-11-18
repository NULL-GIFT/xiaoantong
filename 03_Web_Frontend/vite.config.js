import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => {
  // 加载 .env 环境变量
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [vue(), vueJsx(), vueDevTools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      proxy: {
        '/api': {
          target: env.VITE_APP_BASE_API, // 🌟 从环境变读取 Java 后端地址
          changeOrigin: true,
        },
        '/ws': {
          target: env.VITE_APP_BASE_API, // 🌟 WebSocket 代理地址
          ws: true,
        }
      }
    }
  }
})