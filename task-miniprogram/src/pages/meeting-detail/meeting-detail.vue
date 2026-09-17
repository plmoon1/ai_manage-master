<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      <view class="nav-content">
        <view class="nav-back-wrapper">
          <view class="nav-back" @tap="goBack">
            <view class="back-button">
              <view class="back-arrow"></view>
            </view>
          </view>
        </view>
        <view class="nav-title-wrapper">
          <view class="nav-title">
            <text class="title-text">会议详情</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 内容区域 -->
    <view class="content">
      <!-- 会议标题卡片 -->
      <view class="task-title-card">
        <text class="task-title">{{ meeting.meetingName || '未命名会议' }}</text>
      </view>

      <!-- 会议基本信息卡片 -->
      <view class="info-card">
        <view class="card-title">
          <text class="title-icon">📋</text>
          <text class="title-text">基本信息</text>
        </view>

        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">开始日期</text>
            <text class="info-value">{{ meeting.startDateText || '待定' }}</text>
          </view>

          <view class="info-item" v-if="meeting.endDateText">
            <text class="info-label">结束日期</text>
            <text class="info-value">{{ meeting.endDateText }}</text>
          </view>

          <view class="info-item">
            <text class="info-label">开始时间</text>
            <text class="info-value">{{ meeting.startTimeText || '待定' }}</text>
          </view>

          <view class="info-item" v-if="meeting.endTimeText">
            <text class="info-label">结束时间</text>
            <text class="info-value">{{ meeting.endTimeText }}</text>
          </view>

          <view class="info-item">
            <text class="info-label">会议地点</text>
            <text class="info-value">{{ meeting.location || '待定' }}</text>
          </view>

          <view class="info-item">
            <text class="info-label">主持人</text>
            <text class="info-value">{{ meeting.host || '未知' }}</text>
          </view>

          <view class="info-item">
            <text class="info-label">所属部门</text>
            <text class="info-value">{{ meeting.deptName || '暂无' }}</text>
          </view>

          <view class="info-item full-width">
            <text class="info-label">参会人员</text>
            <text class="info-value">{{ meeting.participants || '无' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 返回顶部按钮 -->
    <view
      v-if="showBackToTop"
      class="back-to-top-btn"
      @tap="backToTop"
    >
      <view class="up-arrow"></view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-action-bar">
      <view class="action-buttons">
        <view class="action-btn edit-btn" @tap="editMeeting">
          <text class="btn-text">修改会议</text>
        </view>
        <view class="action-btn delete-btn" @tap="deleteMeeting">
          <text class="btn-text">删除会议</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { departmentApi, meetingApi, fileApi } from '@/api'
import config from '@/config'

export default {
  data() {
    return {
      meeting: {},
      departments: [],
      showBackToTop: false
    }
  },

  async onLoad(options) {
    // 加载部门数据
    try {
      const userInfo = uni.getStorageSync('userInfo') || {}
      const tid = userInfo.tid || ''
      if (tid) {
        const deptRes = await departmentApi.getDepartmentByTid({ tid })
        if (deptRes.code === '200') {
          this.departments = deptRes.data || []
          console.log('部门数据加载成功:', this.departments)
        }
      }
    } catch (error) {
      console.error('加载部门数据失败:', error)
    }

    if (options.meeting) {
      try {
        const meetingData = JSON.parse(decodeURIComponent(options.meeting))
        this.meeting = meetingData
        this.loadMeetingDetail()
      } catch (error) {
        console.error('解析会议数据失败:', error)
        uni.showToast({
          title: '数据加载失败',
          icon: 'none'
        })
      }
    }
  },

  onPageScroll(e) {
    this.showBackToTop = e.scrollTop > 300
  },

  methods: {
    async loadMeetingDetail() {
      console.log('会议详情原始数据:', this.meeting)

      // 判断是否是单日会议
      const isSingleDayMeeting = () => {
        if (!this.meeting.startDate) return true

        const startDate = new Date(this.meeting.startDate)
        if (isNaN(startDate.getTime())) return true

        if (!this.meeting.endDate) return true

        const endDate = new Date(this.meeting.endDate)
        if (isNaN(endDate.getTime())) return true

        // 比较日期
        return startDate.toDateString() === endDate.toDateString()
      }

      const isSingleDay = isSingleDayMeeting()

      // 格式化开始日期
      if (this.meeting.startDate) {
        try {
          const date = new Date(this.meeting.startDate)
          if (!isNaN(date.getTime())) {
            const month = date.getMonth() + 1
            const day = date.getDate()
            const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
            this.meeting.startDateText = `${month}月${day}日 ${weekday}`
          } else {
            this.meeting.startDateText = this.meeting.startDate
          }
        } catch (e) {
          this.meeting.startDateText = this.meeting.startDate || '待定'
        }
      } else {
        this.meeting.startDateText = '待定'
      }

      // 格式化结束日期
      if (this.meeting.endDate) {
        try {
          const date = new Date(this.meeting.endDate)
          if (!isNaN(date.getTime())) {
            const month = date.getMonth() + 1
            const day = date.getDate()
            const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
            this.meeting.endDateText = `${month}月${day}日 ${weekday}`
          } else {
            this.meeting.endDateText = this.meeting.endDate
          }
        } catch (e) {
          this.meeting.endDateText = this.meeting.endDate || '待定'
        }
      }

      // 格式化开始时间
      let startTimeStr = ''
      if (this.meeting.startStartTime) {
        startTimeStr = this.meeting.startStartTime.replace(/上午|下午/, '')
      } else if (this.meeting.startTime) {
        startTimeStr = this.meeting.startTime.replace(/上午|下午/, '')
      }

      if (startTimeStr) {
        // 根据单日/多日添加前缀
        const startPrefix = isSingleDay ? '当日 ' : '起始日 '
        this.meeting.startTimeText = startPrefix + startTimeStr
      } else {
        this.meeting.startTimeText = '待定'
      }

      // 格式化结束时间
      let endTimeStr = ''
      if (this.meeting.endEndTime) {
        endTimeStr = this.meeting.endEndTime.replace(/上午|下午/, '')
      } else if (this.meeting.endTime) {
        endTimeStr = this.meeting.endTime.replace(/上午|下午/, '')
      }

      if (endTimeStr) {
        // 根据单日/多日添加前缀
        const endPrefix = isSingleDay ? '当日 ' : '结束日 '
        this.meeting.endTimeText = endPrefix + endTimeStr
      }

      // 设置其他字段
      this.meeting.meetingName = this.meeting.meetingName || '未命名会议'
      this.meeting.host = this.meeting.host || '未知'
      this.meeting.deptName = this.meeting.deptName || (this.meeting.did && this.departments.length > 0 ? (this.departments.find(d => d.id === this.meeting.did)?.name || '') : '') || '暂无'
      this.meeting.location = this.meeting.location || '待定'
      this.meeting.participants = this.meeting.participants || '无'

      console.log('会议详情处理后数据:', {
        meetingName: this.meeting.meetingName,
        startDateText: this.meeting.startDateText,
        endDateText: this.meeting.endDateText,
        startTimeText: this.meeting.startTimeText,
        endTimeText: this.meeting.endTimeText
      })
    },

    goBack() {
      console.log('goBack 被调用')
      // 尝试返回上一页
      const pages = getCurrentPages()
      console.log('当前页面栈长度:', pages.length)

      if (pages.length > 1) {
        uni.navigateBack({
          delta: 1,
          success: () => {
            console.log('返回成功')
          },
          fail: (err) => {
            console.log('返回失败:', err)
            // 如果 navigateBack 失败，尝试直接跳转到会议列表页面
            uni.redirectTo({
              url: '/pages/meeting/meeting',
              fail: () => {
                console.log('redirectTo 也失败了')
              }
            })
          }
        })
      } else {
        // 如果页面栈只有当前页面，使用 redirectTo
        console.log('页面栈为空，使用 redirectTo')
        uni.redirectTo({
          url: '/pages/meeting/meeting',
          fail: () => {
            console.log('redirectTo 失败')
          }
        })
      }
    },

    backToTop() {
      uni.pageScrollTo({
        scrollTop: 0,
        duration: 300
      })
    },

    downloadFile(file) {
      if (!file || !file.url) {
        uni.showToast({
          title: '文件地址不存在',
          icon: 'none'
        })
        return
      }

      const downloadUrl = `${config.baseURL}/files/download?file=${encodeURIComponent(file.url)}`

      uni.showLoading({
        title: '下载中...'
      })

      try {
        uni.downloadFile({
          url: downloadUrl,
          success: (res) => {
            uni.hideLoading()

            if (res.statusCode === 200) {
              uni.saveFile({
                tempFilePath: res.tempFilePath,
                success: (saveRes) => {
                  uni.showToast({
                    title: '文件下载成功',
                    icon: 'success'
                  })
                  console.log('文件保存路径:', saveRes.savedFilePath)
                },
                fail: (saveError) => {
                  console.error('保存文件失败:', saveError)
                  uni.showToast({
                    title: '文件保存失败',
                    icon: 'none'
                  })
                }
              })
            } else {
              uni.showToast({
                title: '下载失败',
                icon: 'none'
              })
            }
          },
          fail: (error) => {
            uni.hideLoading()
            console.error('下载文件失败:', error)
            uni.showToast({
              title: '下载文件失败',
              icon: 'none'
            })
          }
        })
      } catch (error) {
        uni.hideLoading()
        console.error('下载附件异常:', error)
        uni.showToast({
          title: '下载异常',
          icon: 'none'
        })
      }
    },

    // 编辑会议
    editMeeting() {
      if (!this.meeting.id) {
        uni.showToast({
          title: '会议ID不存在',
          icon: 'none'
        })
        return
      }

      // 将完整的会议数据传递给编辑页面
      const encodedMeeting = encodeURIComponent(JSON.stringify(this.meeting))

      // 跳转到会议编辑页面
      uni.navigateTo({
        url: `/pages/meeting-add/meeting-add?mode=edit&meeting=${encodedMeeting}`
      })
    },

    // 删除会议
    deleteMeeting() {
      if (!this.meeting.id) {
        uni.showToast({
          title: '会议ID不存在',
          icon: 'none'
        })
        return
      }

      // 显示确认对话框
      uni.showModal({
        title: '确认删除',
        content: '删除后将无法恢复，是否确认删除该会议？',
        confirmText: '确认删除',
        confirmColor: '#ff4d4f',
        success: async (res) => {
          if (res.confirm) {
            await this.confirmDeleteMeeting()
          }
        }
      })
    },

    // 确认删除会议
    async confirmDeleteMeeting() {
      uni.showLoading({
        title: '删除中...',
        mask: true
      })

      try {
        // 使用软删除方式，调用updateMeeting接口设置isdelete='1'
        const deleteData = {
          ...this.meeting,
          isdelete: '1'
        }

        const res = await meetingApi.updateMeeting(deleteData)

        uni.hideLoading()

        if (res.code === '200' || res.code === 200) {
          uni.showToast({
            title: '删除成功',
            icon: 'success',
            duration: 1500
          })

          setTimeout(() => {
            // 返回会议主页并强制刷新
            uni.redirectTo({
              url: '/pages/meeting/meeting?forceRefresh=true'
            })
          }, 1500)
        } else if (res.code === '401' || res.code === 403) {
          throw new Error('您没有权限进行此操作')
        } else {
          throw new Error(res.msg || '删除失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('删除会议失败:', error)
        uni.showToast({
          title: error.message || '删除失败',
          icon: 'none',
          duration: 2000
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
/* 统一样式变量 */
$primary-color: #667eea;
$primary-light: #e6f0ff;
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$bg-base: #f5f7fa;
$border-base: #e4e7ed;
$radius-lg: 24rpx;
$radius-base: 12rpx;

.page {
  background: linear-gradient(180deg, #f8f9fb 0%, #ffffff 100%);
  min-height: 100vh;
  padding-top: calc(env(safe-area-inset-top) + 140rpx);
  padding-bottom: 180rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 自定义导航栏 */
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: linear-gradient(135deg, #023c99 0%, #1890ff 100%);
  padding: 0;
  box-shadow: 0 4rpx 20rpx rgba(2, 60, 153, 0.15);
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: var(--status-bar-height);
  min-height: 40rpx;
  width: 100%;
}

.nav-content {
  height: 70rpx;
  display: flex;
  align-items: center;
  position: relative;
  width: 100%;
}

.nav-back-wrapper {
  position: absolute;
  left: 20rpx;
  display: flex;
  align-items: center;
  height: 100%;
  z-index: 10;
  transform: translateY(-15rpx);
}

.nav-back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 70rpx;
  height: 100%;
}

.back-button {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.85) 100%);
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15), 0 2rpx 6rpx rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  position: relative;
}

.back-button:active {
  transform: scale(0.9);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.12), 0 1rpx 4rpx rgba(0, 0, 0, 0.08);
}

.back-arrow {
  width: 20rpx;
  height: 20rpx;
  border-left: 3rpx solid #023c99;
  border-bottom: 3rpx solid #023c99;
  transform: rotate(45deg);
  margin-left: 6rpx;
}

.nav-title-wrapper {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 90rpx;
  transform: translateY(-15rpx);
}

.nav-title {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.title-text {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 600;
}

/* 内容区域 */
.content {
  padding: 24rpx;
}

/* 任务标题卡片 */
.task-title-card {
  background: linear-gradient(135deg, rgba(2, 60, 153, 0.8) 0%, rgba(0, 86, 179, 0.7) 100%);
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(2, 60, 153, 0.2);
  backdrop-filter: blur(10rpx);
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

.task-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #ffffff;
  line-height: 1.4;
  display: block;
}

/* 信息卡片 */
.info-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid $border-base;
}

.title-icon {
  font-size: 32rpx;
  line-height: 1;
}

.title-text {
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 26rpx;
  color: $text-secondary;
  margin-bottom: 4rpx;
}

.info-value {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 600;
  word-break: break-all;
}

/* 返回顶部按钮 */
.back-to-top-btn {
  position: fixed;
  right: 30rpx;
  bottom: 100rpx;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.85) 100%);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15), 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  transform: translateY(20rpx) scale(0.8);
  animation: fadeInUp 0.3s ease forwards;
}

.up-arrow {
  width: 0;
  height: 0;
  border-left: 14rpx solid transparent;
  border-right: 14rpx solid transparent;
  border-bottom: 20rpx solid #023c99;
  position: relative;
  margin-bottom: 6rpx;
}

.up-arrow::after {
  content: '';
  position: absolute;
  bottom: -20rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 6rpx;
  height: 12rpx;
  background: #023c99;
  border-radius: 2rpx;
}

.back-to-top-btn:active {
  transform: scale(0.9);
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.12), 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20rpx) scale(0.8);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 底部操作栏 */
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
  z-index: 100;
  padding: 20rpx calc(20rpx + env(safe-area-inset-right));
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.action-buttons {
  display: flex;
  gap: 20rpx;
  align-items: center;
}

.action-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 500;
  color: #ffffff;
  transition: all 0.3s ease;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
}

.edit-btn {
  background: linear-gradient(135deg, #023c99 0%, #1890ff 100%);
}

.edit-btn:active {
  transform: scale(0.98);
  opacity: 0.9;
}

.delete-btn {
  background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);
}

.delete-btn:active {
  transform: scale(0.98);
  opacity: 0.9;
}
</style>
