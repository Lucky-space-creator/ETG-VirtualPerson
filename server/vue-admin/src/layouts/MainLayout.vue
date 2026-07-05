<template>
  <el-container class="main-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <h2>VirtualWife</h2>
        <span>管理员后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :background-color="menuBgColor"
        :text-color="menuTextColor"
        :active-text-color="menuActiveColor"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>聊天记录</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/rag">
          <el-icon><Document /></el-icon>
          <span>RAG文档</span>
        </el-menu-item>
        <el-menu-item index="/llm">
          <el-icon><Cpu /></el-icon>
          <span>LLM配置</span>
        </el-menu-item>
        <el-menu-item index="/scenic">
          <el-icon><Histogram /></el-icon>
          <span>景区管理</span>
        </el-menu-item>
        <el-menu-item index="/avatar">
          <el-icon><UserFilled /></el-icon>
          <span>形象管理</span>
        </el-menu-item>
        <el-menu-item index="/route">
          <el-icon><MapLocation /></el-icon>
          <span>路线管理</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><TrendCharts /></el-icon>
          <span>用户分析</span>
        </el-menu-item>
        <el-menu-item index="/tourist-analysis">
          <el-icon><PieChart /></el-icon>
          <span>游客消费分析</span>
        </el-menu-item>
        <el-menu-item index="/tourist-import">
          <el-icon><Upload /></el-icon>
          <span>消费数据导入</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/role">
          <el-icon><Lock /></el-icon>
          <span>角色权限</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h3>{{ route.meta.title }}</h3>
          <el-select
            v-model="currentSpotId"
            placeholder="选择景区"
            size="small"
            style="width:180px;margin-left:24px"
            @change="onSpotChange"
          >
            <el-option v-for="s in spotList" :key="s.id" :label="s.spotName" :value="s.id" />
          </el-select>
        </div>
        <div class="header-right">
          <!-- 主题切换按钮 -->
          <el-tooltip :content="themeLabel" placement="bottom">
            <button class="theme-toggle" @click="themeStore.toggle()">
              <el-icon v-if="themeStore.mode === 'light'" :size="18"><Sunny /></el-icon>
              <el-icon v-else-if="themeStore.mode === 'dark'" :size="18"><Moon /></el-icon>
              <el-icon v-else :size="18"><Monitor /></el-icon>
            </button>
          </el-tooltip>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><Avatar /></el-icon>
              {{ authStore.user?.nickname || authStore.user?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['Dashboard']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

// 景区全局选择
const spotList = ref([])
const currentSpotId = ref(localStorage.getItem('currentSpotId') || '')

const loadSpots = async () => {
  try {
    const res = await request.get('/scenic/page', { params: { pageSize: 50 } })
    spotList.value = res.data?.data?.records || []
    if (!currentSpotId.value && spotList.value.length > 0) {
      currentSpotId.value = spotList.value[0].id
    }
  } catch (_) {}
}

const onSpotChange = () => {
  localStorage.setItem('currentSpotId', currentSpotId.value)
  window.dispatchEvent(new CustomEvent('scenic-changed', { detail: currentSpotId.value }))
}

const themeLabel = computed(() => {
  if (themeStore.mode === 'light') return '浅色模式'
  if (themeStore.mode === 'dark') return '深色模式'
  return '跟随系统'
})

const menuBgColor = computed(() => themeStore.isDark ? 'transparent' : '#ffffff')
const menuTextColor = computed(() => themeStore.isDark ? '#94a3b8' : '#606266')
const menuActiveColor = computed(() => themeStore.isDark ? '#667eea' : '#409EFF')

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/chat')) return '/chat'
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/rag')) return '/rag'
  if (path.startsWith('/llm')) return '/llm'
  if (path.startsWith('/avatar')) return '/avatar'
  if (path.startsWith('/route')) return '/route'
  if (path.startsWith('/tourist-analysis')) return '/tourist-analysis'
  return path
})

const handleCommand = (command) => {
  if (command === 'logout') {
    authStore.logout()
  }
}

onMounted(() => {
  themeStore.init()
  loadSpots()
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background: var(--bg-sidebar);
  overflow-y: auto;
  border-right: 1px solid var(--border-primary);
  transition: background 0.3s ease;
}

.sidebar::-webkit-scrollbar {
  width: 4px;
}
.sidebar::-webkit-scrollbar-thumb {
  background: var(--text-muted);
  border-radius: 2px;
}

.logo {
  padding: 28px 20px;
  text-align: center;
  border-bottom: 1px solid var(--border-primary);
  transition: all 0.3s ease;
}

.logo h2 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo span {
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.header {
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  transition: background 0.3s ease;
}

.header-left h3 {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  border: 1px solid var(--border-input);
}
.user-info:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.main-content {
  background: var(--bg-primary);
  padding: 20px;
  overflow-y: auto;
  transition: background 0.3s ease;
}

.theme-toggle {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border-input);
  background: var(--bg-input);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.theme-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
  border-color: var(--accent-primary);
}
</style>
