<template>
  <view class="container">
    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input 
          class="search-input" 
          placeholder="搜索消息标题或内容" 
          v-model="searchKeyword"
          @input="onSearchInput"
        />
        <text class="clear-icon" v-if="searchKeyword" @tap="onClearSearch">✕</text>
      </view>
    </view>

    <view class="tabs">
      <view 
        class="tab-item" 
        :class="{'active': currentTab === 0}"
        @tap="onTabChange(0)">
        全部
      </view>
      <view 
        class="tab-item" 
        :class="{'active': currentTab === 1}"
        @tap="onTabChange(1)">
        未读
        <view class="badge" v-if="unReadCount > 0">{{unReadCount}}</view>
      </view>
      <view 
        class="tab-item" 
        :class="{'active': currentTab === 2}"
        @tap="onTabChange(2)">
        指派给我
      </view>
    </view>

    <scroll-view scroll-y class="message-list">
      <view 
        class="message-item" 
        :class="{'read': item.isRead}"
        v-for="(item, index) in filteredMessages" 
        :key="item.id"
        @tap="onTapMessage(index)">
        <view class="message-dot" v-if="!item.isRead"></view>
        <view class="message-content">
          <view class="message-header">
            <text class="message-title">{{item.title}}</text>
            <text class="message-time">{{item.createTime}}</text>
          </view>
          <view class="message-body">{{item.content}}</view>
          <view class="message-type">{{getTypeText(item.type)}}</view>
        </view>
      </view>
      <view class="empty-tip" v-if="filteredMessages.length === 0">
        <text>暂无消息</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      currentTab: 0,
      tabs: ['全部', '未读', '指派给我'],
      searchKeyword: '',
      messages: [],
      filteredMessages: [],
      unReadCount: 0
    }
  },

  onLoad() {
    this.loadMessages()
  },

  onShow() {
    this.loadMessages()
  },

  methods: {
    loadMessages() {
      const messages = uni.getStorageSync('messageList') || this.getDemoMessages()
      const userInfo = uni.getStorageSync('userInfo') || {}
      const uid = userInfo.uid || ''
      
      const unreadCount = messages.filter(m => !m.isRead).length
      
      this.messages = messages
      this.unReadCount = unreadCount
      this.filteredMessages = this.filterMessages(messages)
      
      uni.setStorageSync('messageList', messages)
    },

    getDemoMessages() {
      return [
        {
          id: '1',
          title: '新任务通知',
          content: '您有一个新的任务待处理，请及时查看。',
          type: 'task',
          isRead: false,
          assignee: 'user1',
          createTime: '2024-01-15 09:30'
        },
        {
          id: '2',
          title: '任务状态更新',
          content: '您的任务"期末考试安排"已通过审批。',
          type: 'approval',
          isRead: false,
          assignee: 'user1',
          createTime: '2024-01-15 10:20'
        },
        {
          id: '3',
          title: '系统通知',
          content: '系统将于今晚22:00进行维护升级。',
          type: 'system',
          isRead: true,
          assignee: '',
          createTime: '2024-01-14 15:00'
        },
        {
          id: '4',
          title: '任务催办',
          content: '您负责的任务"课程表调整"即将到期，请尽快完成。',
          type: 'remind',
          isRead: true,
          assignee: 'user1',
          createTime: '2024-01-14 11:30'
        }
      ]
    },

    getTypeText(type) {
      switch (type) {
        case 'task':
          return '任务通知'
        case 'approval':
          return '审批通知'
        case 'remind':
          return '催办提醒'
        default:
          return '系统通知'
      }
    },

    filterMessages(messages) {
      const { currentTab, searchKeyword } = this
      const userInfo = uni.getStorageSync('userInfo') || {}
      const uid = userInfo.uid || ''
      
      let filtered = [...messages]
      
      switch (currentTab) {
        case 1:
          filtered = messages.filter(m => !m.isRead)
          break
        case 2:
          filtered = messages.filter(m => m.assignee === uid)
          break
        default:
          filtered = messages
      }
      
      if (searchKeyword) {
        const keyword = searchKeyword.toLowerCase()
        filtered = filtered.filter(m => 
          m.title.toLowerCase().includes(keyword) || 
          m.content.toLowerCase().includes(keyword)
        )
      }
      
      return filtered
    },

    onTabChange(index) {
      this.currentTab = index
      this.filteredMessages = this.filterMessages(this.messages)
    },

    onSearchInput() {
      this.filteredMessages = this.filterMessages(this.messages)
    },

    onClearSearch() {
      this.searchKeyword = ''
      this.filteredMessages = this.filterMessages(this.messages)
    },

    onTapMessage(index) {
      const message = this.filteredMessages[index]
      
      const messages = this.messages.map(m => {
        if (m.id === message.id) {
          return { ...m, isRead: true }
        }
        return m
      })
      
      this.messages = messages
      this.unReadCount = messages.filter(m => !m.isRead).length
      this.filteredMessages = this.filterMessages(messages)
      
      uni.setStorageSync('messageList', messages)
      
      uni.showToast({
        title: message.title,
        icon: 'none'
      })
    }
  }
}
</script>

<style>
.container {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.search-bar {
  padding: 20rpx;
  background-color: white;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background-color: #f0f0f0;
  border-radius: 50rpx;
  padding: 10rpx 20rpx;
}

.search-icon {
  font-size: 32rpx;
  color: #999;
  margin-right: 10rpx;
}

.search-input {
  flex: 1;
  height: 60rpx;
  font-size: 32rpx;
  background-color: transparent;
}

.clear-icon {
  font-size: 32rpx;
  color: #999;
  margin-left: 10rpx;
}

.tabs {
  display: flex;
  background-color: white;
  border-bottom: 1rpx solid #ddd;
}

.tab-item {
  flex: 1;
  height: 90rpx;
  line-height: 90rpx;
  text-align: center;
  font-size: 32rpx;
  position: relative;
}

.tab-item.active {
  color: #007aff;
  border-bottom: 3rpx solid #007aff;
}

.badge {
  position: absolute;
  top: 10rpx;
  right: 20rpx;
  min-width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  background-color: #ff4d4f;
  color: white;
  border-radius: 20rpx;
  font-size: 24rpx;
  text-align: center;
  padding: 0 10rpx;
}

.message-list {
  height: calc(100vh - 200rpx);
  padding: 20rpx;
}

.message-item {
  display: flex;
  background-color: white;
  border-radius: 10rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.message-item.read {
  opacity: 0.7;
}

.message-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background-color: #ff4d4f;
  margin-right: 20rpx;
  margin-top: 20rpx;
}

.message-content {
  flex: 1;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.message-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.message-time {
  font-size: 24rpx;
  color: #999;
}

.message-body {
  font-size: 32rpx;
  color: #666;
  margin-bottom: 10rpx;
  line-height: 1.5;
}

.message-type {
  font-size: 28rpx;
  color: #007aff;
}

.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 32rpx;
}
</style>
