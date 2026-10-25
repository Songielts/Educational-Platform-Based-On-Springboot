/// <reference types="./shims-vue" />
/// <reference types="./shims-pinia" />
/// <reference types="./vite-env" />

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 按顺序使用插件
app.use(createPinia())
app.use(router as any)  // 类型断言解决 Router 与 Plugin 不兼容问题
app.use(ElementPlus)

app.mount('#app')