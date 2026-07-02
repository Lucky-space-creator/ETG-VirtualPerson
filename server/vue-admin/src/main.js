import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 按需加载Element Plus基础样式（CSS变量/重置），组件CSS由unplugin自动注入
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-notification.css'

import './styles/global.css'
import './styles/theme-variables.css'
import './styles/dark-theme.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
