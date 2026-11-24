<template>
  <div class="state-page">
    <div class="state-card">
      <div class="code">403</div>
      <h1>没有访问该页面的权限</h1>
      <p>当前账号角色与页面要求不匹配。菜单只展示你有权限的入口，直接输入地址也会被路由守卫拦住。</p>
      <div class="actions">
        <el-button type="primary" @click="goHome">回到工作台</el-button>
        <el-button @click="relogin">换个账号登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { homeForRole } from '@/constants'

const router = useRouter()
const store = useUserStore()

function goHome() {
  router.replace(store.user ? homeForRole(store.user.role) : '/login')
}

function relogin() {
  store.clearSession()
  router.replace('/login')
}
</script>

<style scoped>
.state-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: radial-gradient(70% 50% at 50% 0%, #eef4fe, #f8fafc 70%);
}

.state-card {
  max-width: 520px;
  padding: 40px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(17, 24, 39, 0.06);
  box-shadow: 0 30px 60px -40px rgba(17, 24, 39, 0.4);
  text-align: center;
}

.code {
  font-size: 72px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -2px;
  color: #dbeafe;
}

h1 {
  margin: 10px 0 10px;
  font-size: 20px;
  color: #111827;
}

p {
  margin: 0 0 24px;
  color: var(--g12-text-secondary);
  font-size: 13px;
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
