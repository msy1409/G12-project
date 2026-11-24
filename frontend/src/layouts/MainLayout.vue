<template>
  <div class="layout" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">G12</div>
        <div class="brand-text">
          <strong>访客预约平台</strong>
          <span>Enterprise Access</span>
        </div>
      </div>

      <div class="menu-caption">工作台</div>
      <nav class="menu">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: isActive(item) }"
        >
          <span class="menu-ico" v-html="item.icon"></span>
          <span class="menu-label">{{ item.label }}</span>
          <span v-if="item.badge && pendingCount > 0" class="menu-badge">{{ pendingCount }}</span>
        </router-link>
      </nav>

      <div class="sidebar-foot">
        <div class="role-card">
          <span class="role-dot"></span>
          <div>
            <div class="role-name">{{ store.roleLabel }}</div>
            <div class="role-dept">{{ store.department }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <button class="collapse-btn" type="button" :title="collapsed ? '展开菜单' : '收起菜单'" @click="collapsed = !collapsed">
          <span></span><span></span><span></span>
        </button>

        <div class="crumb">
          <span class="crumb-root">{{ store.roleLabel }}工作台</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-current">{{ route.meta.title || '首页' }}</span>
        </div>

        <div class="flex-spacer"></div>

        <el-dropdown trigger="click" @command="onCommand">
          <div class="user-chip">
            <div class="avatar">{{ avatarText }}</div>
            <div class="user-meta">
              <strong>{{ store.realName }}</strong>
              <span>{{ store.roleLabel }} · {{ store.department }}</span>
            </div>
            <span class="caret">▾</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item v-if="isMock" command="reset" divided>重置演示数据</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="content">
        <router-view />
      </main>

      <footer class="footer">G12 · Docker 云容器与管理技术 · 访客预约与到访核验平台</footer>
    </div>

    <el-dialog v-model="profileVisible" title="个人信息" width="440px" align-center>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">{{ store.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ store.user?.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ store.roleLabel }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ store.department }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ store.user?.phone || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="profileVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <ConfirmDialog ref="confirmRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { auditApi } from '@/api'
import { ROLES } from '@/constants'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const collapsed = ref(false)
const profileVisible = ref(false)
const pendingCount = ref(0)
const confirmRef = ref(null)
const isMock = import.meta.env.VITE_USE_MOCK === 'true'

const ICONS = {
  mine: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3.5" y="5" width="17" height="16" rx="3"/><path d="M8 3v4M16 3v4M3.5 10h17"/><path d="M8 14h4M8 17.5h7"/></svg>',
  create: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 5v14M5 12h14"/></svg>',
  audit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3.5h9l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 20V5A1.5 1.5 0 0 1 6.5 3.5Z"/><path d="M14.5 3.5V8H19"/><path d="m8.5 15 2.2 2.2 4-4.4"/></svg>',
  gate: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 21V6.5L12 3l8 3.5V21"/><path d="M9 21v-6h6v6"/><path d="M8 10h.01M16 10h.01"/></svg>',
  stats: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>',
  users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="9" cy="8" r="3.2"/><path d="M3.5 20c.6-3.4 2.8-5.2 5.5-5.2s4.9 1.8 5.5 5.2"/><path d="M16 5.2a3 3 0 0 1 0 5.6M17.5 14.8c1.8.5 2.8 2.2 3.1 5.2"/></svg>',
  depts: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 21V4.5h9V21M13 9.5h7V21M2.5 21h19"/><path d="M7 8h3M7 11.5h3M7 15h3M16 13h1.5M16 16.5h1.5"/></svg>'
}

const menus = computed(() => {
  const role = store.role
  if (role === ROLES.EMPLOYEE) {
    return [
      { path: '/apply/mine', label: '我的预约', icon: ICONS.mine },
      { path: '/apply/new', label: '新建预约', icon: ICONS.create }
    ]
  }
  if (role === ROLES.AUDITOR) {
    return [
      { path: '/audit/list', label: '审核管理', icon: ICONS.audit, badge: true },
      { path: '/gate', label: '门岗核验', icon: ICONS.gate },
      { path: '/stats', label: '数据统计', icon: ICONS.stats }
    ]
  }
  return [
    { path: '/audit/list', label: '预约审核', icon: ICONS.audit, badge: true },
    { path: '/gate', label: '门岗核验', icon: ICONS.gate },
    { path: '/stats', label: '数据统计', icon: ICONS.stats },
    { path: '/admin/users', label: '用户管理', icon: ICONS.users },
    { path: '/admin/depts', label: '部门管理', icon: ICONS.depts }
  ]
})

const avatarText = computed(() => {
  const name = store.realName || ''
  return name.slice(-2) || 'G12'
})

function isActive(item) {
  if (item.path === '/audit/list') return route.path.startsWith('/audit')
  if (item.path === '/apply/new') return route.path === '/apply/new'
  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}

async function loadPendingBadge() {
  if (![ROLES.AUDITOR, ROLES.ADMIN].includes(store.role)) return
  try {
    const data = await auditApi.pending({ page: 1, size: 1 })
    pendingCount.value = data.total || 0
  } catch {
    pendingCount.value = 0
  }
}

async function onCommand(command) {
  if (command === 'profile') {
    profileVisible.value = true
    return
  }
  if (command === 'reset') {
    const confirmed = await confirmRef.value.open({
      title: '重置演示数据',
      message: '会清空你在 mock 模式下新增的预约、用户等数据，并恢复到种子数据。',
      confirmText: '重置并刷新',
      danger: true
    })
    if (!confirmed) return
    const { resetDb } = await import('@/mock/db')
    resetDb()
    window.location.reload()
    return
  }
  if (command === 'logout') {
    const confirmed = await confirmRef.value.open({
      title: '退出登录',
      message: '确定要退出当前账号吗？',
      confirmText: '退出'
    })
    if (!confirmed) return
    await store.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}

onMounted(loadPendingBadge)
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: var(--g12-bg);
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  width: 232px;
  height: 100vh;
  padding: 20px 14px 16px;
  background: radial-gradient(120% 60% at 0% 0%, #1e293b 0%, #111827 55%, #0b1220 100%);
  color: #cbd5e1;
  transition: width 0.2s ease;
  z-index: 20;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 6px 20px;
}

.logo {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 0.4px;
  box-shadow: 0 6px 18px -6px rgba(59, 130, 246, 0.8);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
}

.brand-text strong {
  color: #f8fafc;
  font-size: 14px;
}

.brand-text span {
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.menu-caption {
  padding: 0 12px 8px;
  color: #475569;
  font-size: 11px;
  letter-spacing: 1.2px;
  white-space: nowrap;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 42px;
  padding: 0 12px;
  border-radius: 10px;
  color: #94a3b8;
  font-size: 14px;
  white-space: nowrap;
  transition: background 0.15s ease, color 0.15s ease;
}

.menu-item:hover {
  color: #e2e8f0;
  background: rgba(148, 163, 184, 0.1);
}

.menu-item.active {
  color: #fff;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.95), rgba(79, 70, 229, 0.75));
  box-shadow: 0 8px 20px -12px rgba(37, 99, 235, 0.9);
}

.menu-ico {
  flex: none;
  display: inline-flex;
  width: 18px;
  height: 18px;
}

.menu-ico :deep(svg) {
  width: 18px;
  height: 18px;
}

.menu-label {
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-badge {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 20px;
  text-align: center;
}

.sidebar-foot {
  margin-top: auto;
  padding-top: 16px;
}

.role-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 11px;
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.12);
  white-space: nowrap;
  overflow: hidden;
}

.role-dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.16);
}

.role-name {
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 600;
}

.role-dept {
  color: #64748b;
  font-size: 11px;
  margin-top: 2px;
}

/* 收起状态 */
.collapsed .sidebar {
  width: 78px;
  padding-left: 12px;
  padding-right: 12px;
}

.collapsed .brand-text,
.collapsed .menu-caption,
.collapsed .menu-label,
.collapsed .menu-badge,
.collapsed .role-card > div {
  display: none;
}

.collapsed .brand {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.collapsed .menu-item {
  justify-content: center;
  padding: 0;
}

.collapsed .role-card {
  justify-content: center;
  padding: 12px 0;
}

.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 14px;
  height: 64px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
}

.collapse-btn {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  width: 32px;
  height: 32px;
  padding: 0 7px;
  border: 1px solid var(--g12-border);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.collapse-btn span {
  display: block;
  height: 2px;
  border-radius: 1px;
  background: #64748b;
}

.collapse-btn:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--g12-text-secondary);
}

.crumb-root {
  color: #94a3b8;
}

.crumb-sep {
  color: #cbd5e1;
}

.crumb-current {
  color: #1f2937;
  font-weight: 600;
}

.flex-spacer {
  flex: 1;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 8px 5px 5px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-chip:hover {
  background: #f1f5f9;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #bfdbfe, #c7d2fe);
  color: #1e40af;
  font-size: 12px;
  font-weight: 700;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-meta strong {
  font-size: 13px;
  color: #1f2937;
}

.user-meta span {
  font-size: 11px;
  color: #94a3b8;
}

.caret {
  color: #94a3b8;
  font-size: 11px;
}

.content {
  flex: 1;
  padding: 22px 24px;
}

.footer {
  padding: 8px 24px 18px;
  color: #a3aebf;
  font-size: 11px;
  text-align: center;
}

@media (max-width: 820px) {
  .sidebar {
    width: 78px;
    padding-left: 12px;
    padding-right: 12px;
  }
  .brand-text,
  .menu-caption,
  .menu-label,
  .menu-badge,
  .role-card > div {
    display: none;
  }
  .brand {
    justify-content: center;
  }
  .menu-item {
    justify-content: center;
    padding: 0;
  }
  .user-meta {
    display: none;
  }
  .content {
    padding: 16px;
  }
}
</style>
