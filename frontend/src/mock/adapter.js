/**
 * axios mock 适配器
 * 按 docs/api.md 的 27 个接口实现，返回与真实后端一致的
 * Result{code,message,data} 结构和 HTTP 状态，拦截器逻辑不用改。
 */
import {
  loadDb,
  saveDb,
  fmt,
  parseTime,
  nowStr,
  dateOf,
  todayStr,
  monthStr,
  minutesBetween,
  at
} from './db'

const ROLE = { EMPLOYEE: 'EMPLOYEE', AUDITOR: 'AUDITOR', ADMIN: 'ADMIN' }
const STATUS_ALLOWED = { 0: [1, 9], 1: [2, 9], 2: [3, 9], 3: [4] }
const GATES = ['东门-01', '东门-02', '南门-01', '南门-02']

let db = null
function getDb() {
  if (!db) db = loadDb()
  return db
}

function okay(data) {
  return { status: 200, body: { code: 200, message: 'ok', data } }
}

function bad(status, message) {
  return { status, body: { code: status, message, data: null } }
}

function needRole(user, roles) {
  return roles.includes(user.role) ? null : bad(403, 'FORBIDDEN')
}

function needDept(user, apply) {
  if (user.role === ROLE.AUDITOR && apply.deptId !== user.deptId) {
    return bad(403, 'DEPT_SCOPE_DENIED')
  }
  return null
}

function deptOf(d, id) {
  return d.departments.find((x) => x.id === id)
}

function deptName(d, id) {
  return deptOf(d, id)?.deptName || '—'
}

function serializeUser(d, u) {
  return {
    id: u.id,
    username: u.username,
    realName: u.realName,
    phone: u.phone,
    deptId: u.deptId,
    deptName: deptName(d, u.deptId),
    role: u.role,
    status: u.status,
    createTime: u.createTime
  }
}

function applySummary(d, a) {
  return {
    id: a.id,
    applyNo: a.applyNo,
    visitorName: a.visitorName,
    visitorPhone: a.visitorPhone,
    visitorCompany: a.visitorCompany,
    deptId: a.deptId,
    deptName: deptName(d, a.deptId),
    hostName: a.hostName,
    visitReason: a.visitReason,
    visitTime: a.visitTime,
    leaveTime: a.leaveTime,
    status: a.status,
    verifyCode: a.verifyCode,
    createBy: a.createBy,
    createTime: a.createTime
  }
}

function applyDetail(d, a) {
  const detail = applySummary(d, a)
  // 契约：仅状态 >= 2 返回核验码
  detail.verifyCode = a.status >= 2 && a.status !== 9 ? a.verifyCode : null
  detail.processList = d.processes
    .filter((p) => p.applyId === a.id)
    .sort((x, y) => x.id - y.id)
    .map((p) => ({
      action: p.action,
      operatorName: p.operatorName,
      operatorRole: p.operatorRole,
      remark: p.remark,
      createTime: p.createTime
    }))
  detail.statusLogList = d.logs
    .filter((l) => l.applyId === a.id)
    .sort((x, y) => x.id - y.id)
    .map((l) => ({
      fromStatus: l.fromStatus,
      toStatus: l.toStatus,
      action: l.action,
      operatorName: l.operatorName,
      remark: l.remark,
      createTime: l.createTime
    }))
  return detail
}

function paginate(list, params) {
  const page = Math.max(1, Number(params.page) || 1)
  const size = Math.min(100, Math.max(1, Number(params.size) || 10))
  return {
    list: list.slice((page - 1) * size, page * size),
    total: list.length,
    page,
    size
  }
}

function keywordHit(keyword, values) {
  if (!keyword) return true
  const k = String(keyword).trim().toLowerCase()
  if (!k) return true
  return values.some((v) => String(v || '').toLowerCase().includes(k))
}

function byCreateTimeDesc(a, b) {
  return parseTime(b.createTime) - parseTime(a.createTime) || b.id - a.id
}

function transit(d, apply, to, action, user, remark) {
  const from = apply.status
  if (!STATUS_ALLOWED[from] || !STATUS_ALLOWED[from].includes(to)) {
    return bad(400, 'STATE_NOT_ALLOWED')
  }
  const time = nowStr()
  apply.status = to
  d.logs.push({
    id: d.seq.log++,
    applyId: apply.id,
    fromStatus: from,
    toStatus: to,
    action,
    operatorId: user.id,
    operatorName: user.realName,
    remark: remark || null,
    createTime: time
  })
  d.processes.push({
    id: d.seq.process++,
    applyId: apply.id,
    action,
    operatorId: user.id,
    operatorName: user.realName,
    operatorRole: user.role,
    remark: remark || null,
    createTime: time
  })
  saveDb(d)
  return null
}

function uniqueVerifyCode(d) {
  for (let i = 0; i < 20; i++) {
    const code = String(Math.floor(100000 + Math.random() * 900000))
    if (!d.applies.some((a) => a.verifyCode === code)) return code
  }
  return String(Date.now()).slice(-6)
}

function scopeApplies(d, user) {
  if (user.role === ROLE.AUDITOR) return d.applies.filter((a) => a.deptId === user.deptId)
  return d.applies
}

function scopeRecords(d, user) {
  if (user.role !== ROLE.AUDITOR) return d.records
  const ids = new Set(d.applies.filter((a) => a.deptId === user.deptId).map((a) => a.id))
  return d.records.filter((r) => ids.has(r.applyId))
}

/* --------------------------- 认证 --------------------------- */

function login(d, body) {
  const username = String(body.username || '').trim()
  const password = String(body.password || '')
  const user = d.users.find((u) => u.username === username)
  if (!user || user.password !== password) return bad(401, 'INVALID_CREDENTIALS')
  if (user.status === 0) return bad(401, 'USER_DISABLED')
  return okay({
    token: `mock-token-${user.id}-${Date.now()}`,
    user: {
      id: user.id,
      username: user.username,
      realName: user.realName,
      role: user.role,
      deptId: user.deptId,
      deptName: deptName(d, user.deptId)
    }
  })
}

/* --------------------------- 用户 --------------------------- */

function listUsers(d, params) {
  const list = d.users
    .filter((u) => keywordHit(params.keyword, [u.username, u.realName, u.phone]))
    .filter((u) => (params.deptId ? u.deptId === Number(params.deptId) : true))
    .filter((u) => (params.role ? u.role === params.role : true))
    .filter((u) => (params.status !== undefined && params.status !== '' ? u.status === Number(params.status) : true))
    .sort((a, b) => a.id - b.id)
    .map((u) => serializeUser(d, u))
  const page = paginate(list, params)
  return okay(page)
}

function createUser(d, body, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const username = String(body.username || '').trim()
  const password = String(body.password || '')
  const realName = String(body.realName || '').trim()
  const role = body.role
  if (!username || !password || !realName) return bad(400, '用户名、密码、姓名不能为空')
  if (!Object.values(ROLE).includes(role)) return bad(400, '角色不合法')
  if (d.users.some((u) => u.username === username)) return bad(409, 'USERNAME_EXISTS')
  const id = d.seq.user++
  d.users.push({
    id,
    username,
    password,
    realName,
    phone: body.phone || '',
    deptId: Number(body.deptId) || user.deptId,
    role,
    status: 1,
    createTime: nowStr()
  })
  saveDb(d)
  return okay({ id })
}

function updateUser(d, id, body, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const target = d.users.find((u) => u.id === id)
  if (!target) return bad(404, 'NOT_FOUND')
  if (body.role !== undefined && !Object.values(ROLE).includes(body.role)) return bad(400, '角色不合法')
  Object.assign(target, {
    realName: body.realName ?? target.realName,
    phone: body.phone ?? target.phone,
    deptId: body.deptId ? Number(body.deptId) : target.deptId,
    role: body.role ?? target.role
  })
  saveDb(d)
  return okay(null)
}

function setUserStatus(d, id, body, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const target = d.users.find((u) => u.id === id)
  if (!target) return bad(404, 'NOT_FOUND')
  const status = Number(body.status)
  if (![0, 1].includes(status)) return bad(400, '状态不合法')
  if (target.id === user.id && status === 0) return bad(400, 'STATE_NOT_ALLOWED')
  target.status = status
  saveDb(d)
  return okay(null)
}

function removeUser(d, id, user) {
  return setUserStatus(d, id, { status: 0 }, user)
}

/* --------------------------- 部门 --------------------------- */

function listDepartments(d, params) {
  const list = d.departments
    .filter((x) => (params.status !== undefined && params.status !== '' ? x.status === Number(params.status) : true))
    .map((x) => ({
      id: x.id,
      deptCode: x.deptCode,
      deptName: x.deptName,
      managerName: x.managerName,
      phone: x.phone,
      status: x.status,
      createTime: x.createTime
    }))
  return okay(list)
}

function createDept(d, body, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const deptCode = String(body.deptCode || '').trim()
  const deptNameValue = String(body.deptName || '').trim()
  if (!deptCode || !deptNameValue) return bad(400, '部门编码和名称不能为空')
  if (d.departments.some((x) => x.deptCode === deptCode || x.deptName === deptNameValue)) {
    return bad(409, 'DEPT_NAME_EXISTS')
  }
  const id = d.seq.dept++
  d.departments.push({
    id,
    deptCode,
    deptName: deptNameValue,
    managerName: body.managerName || '',
    phone: body.phone || '',
    status: 1,
    createTime: nowStr()
  })
  saveDb(d)
  return okay({ id })
}

function updateDept(d, id, body, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const target = d.departments.find((x) => x.id === id)
  if (!target) return bad(404, 'NOT_FOUND')
  const nextName = String(body.deptName || '').trim()
  if (nextName && d.departments.some((x) => x.id !== id && x.deptName === nextName)) {
    return bad(409, 'DEPT_NAME_EXISTS')
  }
  target.deptName = nextName || target.deptName
  target.managerName = body.managerName ?? target.managerName
  target.phone = body.phone ?? target.phone
  saveDb(d)
  return okay(null)
}

function removeDept(d, id, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const target = d.departments.find((x) => x.id === id)
  if (!target) return bad(404, 'NOT_FOUND')
  const hasUser = d.users.some((u) => u.deptId === id && u.status === 1)
  const hasOpenApply = d.applies.some((a) => a.deptId === id && [0, 1, 2, 3].includes(a.status))
  if (hasUser || hasOpenApply) return bad(400, 'DEPT_IN_USE')
  target.status = 0
  saveDb(d)
  return okay(null)
}

/* --------------------------- 预约 --------------------------- */

function createApply(d, body, user) {
  const deny = needRole(user, [ROLE.EMPLOYEE, ROLE.ADMIN])
  if (deny) return deny
  const required = ['visitorName', 'visitorPhone', 'visitTime', 'hostName']
  if (required.some((k) => !String(body[k] || '').trim())) {
    return bad(400, '访客姓名、手机、到访时间、被访人不能为空')
  }
  const visit = parseTime(body.visitTime)
  if (!visit || visit.getTime() < Date.now() - 60000) {
    return bad(400, '到访时间不能早于当前时间')
  }
  const leave = body.leaveTime ? parseTime(body.leaveTime) : new Date(visit.getTime() + 2 * 3600000)
  if (leave && leave.getTime() <= visit.getTime()) {
    return bad(400, '离开时间必须晚于到访时间')
  }
  const now = new Date()
  const id = d.seq.apply++
  const apply = {
    id,
    applyNo: `AP${fmt(now).slice(0, 10).replace(/-/g, '')}${String(id).padStart(6, '0')}`,
    visitorName: String(body.visitorName).trim(),
    visitorPhone: String(body.visitorPhone).trim(),
    visitorCompany: String(body.visitorCompany || '').trim(),
    deptId: Number(body.deptId) || user.deptId,
    hostName: String(body.hostName).trim(),
    visitReason: String(body.visitReason || '').trim(),
    visitTime: fmt(visit),
    leaveTime: fmt(leave),
    verifyCode: null,
    status: 0,
    createBy: user.id,
    createTime: nowStr()
  }
  d.applies.push(apply)
  d.logs.push({
    id: d.seq.log++,
    applyId: id,
    fromStatus: -1,
    toStatus: 0,
    action: 'CREATE',
    operatorId: user.id,
    operatorName: user.realName,
    remark: '提交预约',
    createTime: apply.createTime
  })
  d.processes.push({
    id: d.seq.process++,
    applyId: id,
    action: 'CREATE',
    operatorId: user.id,
    operatorName: user.realName,
    operatorRole: user.role,
    remark: '提交预约',
    createTime: apply.createTime
  })
  saveDb(d)
  return okay({ id, applyNo: apply.applyNo, status: 0 })
}

function listMine(d, params, user) {
  const deny = needRole(user, [ROLE.EMPLOYEE, ROLE.ADMIN])
  if (deny) return deny
  const list = d.applies
    .filter((a) => a.createBy === user.id)
    .filter((a) => (params.status !== undefined && params.status !== '' ? a.status === Number(params.status) : true))
    .sort(byCreateTimeDesc)
    .map((a) => {
      const s = applySummary(d, a)
      return {
        id: s.id,
        applyNo: s.applyNo,
        visitorName: s.visitorName,
        visitorPhone: s.visitorPhone,
        deptName: s.deptName,
        hostName: s.hostName,
        visitTime: s.visitTime,
        status: s.status,
        createTime: s.createTime
      }
    })
  return okay(paginate(list, params))
}

function viewApply(d, id, user) {
  const apply = d.applies.find((a) => a.id === id)
  if (!apply) return bad(404, 'NOT_FOUND')
  if (user.role === ROLE.EMPLOYEE && apply.createBy !== user.id) return bad(403, 'FORBIDDEN')
  if (user.role === ROLE.AUDITOR && apply.deptId !== user.deptId) return bad(403, 'DEPT_SCOPE_DENIED')
  return okay(applyDetail(d, apply))
}

function cancelApply(d, id, body, user) {
  const deny = needRole(user, [ROLE.EMPLOYEE, ROLE.ADMIN])
  if (deny) return deny
  const apply = d.applies.find((a) => a.id === id)
  if (!apply) return bad(404, 'NOT_FOUND')
  if (user.role === ROLE.EMPLOYEE && apply.createBy !== user.id) return bad(403, 'FORBIDDEN')
  if (![0, 1, 2].includes(apply.status)) return bad(400, 'STATE_NOT_ALLOWED')
  const err = transit(d, apply, 9, 'CANCEL', user, body.reason || '取消预约')
  if (err) return err
  return okay({ status: 9 })
}

/* --------------------------- 审核 --------------------------- */

function listPending(d, params, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const list = d.applies
    .filter((a) => a.status === 0)
    .filter((a) => (user.role === ROLE.AUDITOR ? a.deptId === user.deptId : true))
    .filter((a) => keywordHit(params.keyword, [a.visitorName, a.applyNo, a.visitorCompany]))
    .sort(byCreateTimeDesc)
    .map((a) => {
      const s = applySummary(d, a)
      return {
        id: s.id,
        applyNo: s.applyNo,
        visitorName: s.visitorName,
        visitorCompany: s.visitorCompany,
        deptName: s.deptName,
        hostName: s.hostName,
        visitReason: s.visitReason,
        visitTime: s.visitTime,
        createTime: s.createTime
      }
    })
  return okay(paginate(list, params))
}

function listAuditApplies(d, params, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const list = d.applies
    .filter((a) => (user.role === ROLE.AUDITOR ? a.deptId === user.deptId : true))
    .filter((a) => (params.status !== undefined && params.status !== '' ? a.status === Number(params.status) : true))
    .filter((a) => (params.deptId && user.role === ROLE.ADMIN ? a.deptId === Number(params.deptId) : true))
    .filter((a) => keywordHit(params.keyword, [a.visitorName, a.visitorPhone, a.applyNo, a.visitorCompany]))
    .filter((a) => (params.startDate ? dateOf(a.createTime) >= params.startDate : true))
    .filter((a) => (params.endDate ? dateOf(a.createTime) <= params.endDate : true))
    .sort(byCreateTimeDesc)
    .map((a) => {
      const s = applySummary(d, a)
      return {
        id: s.id,
        applyNo: s.applyNo,
        visitorName: s.visitorName,
        visitorPhone: s.visitorPhone,
        visitorCompany: s.visitorCompany,
        deptName: s.deptName,
        hostName: s.hostName,
        visitReason: s.visitReason,
        visitTime: s.visitTime,
        status: s.status,
        verifyCode: a.status >= 2 && a.status !== 9 ? s.verifyCode : null,
        createTime: s.createTime
      }
    })
  return okay(paginate(list, params))
}

function approveApply(d, id, body, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const apply = d.applies.find((a) => a.id === id)
  if (!apply) return bad(404, 'NOT_FOUND')
  const scope = needDept(user, apply)
  if (scope) return scope
  if (apply.status !== 0) return bad(400, 'STATE_NOT_ALLOWED')
  const err = transit(d, apply, 1, 'APPROVE', user, body.remark || '同意来访')
  if (err) return err
  return okay({ status: 1 })
}

function rejectApply(d, id, body, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const apply = d.applies.find((a) => a.id === id)
  if (!apply) return bad(404, 'NOT_FOUND')
  const scope = needDept(user, apply)
  if (scope) return scope
  const reason = String(body.reason || '').trim()
  if (!reason) return bad(400, '驳回原因必填')
  if (reason.length > 255) return bad(400, '驳回原因不能超过 255 字')
  if (apply.status !== 0) return bad(400, 'STATE_NOT_ALLOWED')
  const err = transit(d, apply, 9, 'REJECT', user, reason)
  if (err) return err
  return okay({ status: 9 })
}

function generateVerifyCode(d, id, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const apply = d.applies.find((a) => a.id === id)
  if (!apply) return bad(404, 'NOT_FOUND')
  const scope = needDept(user, apply)
  if (scope) return scope
  if (apply.status !== 1) return bad(400, 'STATE_NOT_ALLOWED')
  const code = uniqueVerifyCode(d)
  apply.verifyCode = code
  const err = transit(d, apply, 2, 'VERIFY_CODE', user, `生成核验码 ${code}`)
  if (err) return err
  saveDb(d)
  return okay({ status: 2, verifyCode: code })
}

/* --------------------------- 门岗 --------------------------- */

function checkin(d, body, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const code = String(body.verifyCode || '').trim()
  if (!/^\d{6}$/.test(code)) return bad(400, 'VERIFY_CODE_INVALID')
  const apply = d.applies.find((a) => a.verifyCode === code)
  if (!apply) return bad(400, 'VERIFY_CODE_INVALID')
  if (d.records.some((r) => r.applyId === apply.id)) return bad(409, 'DUPLICATE_CHECKIN')
  if (apply.status !== 2) return bad(400, 'STATE_NOT_ALLOWED')
  const scope = needDept(user, apply)
  if (scope) return scope
  const gate = body.gate || GATES[0]
  const checkinTime = nowStr()
  const recordId = d.seq.record++
  d.records.push({
    id: recordId,
    applyId: apply.id,
    checkinTime,
    checkoutTime: null,
    checkinBy: user.id,
    checkoutBy: null,
    gate,
    createTime: checkinTime
  })
  const err = transit(d, apply, 3, 'CHECKIN', user, `${gate} 入场`)
  if (err) return err
  return okay({
    recordId,
    applyId: apply.id,
    visitorName: apply.visitorName,
    checkinTime,
    status: 3
  })
}

function checkout(d, body, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const applyId = Number(body.applyId)
  const apply = d.applies.find((a) => a.id === applyId)
  if (!apply) return bad(404, 'NOT_FOUND')
  const scope = needDept(user, apply)
  if (scope) return scope
  if (apply.status !== 3) {
    return bad(400, [0, 1, 2].includes(apply.status) ? 'NOT_CHECKED_IN' : 'STATE_NOT_ALLOWED')
  }
  const record = d.records.find((r) => r.applyId === apply.id && !r.checkoutTime)
  if (!record) return bad(400, 'NOT_CHECKED_IN')
  const gate = body.gate || record.gate
  const checkoutTime = nowStr()
  record.checkoutTime = checkoutTime
  record.checkoutBy = user.id
  record.gate = gate
  const stayMinutes = minutesBetween(record.checkinTime, checkoutTime)
  const err = transit(d, apply, 4, 'CHECKOUT', user, `${gate} 离场`)
  if (err) return err
  return okay({ applyId: apply.id, checkoutTime, stayMinutes, status: 4 })
}

function listRecords(d, params, user) {
  const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
  if (deny) return deny
  const list = scopeRecords(d, user)
    .map((r) => {
      const apply = d.applies.find((a) => a.id === r.applyId)
      return { r, apply }
    })
    .filter(({ apply }) => (params.deptId && user.role === ROLE.ADMIN ? apply?.deptId === Number(params.deptId) : true))
    .filter(({ r }) => (params.date ? dateOf(r.checkinTime) === params.date : true))
    .filter(({ r, apply }) => keywordHit(params.keyword, [apply?.visitorName, apply?.applyNo, r.gate]))
    .sort((x, y) => parseTime(y.r.checkinTime) - parseTime(x.r.checkinTime))
    .map(({ r, apply }) => ({
      id: r.id,
      applyId: r.applyId,
      applyNo: apply?.applyNo || '—',
      visitorName: apply?.visitorName || '—',
      deptName: apply ? deptName(d, apply.deptId) : '—',
      gate: r.gate,
      checkinTime: r.checkinTime,
      checkoutTime: r.checkoutTime,
      stayMinutes: r.checkoutTime ? minutesBetween(r.checkinTime, r.checkoutTime) : null,
      status: apply?.status
    }))
  return okay(paginate(list, params))
}

/* --------------------------- 统计 --------------------------- */

function overview(d, user) {
  const applies = scopeApplies(d, user)
  const records = scopeRecords(d, user)
  const today = todayStr()
  const month = monthStr()
  const closed = records.filter((r) => r.checkinTime && r.checkoutTime)
  const avg = closed.length
    ? Math.round((closed.reduce((sum, r) => sum + minutesBetween(r.checkinTime, r.checkoutTime), 0) / closed.length) * 10) / 10
    : 0
  return okay({
    todayApplyCount: applies.filter((a) => dateOf(a.createTime) === today).length,
    pendingCount: applies.filter((a) => a.status === 0).length,
    todayCheckinCount: records.filter((r) => dateOf(r.checkinTime) === today).length,
    monthCheckinCount: records.filter((r) => dateOf(r.checkinTime).slice(0, 7) === month).length,
    avgStayMinutes: avg
  })
}

function trend(d, params, user) {
  const days = Math.min(30, Math.max(1, Number(params.days) || 7))
  const applies = scopeApplies(d, user)
  const records = scopeRecords(d, user)
  const result = []
  for (let i = days - 1; i >= 0; i--) {
    const date = fmt(at(-i, 0, 0)).slice(0, 10)
    result.push({
      date,
      applyCount: applies.filter((a) => dateOf(a.createTime) === date).length,
      checkinCount: records.filter((r) => dateOf(r.checkinTime) === date).length
    })
  }
  return okay(result)
}

function departmentStats(d, user) {
  const deny = needRole(user, [ROLE.ADMIN])
  if (deny) return deny
  const result = d.departments
    .filter((dept) => dept.status === 1)
    .map((dept) => {
      const applies = d.applies.filter((a) => a.deptId === dept.id)
      const ids = new Set(applies.map((a) => a.id))
      return {
        deptId: dept.id,
        deptName: dept.deptName,
        applyCount: applies.length,
        checkinCount: d.records.filter((r) => ids.has(r.applyId)).length
      }
    })
    .filter((x) => x.applyCount > 0 || x.checkinCount > 0)
  return okay(result)
}

/* --------------------------- 分发 --------------------------- */

function parseRequest(config) {
  let url = config.url || '/'
  const params = { ...(config.params || {}) }
  const qi = url.indexOf('?')
  if (qi >= 0) {
    new URLSearchParams(url.slice(qi + 1)).forEach((v, k) => {
      if (params[k] === undefined) params[k] = v
    })
    url = url.slice(0, qi)
  }
  url = url.replace(/^https?:\/\/[^/]+/, '')
  url = url.replace(/^\/api(?=\/|$)/, '')
  if (!url.startsWith('/')) url = '/' + url
  return { path: url, params }
}

function bodyOf(config) {
  if (!config.data) return {}
  if (typeof config.data === 'string') {
    try {
      return JSON.parse(config.data)
    } catch {
      return {}
    }
  }
  return config.data
}

function currentUser(d, config) {
  const headers = config.headers || {}
  let auth = ''
  if (typeof headers.get === 'function') auth = headers.get('Authorization') || ''
  else auth = headers.Authorization || headers.authorization || ''
  const match = /^Bearer\s+mock-token-(\d+)-/.exec(String(auth))
  if (!match) return null
  return d.users.find((u) => u.id === Number(match[1])) || null
}

function dispatch(config) {
  const d = getDb()
  const method = String(config.method || 'get').toUpperCase()
  const { path, params } = parseRequest(config)
  const body = bodyOf(config)

  // 公开接口
  if (method === 'POST' && path === '/auth/login') return login(d, body)
  if (method === 'GET' && path === '/actuator/health') return okay({ status: 'UP' })

  const user = currentUser(d, config)
  if (!user) return bad(401, 'TOKEN_EXPIRED')
  if (user.status === 0) return bad(401, 'USER_DISABLED')

  /* auth */
  if (method === 'GET' && path === '/auth/me') return okay(serializeUser(d, user))
  if (method === 'POST' && path === '/auth/logout') return okay(null)

  /* users */
  if (path === '/users' && method === 'GET') {
    const deny = needRole(user, [ROLE.ADMIN])
    return deny || listUsers(d, params)
  }
  if (path === '/users' && method === 'POST') return createUser(d, body, user)
  let m = /^\/users\/(\d+)$/.exec(path)
  if (m && method === 'PUT') return updateUser(d, Number(m[1]), body, user)
  if (m && method === 'DELETE') return removeUser(d, Number(m[1]), user)
  m = /^\/users\/(\d+)\/status$/.exec(path)
  if (m && method === 'PUT') return setUserStatus(d, Number(m[1]), body, user)

  /* departments */
  if (path === '/departments' && method === 'GET') return listDepartments(d, params)
  if (path === '/departments' && method === 'POST') return createDept(d, body, user)
  m = /^\/departments\/(\d+)$/.exec(path)
  if (m && method === 'PUT') return updateDept(d, Number(m[1]), body, user)
  if (m && method === 'DELETE') return removeDept(d, Number(m[1]), user)

  /* applies */
  if (path === '/applies/mine' && method === 'GET') return listMine(d, params, user)
  if (path === '/applies/pending' && method === 'GET') return listPending(d, params, user)
  if (path === '/applies' && method === 'POST') return createApply(d, body, user)
  if (path === '/applies' && method === 'GET') return listAuditApplies(d, params, user)
  m = /^\/applies\/(\d+)$/.exec(path)
  if (m && method === 'GET') return viewApply(d, Number(m[1]), user)
  m = /^\/applies\/(\d+)\/(cancel|approve|reject|verify-code)$/.exec(path)
  if (m && method === 'PUT') {
    const id = Number(m[1])
    if (m[2] === 'cancel') return cancelApply(d, id, body, user)
    if (m[2] === 'approve') return approveApply(d, id, body, user)
    if (m[2] === 'reject') return rejectApply(d, id, body, user)
    return generateVerifyCode(d, id, user)
  }

  /* records */
  if (path === '/records/checkin' && method === 'POST') return checkin(d, body, user)
  if (path === '/records/checkout' && method === 'POST') return checkout(d, body, user)
  if (path === '/records' && method === 'GET') return listRecords(d, params, user)

  /* stats */
  if (path === '/stats/overview' && method === 'GET') {
    const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
    return deny || overview(d, user)
  }
  if (path === '/stats/trend' && method === 'GET') {
    const deny = needRole(user, [ROLE.AUDITOR, ROLE.ADMIN])
    return deny || trend(d, params, user)
  }
  if (path === '/stats/department' && method === 'GET') return departmentStats(d, user)

  return bad(404, 'NOT_FOUND')
}

export default function mockAdapter(config) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      let result
      try {
        result = dispatch(config)
      } catch (e) {
        console.error('[mock] 未捕获异常', e)
        result = bad(500, '系统错误')
      }

      if (result.status >= 200 && result.status < 300) {
        resolve({
          data: result.body,
          status: result.status,
          statusText: 'OK',
          headers: { 'content-type': 'application/json' },
          config,
          request: {}
        })
        return
      }

      const error = new Error(result.body.message || 'Request failed')
      error.isAxiosError = true
      error.config = config
      error.response = {
        data: result.body,
        status: result.status,
        statusText: '',
        headers: {},
        config,
        request: {}
      }
      reject(error)
    }, 80)
  })
}
