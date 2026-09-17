/**
 * 统一数据管理器
 * 解决页面间数据同步、缓存策略、生命周期管理等问题
 */

import { taskApi } from '@/api'
import request from '@/utils/request'

class DataManager {
  constructor() {
    // 全局缓存
    this.cache = {
      // 用户相关数据
      userInfo: null,
      token: null,
      // 字典数据
      dictData: null,
      // 部门列表
      departments: [],
      // 状态统计（全局）
      statusStatistics: {},
      // 部门任务数量统计
      departmentTaskCounts: {},
      // 学院信息
      collegeName: '',
      // 最后更新时间戳
      lastUpdateTimes: {}
    }

    // 缓存配置
    this.cacheConfig = {
      // 短期缓存（5分钟）- 用户数据、字典数据
      SHORT_TERM: 5 * 60 * 1000,
      // 中期缓存（15分钟）- 部门列表、统计数据
      MEDIUM_TERM: 15 * 60 * 1000,
      // 长期缓存（1小时）- 学院信息
      LONG_TERM: 60 * 60 * 1000
    }

    // 加载状态管理
    this.loadingStates = {}

    // 请求防抖控制
    this.requestDebounceTimers = {}
  }

  /**
   * 检查缓存是否过期
   */
  isCacheExpired(cacheKey, cacheType = 'SHORT_TERM') {
    const lastUpdate = this.cache.lastUpdateTimes[cacheKey]
    if (!lastUpdate) return true

    const cacheTime = this.cacheConfig[cacheType] || this.cacheConfig.SHORT_TERM
    return Date.now() - lastUpdate > cacheTime
  }

  /**
   * 更新缓存时间戳
   */
  updateCacheTime(cacheKey) {
    this.cache.lastUpdateTimes[cacheKey] = Date.now()
  }

  /**
   * 获取用户信息（带缓存）
   */
  async getUserInfo(forceRefresh = false) {
    if (!forceRefresh && !this.isCacheExpired('userInfo', 'SHORT_TERM')) {
      return this.cache.userInfo
    }

    try {
      const userInfo = uni.getStorageSync('userInfo')
      if (userInfo) {
        this.cache.userInfo = userInfo
        this.updateCacheTime('userInfo')
        return userInfo
      }
      return null
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  /**
   * 获取字典数据（带缓存）
   */
  async getDictData(forceRefresh = false) {
    if (!forceRefresh && !this.isCacheExpired('dictData', 'LONG_TERM')) {
      return this.cache.dictData
    }

    try {
      const cachedDict = uni.getStorageSync('dictData')
      if (cachedDict) {
        this.cache.dictData = cachedDict
        this.updateCacheTime('dictData')
        return cachedDict
      }

      // 如果本地没有缓存，从接口获取
      const res = await request({
        url: '/sysdict/getAllSysdicByTid',
        method: 'GET'
      })

      if (res.code === '200') {
        this.cache.dictData = res.data
        this.updateCacheTime('dictData')
        uni.setStorageSync('dictData', res.data)
        return res.data
      }

      return null
    } catch (error) {
      console.error('获取字典数据失败:', error)
      return this.cache.dictData || null
    }
  }

  /**
   * 获取部门列表（带缓存）
   */
  async getDepartments(forceRefresh = false) {
    if (!forceRefresh && !this.isCacheExpired('departments', 'MEDIUM_TERM')) {
      return this.cache.departments
    }

    try {
      const userInfo = await this.getUserInfo()
      const tid = userInfo?.tid

      if (!tid) {
        console.error('用户信息中tid为空，无法获取部门列表')
        return []
      }

      const res = await request({
        url: '/department/getDepartmentByTid',
        method: 'GET',
        params: { tid }
      })

      if (res.code === '200') {
        const departments = (res.data || []).map(dept => ({
          ...dept,
          did: dept.id
        }))
        this.cache.departments = departments
        this.updateCacheTime('departments')
        return departments
      }

      return []
    } catch (error) {
      console.error('获取部门列表失败:', error)
      return this.cache.departments || []
    }
  }

  /**
   * 获取学院信息（带缓存）
   */
  async getCollegeName(forceRefresh = false) {
    if (!forceRefresh && !this.isCacheExpired('collegeName', 'LONG_TERM')) {
      return this.cache.collegeName
    }

    try {
      const userInfo = await this.getUserInfo()

      // 优先从用户信息中获取
      if (userInfo?.tenant) {
        this.cache.collegeName = userInfo.tenant
        this.updateCacheTime('collegeName')
        return userInfo.tenant
      }

      const tid = userInfo?.tid
      if (!tid) {
        return '未知学院'
      }

      const res = await request({
        url: '/college/getAllCollegeByTid',
        method: 'GET'
      })

      if (res.code === '200') {
        const colleges = res.data || []
        const tidStr = String(tid)
        const college = colleges.find(item => String(item.id) === tidStr)

        if (college) {
          this.cache.collegeName = college.name
          this.updateCacheTime('collegeName')
          return college.name
        }
      }

      return '未知学院'
    } catch (error) {
      console.error('获取学院信息失败:', error)
      return this.cache.collegeName || '未知学院'
    }
  }

  /**
   * 获取状态统计（轻量级，用于状态徽章显示）
   */
  async getStatusStatistics(forceRefresh = false) {
    if (!forceRefresh && !this.isCacheExpired('statusStatistics', 'MEDIUM_TERM')) {
      return this.cache.statusStatistics
    }

    try {
      const userInfo = await this.getUserInfo()
      const tid = userInfo?.tid

      if (!tid) {
        console.error('用户信息中tid为空，无法获取状态统计')
        return {}
      }

      // 使用getAllPlanByTenant接口（轻量级，只用于统计）
      const res = await taskApi.getAllPlanByTenant({ tid })

      if (res.code === '200') {
        const allTasks = res.data || []
        const statistics = {}

        // 统计状态分布
        allTasks.forEach(task => {
          const statusValue = task.statusValue || task.status
          const statusName = task.status || task.statusValue

          if (statusValue) {
            statistics[statusValue] = (statistics[statusValue] || 0) + 1
          }
          if (statusName && statusName !== statusValue) {
            statistics[statusName] = (statistics[statusName] || 0) + 1
          }
        })

        this.cache.statusStatistics = statistics
        this.updateCacheTime('statusStatistics')
        return statistics
      }

      return {}
    } catch (error) {
      console.error('获取状态统计失败:', error)
      return this.cache.statusStatistics || {}
    }
  }

  /**
   * 获取部门任务数量统计（轻量级）
   */
  async getDepartmentTaskCounts(forceRefresh = false) {
    if (!forceRefresh && !this.isCacheExpired('departmentTaskCounts', 'MEDIUM_TERM')) {
      return this.cache.departmentTaskCounts
    }

    try {
      const userInfo = await this.getUserInfo()
      const departments = await this.getDepartments()
      const tid = userInfo?.tid

      if (!tid || !departments.length) {
        return {}
      }

      const taskCounts = {}

      // 并行获取所有部门的任务数量
      const promises = departments.map(async (dept) => {
        try {
          const res = await taskApi.getPlanByDepartment({
            tid: tid,
            did: dept.id
          })

          if (res.code === '200') {
            taskCounts[dept.id] = (res.data || []).length
          } else {
            taskCounts[dept.id] = 0
          }
        } catch (error) {
          console.error('获取部门任务数量失败:', dept.name, error)
          taskCounts[dept.id] = 0
        }
      })

      await Promise.all(promises)

      this.cache.departmentTaskCounts = taskCounts
      this.updateCacheTime('departmentTaskCounts')
      return taskCounts
    } catch (error) {
      console.error('获取部门任务数量统计失败:', error)
      return this.cache.departmentTaskCounts || {}
    }
  }

  /**
   * 统一的任务加载方法 - 分页加载
   */
  async loadTasks(options = {}) {
    const {
      tid,
      pageNum = 1,
      pageSize = 100,
      did = null,           // 部门筛选
      status = null,       // 状态筛选
      startTime = null,    // 开始时间筛选
      endTime = null      // 结束时间筛选
    } = options

    try {
      // 如果有筛选条件，使用筛选接口
      if (did || status || startTime || endTime) {
        const params = { tid, pageNum, pageSize }
        if (did) params.did = did
        if (status) params.status = status
        if (startTime) params.startTime = startTime
        if (endTime) params.endTime = endTime

        return await taskApi.searchPlanByConditionPage(params)
      } else {
        // 否则使用普通分页接口
        return await taskApi.getAllPlanByTenantPage({
          tid,
          pageNum,
          pageSize
        })
      }
    } catch (error) {
      console.error('加载任务失败:', error)
      throw error
    }
  }

  /**
   * 防抖请求 - 防止短时间内重复请求
   */
  async debouncedRequest(key, requestFn, debounceTime = 500) {
    // 清除之前的定时器
    if (this.requestDebounceTimers[key]) {
      clearTimeout(this.requestDebounceTimers[key])
    }

    return new Promise((resolve, reject) => {
      this.requestDebounceTimers[key] = setTimeout(async () => {
        try {
          const result = await requestFn()
          resolve(result)
        } catch (error) {
          reject(error)
        } finally {
          delete this.requestDebounceTimers[key]
        }
      }, debounceTime)
    })
  }

  /**
   * 清除所有缓存
   */
  clearAllCache() {
    this.cache = {
      userInfo: null,
      token: null,
      dictData: null,
      departments: [],
      statusStatistics: {},
      departmentTaskCounts: {},
      collegeName: '',
      lastUpdateTimes: {}
    }
  }

  /**
   * 清除特定缓存
   */
  clearCache(...keys) {
    keys.forEach(key => {
      if (this.cache.hasOwnProperty(key)) {
        this.cache[key] = null
      }
      if (this.cache.lastUpdateTimes[key]) {
        delete this.cache.lastUpdateTimes[key]
      }
    })
  }

  /**
   * 批量预加载常用数据（用于应用启动时）
   */
  async preloadCommonData() {
    try {
      await Promise.all([
        this.getUserInfo(),
        this.getDictData(),
        this.getDepartments(),
        this.getCollegeName()
      ])
      console.log('✅ 常用数据预加载完成')
    } catch (error) {
      console.error('❌ 预加载数据失败:', error)
    }
  }

  /**
   * 获取缓存状态信息（用于调试）
   */
  getCacheStatus() {
    return {
      cacheSize: Object.keys(this.cache.lastUpdateTimes).length,
      cacheKeys: Object.keys(this.cache.lastUpdateTimes),
      lastUpdates: this.cache.lastUpdateTimes,
      dataTypes: {
        userInfo: !!this.cache.userInfo,
        dictData: !!this.cache.dictData,
        departments: this.cache.departments.length,
        statusStatistics: Object.keys(this.cache.statusStatistics).length,
        departmentTaskCounts: Object.keys(this.cache.departmentTaskCounts).length,
        collegeName: !!this.cache.collegeName
      }
    }
  }
}

// 创建单例实例
const dataManager = new DataManager()

export default dataManager
export { DataManager }