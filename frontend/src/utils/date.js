export function parseDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  const date = new Date(String(value).replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? null : date
}

export function isFuture(value) {
  const date = parseDate(value)
  return Boolean(date && date.getTime() > Date.now())
}

export function isBefore(value, base) {
  const a = parseDate(value)
  const b = parseDate(base)
  if (!a || !b) return false
  return a.getTime() <= b.getTime()
}

export function todayString() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function formatMinutes(minutes) {
  const m = Number(minutes)
  if (!Number.isFinite(m) || m < 0) return '—'
  if (m < 60) return `${m} 分钟`
  const h = Math.floor(m / 60)
  const rest = Math.round(m % 60)
  return rest ? `${h} 小时 ${rest} 分钟` : `${h} 小时`
}

// 用于日期选择器：禁用今天之前的日期
export function disabledPastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}
