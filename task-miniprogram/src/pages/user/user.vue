<template>
  <view class="container">
    <!-- AI助手悬浮入口（爱宝） -->
    <aibao-float />

    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="nav-content">
        <view class="nav-title">
          <text>个人中心</text>
        </view>
      </view>
    </view>

    <!-- 安全区域容器 -->
    <view class="safe-content">
      <!-- 用户信息卡片 -->
      <view class="user-header">
        <text class="user-name">{{user.name || user.username || '用户'}}</text>
        <text class="user-role">{{roleText}}</text>
      </view>

      <!-- 功能菜单 -->
      <view class="menu-list">
        <view class="menu-item" @tap="showEditModal('email')">
          <view class="menu-content">
            <text class="menu-label">邮箱</text>
            <text class="menu-value">{{user.email || '未设置'}}</text>
          </view>
          <view class="menu-arrow">›</view>
        </view>

        <view class="menu-item" @tap="showEditModal('phone')">
          <view class="menu-content">
            <text class="menu-label">电话</text>
            <text class="menu-value">{{user.phone || '未设置'}}</text>
          </view>
          <view class="menu-arrow">›</view>
        </view>

        <view class="menu-item">
          <view class="menu-content">
            <text class="menu-label">工号</text>
            <text class="menu-value">{{user.id}}</text>
          </view>
        </view>

        <view class="menu-item">
          <view class="menu-content">
            <text class="menu-label">学院</text>
            <text class="menu-value">{{user.tenant || collegeName || '未设置'}}</text>
          </view>
        </view>

        <view class="menu-item" v-if="user.did && user.rid !== '4' && user.rid !== '5' && user.rid !== 4 && user.rid !== 5">
          <view class="menu-content">
            <text class="menu-label">科室</text>
            <text class="menu-value">{{user.department || getDeptName(user.did)}}</text>
          </view>
        </view>
      </view>

      <!-- 退出登录按钮 -->
      <view class="logout-wrapper">
        <view class="logout-btn" @tap="logout">
          <text class="logout-text">退出登录</text>
        </view>
      </view>
      
      <!-- 底部导航栏 -->
      <BottomNav />
    </view>

    <!-- 编辑弹窗 -->
    <view class="modal-mask" v-if="showModal">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">编辑{{fieldName}}</text>
        </view>
        <view class="modal-body">
          <input class="edit-input" :placeholder="`请输入${fieldName}`" v-model="editValue" />
        </view>
        <view class="modal-footer">
          <view class="btn cancel" @tap="hideModal">取消</view>
          <view class="btn confirm" @tap="saveField">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { departmentApi, userApi } from '@/api'

export default {
  data() {
    return {
      user: {},
      roleText: '',
      showModal: false,
      editField: '',
      editValue: '',
      fieldName: '',
      departments: [], // 科室列表
      collegeName: '' // 学院名称
    }
  },

  onLoad() {
    const user = uni.getStorageSync('userInfo')

    if (!user) {
      uni.reLaunch({
        url: '/pages/login/login'
      })
      return
    }

    this.user = user
    this.roleText = this.getRoleText(user.role)

    console.log('用户页面 - 用户信息:', user)
    console.log('用户页面 - user.did:', user.did)
    console.log('用户页面 - user.department:', user.department)
    console.log('用户页面 - user.tenant:', user.tenant)
    console.log('用户页面 - user.rid:', user.rid)
    console.log('用户页面 - user.role:', user.role)

    // 如果没有department字段，则从科室列表中查找
    if (!user.department && user.did) {
      console.log('登录时未返回department字段，从科室列表中查找')
      this.getDepartments(user.tid)
    }
  },

  methods: {
    getRoleText(role) {
      // 后端直接返回角色名称，无需映射
      return role || ''
    },

    // 获取科室列表
    async getDepartments(tid) {
      if (!tid) return

      try {
        const res = await departmentApi.getDepartmentByTid({
          tid: tid
        })

        console.log('获取科室列表响应:', res)

        if (res.code === '200') {
          this.departments = res.data || []
          console.log('科室列表数据:', this.departments)
          console.log('用户did:', this.user.did)
          // 测试查找
          const deptName = this.getDeptName(this.user.did)
          console.log('找到的科室名称:', deptName)
        }
      } catch (error) {
        console.error('获取科室列表失败:', error)
      }
    },

    getDeptName(id) {
      if (!id) return ''
      console.log('查找科室, id:', id, '类型:', typeof id)
      console.log('科室列表:', this.departments)

      // 从科室列表中查找对应id的科室名称
      // 尝试多种匹配方式
      const dept = this.departments.find(item => {
        console.log('比较:', item.id, 'vs', id, item.id == id, item.id === String(id), item.id === parseInt(id))
        return item.id == id || item.id === String(id) || item.id === parseInt(id)
      })

      console.log('找到的科室:', dept)
      return dept ? dept.name : ''
    },

    showEditModal(field) {
      const fieldNames = {
        email: '邮箱',
        phone: '电话'
      }
      this.showModal = true
      this.editField = field
      this.editValue = this.user[field] || ''
      this.fieldName = fieldNames[field] || ''
    },

    hideModal() {
      this.showModal = false
    },

    async saveField() {
      const { editField, editValue, user } = this
      if (!editValue) {
        uni.showToast({ title: '内容不能为空', icon: 'none' })
        return
      }

      try {
        const res = await userApi.updateUserBySelf({
          id: user.id || user.uid,
          [editField]: editValue,
          tid: user.tid || '',
          did: user.did || ''
        })

        if (res.code === '200') {
          user[editField] = editValue
          this.user = { ...user }
          this.showModal = false
          uni.setStorageSync('userInfo', user)
          uni.showToast({ title: '修改成功', icon: 'success' })
        }
      } catch (error) {
        console.error('更新用户信息失败:', error)
      }
    },

    logout() {
      uni.showModal({
        title: '确认退出',
        content: '您确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.clearStorageSync()
            uni.reLaunch({
              url: '/pages/login/login'
            })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 统一样式变量 - 匹配index主题 */
$primary-color: #023c99;
$accent-color: #1890ff;
$primary-gradient: linear-gradient(135deg, #023c99 0%, #1890ff 100%);
$primary-light: rgba(2, 60, 153, 0.1);
$border-lighter: #f0f0f0;
$text-regular: #606266;
$border-base: #dcdfe6;
$radius-xl: 40rpx;
$danger-gradient: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
$text-primary: #303133;
$text-secondary: #909399;
$bg-primary: #ffffff;
$bg-base: #f5f7fa;
$border-base: #e4e7ed;
$radius-lg: 24rpx;
$radius-base: 12rpx;
$spacing-base: 20rpx;
$spacing-lg: 32rpx;
$spacing-xl: 40rpx;
$shadow-base: 0 4rpx 20rpx rgba(2, 60, 153, 0.08);
$shadow-sm: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
$shadow-md: 0 8rpx 24rpx rgba(2, 60, 153, 0.15);

.container {
  background: linear-gradient(180deg, #f8f9fb 0%, #ffffff 100%);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 自定义导航栏 */
.custom-nav {
  background: linear-gradient(135deg, #023c99 0%, #1890ff 100%);
  backdrop-filter: blur(20rpx);
  padding: calc(env(safe-area-inset-top) + 20rpx) 30rpx 20rpx;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  box-shadow: 0 4rpx 20rpx rgba(2, 60, 153, 0.15);
}

.nav-content {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 70rpx;
}

.nav-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.2);
}

/* 安全区域容器 */
.safe-content {
  padding: $spacing-lg 30rpx;
  padding-top: calc(env(safe-area-inset-top) + 130rpx);
  padding-bottom: 150rpx;
  flex: 1;
}

/* 用户头部卡片 */
.user-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: $primary-gradient;
  border-radius: $radius-lg;
  padding: 60rpx 40rpx;
  margin-bottom: $spacing-base;
  box-shadow: $shadow-md;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
    animation: rotate 20s linear infinite;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.user-name {
  font-size: 44rpx;
  font-weight: 700;
  color: white;
  margin-bottom: 16rpx;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.user-role {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.95);
  background-color: rgba(255, 255, 255, 0.2);
  padding: 10rpx 28rpx;
  border-radius: $radius-xl;
  border: 1rpx solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10rpx);
  position: relative;
  z-index: 1;
  font-weight: 500;
}

/* 功能菜单列表 */
.menu-list {
  background-color: white;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-base;
  margin-bottom: $spacing-base;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 40rpx;
  border-bottom: 1rpx solid #f5f5f5;
  transition: all 0.3s ease;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    left: 32rpx;
    right: 32rpx;
    bottom: 0;
    height: 1rpx;
    background: linear-gradient(90deg, transparent 0%, #f0f0f0 50%, transparent 100%);
    transform: scaleY(0.5);
  }
}

.menu-item:last-child {
  border-bottom: none;

  &::after {
    display: none;
  }
}

.menu-item:active {
  background-color: #f8f9fa;
  transform: scale(0.99);
}

.menu-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  padding-left: 0;
}

.menu-label {
  font-size: 24rpx;
  color: #909399;
  font-weight: 600;
  letter-spacing: 0.5rpx;
}

.menu-value {
  font-size: 30rpx;
  color: #303133;
  font-weight: 700;
}

.menu-arrow {
  font-size: 36rpx;
  color: #c0c4cc;
  margin-left: 15rpx;
  font-weight: 300;
}

/* 退出登录区域 */
.logout-wrapper {
  background-color: white;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-base;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36rpx;
  background: $danger-gradient;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transition: left 0.5s ease;
  }

  &:active::before {
    left: 100%;
  }
}

.logout-btn:active {
  transform: scale(0.98);
}

.logout-text {
  font-size: 34rpx;
  color: white;
  font-weight: 600;
  position: relative;
  z-index: 1;
  letter-spacing: 2rpx;
}

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(10rpx);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  width: 85%;
  max-width: 600rpx;
  background-color: white;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-md;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(50rpx);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  padding: $spacing-lg;
  border-bottom: 1rpx solid $border-lighter;
  text-align: center;
  background: linear-gradient(135deg, #f8f9fb 0%, #ffffff 100%);
}

.modal-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $text-primary;
}

.modal-body {
  padding: 36rpx 28rpx;
}

.edit-input {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  padding: 0 24rpx;
  border: 2rpx solid $border-base;
  border-radius: $radius-base;
  font-size: 30rpx;
  background-color: #fafafa;
  box-sizing: border-box;
  transition: all 0.3s ease;
}

.edit-input:focus {
  border-color: $primary-color;
  background-color: white;
  box-shadow: 0 0 0 6rpx rgba(2, 60, 153, 0.1);
  outline: none;
}

.modal-footer {
  display: flex;
  border-top: 1rpx solid $border-lighter;
}

.btn {
  flex: 1;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  transition: all 0.3s ease;
}

.btn:active {
  background-color: #f0f0f0;
  transform: scale(0.98);
}

.btn.cancel {
  color: $text-regular;
  border-right: 1rpx solid $border-base;
}

.btn.confirm {
  color: $primary-color;
  background: linear-gradient(135deg, rgba(2, 60, 153, 0.05) 0%, rgba(24, 144, 255, 0.05) 100%);
}
</style>
