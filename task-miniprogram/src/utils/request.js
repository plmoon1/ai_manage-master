/**
 * Axios 请求封装
 * 适配 H5 平台，支持 JWT token 认证
 * 增强功能：支持请求取消，防止ClientAbortException
 */

import axios from 'axios'
import config from '@/config'
import { Message } from '@/utils/message'

let loadingCount = 0
let isLoginRedirecting = false
let silentMode = false // 静默模式：不显示错误toast

// 请求管理器 - 用于页面切换时取消待处理请求
const requestManager = {
  pendingRequests: new Map(), // 存储待处理的请求

  // 生成请求唯一标识
  generateRequestKey(config) {
    return `${config.method}_${config.url}_${JSON.stringify(config.params || {})}_${JSON.stringify(config.data || {})}`
  },

  // 添加请求到管理器
  addRequest(config, cancelToken) {
    const key = this.generateRequestKey(config)
    this.pendingRequests.set(key, cancelToken)
    console.log('📤 请求已添加到管理器:', key.substring(0, 50))
  },

  // 移除请求
  removeRequest(config) {
    const key = this.generateRequestKey(config)
    this.pendingRequests.delete(key)
  },

  // 取消指定页面的所有请求
  cancelPageRequests(pageId = null) {
    if (this.pendingRequests.size === 0) {
      console.log('📭 没有待处理的请求')
      return
    }

    let cancelledCount = 0
    for (const [key, cancel] of this.pendingRequests.entries()) {
      try {
        cancel('页面切换，取消请求')
        cancelledCount++
      } catch (error) {
        console.error('取消请求失败:', error)
      }
    }

    this.pendingRequests.clear()
    console.log(`🛑 已取消 ${cancelledCount} 个待处理请求`)
  },

  // 取消所有请求
  cancelAllRequests() {
    this.cancelPageRequests()
  }
}

// 显示 loading
function showLoading() {
  if (loadingCount === 0 && !silentMode) {
    uni.showLoading({
      title: '加载中...',
      mask: true
    })
  }
  loadingCount++
}

// 隐藏 loading
function hideLoading() {
  loadingCount--
  if (loadingCount <= 0) {
    loadingCount = 0
    if (!silentMode) {
      uni.hideLoading()
    }
  }
}

// 跳转到登录页
function redirectToLogin() {
  if (isLoginRedirecting) return
  isLoginRedirecting = true

  uni.clearStorageSync()

  uni.reLaunch({
    url: '/pages/login/login',
    complete() {
      isLoginRedirecting = false
    }
  })
}

// 创建 axios 实例
const service = axios.create({
  baseURL: config.baseURL,
  timeout: config.apiTimeout,
  headers: {
    'Content-Type': 'application/json'
  },
  // 优化：启用缓存，减少重复请求
  cache: false
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 创建取消令牌
    const source = axios.CancelToken.source()
    config.cancelToken = source.token

    // 添加到请求管理器
    requestManager.addRequest(config, source.cancel)

    // 显示 loading
    showLoading()

    // 添加 token
    const token = uni.getStorageSync('token')
    if (token) {
      config.headers.token = token
    }

    return config
  },
  error => {
    hideLoading()
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 从请求管理器中移除已完成的请求
    requestManager.removeRequest(response.config)

    hideLoading()

    const { status, data } = response

    // HTTP 层异常处理
    if (status === 401) {
      redirectToLogin()
      return Promise.reject(response)
    }

    if (status === 404) {
      if (!silentMode) {
        uni.showToast({
          title: '接口不存在',
          icon: 'none'
        })
      }
      return Promise.reject(response)
    }

    if (status >= 500) {
      if (!silentMode) {
        uni.showToast({
          title: '服务器异常',
          icon: 'none'
        })
      }
      return Promise.reject(response)
    }

    // 业务层异常处理
    if (data && data.code) {
      // token 相关错误统一回登录
      if (
        data.code === '401' ||
        data.code === '5001' ||
        data.code === '5002'
      ) {
        redirectToLogin()
        return Promise.reject(data)
      }

      // 其他业务错误
      if (data.code !== '200') {
        if (!silentMode) {
          uni.showToast({
            title: data.msg || '请求失败',
            icon: 'none'
          })
        }
        return Promise.reject(data)
      }
    }

    return data
  },
  error => {
    // 从请求管理器中移除失败的请求
    if (error.config) {
      requestManager.removeRequest(error.config)
    }

    hideLoading()

    console.error('响应错误:', error)

    // 请求取消处理（静默处理，不显示错误提示）
    if (axios.isCancel(error)) {
      console.log('🛑 请求已取消:', error.message)
      return Promise.reject({ code: 'CANCELLED', message: '请求已取消' })
    }

    // 网络错误处理
    if (error.code === 'ECONNABORTED') {
      if (!silentMode) {
        uni.showToast({
          title: '请求超时',
          icon: 'none'
        })
      }
    } else if (!error.response) {
      if (!silentMode) {
        uni.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
      }
    } else {
      if (!silentMode) {
        uni.showToast({
          title: error.response?.data?.msg || '请求失败',
          icon: 'none'
        })
      }
    }

    return Promise.reject(error)
  }
)

// 导出服务实例和辅助函数
export default service
export { silentMode, requestManager }

// 设置静默模式的辅助函数
export function setSilentMode(mode) {
  silentMode = mode
}

// 批量请求时的静默辅助函数
export async function withSilentMode(fn) {
  const oldMode = silentMode
  silentMode = true
  try {
    return await fn()
  } finally {
    silentMode = oldMode
  }
}

// 页面切换时的清理函数 - 应该在 onHide/onUnload 中调用
export function cleanupPendingRequests() {
  requestManager.cancelAllRequests()
}
