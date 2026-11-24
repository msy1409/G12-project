import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { homeForRole } from '@/constants'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: 'apply/new',
        name: 'ApplyNew',
        component: () => import('@/views/ApplyNew.vue'),
        meta: { roles: ['EMPLOYEE', 'ADMIN'], title: '新建预约' }
      },
      {
        path: 'apply/mine',
        name: 'MyApplies',
        component: () => import('@/views/MyApplies.vue'),
        meta: { roles: ['EMPLOYEE'], title: '我的预约' }
      },
      {
        path: 'audit/list',
        name: 'AuditList',
        component: () => import('@/views/AuditList.vue'),
        meta: { roles: ['AUDITOR', 'ADMIN'], title: '审核管理' }
      },
      {
        path: 'audit/:id',
        name: 'AuditDetail',
        component: () => import('@/views/AuditDetail.vue'),
        meta: { roles: ['AUDITOR', 'ADMIN'], title: '预约详情' }
      },
      {
        path: 'gate',
        name: 'GateCheck',
        component: () => import('@/views/GateCheck.vue'),
        meta: { roles: ['AUDITOR', 'ADMIN'], title: '门岗核验' }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/AdminUsers.vue'),
        meta: { roles: ['ADMIN'], title: '用户管理' }
      },
      {
        path: 'admin/depts',
        name: 'AdminDepts',
        component: () => import('@/views/AdminDepts.vue'),
        meta: { roles: ['ADMIN'], title: '部门管理' }
      },
      {
        path: 'stats',
        name: 'StatsDashboard',
        component: () => import('@/views/StatsDashboard.vue'),
        meta: { roles: ['AUDITOR', 'ADMIN'], title: '数据统计' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权访问' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  const token = localStorage.getItem('g12.token')

  if (to.meta.public) {
    if (to.path === '/login' && token && store.user) {
      return homeForRole(store.user.role)
    }
    return true
  }

  if (!token) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }

  if (!store.user) {
    try {
      await store.fetchMe()
    } catch {
      store.clearSession()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.path === '/') {
    return homeForRole(store.user.role)
  }

  const roles = to.meta.roles
  if (roles && roles.length && !roles.includes(store.user.role)) {
    return '/403'
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · G12 访客预约平台` : 'G12 访客预约平台'
})

export default router
