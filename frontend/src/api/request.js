import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import { bizMessage } from '@/constants'
import mockAdapter from '@/mock/adapter'

/**
 * 统一 axios 实例
 * - baseURL '/api'：页面里只写 /applies 这种相对路径
 * - 自动带 Authorization: Bearer <token>
 * - 401 清 token 跳登录；其余错误统一弹 Message
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 开发环境用 mock 适配器接住全部请求，生产构建里这段分支不会启用
if (import.meta.env.VITE_USE_MOCK === 'true') {
  service.defaults.adapter = mockAdapter
}

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('g12.token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 同一时间多个请求失败时避免连续弹一屏提示
let lastMessage = ''
let lastMessageAt = 0
function notify(text, type = 'error') {
  const now = Date.now()
  if (text === lastMessage && now - lastMessageAt < 1200) return
  lastMessage = text
  lastMessageAt = now
  ElMessage({ message: text, type, duration: 2600, showClose: true })
}

service.interceptors.response.use(
  (response) => {
    const payload = response.data
    // 契约规定 HTTP 状态与业务 code 同步；这里再兜一层，防止 200 包着错误码
    if (payload && typeof payload.code === 'number' && payload.code !== 200) {
      notify(bizMessage(payload.message, '请求失败'))
      return Promise.reject(Object.assign(new Error(payload.message || '请求失败'), { payload }))
    }
    return payload?.data
  },
  (error) => {
    const status = error.response?.status
    const raw = error.response?.data?.message
    const text = bizMessage(raw, error.message || '网络异常，请稍后重试')

    if (status === 401) {
      const store = useUserStore()
      store.clearSession()
      const isLogin = (error.config?.url || '').includes('/auth/login')
      if (!isLogin) {
        notify(text || '登录已过期，请重新登录')
        const current = router.currentRoute.value
        if (current.path !== '/login') {
          router.replace({ path: '/login', query: { redirect: current.fullPath } })
        }
      } else {
        notify(text)
      }
    } else if (status === 403) {
      notify(text || '没有权限执行该操作')
    } else {
      notify(text)
    }

    return Promise.reject(error)
  }
)

export default service
