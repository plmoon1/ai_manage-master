/**
 * 日期辅助工具
 * 用于计算和存储常用日期
 */

/**
 * 获取所有预定义日期
 * @returns {Object} 包含今天、明天、本周、本月等日期
 */
export function getAllStoredDates() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  // 明天
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)

  // 本周开始（周一）
  const weekStart = new Date(today)
  const dayOfWeek = today.getDay()
  const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  weekStart.setDate(today.getDate() + diff)

  // 本周结束（周日）
  const weekEnd = new Date(weekStart)
  weekEnd.setDate(weekStart.getDate() + 6)

  // 本月开始
  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1)

  // 本月结束
  const monthEnd = new Date(today.getFullYear(), today.getMonth() + 1, 0)

  return {
    today: formatDate(today),
    tomorrow: formatDate(tomorrow),
    weekStart: formatDate(weekStart),
    weekEnd: formatDate(weekEnd),
    monthStart: formatDate(monthStart),
    monthEnd: formatDate(monthEnd)
  }
}

/**
 * 格式化日期为 YYYY-MM-DD
 * @param {Date} date - 日期对象
 * @returns {String} 格式化后的日期字符串
 */
function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 存储日期到 localStorage
 */
export function storeDates() {
  const dates = getAllStoredDates()
  uni.setStorageSync('todayDateStr', dates.today)
  uni.setStorageSync('tomorrowDateStr', dates.tomorrow)
  uni.setStorageSync('weekStartStr', dates.weekStart)
  uni.setStorageSync('weekEndStr', dates.weekEnd)
  uni.setStorageSync('monthStartStr', dates.monthStart)
  uni.setStorageSync('monthEndStr', dates.monthEnd)
  return dates
}

/**
 * 从 localStorage 获取存储的日期
 * 如果不存在则计算并存储
 */
export function getStoredDates() {
  let dates = {
    today: uni.getStorageSync('todayDateStr'),
    tomorrow: uni.getStorageSync('tomorrowDateStr'),
    weekStart: uni.getStorageSync('weekStartStr'),
    weekEnd: uni.getStorageSync('weekEndStr'),
    monthStart: uni.getStorageSync('monthStartStr'),
    monthEnd: uni.getStorageSync('monthEndStr')
  }

  // 如果没有存储的日期，重新计算
  if (!dates.today) {
    dates = storeDates()
  }

  return dates
}

export default {
  getAllStoredDates,
  storeDates,
  getStoredDates
}
