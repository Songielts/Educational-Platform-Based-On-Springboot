import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    proxy: {
      // ==================== API 接口代理 ====================
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 后端有 context-path: /api，保留前缀
      },
      // ==================== 上传文件代理（视频/图片等）====================
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 如果后端 uploads 接口也在 /api 上下文下，取消下面注释
        // rewrite: (path) => `/api${path}`,
      },
    },
  },
  // ==================== 生产打包防泄露与优化配置 ====================
  build: {
    // 1. 彻底关闭 SourceMap，防止线上浏览器 F12 还原 Vue 源码
    sourcemap: false,
    
    // 2. 混淆与压缩配置 (esbuild 方案比 terser 更快，且无需额外安装依赖)
    minify: 'esbuild',
    
    // 3. 打包自动清理 console 和 debugger，防止泄露接口与数据结构
    esbuild: {
      drop: ['console', 'debugger'],
    },

    // 4. 构建资源打包目录拆分（可选：让打包出来的 static 目录结构更清晰）
    rollupOptions: {
      output: {
        chunkFileNames: 'static/js/[name]-[hash].js',
        entryFileNames: 'static/js/[name]-[hash].js',
        assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
      },
    },
  },
})