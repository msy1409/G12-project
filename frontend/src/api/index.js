import request from './request'

// 27 个业务接口，路径与 docs/api.md 一一对应；页面里只调这里的方法

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  logout: () => request.post('/auth/logout')
}

export const userApi = {
  list: (params) => request.get('/users', { params }),
  create: (data) => request.post('/users', data),
  update: (id, data) => request.put(`/users/${id}`, data),
  setStatus: (id, status) => request.put(`/users/${id}/status`, { status }),
  remove: (id) => request.delete(`/users/${id}`)
}

export const deptApi = {
  list: (params) => request.get('/departments', { params }),
  create: (data) => request.post('/departments', data),
  update: (id, data) => request.put(`/departments/${id}`, data),
  remove: (id) => request.delete(`/departments/${id}`)
}

export const applyApi = {
  create: (data) => request.post('/applies', data),
  mine: (params) => request.get('/applies/mine', { params }),
  detail: (id) => request.get(`/applies/${id}`),
  cancel: (id, data) => request.put(`/applies/${id}/cancel`, data)
}

export const auditApi = {
  pending: (params) => request.get('/applies/pending', { params }),
  approve: (id, data) => request.put(`/applies/${id}/approve`, data || {}),
  reject: (id, data) => request.put(`/applies/${id}/reject`, data),
  verifyCode: (id) => request.put(`/applies/${id}/verify-code`),
  list: (params) => request.get('/applies', { params })
}

export const recordApi = {
  checkin: (data) => request.post('/records/checkin', data),
  checkout: (data) => request.post('/records/checkout', data),
  list: (params) => request.get('/records', { params })
}

export const statsApi = {
  overview: () => request.get('/stats/overview'),
  trend: (days = 7) => request.get('/stats/trend', { params: { days } }),
  department: () => request.get('/stats/department')
}
