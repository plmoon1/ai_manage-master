<template>
  <view class="login-container">
    <!-- 背景装饰元素 -->
    <view class="bg-circle circle-1"></view>
    <view class="bg-circle circle-2"></view>
    <view class="bg-circle circle-3"></view>

    <!-- 头部Logo区域 -->
    <view class="header-section">
      <view class="logo-box">
        <view class="logo-inner">
          <text class="logo-icon">🎓</text>
        </view>
      </view>
      <text class="app-name">学院任务管理系统</text>
      <text class="app-subtitle">高效办公，智慧管理</text>
    </view>

    <!-- 登录表单区域 -->
    <view class="form-section">
      <view class="form-card">
        <!-- 账号输入 -->
        <view class="input-group">
          <view class="input-wrapper">
            <view class="icon-box">
              <text class="icon">👤</text>
            </view>
            <input
              class="input"
              type="text"
              placeholder="请输入教职工号"
              placeholder-class="input-placeholder"
              v-model="id"
              @focus="onFocus('id')"
              @blur="onBlur"
            />
          </view>
        </view>

        <!-- 密码输入 -->
        <view class="input-group">
          <view class="input-wrapper">
            <view class="icon-box">
              <text class="icon">🔒</text>
            </view>
            <input
              class="input"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              placeholder-class="input-placeholder"
              v-model="password"
              @focus="onFocus('password')"
              @blur="onBlur"
            />


            <view class="password-toggle" @tap="togglePassword">
              <text class="toggle-icon">{{ showPassword ? '👁️' : '👁️‍🗨️' }}</text>
            </view>
          </view>
        </view>

        <!-- 记住密码 -->
        <view class="remember-section">
          <view class="checkbox-wrapper" @tap="toggleRemember">
            <view class="checkbox" :class="{ 'checked': rememberPassword }">
              <text v-if="rememberPassword" class="check-icon">✓</text>
            </view>
            <text class="checkbox-label">记住密码</text>
          </view>
        </view>

        <!-- 登录按钮 -->
        <button
          class="login-btn"
          :class="{ 'btn-loading': loading, 'btn-disabled': !canLogin }"
          :disabled="loading || !canLogin"
          @tap="handleLogin"
        >
          <text v-if="!loading">登录</text>
          <view v-else class="loading-box">
            <view class="loading-spinner"></view>
            <text class="loading-text">登录中...</text>
          </view>
        </button>
      </view>

      <!-- 底部提示 -->
      <view class="footer-tip">
        <text class="tip-text">首次登录请联系管理员获取账号</text>
      </view>
    </view>
  </view>
</template>

<script>
import { authApi } from '@/api'

export default {
  data() {
    return {
      id: '',
      password: '',
      showPassword: false,
      loading: false,
      focusedField: null,
      rememberPassword: false
    }
  },

  computed: {
    canLogin() {
      return this.id.trim() && this.password.trim()
    }
  },

  onLoad() {
    // 检查是否已登录
    const token = uni.getStorageSync('token')
    if (token) {
      const userInfo = uni.getStorageSync('userInfo')
      if (userInfo) {
        this.redirectByRole(userInfo.role)
      }
    }

    // 读取记住密码的信息
    const savedAccount = uni.getStorageSync('savedAccount')
    if (savedAccount) {
      this.id = savedAccount.id || ''
      this.password = savedAccount.password || ''
      this.rememberPassword = savedAccount.remember || false
    }
  },

  methods: {
    onFocus(field) {
      this.focusedField = field
    },

    onBlur() {
      this.focusedField = null
    },

    togglePassword() {
      this.showPassword = !this.showPassword
    },

    toggleRemember() {
      this.rememberPassword = !this.rememberPassword
    },

    async handleLogin() {
      const { id, password } = this

      if (!id.trim() || !password.trim()) {
        uni.showToast({
          title: '请输入账号和密码',
          icon: 'none'
        })
        return
      }

      this.loading = true

      try {
        // 使用封装的 API
        const result = await authApi.login({
          id: id.trim(),
          password: password.trim()
        })

        const userInfo = result.data

        // 打印完整的用户信息，查看是否包含department字段
        console.log('登录成功 - 用户完整信息:', userInfo)
        console.log('登录成功 - user.department:', userInfo.department)
        console.log('登录成功 - user.tenant:', userInfo.tenant)
        console.log('登录成功 - user.did:', userInfo.did)
        console.log('登录成功 - user.rid:', userInfo.rid)

        // 存储token和用户信息
        uni.setStorageSync('token', userInfo.token)
        uni.setStorageSync('userInfo', userInfo)

        // 处理记住密码
        if (this.rememberPassword) {
          uni.setStorageSync('savedAccount', {
            id: this.id.trim(),
            password: this.password.trim(),
            remember: true
          })
        } else {
          uni.removeStorageSync('savedAccount')
        }

        // 加载字典数据
        await this.loadDictData(userInfo.tid)

        uni.showToast({
          title: '登录成功',
          icon: 'success'
        })

        setTimeout(() => {
          this.redirectByRole(userInfo.role)
          this.controlTabBarByRole()
        }, 500)

        // 清除页面缓存，确保数据刷新
        uni.removeStorageSync('indexPageCache')
        uni.removeStorageSync('meetingPageCache')

      } catch (e) {
        console.error('登录异常', e)
        uni.showToast({
          title: '网络连接失败，请检查网络设置',
          icon: 'none',
          duration: 2000
        })
      } finally {
        this.loading = false
      }
    },

    redirectByRole(role) {
      if (!role) {
        uni.reLaunch({
          url: '/pages/login/login'
        })
        return
      }

      const routeMap = {
        '学院管理员': '/pages/admin/admin',
        '学院领导': '/pages/task/task',
        '科室领导': '/pages/index/index?refresh=true',
        '科室职员': '/pages/index/index?refresh=true',
        '普通用户': '/pages/index/index?refresh=true'
      }

      const targetUrl = routeMap[role] || '/pages/index/index?refresh=true'

      uni.reLaunch({
        url: targetUrl
      })
    },

    controlTabBarByRole() {
      // 可在此处根据角色控制tabBar显示
    },

    // 加载字典数据
    async loadDictData(tid) {
      try {
        // 使用封装的 API
        const res = await authApi.getDictData(tid)

        if (res.code === '200') {
          const dictData = res.data || []

          // 将字典数据按 field 分组存储
          const dictGrouped = {}

          dictData.forEach(item => {
            // 如果有 children，只处理 children（不添加父节点）
            if (item.children && item.children.length > 0) {
              // 只处理 children，不添加父节点
              item.children.forEach(child => {
                if (!dictGrouped[child.field]) {
                  dictGrouped[child.field] = []
                }
                // 只添加有 value 的子节点，使用 value 作为显示名称
                if (child.value) {
                  dictGrouped[child.field].push({
                    id: child.id,
                    value: child.value,
                    name: child.value,  // 直接使用 value 作为显示名称
                    field: child.field
                  })
                }
              })
            } else {
              // 没有 children，只有当有 value 时才添加
              if (item.value && item.field) {
                if (!dictGrouped[item.field]) {
                  dictGrouped[item.field] = []
                }
                dictGrouped[item.field].push({
                  id: item.id,
                  value: item.value,
                  name: item.value,  // 直接使用 value 作为显示名称
                  field: item.field
                })
              }
            }
          })

          // 存储字典数据（分组后的格式）
          uni.setStorageSync('dictData', dictGrouped)
          // 同时存储原始数据
          uni.setStorageSync('dictDataRaw', dictData)

          console.log('字典数据加载完成，字段列表:', Object.keys(dictGrouped))
          console.log('字典数据详情:', dictGrouped)
        }
      } catch (error) {
        console.error('加载字典数据失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
/* 颜色变量 */
$primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
$primary-color: #667eea;
$secondary-color: #764ba2;
$text-dark: #2d3748;
$text-gray: #718096;
$text-light: #a0aec0;
$bg-light: #f7fafc;
$white: #ffffff;

.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 背景装饰圆圈 */
.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 400rpx;
  height: 400rpx;
  top: -100rpx;
  right: -100rpx;
  animation: float 6s ease-in-out infinite;
}

.circle-2 {
  width: 300rpx;
  height: 300rpx;
  bottom: 100rpx;
  left: -80rpx;
  animation: float 8s ease-in-out infinite 1s;
}

.circle-3 {
  width: 200rpx;
  height: 200rpx;
  top: 30%;
  left: 50%;
  animation: float 7s ease-in-out infinite 0.5s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20rpx);
  }
}

/* 头部区域 */
.header-section {
  padding: 100rpx 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
}

.logo-box {
  width: 160rpx;
  height: 160rpx;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.15);
  margin-bottom: 30rpx;
  animation: logoPulse 2s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.logo-inner {
  width: 140rpx;
  height: 140rpx;
  background: $primary-gradient;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon {
  font-size: 70rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: 700;
  color: $white;
  text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
  margin-bottom: 12rpx;
  letter-spacing: 2rpx;
}

.app-subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 300;
}

/* 表单区域 */
.form-section {
  flex: 1;
  padding: 0 40rpx 80rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.form-card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 32rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.15);
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 输入框组 */
.input-group {
  margin-bottom: 32rpx;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: $bg-light;
  border-radius: 24rpx;
  padding: 0 24rpx;
  border: none !important; /* 强制去掉边框 */
  transition: all 0.3s ease;
  position: relative;
  overflow: visible;
  box-shadow: none !important; /* 强制去掉阴影 */
  outline: none !important; /* 强制去掉轮廓线 */
}

.input-wrapper::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 4rpx;
  background: linear-gradient(90deg, $primary-color, $secondary-color);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.input-wrapper:focus-within {
  background: $white;
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.08);
}

.input-wrapper:focus-within::after {
  transform: scaleX(1);
}

.icon-box {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.icon {
  font-size: 32rpx;
}

.input {
  flex: 1;
  height: 96rpx;
  font-size: 32rpx;
  color: $text-dark;
  border: none !important;
  outline: none !important;
  background: transparent !important;
}

/* 隐藏浏览器默认的密码显示按钮 */
.input::-ms-reveal,
.input::-ms-clear {
  display: none !important;
}

/* 隐藏 Webkit 浏览器的密码显示按钮 */
.input::-webkit-credentials-auto-fill-button,
.input::-webkit-caps-lock-indicator,
.input::-webkit-strong-password-auto-fill-button {
  display: none !important;
  -webkit-appearance: none !important;
}

.input-placeholder {
  color: $text-light;
}

.password-toggle {
  padding: 10rpx 16rpx;
  margin-left: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 60rpx;
  min-height: 60rpx;
  cursor: pointer;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

.toggle-icon {
  font-size: 36rpx;
  opacity: 0.5;
  transition: opacity 0.2s ease;
  user-select: none;
  -webkit-user-select: none;
}

.password-toggle:active .toggle-icon {
  opacity: 0.8;
}

/* 记住密码 */
.remember-section {
  margin-bottom: 24rpx;
  position: relative; /* 确保不影响其他元素 */
  z-index: 1; /* 确保在正常层级 */
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  padding: 8rpx 0;
}

.checkbox {
  width: 40rpx;
  height: 40rpx;
  border: 3rpx solid #cbd5e0;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  transition: all 0.3s ease;
  background: #fff;
}

.checkbox.checked {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
}

.check-icon {
  color: #fff;
  font-size: 28rpx;
  font-weight: bold;
}

.checkbox-label {
  font-size: 28rpx;
  color: #4a5568;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 100rpx;
  background: $primary-gradient;
  border-radius: 24rpx;
  border: none;
  margin-top: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn::after {
  border: none;
}

.login-btn:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 16rpx rgba(102, 126, 234, 0.3);
}

.login-btn button {
  color: $white;
  font-size: 36rpx;
  font-weight: 600;
}

.btn-disabled {
  opacity: 0.5;
  box-shadow: none !important;
}

.btn-loading {
  opacity: 0.8;
}

.loading-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.loading-spinner {
  width: 36rpx;
  height: 36rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: $white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  color: $white;
  font-size: 32rpx;
}

/* 底部提示 */
.footer-tip {
  margin-top: 40rpx;
  text-align: center;
}

.tip-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
}
</style>
