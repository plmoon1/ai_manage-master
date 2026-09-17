<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      <view class="nav-content">
        <view class="nav-title-wrapper">
          <view class="nav-title">
            <text class="title-text">{{ isEditMode ? '修改会议' : '新增会议' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 表单内容 -->
    <view class="form-content">
      <!-- 基本信息卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">📋</text>
          <text class="title-text">基本信息</text>
        </view>

        <!-- 会议名称 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">会议名称</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <input
              class="form-input"
              type="text"
              v-model="formData.meetingName"
              placeholder="请输入会议名称"
              maxlength="100"
            />
          </view>
        </view>

        <!-- 主持人 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">主持人</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <picker
              mode="selector"
              :range="userOptions"
              range-key="username"
              :value="hostIndex"
              @change="onHostChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.host }">
                  {{ hostName || '请选择主持人' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 参加对象 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">参加对象</text>
          </view>
          <view class="item-content">
            <textarea
              class="form-textarea"
              v-model="formData.participants"
              placeholder="请输入参加对象"
              maxlength="500"
              :show-confirm-bar="false"
            ></textarea>
            <text class="char-count">{{ formData.participants.length }}/500</text>
          </view>
        </view>

        <!-- 会议地点 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">会议地点</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <input
              class="form-input"
              type="text"
              v-model="formData.location"
              placeholder="请输入会议地点"
              maxlength="100"
            />
          </view>
        </view>
      </view>

      <!-- 时间设置卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">📅</text>
          <text class="title-text">时间设置</text>
        </view>

        <!-- 开始日期 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">开始日期</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <picker
              mode="date"
              :value="formData.startDate"
              @change="onStartDateChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.startDate }">
                  {{ formData.startDate || '请选择开始日期' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 结束日期 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">结束日期</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <picker
              mode="date"
              :value="formData.endDate"
              :start="formData.startDate"
              @change="onEndDateChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.endDate }">
                  {{ formData.endDate || '请选择结束日期' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 开始时间 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">开始时间</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <picker
              mode="time"
              :value="formData.startStartTime"
              @change="onStartTimeChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.startStartTime }">
                  {{ formData.startStartTime || '请选择开始时间' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 结束时间 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">结束时间</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <picker
              mode="time"
              :value="formData.endEndTime"
              @change="onEndTimeChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.endEndTime }">
                  {{ formData.endEndTime || '请选择结束时间' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>
      </view>

      <!-- 其他设置卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">⚙️</text>
          <text class="title-text">其他设置</text>
        </view>

        <!-- 会议状态 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">会议状态</text>
          </view>
          <view class="item-content">
            <picker
              mode="selector"
              :range="statusOptions"
              range-key="name"
              :value="statusIndex"
              @change="onStatusChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.status }">
                  {{ statusName || '请选择会议状态' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>
      </view>

      <!-- 底部间距 -->
      <view class="bottom-space"></view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="cancel-btn" @tap="goBack">
        <text class="cancel-text">取消</text>
      </view>
      <view class="confirm-btn" @tap="saveMeeting">
        <text class="confirm-text">确认保存</text>
      </view>
    </view>
  </view>
</template>

<script>
import { meetingApi, userApi, fileApi } from '@/api'
import config from '@/config'

export default {
  data() {
    return {
      // 编辑模式标识
      isEditMode: false,
      editingMeetingId: '',
      editingMeetingData: null,  // 编辑的会议数据

      // 表单数据
      formData: {
        id: '',                 // 会议ID（UUID）
        meetingName: '',        // 会议名称
        host: '',               // 主持人ID
        participants: '',        // 参加对象
        location: '',            // 会议地点
        startDate: '',           // 开始日期
        endDate: '',             // 结束日期
        startStartTime: '',      // 开始时间
        endEndTime: '',          // 结束时间
        status: '',              // 会议状态
        startWeek: '',           // 开始周数
        endWeek: '',             // 结束周数
        tid: '',                 // 租户ID
        uid: ''                  // 创建人ID
      },

      // 用户信息
      currentUser: {},
      userInfo: {},

      // 选项数据
      userOptions: [],         // 用户列表
      statusOptions: [],        // 会议状态

      // 选择器索引
      hostIndex: -1,
      statusIndex: -1,

      // 加载状态
      isLoading: false
    }
  },

  computed: {
    // 主持人名称
    hostName() {
      if (this.hostIndex >= 0 && this.userOptions[this.hostIndex]) {
        return this.userOptions[this.hostIndex].username
      }
      return ''
    },

    // 会议状态名称
    statusName() {
      if (this.statusIndex >= 0 && this.statusOptions[this.statusIndex]) {
        return this.statusOptions[this.statusIndex].name
      }
      return ''
    }
  },

  async onLoad(options) {
    // 检测是否为编辑模式
    if (options.mode === 'edit' && options.meeting) {
      this.isEditMode = true
      try {
        const meetingData = JSON.parse(decodeURIComponent(options.meeting))
        this.editingMeetingId = meetingData.id
        this.editingMeetingData = meetingData
      } catch (error) {
        console.error('解析会议数据失败:', error)
      }
    }

    // 获取当前用户信息
    const userInfo = uni.getStorageSync('userInfo')
    if (!userInfo) {
      uni.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        uni.reLaunch({
          url: '/pages/login/login'
        })
      }, 1000)
      return
    }

    this.userInfo = userInfo
    this.currentUser = userInfo
    this.formData.uid = userInfo.id
    this.formData.tid = userInfo.tid

    // 默认设置主持人为当前用户
    this.formData.host = userInfo.id
    this.hostIndex = 0

    // 加载所需数据
    await this.loadInitialData()

    // 如果是编辑模式，加载会议数据
    if (this.isEditMode && this.editingMeetingData) {
      this.loadMeetingData()
    }
  },

  methods: {
    // 计算日期对应的周数
    getWeekNumber(dateStr) {
      if (!dateStr) return ''

      const date = new Date(dateStr)
      const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
      const dayNum = d.getUTCDay() || 7
      d.setUTCDate(d.getUTCDate() + 4 - dayNum)
      const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
      const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7)

      return weekNo.toString()
    },

    // 生成UUID (8-4-4-4-12格式)
    generateUUID() {
      const chars = '0123456789abcdef'
      const uuid = []
      // 8-4-4-4-12 format
      const format = [8, 4, 4, 4, 12]

      format.forEach((length, index) => {
        for (let i = 0; i < length; i++) {
          uuid.push(chars[Math.floor(Math.random() * 16)])
        }
        if (index < format.length - 1) {
          uuid.push('-')
        }
      })

      return uuid.join('')
    },

    // 加载初始数据
    async loadInitialData() {
      try {
        // 并行加载所有数据
        await Promise.all([
          this.loadUsers(),
          this.loadStatusOptions()
        ])

        console.log('初始数据加载完成')
      } catch (error) {
        console.error('加载初始数据失败:', error)
        uni.showToast({
          title: '加载数据失败',
          icon: 'none'
        })
      }
    },

    // 加载用户列表
    async loadUsers() {
      try {
        const res = await userApi.getUserByCondition({
          tid: this.userInfo.tid
        })

        if (res.code === '200' && res.data) {
          this.userOptions = res.data

          // 设置当前用户为默认主持人
          const currentUserIndex = this.userOptions.findIndex(
            user => user.id === this.userInfo.id
          )
          if (currentUserIndex >= 0) {
            this.hostIndex = currentUserIndex
            this.formData.host = this.userOptions[currentUserIndex].id
          }
        }
      } catch (error) {
        console.error('加载用户列表失败:', error)
      }
    },

    // 加载会议状态选项
    async loadStatusOptions() {
      try {
        // 使用字典数据
        const dictData = uni.getStorageSync('dictDataRaw')
        if (dictData) {
          const statusData = dictData.filter(item => item.field === 'meeting_status')
          if (statusData.length > 0) {
            this.statusOptions = statusData.map(item => ({
              id: item.id,
              value: item.id,
              name: item.value
            }))
            console.log('会议状态:', this.statusOptions)
          }
        }

        // 如果没有字典数据，使用固定选项
        if (this.statusOptions.length === 0) {
          this.statusOptions = [
            { id: '1', value: '1', name: '未开始' },
            { id: '2', value: '2', name: '进行中' },
            { id: '3', value: '3', name: '已结束' },
            { id: '4', value: '4', name: '已取消' }
          ]
        }

        // 默认设置为未开始
        const defaultIndex = this.statusOptions.findIndex(s => s.name === '未开始')
        if (defaultIndex >= 0) {
          this.statusIndex = defaultIndex
          this.formData.status = this.statusOptions[defaultIndex].value
        }
      } catch (error) {
        console.error('加载状态选项失败:', error)
      }
    },

    // 加载会议数据（编辑模式）
    async loadMeetingData() {
      if (!this.editingMeetingData) {
        uni.showToast({
          title: '会议数据不存在',
          icon: 'none'
        })
        return
      }

      try {
        const meetingData = this.editingMeetingData

        // 回填表单数据
        this.formData.id = meetingData.id || ''
        this.formData.meetingName = meetingData.meetingName || ''
        this.formData.participants = meetingData.participants || ''
        this.formData.location = meetingData.location || ''
        this.formData.startDate = meetingData.startDate || ''
        this.formData.endDate = meetingData.endDate || ''
        this.formData.startStartTime = meetingData.startTime || ''
        this.formData.endEndTime = meetingData.endTime || ''
        this.formData.tid = meetingData.tid || this.userInfo.tid
        this.formData.uid = meetingData.uid || this.userInfo.id

        // 计算周数
        if (this.formData.startDate) {
          this.formData.startWeek = this.getWeekNumber(this.formData.startDate)
        }
        if (this.formData.endDate) {
          this.formData.endWeek = this.getWeekNumber(this.formData.endDate)
        }

        // 回填主持人选择器
        if (meetingData.host && this.userOptions.length > 0) {
          const hostIndex = this.userOptions.findIndex(user => user.id === meetingData.host)
          if (hostIndex >= 0) {
            this.hostIndex = hostIndex
            this.formData.host = this.userOptions[hostIndex].id
          }
        }

        // 回填状态选择器
        if (meetingData.status && this.statusOptions.length > 0) {
          const statusIndex = this.statusOptions.findIndex(status => status.value === meetingData.status)
          if (statusIndex >= 0) {
            this.statusIndex = statusIndex
            this.formData.status = this.statusOptions[statusIndex].value
          }
        }

        console.log('会议数据加载完成:', this.formData)
      } catch (error) {
        console.error('加载会议数据失败:', error)
        uni.showToast({
          title: error.message || '加载会议数据失败',
          icon: 'none'
        })
      }
    },

    // 主持人选择
    onHostChange(e) {
      this.hostIndex = e.detail.value
      this.formData.host = this.userOptions[this.hostIndex].id
    },

    // 开始日期选择
    onStartDateChange(e) {
      this.formData.startDate = e.detail.value
      // 如果结束日期早于开始日期，重置结束日期
      if (this.formData.endDate && this.formData.endDate < this.formData.startDate) {
        this.formData.endDate = ''
      }
    },

    // 结束日期选择
    onEndDateChange(e) {
      this.formData.endDate = e.detail.value
      // 验证日期逻辑
      if (this.formData.startDate && this.formData.endDate < this.formData.startDate) {
        uni.showToast({
          title: '结束日期不能早于开始日期',
          icon: 'none'
        })
        this.formData.endDate = ''
      }
    },

    // 开始时间选择
    onStartTimeChange(e) {
      this.formData.startStartTime = e.detail.value
    },

    // 结束时间选择
    onEndTimeChange(e) {
      this.formData.endEndTime = e.detail.value
      // 如果开始和结束日期相同，验证时间逻辑
      if (this.formData.startDate === this.formData.endDate) {
        if (this.formData.endEndTime < this.formData.startStartTime) {
          uni.showToast({
            title: '结束时间不能早于开始时间',
            icon: 'none'
          })
          this.formData.endEndTime = ''
        }
      }
    },

    // 会议状态选择
    onStatusChange(e) {
      this.statusIndex = e.detail.value
      this.formData.status = this.statusOptions[this.statusIndex].id
    },


    // 验证表单
    validateForm() {
      if (!this.formData.meetingName.trim()) {
        uni.showToast({
          title: '请输入会议名称',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.host) {
        uni.showToast({
          title: '请选择主持人',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.location.trim()) {
        uni.showToast({
          title: '请输入会议地点',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.startDate) {
        uni.showToast({
          title: '请选择开始日期',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.endDate) {
        uni.showToast({
          title: '请选择结束日期',
          icon: 'none'
        })
        return false
      }

      if (this.formData.endDate < this.formData.startDate) {
        uni.showToast({
          title: '结束日期不能早于开始日期',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.startStartTime) {
        uni.showToast({
          title: '请选择开始时间',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.endEndTime) {
        uni.showToast({
          title: '请选择结束时间',
          icon: 'none'
        })
        return false
      }

      return true
    },

    // 保存会议
    async saveMeeting() {
      if (!this.validateForm()) {
        return
      }

      if (this.isLoading) {
        return
      }

      this.isLoading = true

      try {
        uni.showLoading({
          title: this.isEditMode ? '更新中...' : '保存中...'
        })

        // 构建保存数据
        const saveData = {
          ...this.formData,
          // 计算周数
          startWeek: this.getWeekNumber(this.formData.startDate),
          endWeek: this.getWeekNumber(this.formData.endDate),
          // 确保必要字段存在
          uid: this.formData.uid || this.userInfo.id,
          tid: this.formData.tid || this.userInfo.tid,
          isdelete: '0'  // 设置为未删除状态
        }

        // 根据模式选择API
        let res
        if (this.isEditMode) {
          // 编辑模式：使用现有ID，调用更新接口
          saveData.id = this.editingMeetingId
          res = await meetingApi.updateMeeting(saveData)
        } else {
          // 新增模式：生成新ID，调用创建接口
          saveData.id = this.generateUUID()
          res = await meetingApi.createMeeting(saveData)
        }

        uni.hideLoading()

        if (res.code === '200' || res.code === 200) {
          const successTitle = this.isEditMode ? '更新成功' : '保存成功'
          uni.showToast({
            title: successTitle,
            icon: 'success',
            duration: 1500
          })

          setTimeout(() => {
            // 跳转到会议主页并强制刷新
            uni.redirectTo({
              url: '/pages/meeting/meeting?forceRefresh=true',
              success: function() {
                console.log('跳转到会议主页成功，页面将自动刷新')
              },
              fail: function(err) {
                console.error('跳转到会议主页失败:', err)
              }
            })
          }, 1500)
        } else if (res.code === '401' || res.code === 403) {
          throw new Error('您没有权限进行此操作')
        } else {
          throw new Error(res.msg || '保存失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('保存会议失败:', error)
        uni.showToast({
          title: error.message || '保存失败',
          icon: 'none',
          duration: 2000
        })
      } finally {
        this.isLoading = false
      }
    },

    // 返回上一页
    goBack() {
      // 总是跳转到会议主页并强制刷新
      uni.redirectTo({
        url: '/pages/meeting/meeting?forceRefresh=true',
        success: function() {
          console.log('返回会议主页成功，页面将自动刷新')
        },
        fail: function(err) {
          console.error('返回会议主页失败:', err)
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 复用任务新增页面的样式 */
/* 统一样式变量 */
$primary-color: #023c99;
$primary-light: #e6f0ff;
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$border-color: #e4e7ed;
$bg-color: #f5f7fa;

.page {
  background: linear-gradient(180deg, #f8f9fb 0%, #ffffff 100%);
  min-height: 100vh;
  padding-top: calc(env(safe-area-inset-top) + 150rpx);
  padding-bottom: 180rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 自定义导航栏 */
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: $primary-color;
  padding: 0 40rpx;
  box-shadow: none;
}

.status-bar {
  height: var(--status-bar-height);
  min-height: 40rpx;
}

.nav-content {
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.nav-back-wrapper {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
}

.nav-back {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;

  &:active {
    background: rgba(255, 255, 255, 0.2);
  }
}

.back-button {
  position: relative;
  width: 40rpx;
  height: 40rpx;
}

.back-arrow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 24rpx;
  height: 24rpx;
  border-left: 4rpx solid #ffffff;
  border-bottom: 4rpx solid #ffffff;
  border-radius: 4rpx;
  transform: translate(-50%, -50%) rotate(45deg);
}

.nav-title-wrapper {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.nav-title {
  display: flex;
  align-items: center;
}

.title-text {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 600;
}

.nav-right {
  display: flex;
  align-items: center;
}

.save-btn {
  padding: 12rpx 24rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;

  &:active {
    background: rgba(255, 255, 255, 0.3);
  }
}

.save-text {
  font-size: 28rpx;
  color: #ffffff;
  font-weight: 500;
}

/* 表单内容 */
.form-content {
  padding: 20rpx 32rpx;
}

/* 表单卡片 */
.form-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
}

.card-title {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.title-icon {
  font-size: 36rpx;
  margin-right: 12rpx;
}

.title-text {
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
}

/* 表单项 */
.form-item {
  margin-bottom: 32rpx;

  &.required {
    position: relative;
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.item-label {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.label-text {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

.required-mark {
  font-size: 28rpx;
  color: #ff4d4f;
  margin-left: 4rpx;
}

.item-content {
  position: relative;
}

/* 表单输入框 */
.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: $text-primary;
  background: #f5f7fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;

  &:focus {
    background: #ffffff;
    border-color: $primary-color;
  }
}

.form-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 16rpx 24rpx;
  font-size: 28rpx;
  color: $text-primary;
  background: #f5f7fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
  line-height: 1.6;

  &:focus {
    background: #ffffff;
    border-color: $primary-color;
  }
}

.char-count {
  position: absolute;
  bottom: 12rpx;
  right: 24rpx;
  font-size: 24rpx;
  color: $text-secondary;
}

/* 选择器 */
.picker-view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  padding: 0 24rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
}

.picker-text {
  font-size: 28rpx;
  color: $text-primary;

  &.placeholder {
    color: $text-secondary;
  }
}

.picker-arrow {
  font-size: 40rpx;
  color: $text-secondary;
  font-weight: 300;
}

/* 信息显示 */
.info-display {
  height: 80rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
}

.info-text {
  font-size: 28rpx;
  color: $text-primary;
}

/* 开关样式 */
switch {
  transform: scale(0.8);
}

/* 底部间距 */
.bottom-space {
  height: 40rpx;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
  z-index: 999;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.98);
  }
}

.cancel-btn {
  background: #f5f7fa;
  border: 2rpx solid $border-color;
}

.cancel-text {
  font-size: 28rpx;
  color: $text-regular;
  font-weight: 500;
}

.confirm-btn {
  background: linear-gradient(135deg, $primary-color 0%, #0247b3 100%);
  box-shadow: 0 4rpx 12rpx rgba(2, 60, 153, 0.3);
}

.confirm-text {
  font-size: 28rpx;
  color: #ffffff;
  font-weight: 600;
}
</style>