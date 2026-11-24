<template>
  <div class="state-page">
    <div class="state-card">
      <div class="code">404</div>
      <h1>页面不存在</h1>
      <p>地址可能输错了，或者这个页面已经下线。</p>
      <el-button type="primary" @click="goHome">回到首页</el-button>
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
  if (!store.user) {
    router.replace('/login')
    return
  }
  router.replace(homeForRole(store.user.role))
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
  max-width: 460px;
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
  margin: 10px 0;
  font-size: 20px;
  color: #111827;
}

p {
  margin: 0 0 24px;
  color: var(--g12-text-secondary);
  font-size: 13px;
}
</style>
