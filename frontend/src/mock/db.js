/**
 * mock 数据库：按 database/seed.sql 的口径生成假数据
 * 只用于 VITE_USE_MOCK=true 的开发/演示，不参与生产构建逻辑
 * 数据存 localStorage，刷新不丢；页面上可“重置演示数据”
 */
const DB_KEY = 'g12.mock.db.v1'
const DB_VERSION = 1

export const pad = (n) => String(n).padStart(2, '0')

export function fmt(date) {
  const d = date instanceof Date ? date : new Date(date)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function parseTime(value) {
  if (!value) return null
  if (value instanceof Date) return new Date(value.getTime())
  return new Date(String(value).replace(/-/g, '/'))
}

export function timeValue(value) {
  const d = parseTime(value)
  return d ? d.getTime() : 0
}

export function at(dayOffset, hour = 9, minute = 0) {
  const d = new Date()
  d.setDate(d.getDate() + dayOffset)
  d.setHours(hour, minute, 0, 0)
  return d
}

export function addMinutes(date, minutes) {
  return new Date(date.getTime() + minutes * 60000)
}

export function nowStr() {
  return fmt(new Date())
}

export function dateOf(value) {
  return String(value || '').slice(0, 10)
}

export function todayStr() {
  return dateOf(nowStr())
}

export function monthStr() {
  return todayStr().slice(0, 7)
}

export function minutesBetween(start, end) {
  const a = parseTime(start)
  const b = parseTime(end)
  if (!a || !b) return 0
  return Math.max(0, Math.round((b.getTime() - a.getTime()) / 60000))
}

function ymd(date) {
  const d = parseTime(date)
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}`
}

/* ------------------------------ 种子 ------------------------------ */

const DEPARTMENTS = [
  [1, 'D001', '行政部', '张伟', '010-80000001'],
  [2, 'D002', '人力资源部', '李娜', '010-80000002'],
  [3, 'D003', '财务部', '王强', '010-80000003'],
  [4, 'D004', '技术研发部', '刘洋', '010-80000004'],
  [5, 'D005', '市场部', '陈静', '010-80000005'],
  [6, 'D006', '销售部', '赵磊', '010-80000006'],
  [7, 'D007', '法务部', '孙丽', '010-80000007'],
  [8, 'D008', '采购部', '周涛', '010-80000008'],
  [9, 'D009', '客户服务部', '吴敏', '010-80000009'],
  [10, 'D010', '安保部', '郑军', '010-80000010']
]

const USERS = [
  [1, 'admin', '系统管理员', '13900000001', 1, 'ADMIN'],
  [2, 'auditor01', '刘洋', '13900000002', 4, 'AUDITOR'],
  [3, 'auditor02', '陈静', '13900000003', 5, 'AUDITOR'],
  [4, 'auditor03', '赵磊', '13900000004', 6, 'AUDITOR'],
  [5, 'emp01', '王小明', '13900000005', 4, 'EMPLOYEE'],
  [6, 'emp02', '李小红', '13900000006', 4, 'EMPLOYEE'],
  [7, 'emp03', '张大力', '13900000007', 5, 'EMPLOYEE'],
  [8, 'emp04', '赵小美', '13900000008', 5, 'EMPLOYEE'],
  [9, 'emp05', '陈小刚', '13900000009', 6, 'EMPLOYEE'],
  [10, 'emp06', '林小雨', '13900000010', 2, 'EMPLOYEE'],
  [11, 'emp07', '黄小峰', '13900000011', 3, 'EMPLOYEE'],
  [12, 'emp08', '周小云', '13900000012', 1, 'EMPLOYEE']
]

// day / created 都相对“今天”，保证打开页面就有当天和近 7 天数据
const APPLY_SEED = [
  { visitorName: '王建国', visitorPhone: '13800000001', visitorCompany: '华信科技', deptId: 4, hostName: '刘洋', visitReason: '项目对接', day: -6, hour: 9, duration: 120, status: 4, verifyCode: '741852', createBy: 5, created: -7 },
  { visitorName: '李梅', visitorPhone: '13800000002', visitorCompany: '中远物流', deptId: 5, hostName: '陈静', visitReason: '市场调研', day: -5, hour: 14, duration: 120, status: 4, verifyCode: '529361', createBy: 7, created: -6 },
  { visitorName: '张强', visitorPhone: '13800000003', visitorCompany: '蓝海咨询', deptId: 6, hostName: '赵磊', visitReason: '商务洽谈', day: -4, hour: 10, duration: 120, status: 4, verifyCode: '610472', createBy: 9, created: -5 },
  { visitorName: '刘芳', visitorPhone: '13800000004', visitorCompany: '新视野传媒', deptId: 4, hostName: '刘洋', visitReason: '技术交流', day: -3, hour: 9, duration: 120, status: 4, verifyCode: '285913', createBy: 6, created: -4 },
  { visitorName: '陈刚', visitorPhone: '13800000005', visitorCompany: '恒达建设', deptId: 5, hostName: '陈静', visitReason: '合作洽谈', day: -2, hour: 13, duration: 120, status: 3, verifyCode: '374825', createBy: 8, created: -3 },
  { visitorName: '杨丽', visitorPhone: '13800000006', visitorCompany: '康泰医疗', deptId: 6, hostName: '赵磊', visitReason: '产品演示', day: -1, hour: 10, duration: 120, status: 3, verifyCode: '906137', createBy: 9, created: -2 },
  { visitorName: '周杰', visitorPhone: '13800000007', visitorCompany: '华宇电子', deptId: 4, hostName: '刘洋', visitReason: '项目验收', day: 0, hour: 9, duration: 120, status: 3, verifyCode: '158204', createBy: 5, created: -1 },
  { visitorName: '吴敏', visitorPhone: '13800000008', visitorCompany: '星辰教育', deptId: 2, hostName: '林小雨', visitReason: '招聘合作', day: 0, hour: 15, duration: 120, status: 3, verifyCode: '463981', createBy: 10, created: -1 },
  { visitorName: '徐磊', visitorPhone: '13800000009', visitorCompany: '金桥贸易', deptId: 3, hostName: '黄小峰', visitReason: '财务对账', day: 1, hour: 10, duration: 120, status: 2, verifyCode: '827350', createBy: 11, created: -1 },
  { visitorName: '孙娜', visitorPhone: '13800000010', visitorCompany: '云图设计', deptId: 1, hostName: '周小云', visitReason: '方案汇报', day: 1, hour: 14, duration: 120, status: 2, verifyCode: '319746', createBy: 12, created: -1 },
  { visitorName: '马超', visitorPhone: '13800000011', visitorCompany: '锐达科技', deptId: 4, hostName: '刘洋', visitReason: '需求沟通', day: 2, hour: 9, duration: 120, status: 1, verifyCode: null, createBy: 5, created: -1 },
  { visitorName: '朱婷', visitorPhone: '13800000012', visitorCompany: '海通咨询', deptId: 5, hostName: '陈静', visitReason: '市场分析', day: 2, hour: 14, duration: 120, status: 1, verifyCode: null, createBy: 7, created: -1 },
  { visitorName: '胡军', visitorPhone: '13800000013', visitorCompany: '远景能源', deptId: 6, hostName: '赵磊', visitReason: '商务拜访', day: 3, hour: 10, duration: 120, status: 0, verifyCode: null, createBy: 9, created: 0 },
  { visitorName: '郭静', visitorPhone: '13800000014', visitorCompany: '明德律所', deptId: 4, hostName: '刘洋', visitReason: '合同评审', day: 2, hour: 9, duration: 120, status: 9, verifyCode: null, createBy: 5, created: 0 }
]

const GATES = ['东门-01', '东门-02', '南门-01', '南门-02']

function buildSeed() {
  const departments = DEPARTMENTS.map(([id, deptCode, deptName, managerName, phone]) => ({
    id,
    deptCode,
    deptName,
    managerName,
    phone,
    status: 1,
    createTime: '2026-01-05 09:00:00'
  }))

  const users = USERS.map(([id, username, realName, phone, deptId, role]) => ({
    id,
    username,
    password: '123456',
    realName,
    phone,
    deptId,
    role,
    status: 1,
    createTime: '2026-01-07 09:00:00'
  }))

  const admin = users.find((u) => u.role === 'ADMIN')
  const auditorOf = (id) => users.find((u) => u.role === 'AUDITOR' && u.deptId === id) || admin

  const applies = []
  const records = []
  const processes = []
  const logs = []
  let pid = 1
  let lid = 1
  let rid = 1

  APPLY_SEED.forEach((seed, index) => {
    const id = index + 1
    const creator = users.find((u) => u.id === seed.createBy)
    const visitStart = at(seed.day, seed.hour, 0)
    const visitTime = fmt(visitStart)
    const leaveTime = fmt(addMinutes(visitStart, seed.duration))
    const createDate = at(seed.created, 10, 0)
    const createTime = fmt(createDate)
    const auditor = auditorOf(seed.deptId)
    const gate = GATES[index % GATES.length]

    const apply = {
      id,
      applyNo: `AP${ymd(createDate)}${String(id).padStart(6, '0')}`,
      visitorName: seed.visitorName,
      visitorPhone: seed.visitorPhone,
      visitorCompany: seed.visitorCompany,
      deptId: seed.deptId,
      hostName: seed.hostName,
      visitReason: seed.visitReason,
      visitTime,
      leaveTime,
      verifyCode: seed.verifyCode,
      status: seed.status,
      createBy: seed.createBy,
      createTime
    }
    applies.push(apply)

    function push(action, from, to, operator, remark, time) {
      processes.push({
        id: pid++,
        applyId: id,
        action,
        operatorId: operator.id,
        operatorName: operator.realName,
        operatorRole: operator.role,
        remark,
        createTime: time
      })
      logs.push({
        id: lid++,
        applyId: id,
        fromStatus: from,
        toStatus: to,
        action,
        operatorId: operator.id,
        operatorName: operator.realName,
        remark,
        createTime: time
      })
    }

    push('CREATE', -1, 0, creator, '提交预约', createTime)
    const approveTime = fmt(addMinutes(createDate, 120))
    const verifyTime = fmt(addMinutes(createDate, 240))

    if ([1, 2, 3, 4].includes(seed.status)) {
      push('APPROVE', 0, 1, auditor, '同意来访', approveTime)
    }
    if ([2, 3, 4].includes(seed.status)) {
      push('VERIFY_CODE', 1, 2, auditor, `生成核验码 ${seed.verifyCode}`, verifyTime)
    }
    if ([3, 4].includes(seed.status)) {
      push('CHECKIN', 2, 3, auditor, `${gate} 入场`, visitTime)
    }
    if (seed.status === 4) {
      push('CHECKOUT', 3, 4, auditor, `${gate} 离场`, leaveTime)
    }
    if (seed.status === 9) {
      push('REJECT', 0, 9, auditor, '访客信息不完整，驳回', fmt(addMinutes(createDate, 60)))
    }

    if ([3, 4].includes(seed.status)) {
      records.push({
        id: rid++,
        applyId: id,
        checkinTime: visitTime,
        checkoutTime: seed.status === 4 ? leaveTime : null,
        checkinBy: auditor.id,
        checkoutBy: seed.status === 4 ? auditor.id : null,
        gate,
        createTime: visitTime
      })
    }
  })

  return {
    version: DB_VERSION,
    departments,
    users,
    applies,
    records,
    processes,
    logs,
    seq: {
      user: 13,
      dept: 11,
      apply: 15,
      record: rid,
      process: pid,
      log: lid
    }
  }
}

export function saveDb(db) {
  try {
    localStorage.setItem(DB_KEY, JSON.stringify(db))
  } catch {
    // 隐私模式下写不进去也不影响内存里的演示
  }
}

export function loadDb() {
  try {
    const raw = localStorage.getItem(DB_KEY)
    if (raw) {
      const db = JSON.parse(raw)
      if (db && db.version === DB_VERSION) return db
    }
  } catch {
    // 数据损坏就走重建
  }
  const db = buildSeed()
  saveDb(db)
  return db
}

export function resetDb() {
  try {
    localStorage.removeItem(DB_KEY)
  } catch {
    // ignore
  }
  return loadDb()
}
