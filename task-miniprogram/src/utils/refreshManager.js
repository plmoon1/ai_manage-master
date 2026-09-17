/**
 * 统一刷新管理器
 * 优化所有页面的下拉刷新体验，防止误触，提升用户体验
 */

class RefreshManager {
  constructor() {
    // 页面刷新状态管理
    this.pageRefreshStates = {}

    // 防误触配置
    this.config = {
      // 最小下拉距离（像素），小于此距离认为是误触
      minPullDistance: 30,
      // 最大下拉速度（像素/毫秒），超过此速度认为是快速滑动
      maxPullSpeed: 0.5,
      // 最小下拉持续时间（毫秒）
      minPullDuration: 200,
      // 刷新状态保持时间（毫秒）
      refreshStateDuration: 500,
      // 刷新禁用恢复时间（毫秒）
      refreshRecoveryTime: 1000
    }

    // 触摸状态跟踪
    this.touchStates = {}
  }

  /**
   * 初始化页面刷新状态
   */
  initPageState(pageId, options = {}) {
    const defaultOptions = {
      refreshEnabled: true,           // 是否启用刷新
      refreshThreshold: 100,         // 触发刷新的距离阈值
      upperThreshold: 50,            // 允许刷新的顶部距离
      preventFastSwipe: true,         // 防止快速滑动触发
      preventShortPull: true,         // 防止短距离拉动触发
      showRefreshIndicator: true,    // 显示刷新指示器
      customRefreshLogic: null       // 自定义刷新逻辑
    }

    this.pageRefreshStates[pageId] = {
      ...defaultOptions,
      ...options,
      isRefreshing: false,
      canRefresh: false,
      lastScrollTop: 0,
      isNearTop: false,
      refreshDisabled: false,
      refreshCount: 0,
      lastRefreshTime: null,
      consecutiveFastSwipes: 0  // 连续快速滑动次数
    }

    // 初始化触摸状态
    this.touchStates[pageId] = {
      touchStartTime: 0,
      touchStartY: 0,
      lastTouchY: 0,
      isPulling: false,
      pullDistance: 0,
      pullSpeed: 0
    }

    return this.pageRefreshStates[pageId]
  }

  /**
   * 检查页面刷新状态
   */
  getPageState(pageId) {
    return this.pageRefreshStates[pageId] || null
  }

  /**
   * 更新页面滚动状态
   */
  updateScrollState(pageId, scrollTop) {
    const state = this.getPageState(pageId)
    if (!state) return

    // 判断是否接近顶部
    state.isNearTop = scrollTop <= state.upperThreshold

    // 向下滚动时重新启用刷新
    if (scrollTop > state.lastScrollTop && state.isNearTop) {
      state.refreshDisabled = false
      state.consecutiveFastSwipes = 0
    }

    state.lastScrollTop = scrollTop
  }

  /**
   * 检查是否允许刷新
   */
  canRefresh(pageId) {
    const state = this.getPageState(pageId)
    if (!state) return false

    return (
      state.refreshEnabled &&
      !state.isRefreshing &&
      !state.refreshDisabled &&
      state.isNearTop
    )
  }

  /**
   * 触摸开始
   */
  onTouchStart(pageId, touchY) {
    const touchState = this.touchStates[pageId]
    if (!touchState) return

    touchState.touchStartTime = Date.now()
    touchState.touchStartY = touchY
    touchState.lastTouchY = touchY
    touchState.isPulling = false
    touchState.pullDistance = 0
    touchState.pullSpeed = 0
  }

  /**
   * 触摸移动
   */
  onTouchMove(pageId, currentY) {
    const state = this.getPageState(pageId)
    const touchState = this.touchStates[pageId]

    if (!state || !touchState || !state.refreshEnabled) return

    const pullDistance = currentY - touchState.touchStartY
    const timeDiff = Date.now() - touchState.touchStartTime

    // 计算拉动速度
    if (timeDiff > 0) {
      touchState.pullSpeed = Math.abs(pullDistance) / timeDiff
    }

    // 只有在顶部附近向下拉时才认为是下拉刷新手势
    if (pullDistance > 0 && state.isNearTop) {
      touchState.isPulling = true
      touchState.pullDistance = pullDistance

      // 防误触检查
      let shouldDisable = false

      // 1. 检查拉动速度（防止快速滑动）
      if (state.preventFastSwipe && touchState.pullSpeed > this.config.maxPullSpeed) {
        shouldDisable = true
        state.consecutiveFastSwipes++
        console.log('🚫 快速滑动检测，连续次数:', state.consecutiveFastSwipes)

        // 连续3次快速滑动时，暂时禁用刷新
        if (state.consecutiveFastSwipes >= 3) {
          state.refreshDisabled = true
          setTimeout(() => {
            state.refreshDisabled = false
            state.consecutiveFastSwipes = 0
          }, this.config.refreshRecoveryTime)
        }
      }

      // 2. 检查拉动距离（防止误触）
      if (state.preventShortPull && pullDistance < this.config.minPullDistance) {
        shouldDisable = true
      }

      // 3. 检查拉动时间（防止误触）
      if (timeDiff < this.config.minPullDuration && pullDistance > 50) {
        shouldDisable = true
      }

      if (shouldDisable) {
        touchState.isPulling = false
        touchState.pullDistance = 0
      }
    } else {
      touchState.isPulling = false
      touchState.pullDistance = 0
    }
  }

  /**
   * 触摸结束
   */
  onTouchEnd(pageId) {
    const touchState = this.touchStates[pageId]
    if (!touchState) return

    // 短暂延迟后重置状态
    setTimeout(() => {
      touchState.isPulling = false
      touchState.pullDistance = 0
      touchState.pullSpeed = 0
    }, 100)
  }

  /**
   * 判断是否应该触发刷新
   */
  shouldTriggerRefresh(pageId) {
    const state = this.getPageState(pageId)
    const touchState = this.touchStates[pageId]

    if (!state || !touchState) return false

    // 检查各种条件
    const isPullGesture = touchState.isPulling && touchState.pullDistance > 0
    const isNotRefreshing = !state.isRefreshing
    const isEnabled = state.refreshEnabled && !state.refreshDisabled
    const isNearTop = state.isNearTop

    return isPullGesture && isNotRefreshing && isEnabled && isNearTop
  }

  /**
   * 开始刷新
   */
  async startRefresh(pageId, refreshFn) {
    const state = this.getPageState(pageId)
    if (!state) return

    // 如果不应该触发刷新，返回false
    if (!this.shouldTriggerRefresh(pageId)) {
      console.log('🚫 刷新条件不满足，阻止刷新')
      // 短暂显示刷新状态然后关闭
      state.isRefreshing = true
      setTimeout(() => {
        state.isRefreshing = false
      }, 300)
      return false
    }

    console.log('🔄 开始刷新页面:', pageId)
    state.isRefreshing = true
    state.refreshDisabled = true
    state.refreshCount++
    state.lastRefreshTime = Date.now()

    try {
      // 执行刷新逻辑
      if (refreshFn && typeof refreshFn === 'function') {
        await refreshFn()
      }
      return true
    } catch (error) {
      console.error('❌ 刷新失败:', error)
      throw error
    } finally {
      // 延迟关闭刷新状态
      setTimeout(() => {
        state.isRefreshing = false
        // 延迟重新启用刷新
        setTimeout(() => {
          state.refreshDisabled = false
        }, this.config.refreshRecoveryTime)
      }, this.config.refreshStateDuration)
    }
  }

  /**
   * 手动完成刷新
   */
  completeRefresh(pageId) {
    const state = this.getPageState(pageId)
    if (!state) return

    console.log('✅ 手动完成刷新:', pageId)
    state.isRefreshing = false

    setTimeout(() => {
      state.refreshDisabled = false
    }, this.config.refreshStateDuration)
  }

  /**
   * 临时禁用刷新
   */
  disableRefresh(pageId, duration = 2000) {
    const state = this.getPageState(pageId)
    if (!state) return

    state.refreshDisabled = true

    if (duration > 0) {
      setTimeout(() => {
        state.refreshDisabled = false
      }, duration)
    }
  }

  /**
   * 重新启用刷新
   */
  enableRefresh(pageId) {
    const state = this.getPageState(pageId)
    if (!state) return

    state.refreshDisabled = false
    state.consecutiveFastSwipes = 0
  }

  /**
   * 获取刷新统计信息
   */
  getRefreshStats(pageId) {
    const state = this.getPageState(pageId)
    if (!state) return null

    return {
      refreshCount: state.refreshCount,
      lastRefreshTime: state.lastRefreshTime,
      isRefreshing: state.isRefreshing,
      canRefresh: this.canRefresh(pageId),
      isNearTop: state.isNearTop
    }
  }

  /**
   * 重置页面状态
   */
  resetPageState(pageId) {
    if (this.pageRefreshStates[pageId]) {
      delete this.pageRefreshStates[pageId]
    }
    if (this.touchStates[pageId]) {
      delete this.touchStates[pageId]
    }
  }

  /**
   * 清理所有状态
   */
  cleanup() {
    this.pageRefreshStates = {}
    this.touchStates = {}
  }
}

// 创建单例实例
const refreshManager = new RefreshManager()

export default refreshManager
export { RefreshManager }