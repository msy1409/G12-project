import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { ROLES, ROLE_LABEL } from '@/constants'

const TOKEN_KEY = 'g12.token'
const USER_KEY = 'g12.user'

function readJSON(key) {
  try {
    return JSON.parse(localStorage.getItem(key) || 'null')
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readJSON(USER_KEY)
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state) => state.user?.role || '',
    roleLabel: (state) => ROLE_LABEL[state.user?.role] || '访客',
    realName: (state) => state.user?.realName || state.user?.username || '',
    department: (state) => state.user?.deptName || '—'
  },

  actions: {
    setSession(token, user) {
      this.token = token || ''
      this.user = user || null
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
      if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
      else localStorage.removeItem(USER_KEY)
    },

    async login(payload) {
      const data = await authApi.login(payload)
      // 登录响应里的 user 补充一个展示用字段
      this.setSession(data.token, { ...data.user, status: 1 })
      return data.user
    },

    async fetchMe() {
      const me = await authApi.me()
      this.user = me
      localStorage.setItem(USER_KEY, JSON.stringify(me))
      return me
    },

    async logout() {
      try {
        if (this.token) await authApi.logout()
      } catch {
        // JWT 无状态，退出失败也直接清本地
      }
      this.clearSession()
    },

    clearSession() {
      this.setSession('', null)
    },

    hasRole(roles) {
      if (!roles || !roles.length) return true
      return roles.includes(this.role)
    },

    isAdmin() {
      return this.role === ROLES.ADMIN
    }
  }
})
