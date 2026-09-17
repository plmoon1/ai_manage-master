/**
 * 统一页面生命周期管理器
 * 提供标准化的页面加载逻辑，减少代码重复
 */

import dataManager from './dataManager'

class PageLifecycleManager {
  constructor() {
    this.pageStates = {}
    this.refreshTimers = {}
  }

  /**
   * 标准页面初始化流程
   * 适用于列表页面（如index、works、meeting等）
   */
  async initListPage(pageContext, options = {}) {
    const {
      needUserInfo = true,
      needDictData = true,
      needDepartments = true,
      needCollegeName = true,
      needStatusStatistics = true,
      needDepartmentCounts = true,
      customInitFn = null      // 自定义初始化函数
    } = options

    try {
      console.log(`🚀 开始初始化${pageContext.pageName || '列表'}页面...`)

      // 1. 基础数据加载（并行）
      const baseDataPromises = []

      if (needUserInfo) baseDataPromises.push('userInfo')
      if (needDictData) baseDataPromises.push('dictData')
      if (needDepartments) baseDataPromises.push('departments')
      if (needCollegeName) baseDataPromises.push('collegeName')

      // 执行基础数据加载
      const baseDataResults = await Promise.all(
        baseDataPromises.map(key => this.loadDataByKey(key))
      )

      // 将基础数据设置到页面上下文
      const result = {
        userInfo: null,
        dictData: null,
        departments: [],
        collegeName: ''
      }

      baseDataResults.forEach((data, index) => {
        const key = baseDataPromises[index]
        result[key] = data
      })

      // 2. 统计数据加载（并行）
      if (needStatusStatistics || needDepartmentCounts) {
        const statsPromises = []
        if (needStatusStatistics) statsPromises.push(dataManager.getStatusStatistics())
        if (needDepartmentCounts) statsPromises.push(dataManager.getDepartmentTaskCounts())

        const statsResults = await Promise.all(statsPromises)

        if (needStatusStatistics) {
          result.statusStatistics = statsResults[0]
        }
        if (needDepartmentCounts) {
          result.departmentTaskCounts = statsResults[needStatusStatistics ? 1 : 0]
        }
      }

      // 3. 自定义初始化
      if (customInitFn && typeof customInitFn === 'function') {
        const customResult = await customInitFn(result)
        Object.assign(result, customResult)
      }

      console.log(`✅ ${pageContext.pageName || '列表'}页面初始化完成`)

      return result
    } catch (error) {
      console.error(`❌ ${pageContext.pageName || '列表'}页面初始化失败:`, error)
      throw error
    }
  }

  /**
   * 标准页面显示流程（onShow）
   */
  async onPageShow(pageContext, options = {}) {
    const {
      refreshData = true,
      refreshTaskList = true,
      refreshStatistics = true,
      customShowFn = null
    } = options

    try {
      console.log(`📱 ${pageContext.pageName || '页面'}显示`)

      if (!refreshData) {
        return { success: true, message: '跳过数据刷新' }
      }

      const refreshPromises = []

      // 刷新任务列表
      if (refreshTaskList && pageContext.loadTaskList) {
        refreshPromises.push(pageContext.loadTaskList())
      }

      // 刷新统计数据
      if (refreshStatistics) {
        refreshPromises.push(dataManager.getDepartmentTaskCounts())
        refreshPromises.push(dataManager.getStatusStatistics())
      }

      // 执行刷新
      await Promise.all(refreshPromises)

      // 自定义显示逻辑
      if (customShowFn && typeof customShowFn === 'function') {
        await customShowFn()
      }

      return { success: true, message: '数据刷新完成' }
    } catch (error) {
      console.error(`❌ ${pageContext.pageName || '页面'}显示时数据刷新失败:`, error)
      return { success: false, error }
    }
  }

  /**
   * 标准页面刷新流程（下拉刷新）
   */
  async onPageRefresh(pageContext, options = {}) {
    const {
      refreshAll = true,
      clearCache = false,
      customRefreshFn = null
    } = options

    try {
      console.log(`🔄 ${pageContext.pageName || '页面'}刷新`)

      if (clearCache) {
        dataManager.clearCache()
      }

      const refreshPromises = []

      // 刷新基础数据
      if (refreshAll) {
        refreshPromises.push(dataManager.preloadCommonData())
      }

      // 刷新页面数据
      if (pageContext.loadPageData) {
        refreshPromises.push(pageContext.loadPageData())
      }

      await Promise.all(refreshPromises)

      // 自定义刷新逻辑
      if (customRefreshFn && typeof customRefreshFn === 'function') {
        await customRefreshFn()
      }

      return { success: true, message: '刷新完成' }
    } catch (error) {
      console.error(`❌ ${pageContext.pageName || '页面'}刷新失败:`, error)
      return { success: false, error }
    }
  }

  /**
   * 页面卸载流程
   */
  onPageHide(pageContext, options = {}) {
    const {
      clearTimers = true,
      saveState = false,
      customHideFn = null
    } = options

    try {
      console.log(`📤 ${pageContext.pageName || '页面'}隐藏`)

      // 清除定时器
      if (clearTimers && this.refreshTimers[pageContext.pageId]) {
        clearInterval(this.refreshTimers[pageContext.pageId])
        delete this.refreshTimers[pageContext.pageId]
      }

      // 保存页面状态
      if (saveState && pageContext.savePageState) {
        pageContext.savePageState()
      }

      // 自定义隐藏逻辑
      if (customHideFn && typeof customHideFn === 'function') {
        customHideFn()
      }

      return { success: true }
    } catch (error) {
      console.error(`❌ ${pageContext.pageName || '页面'}隐藏处理失败:`, error)
      return { success: false, error }
    }
  }

  /**
   * 页面卸载流程
   */
  onPageUnload(pageContext, options = {}) {
    const {
      clearTimers = true,
      clearCache = false,
      customUnloadFn = null
    } = options

    try {
      console.log(`🗑️ ${pageContext.pageName || '页面'}卸载`)

      // 清除定时器
      if (clearTimers && this.refreshTimers[pageContext.pageId]) {
        clearInterval(this.refreshTimers[pageContext.pageId])
        delete this.refreshTimers[pageContext.pageId]
      }

      // 清除缓存
      if (clearCache) {
        dataManager.clearCache()
      }

      // 自定义卸载逻辑
      if (customUnloadFn && typeof customUnloadFn === 'function') {
        customUnloadFn()
      }

      // 清除页面状态
      delete this.pageStates[pageContext.pageId]

      return { success: true }
    } catch (error) {
      console.error(`❌ ${pageContext.pageName || '页面'}卸载处理失败:`, error)
      return { success: false, error }
    }
  }

  /**
   * 设置定时刷新
   */
  setAutoRefresh(pageContext, refreshFn, interval = 10 * 60 * 1000) {
    // 清除已有定时器
    this.clearAutoRefresh(pageContext)

    // 设置新定时器
    this.refreshTimers[pageContext.pageId] = setInterval(async () => {
      try {
        console.log(`⏰ ${pageContext.pageName || '页面'}自动刷新`)
        await refreshFn()
      } catch (error) {
        console.error(`❌ ${pageContext.pageName || '页面'}自动刷新失败:`, error)
      }
    }, interval)

    console.log(`✅ ${pageContext.pageName || '页面'}自动刷新已设置，间隔: ${interval / 1000}秒`)
  }

  /**
   * 清除定时刷新
   */
  clearAutoRefresh(pageContext) {
    if (this.refreshTimers[pageContext.pageId]) {
      clearInterval(this.refreshTimers[pageContext.pageId])
      delete this.refreshTimers[pageContext.pageId]
      console.log(`🛑 ${pageContext.pageName || '页面'}自动刷新已清除`)
    }
  }

  /**
   * 加载指定类型的数据
   */
  async loadDataByKey(key) {
    const dataLoaderMap = {
      'userInfo': () => dataManager.getUserInfo(),
      'dictData': () => dataManager.getDictData(),
      'departments': () => dataManager.getDepartments(),
      'collegeName': () => dataManager.getCollegeName(),
      'statusStatistics': () => dataManager.getStatusStatistics(),
      'departmentTaskCounts': () => dataManager.getDepartmentTaskCounts()
    }

    const loader = dataLoaderMap[key]
    if (!loader) {
      console.warn(`⚠️ 未知的数据类型: ${key}`)
      return null
    }

    return await loader()
  }

  /**
   * 创建统一的页面配置对象
   */
  createPageConfig(pageName, customOptions = {}) {
    return {
      pageName,
      pageId: `${pageName}_${Date.now()}`,
      ...customOptions
    }
  }

  /**
   * 检查页面状态
   */
  checkPageState(pageId) {
    return this.pageStates[pageId] || null
  }

  /**
   * 更新页面状态
   */
  updatePageState(pageId, state) {
    this.pageStates[pageId] = {
      ...this.pageStates[pageId],
      ...state,
      lastUpdate: Date.now()
    }
  }
}

// 创建单例实例
const pageLifecycleManager = new PageLifecycleManager()

export default pageLifecycleManager
export { PageLifecycleManager }