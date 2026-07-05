import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

// GET请求去重：同一URL在pending期间不会重复发送
const pendingRequests = new Map()

const generateKey = (config) => {
  const { method, url, params } = config
  return [method, url, JSON.stringify(params)].join('&')
}

const addPending = (config) => {
  const key = generateKey(config)
  if (pendingRequests.has(key)) {
    const cancel = pendingRequests.get(key)
    cancel('duplicate')
    pendingRequests.delete(key)
  }
  config.cancelToken = new axios.CancelToken((cancel) => {
    pendingRequests.set(key, cancel)
  })
}

const removePending = (config) => {
  const key = generateKey(config)
  pendingRequests.delete(key)
}

const request = axios.create({
  baseURL: '/api/admin',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    // 全局景区ID参数
    const spotId = localStorage.getItem('currentSpotId')
    if (spotId) {
      config.params = { ...config.params, scenicSpotId: spotId }
    }
    // GET请求去重
    if (config.method === 'get') addPending(config)
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    removePending(response.config)
    const res = response.data
    if (res.code === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(new Error('Token expired'))
    }
    return response
  },
  (error) => {
    if (axios.isCancel(error)) return Promise.resolve({ data: { code: 0, data: null } })
    removePending(error.config || {})
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('登录已过期')
    }
    return Promise.reject(error)
  }
)

export default request
