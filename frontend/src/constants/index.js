// 契约常量：状态、角色、动作 —— 与 docs/api.md、requirements.md 保持一致

export const ROLES = {
  EMPLOYEE: 'EMPLOYEE',
  AUDITOR: 'AUDITOR',
  ADMIN: 'ADMIN'
}

export const ROLE_LABEL = {
  EMPLOYEE: '员工',
  AUDITOR: '审核员',
  ADMIN: '管理员'
}

export const ROLE_TAG_TYPE = {
  EMPLOYEE: 'info',
  AUDITOR: 'warning',
  ADMIN: 'danger'
}

// 0 待审核 / 1 审核通过 / 2 待到访 / 3 已到访 / 4 已离开 / 9 驳回或取消
export const STATUS = {
  0: { label: '待审核', type: 'warning', color: '#d97706', bg: '#fef3c7' },
  1: { label: '审核通过', type: 'primary', color: '#2563eb', bg: '#dbeafe' },
  2: { label: '待到访', type: 'success', color: '#059669', bg: '#d1fae5' },
  3: { label: '已到访', type: 'warning', color: '#b45309', bg: '#fde68a' },
  4: { label: '已离开', type: 'info', color: '#475569', bg: '#e2e8f0' },
  9: { label: '驳回/取消', type: 'danger', color: '#dc2626', bg: '#fee2e2' }
}

export const STATUS_OPTIONS = Object.entries(STATUS).map(([value, meta]) => ({
  value: Number(value),
  label: meta.label
}))

export const ACTION_LABEL = {
  CREATE: '提交预约',
  APPROVE: '审核通过',
  REJECT: '驳回',
  VERIFY_CODE: '生成核验码',
  CHECKIN: '入场核验',
  CHECKOUT: '离场登记',
  CANCEL: '取消预约'
}

export const GATE_OPTIONS = ['东门-01', '东门-02', '南门-01', '南门-02', '北门-01']

export const PAGE_SIZES = [10, 20, 50]

// 后端业务码 → 中文提示
export const BIZ_MESSAGE = {
  STATE_NOT_ALLOWED: '当前状态不允许该操作',
  INVALID_CREDENTIALS: '用户名或密码错误',
  USER_DISABLED: '账号已停用，请联系管理员',
  TOKEN_EXPIRED: '登录已过期，请重新登录',
  FORBIDDEN: '没有权限执行该操作',
  DEPT_SCOPE_DENIED: '只能操作本部门的数据',
  NOT_FOUND: '数据不存在或已被删除',
  USERNAME_EXISTS: '用户名已存在',
  DEPT_NAME_EXISTS: '部门编码或名称已存在',
  DEPT_IN_USE: '该部门仍在使用中，不能停用',
  DUPLICATE_CHECKIN: '该预约已入场，请勿重复登记',
  NOT_CHECKED_IN: '该访客尚未入场，不能登记离场',
  VERIFY_CODE_INVALID: '核验码错误或已使用'
}

export function bizMessage(code, fallback) {
  if (!code) return fallback || '请求失败'
  if (BIZ_MESSAGE[code]) return BIZ_MESSAGE[code]
  if (/[\u4e00-\u9fa5]/.test(code)) return code
  return fallback || '请求失败'
}

// 各角色登录后的默认首页
export function homeForRole(role) {
  if (role === ROLES.ADMIN) return '/stats'
  if (role === ROLES.AUDITOR) return '/audit/list'
  return '/apply/mine'
}
