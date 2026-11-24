<template>
  <div class="login-page">
    <section class="intro">
      <div class="brand">
        <div class="logo">G12</div>
        <span>Visitor Access Platform</span>
      </div>

      <div class="intro-main">
        <h1>企业访客<br />预约与核验平台</h1>
        <p class="slogan">从员工提交、审核通过、生成核验码，到门岗入场与离场登记，一条链路全部留痕。</p>

        <ul class="features">
          <li v-for="(item, index) in features" :key="item.title">
            <span class="idx">{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </li>
        </ul>
      </div>

      <div class="intro-foot">
        <span>Docker 云容器与管理技术</span>
        <span>Group 12 · 课程设计</span>
      </div>
    </section>

    <section class="panel">
      <div class="form-card">
        <div class="form-head">
          <h2>账号登录</h2>
          <p>三种角色进入各自工作台</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" autocomplete="username" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>

          <el-button class="submit" type="primary" :loading="loading" native-type="submit">
            登 录
          </el-button>
        </el-form>

        <div class="demo">
          <div class="demo-title">
            演示账号
            <span>密码统一 123456</span>
          </div>
          <div class="demo-list">
            <button v-for="account in accounts" :key="account.username" type="button" class="demo-item" @click="fill(account)">
              <span class="role">{{ account.role }}</span>
              <span class="username mono">{{ account.username }}</span>
            </button>
          </div>
        </div>

        <div v-if="isMock" class="mock-note">
          当前为 mock 模式：接口按 <code>docs/api.md</code> 在浏览器内模拟，数据保存在 localStorage，可在右上角菜单重置。
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { homeForRole } from '@/constants'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const isMock = import.meta.env.VITE_USE_MOCK === 'true'

const form = reactive({
  username: 'emp01',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { role: '员工', username: 'emp01' },
  { role: '审核员', username: 'auditor01' },
  { role: '管理员', username: 'admin' }
]

const features = [
  { title: '受控状态机', desc: '0→1→2→3→4，非法流转后端直接拒绝' },
  { title: '6 位核验码', desc: '审核通过后再生成，门岗凭码登记入场' },
  { title: '全量审计', desc: '每次状态变更同步写处理记录与审计日志' },
  { title: '角色隔离', desc: '员工、审核员、管理员按权限看到不同工作台' }
]

function fill(account) {
  form.username = account.username
  form.password = '123456'
}

async function submit() {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const user = await store.login({ username: form.username, password: form.password })
    ElMessage.success(`欢迎回来，${user.realName || user.username}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.replace(redirect || homeForRole(user.role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f8fafc;
}

.intro {
  position: relative;
  flex: 1.15;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px 56px;
  overflow: hidden;
  color: #e2e8f0;
  background:
    radial-gradient(90% 70% at 10% 0%, rgba(59, 130, 246, 0.35) 0%, transparent 60%),
    radial-gradient(80% 70% at 90% 100%, rgba(99, 102, 241, 0.28) 0%, transparent 60%),
    linear-gradient(155deg, #0f172a 0%, #111827 50%, #0b1220 100%);
}

.intro::after {
  content: '';
  position: absolute;
  right: -120px;
  top: 120px;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 0 0 60px rgba(148, 163, 184, 0.04), 0 0 0 130px rgba(148, 163, 184, 0.03);
  pointer-events: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  letter-spacing: 1.6px;
  text-transform: uppercase;
  color: #94a3b8;
  z-index: 1;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.6px;
  box-shadow: 0 10px 26px -8px rgba(59, 130, 246, 0.8);
}

.intro-main {
  max-width: 540px;
  padding: 40px 0;
  z-index: 1;
}

h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 48px);
  line-height: 1.18;
  letter-spacing: -1px;
  color: #f8fafc;
}

.slogan {
  margin: 18px 0 26px;
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.8;
  max-width: 440px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.features li {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.idx {
  padding-top: 2px;
  color: #60a5fa;
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.features strong {
  display: block;
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
}

.features p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.6;
}

.intro-foot {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #475569;
  font-size: 11px;
  letter-spacing: 0.4px;
  z-index: 1;
}

.panel {
  flex: 0.85;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  background:
    radial-gradient(70% 50% at 100% 0%, rgba(37, 99, 235, 0.06), transparent 70%),
    #f8fafc;
}

.form-card {
  width: 100%;
  max-width: 392px;
}

.form-head h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.form-head p {
  margin: 8px 0 26px;
  color: var(--g12-text-secondary);
  font-size: 13px;
}

.submit {
  width: 100%;
  margin-top: 4px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.demo {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px dashed #e2e8f0;
}

.demo-title {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.demo-title span {
  color: #94a3b8;
  font-weight: 400;
}

.demo-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.demo-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 9px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.demo-item:hover {
  border-color: #93c5fd;
  box-shadow: 0 6px 16px -10px rgba(37, 99, 235, 0.5);
  transform: translateY(-1px);
}

.demo-item .role {
  color: #64748b;
  font-size: 11px;
}

.demo-item .username {
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.mock-note {
  margin-top: 18px;
  padding: 10px 12px;
  border-radius: 9px;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  color: #3b5bdb;
  font-size: 11.5px;
  line-height: 1.6;
}

.mock-note code {
  font-family: 'SFMono-Regular', Consolas, monospace;
}

@media (max-width: 960px) {
  .intro {
    display: none;
  }
  .panel {
    flex: 1;
  }
}
</style>
