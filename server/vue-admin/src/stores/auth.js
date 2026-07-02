import { defineStore } from 'pinia'
import { ref } from 'vue'
import router from '@/router'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')

  // 延迟解析user，避免JSON.parse在每次store创建时执行
  const _cachedUser = localStorage.getItem('user')
  const user = ref(_cachedUser && _cachedUser !== 'null' ? JSON.parse(_cachedUser) : null)

  const login = async (username, password) => {
    const res = await request.post('/auth/login', { username, password })
    const data = res.data.data
    token.value = data.token
    user.value = data.user
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    return data
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }

  const isLoggedIn = () => !!token.value

  return { token, user, login, logout, isLoggedIn }
})
