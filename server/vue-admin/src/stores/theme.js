import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 主题模式: 'light' | 'dark' | 'system'
  const mode = ref(localStorage.getItem('theme-mode') || 'system')

  // 当前实际主题
  const isDark = ref(false)

  // 系统主题监听
  const systemDark = ref(false)

  // 初始化
  function init() {
    // 检测系统主题
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemDark.value = mediaQuery.matches

    // 监听系统主题变化
    mediaQuery.addEventListener('change', (e) => {
      systemDark.value = e.matches
      if (mode.value === 'system') {
        applyTheme(e.matches)
      }
    })

    // 应用主题
    updateTheme()
  }

  // 更新主题
  function updateTheme() {
    if (mode.value === 'system') {
      applyTheme(systemDark.value)
    } else {
      applyTheme(mode.value === 'dark')
    }
  }

  // 应用主题
  function applyTheme(dark) {
    isDark.value = dark
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    document.documentElement.classList.toggle('dark', dark)
  }

  // 设置主题模式
  function setMode(newMode) {
    mode.value = newMode
    localStorage.setItem('theme-mode', newMode)
    updateTheme()
  }

  // 切换主题
  function toggle() {
    if (mode.value === 'light') {
      setMode('dark')
    } else if (mode.value === 'dark') {
      setMode('system')
    } else {
      setMode('light')
    }
  }

  // 监听模式变化
  watch(mode, () => {
    updateTheme()
  })

  return {
    mode,
    isDark,
    systemDark,
    init,
    setMode,
    toggle
  }
})
