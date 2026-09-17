<template>
  <view class="page">
    <!-- AI助手悬浮入口（爱宝） -->
    <aibao-float />

    <!-- 顶部导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      
      <view class="nav-content">
        <view class="nav-left">
          <view class="nav-back" @tap="onBack">
              <text class="back-icon">‹</text>
            </view>
          <image class="nav-logo" src="/static/logo.png" mode="aspectFit"></image>
        </view>
        <view class="nav-center">
          <text class="nav-dept-name">{{deptName}}</text>
        </view>
        <view class="nav-right">
          <image class="nav-logo2" src="/static/logo2.png" mode="aspectFit"></image>
        </view>
      </view>
    </view>

    <!-- 状态筛选按钮区域 -->
    <scroll-view
      scroll-x
      class="state-filter-scroll"
      scroll-with-animation
      :show-scrollbar="false"
    >
      <view class="state-filter">
        <!-- 全部按钮 -->
        <view
          class="state-btn"
          :class="{ 'active': selectedState === '' }"
          @tap="onStateFilter('')"
        >
          <text class="btn-text">全部</text>
          <text class="btn-count">{{ calculatedStateCount['全部'] }}</text>
        </view>
        <!-- 动态生成状态按钮 -->
        <view
          v-for="state in stateDict"
          :key="state.code"
          class="state-btn"
          :class="{ 'active': selectedState === state.name }"
          @tap="onStateFilter(state.name)"
        >
          <text class="btn-text">{{ state.name }}</text>
          <text class="btn-count">{{ calculatedStateCount[state.name] || 0 }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          placeholder="搜索任务名称"
          v-model="searchKeyword"
          @input="onSearchInput"
        />
        <text class="clear-icon" v-if="searchKeyword" @tap="onClearSearch">✕</text>
      </view>
    </view>

    <!-- 安全区域容器，所有内容都放在里面 -->
    <view class="safe-content">
      <!-- 任务列表 -->
      <view class="tasks-container">
        <view 
          v-for="(item, index) in processedTasks" 
          :key="item.id"
          class="task-item-wrapper"
          :style="{'--animation-delay': `${index * 100}ms`}"
        >
          <view 
            class="task-strip-card"
            :style="{
              '--bg-color': item.severityColor,
              '--animation-delay': `${index * 100}ms`
            }"
            @tap="onTaskClick(index)"
          >
            <view class="task-strip-content">
              <view class="task-strip-header">
                <view class="task-strip-main">
                  <text class="task-strip-name">{{item.taskName}}</text>
                  <view class="task-strip-tags">
                    <view class="task-strip-state" :class="item.stateClass">
                      {{item.state}}
                    </view>
                    <view class="task-strip-severity" :class="item.severityClassName">
                      {{item.severityName}}
                    </view>
                  </view>
                </view>
              </view>
              <view class="task-strip-meta">
                <view class="task-strip-info">
                  <text class="task-strip-type">{{item.typeIcon}} {{item.taskType}}</text>
                  <text class="task-strip-date">{{item.formattedStartDate}} - {{item.formattedEndDate}}</text>
                  <text class="task-strip-people">创建人: {{item.createBy || item.uid || '未知'}}</text>
                  <text class="task-strip-people">负责人: {{item.head || '未知'}}</text>
                </view>
                <view class="task-strip-progress">
                  <text class="task-strip-progress-text">{{item.progress}}</text>
                  <view class="task-strip-progress-bar">
                    <view class="task-strip-progress-fill" :style="{'--progress-width': item.progress, width: item.progress}"></view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view class="empty-tip" v-if="processedTasks.length === 0">
          <text>暂无任务</text>
        </view>
      </view>
      
      <!-- 底部导航栏 -->
      <BottomNav />
    </view>
  </view>
</template>

<script>
import { mockServerityDict, mockTaskTypeDict } from '@/mock/dict.js'
import BottomNav from '@/components/BottomNav.vue'
import { taskApi } from '@/api'

export default {
  data() {
    // 获取当前日期
    const today = new Date()
    const currentDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

    return {
      deptCode: '',
      deptName: '',
      did: '', // 部门ID
      tasks: [],
      searchKeyword: '',
      // 日期选择相关
      selectedDate: '',
      // 状态字典
      stateDict: [
        { code: '01', name: '已完成' },
        { code: '02', name: '进行中' },
        { code: '03', name: '已撤销' },
        { code: '04', name: '待开始' }
      ],
      // 当前选中的状态，空字符串表示全部
      selectedState: '',
      // 状态任务计数
      stateCount: {
        '全部': 0,
        '已完成': 0,
        '进行中': 0,
        '已撤销': 0,
        '待开始': 0
      },
      // 重要程度字典
      severityDictionary: [
        { code: '01', name: '日常' },
        { code: '02', name: '常规' },
        { code: '03', name: '重要' },
        { code: '04', name: '紧急' }
      ],
      // 任务类型字典
      taskTypeDictionary: [],
      // 用户信息
      userInfo: uni.getStorageSync('userInfo') || {}
    }
  },

  async onLoad(options) {
    // 调试：打印接收到的参数
    console.log('Received options:', options)

    // 加载任务类型字典
    try {
      this.taskTypeDictionary = await mockTaskTypeDict()
    } catch (error) {
      console.error('加载任务类型字典失败:', error)
    }

    // 获取从index页面传递的科室信息
    this.deptCode = options.deptCode || '01'
    this.deptName = options.deptName || '默认科室'
    this.did = options.did || ''

    // 无论如何，都根据科室code设置科室名称，确保有值
    const deptNames = {
      '01': '工办',
      '02': '研办',
      '03': '科研办',
      '04': '院办'
    }
    this.deptName = deptNames[this.deptCode] || '未知科室'

    // 获取用户信息
    this.userInfo = uni.getStorageSync('userInfo') || {}

    // 调试：打印最终的deptName和did
    console.log('Final deptName:', this.deptName)
    console.log('Final did:', this.did)
    console.log('Current user info:', this.userInfo)

    // 调用后端接口获取部门任务
    this.getTasks()
  },

  methods: {
    // 获取任务列表
    async getTasks() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        console.log('调用getTasks方法，参数:', {
          tid: tid,
          did: this.did
        })

        // 调用后端接口获取部门任务列表
        const res = await taskApi.getPlanByDepartment({
          tid: tid,
          did: this.did
        })

        console.log('getAllPlanByDepartment接口返回结果:', res)

        if (res.code === '200') {
          // 从API获取的任务列表，根据did过滤
          const allTasks = res.data || []
          // 确保只显示当前部门的任务
          this.tasks = allTasks.filter(task => {
            // 处理did的类型转换，确保比较正确
            return String(task.did) === String(this.did)
          })
          console.log('获取到的任务列表:', this.tasks)
          // 获取任务列表后，同步获取状态数量
          this.getStateCounts()
        }
      } catch (error) {
        console.error('获取任务列表失败:', error)
      }
    },

    // 获取各状态任务数量
    async getStateCounts() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''
        const did = this.did || ''

        console.log('调用部门状态数量接口，tid:', tid, 'did:', did)

        // 同时调用四个状态数量接口
        const [completedRes, doingRes, cancelRes, prepareRes] = await Promise.all([
          taskApi.getCompletedPlanCount({ tid, did }),
          taskApi.getDoingPlanCount({ tid, did }),
          taskApi.getCancelledPlanCount({ tid, did }),
          taskApi.getPreparePlanCount({ tid, did })
        ])

        console.log('部门状态数量接口响应:', {
          completedRes,
          doingRes,
          cancelRes,
          prepareRes
        })

        // 更新状态数量
        this.stateCount = {
          '全部': this.tasks.length,
          '已完成': completedRes.code === '200' ? completedRes.data : 0,
          '进行中': doingRes.code === '200' ? doingRes.data : 0,
          '已撤销': cancelRes.code === '200' ? cancelRes.data : 0,
          '待开始': prepareRes.code === '200' ? prepareRes.data : 0
        }
      } catch (error) {
        console.error('获取状态数量失败:', error)
      }
    },

    // 日期选择相关方法
    onDateChange(e) {
      this.selectedDate = e.detail.value
    },

    // 清除日期筛选
    clearDateFilter() {
      this.selectedDate = ''
    },

    onSearchInput() {
      // 搜索输入时自动更新filteredTasks
      this.searchTasks()
    },

    onClearSearch() {
      this.searchKeyword = ''
      this.getTasks() // 清除搜索后重新获取全部任务
    },

    // 搜索任务
    async searchTasks() {
      if (!this.searchKeyword) {
        this.getTasks() // 无搜索关键词时获取全部任务
        return
      }

      try {
        // 调用后端搜索接口
        const res = await taskApi.searchPlanByCondition({
          name: this.searchKeyword, // 后端搜索接口使用name字段
          did: this.did
        })

        if (res.code === '200') {
          this.tasks = res.data || []
        } else {
          uni.showToast({
            title: '搜索失败',
            icon: 'none'
          })
        }
      } catch (error) {
        console.error('搜索任务失败:', error)
        uni.showToast({
          title: '网络错误',
          icon: 'none'
        })
      }
    },

    // 状态筛选按钮点击事件
    onStateFilter(state) {
      this.selectedState = state
    },

    // 查看任务详情
    onTapView(index) {
      // 使用filteredTasks而不是processedTasks，因为filteredTasks包含原始任务数据
      const task = this.filteredTasks[index]

      // 跳转到任务详情页面
      uni.navigateTo({
        url: `/pages/task-detail/task-detail?task=${encodeURIComponent(JSON.stringify(task))}`
      })
    },

    // 任务卡片点击事件
    async onTaskClick(index) {
      const task = this.processedTasks[index];
      const taskId = task.id || task.pid;

      if (!taskId) {
        uni.showToast({ title: '任务ID不存在', icon: 'none' })
        return
      }

      // 跳转到任务详情页面
      uni.showLoading({ title: '加载任务数据...' })

      try {
        const res = await taskApi.searchPlanByCondition({ id: taskId })
        uni.hideLoading()

        if (res.code === '200') {
          const taskData = res.data
          if (Array.isArray(taskData) && taskData.length > 0) {
            const encodedTask = encodeURIComponent(JSON.stringify(taskData[0]));
            uni.navigateTo({
              url: `/pages/task-detail/task-detail?task=${encodedTask}`
            })
          } else {
            uni.showToast({ title: '未找到任务数据', icon: 'none' })
          }
        } else {
          const encodedTask = encodeURIComponent(JSON.stringify(task));
          uni.navigateTo({
            url: `/pages/task-detail/task-detail?task=${encodedTask}`
          })
        }
      } catch (error) {
        uni.hideLoading()
        const encodedTask = encodeURIComponent(JSON.stringify(task));
        uni.navigateTo({
          url: `/pages/task-detail/task-detail?task=${encodedTask}`
        })
      }
    },

    // 获取滑动操作按钮配置
    swipeOptions() {
      return [
        {
          text: '查看',
          style: {
            backgroundColor: '#1890ff'
          },
          value: 'view'
        }
      ]
    },

    // 滑动按钮点击事件
    onSwipeClick(event, taskIndex) {
      const buttonIndex = event.index !== undefined ? event.index : event;

      if (buttonIndex === 0) {
        // 查看按钮
        this.onTaskClick(taskIndex);
      }
    },

    // 滑动打开事件
    onSwipeOpen(index) {
      console.log('滑动打开', index)
    },

    // 滑动关闭事件
    onSwipeClose(index) {
      console.log('滑动关闭', index)
    },

    // 返回按钮点击事件，返回index界面
    onBack() {
      uni.navigateBack()
    }
  },

  computed: {
    // 筛选任务，同时支持关键词和状态筛选
    filteredTasks() {
      const { tasks, searchKeyword, selectedState } = this

      return tasks.filter(task => {
        // 关键词筛选
        const keywordMatch = !searchKeyword ||
          (task.name && task.name.toLowerCase().includes(searchKeyword.toLowerCase()))

        // 状态筛选
        const stateMatch = !selectedState || task.status === selectedState

        return keywordMatch && stateMatch
      })
    },

    // 处理任务数据，添加计算属性
    processedTasks() {
      return this.filteredTasks.map(task => {
        console.log('单个任务数据:', task) // 调试日志

        // 格式化日期
        const formatDate = (dateStr) => {
          if (!dateStr) return ''
          const date = new Date(dateStr)
          return `${date.getMonth() + 1}月${date.getDate()}日`
        }

        // 根据任务状态code获取名称
        const getStateName = (stateCode) => {
          const stateMap = {
            '01': '已完成',
            '02': '进行中',
            '03': '已撤销',
            '04': '待开始'
          }
          return stateMap[stateCode] || stateCode
        }

        // 根据任务重要程度code获取名称
        const getSeverityName = (severityCode) => {
          const severityMap = {
            '01': '重要',
            '02': '常规',
            '03': '不重要',
            '04': '日常'
          }
          return severityMap[severityCode] || severityCode
        }

        // 根据任务类型code获取名称
        const getTaskTypeName = (typeCode) => {
          const typeItem = this.taskTypeDictionary.find(item => item.code === typeCode)
          return typeItem ? typeItem.name : typeCode
        }

        // 根据任务重要程度获取颜色
        const getSeverityColor = (severityCode) => {
          const colorMap = {
            '04': '#ff4d4f', // 紧急
            '03': '#ff7a45', // 重要
            '02': '#faad14', // 常规
            '01': '#52c41a'  // 日常
          }
          return colorMap[severityCode] || '#595959'
        }

        // 根据任务状态获取CSS类
        const getStateClass = (stateName) => {
          const classMap = {
            '已完成': 'state-completed',
            '进行中': 'state-progress',
            '已撤销': 'state-overdue',
            '待开始': 'state-pending',
            '已延期': 'state-overdue'
          }
          return classMap[stateName] || 'state-default'
        }

        // 根据任务类型获取图标
        const getTypeIcon = (typeName) => {
          const iconMap = {
            '本科生培养': '[培养]',
            '前期培养': '[前期]',
            '人才引进': '[人才]',
            '教学任务': '[教学]',
            '科研申报': '[科研]',
            '人才培养': '[培养]',
            '研究生培养': '[培养]',
            '其他': '[其他]'
          }
          return iconMap[typeName] || '[其他]'
        }

        // 清除HTML标签和URL，只保留纯文本
        const stripHtmlAndUrls = (html) => {
          if (!html) return ''
          // 移除HTML标签
          let text = html.replace(/<[^>]*>/g, '')
          // 移除URL
          text = text.replace(/https?:\/\/[^\s]+/g, '')
          // 移除乱码和特殊字符
          text = text.replace(/[\r\n\t]+/g, ' ')
          text = text.replace(/\s+/g, ' ')
          return text.trim()
        }

        // 截断任务描述
        const truncateDescription = (description) => {
          if (!description) return ''
          const plainText = stripHtmlAndUrls(description)
          if (plainText.length <= 100) {
            return plainText
          }
          return plainText.substring(0, 100) + '...'
        }

        // 根据重要程度获取对应的CSS类名
        const getSeverityClassName = (severityCode) => {
          const classMap = {
            '04': 'severity-emergency', // 紧急
            '03': 'severity-high',      // 重要
            '02': 'severity-medium',    // 常规
            '01': 'severity-low'        // 日常
          }
          return classMap[severityCode] || ''
        }

        // 获取正确的名称
        const taskTypeName = getTaskTypeName(task.type)
        const stateName = getStateName(task.status)
        const severityName = getSeverityName(task.imp)

        // 使用后端返回的username字段作为创建人名称
        const creatorName = task.username || `用户${task.uid || task.createBy}`
        // 负责人名称直接使用head字段，因为后端返回的head已经是名称
        const headName = task.head || '未知'

        return {
          ...task,
          taskName: task.name, // 后端返回的是name，前端模板使用的是taskName
          state: stateName, // 后端返回的是status（code），前端模板使用的是state（name）
          taskType: taskTypeName, // 后端返回的是type（code），前端模板使用的是taskType（name）
          description: stripHtmlAndUrls(task.detail), // 后端返回的是detail，前端模板使用的是description，清除HTML标签和URL
          formattedStartDate: formatDate(task.startTime),
          formattedEndDate: formatDate(task.endTime),
          severityColor: getSeverityColor(task.imp),
          stateClass: getStateClass(stateName),
          typeIcon: getTypeIcon(taskTypeName),
          shortDescription: truncateDescription(task.detail),
          severityClassName: getSeverityClassName(task.imp),
          severityName: severityName, // 根据code获取显示名称
          createBy: creatorName, // 显示创建人名称
          head: headName, // 显示负责人名称
          progress: `${task.progress || 0}%` // 确保进度值带百分比符号
        }
      })
    },

    // 计算状态任务计数
    calculatedStateCount() {
      const count = {
        '全部': this.filteredTasks.length
      }

      // 初始化所有状态的计数为0
      this.stateDict.forEach(state => {
        count[state.name] = 0
      })

      // 统计各状态的任务数量
      this.filteredTasks.forEach(task => {
        // 后端返回的是status字段，直接使用任务的status属性
        const taskState = task.status || '未知状态'
        if (count[taskState] !== undefined) {
          count[taskState]++
        } else {
          // 如果任务状态不在字典中，也统计
          count[taskState] = (count[taskState] || 0) + 1
        }
      })

      return count
    },
    
    // 跳转到创建任务页面
    goToCreateTask() {
      uni.navigateTo({
        url: '/pages/declaration/declaration'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 统一样式变量 */
$primary-gradient: linear-gradient(135deg, #1890ff 0%, #52c41a 100%);
$primary-color: #1890ff;
$primary-light: rgba(24, 144, 255, 0.1);
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$bg-primary: #ffffff;
$bg-base: #f5f7fa;
$border-base: #e4e7ed;
$border-lighter: #f2f6fc;
$radius-base: 12rpx;
$create-btn-color: #3f5dfd;
$radius-lg: 24rpx;
$spacing-base: 20rpx;
$spacing-lg: 32rpx;
$shadow-sm: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
$shadow-base: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);

.page {
  background: linear-gradient(135deg, #ffffff 0%, #ffffff 100%);
  min-height: 100vh;
  padding-top: env(safe-area-inset-top);
  padding-top: calc(env(safe-area-inset-top) + 200rpx);
  padding-bottom: 150rpx; /* 添加底部内边距，避免内容被底部导航栏遮挡 */
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 自定义导航栏样式 */
.custom-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  /* 毛玻璃叠加微弱渐变 */
  background: #023c99;
  padding: 0 40rpx;
  /* 使用阴影代替边框，视觉更轻盈 */
  box-shadow: 0 4rpx 30rpx rgba(0, 0, 0, 0.03);
  animation: navSlideDown 0.8s cubic-bezier(0.25, 1, 0.5, 1) forwards;
}

.status-bar {
  height: var(--status-bar-height); // 适配系统状态栏高度
  min-height: 40rpx;
}

.nav-content {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 导航栏中间部门名称 */
.nav-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 20rpx;
}

.nav-dept-name {
  font-size: 36rpx;
  font-weight: bold;
  color: white;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300rpx;
}

.nav-left {
  display: flex;
  align-items: center;
}

.nav-logo {
  width: 120rpx;
  height: 120rpx;
  margin-bottom: 30rpx;
  margin-left: -20rpx;
  border-radius: 8rpx;
}

/* 导航栏返回按钮 */
.nav-back {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.back-icon {
  font-size: 48rpx;
  color: white;
  font-weight: bold;
}

/* 优化后的动画 */
@keyframes navSlideDown {
  0% { transform: translateY(-100%); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}

/* 状态筛选滚动容器 */
.state-filter-scroll {
  position: fixed;
  top: 188rpx;
  left: 0;
  right: 0;
  z-index: 99;
  white-space: nowrap;
  background-color: white;
  padding: 10rpx 20rpx;
  box-sizing: border-box;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-bottom: 1rpx solid #e8e8e8;
}

/* 状态筛选按钮区域 */
.state-filter {
  display: flex;
  gap: 12rpx;
  padding: 10rpx 0;
  box-sizing: border-box;
}

/* 状态筛选按钮 */
.state-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 20rpx;
  border: 2rpx solid #e4e7ed;
  border-radius: 50rpx;
  background-color: white;
  font-size: 28rpx;
  color: #606266;
  white-space: nowrap;
  transition: all 0.3s ease;
  height: 60rpx;
  box-sizing: border-box;
}

/* 按钮激活状态 */
.state-btn.active {
  background-color: #007aff;
  border-color: #007aff;
  color: white;
}

/* 搜索栏 */
.search-bar {
  position: fixed;
  top: 268rpx;
  left: 0;
  right: 0;
  z-index: 98;
  padding: 20rpx;
  background-color: white;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  border-bottom: 1rpx solid #e8e8e8;
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
  color: #909399;
  margin-right: 16rpx;
}

.search-input {
  flex: 1;
  height: 48rpx;
  font-size: 28rpx;
  color: #303133;
  background-color: transparent;
}

.clear-icon {
  font-size: 32rpx;
  color: #909399;
  margin-left: 16rpx;
  padding: 8rpx;
  border-radius: 50%;
}

/* 任务容器 */
.tasks-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60rpx;
  padding: 60rpx;
  width: 100%;
  box-sizing: border-box;
}

/* 卡片入场包装器 */
.card-entrance-wrapper {
  animation: cardSlideUp 0.6s ease-out forwards;
  animation-delay: var(--animation-delay, 0ms);
  opacity: 0;
  width: 100%; /* 确保撑满网格 */
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 任务卡片样式 */
.task-card {
  position: relative;
  background: #023c99;
  border-radius: 20rpx;
  width: 100%;
  min-height: 300rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.2);
  cursor: pointer;
  margin-bottom: 20rpx;
  padding: 32rpx;
  box-sizing: border-box;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 任务卡片悬停效果 */
.task-card:hover {
  transform: translateY(-15rpx);
  box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.35);
  z-index: 10;
}

/* 任务信息 */
.task-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

/* 任务头部 */
.task-header {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

/* 任务名称容器 */
.task-name-container {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

/* 任务名称 */
.task-name {
  font-size: 36rpx;
  font-weight: bold;
  color: white;
  line-height: 1.4;
}

/* 任务状态 */
.task-state {
  padding: 8rpx 16rpx;
  border-radius: 50rpx;
  font-size: 24rpx;
  color: white;
  align-self: flex-start;
}

/* 任务元信息 */
.task-meta {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 12rpx;
}

/* 任务类型、日期、人员等信息 */
.task-type, .task-date, .task-person, .task-progress {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
}

/* 进度条容器 */
.progress-container {
  flex: 1;
  height: 40rpx;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  overflow: hidden;
  margin: 0 10rpx;
  position: relative;
}

/* 进度条 */
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  border-radius: 20rpx;
  transition: width 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: absolute;
  left: 0;
  top: 0;
  animation: progressAnimate 1s ease-out forwards;
  width: 0;
}

/* 进度文本 */
.progress-text-inside {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  font-size: 24rpx;
  font-weight: bold;
  color: white;
  z-index: 1;
}

/* 空状态提示 */
.empty-tip {
  text-align: center;
  padding: 200rpx 40rpx;
  color: #909399;
  font-size: 32rpx;
}

/* 卡片入场动画 */
@keyframes cardSlideUp {
  0% {
    opacity: 0;
    transform: translateY(40rpx);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 进度条动画 */
@keyframes progressAnimate {
  0% {
    width: 0;
  }
  100% {
    width: var(--progress-width, 0%);
  }
}

/* 重要程度标签 */
.severity-tag {
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
  color: white;
  align-self: flex-start;
}

/* 不同重要程度的颜色样式 */
.severity-emergency {
  background-color: #ff4d4f;
  border: 1rpx solid #ffccc7;
}

.severity-high {
  background-color: #fa8c16;
  border: 1rpx solid #ffd591;
}

.severity-medium {
  background-color: #52c41a;
  border: 1rpx solid #b7eb8f;
}

.severity-low {
  background-color: #1890ff;
  border: 1rpx solid #91d5ff;
}

/* 任务状态颜色 */
.state-pending {
  background-color: rgba(24, 144, 255, 0.8);
}

.state-progress {
  background-color: rgba(250, 173, 20, 0.8);
}

.state-completed {
  background-color: rgba(82, 196, 26, 0.8);
}

.state-overdue {
  background-color: rgba(255, 77, 79, 0.8);
}/* 状态默认 */
.state-default {
  background-color: rgba(144, 147, 153, 0.8);
}

/* 任务容器 */
.tasks-container {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 32rpx;
  width: 100%;
  box-sizing: border-box;
}

/* 任务项包装器 */
.task-item-wrapper {
  animation: stripSlideUp 0.6s ease-out forwards;
  animation-delay: var(--animation-delay, 0ms);
  opacity: 0;
  width: 100%;
  transform: translateY(20rpx);
}

/* 任务条形卡片样式 */
.task-strip-card {
  position: relative;
  background: linear-gradient(135deg, rgba(2, 60, 153, 0.8) 0%, rgba(0, 86, 179, 0.7) 100%);
  border-radius: 24rpx;
  width: 95%;
  margin: 0 auto;
  min-height: 180rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4rpx 20rpx rgba(2, 60, 153, 0.2);
  cursor: pointer;
  padding: 24rpx;
  box-sizing: border-box;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  backdrop-filter: blur(10rpx);
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

/* 任务条形卡片悬停效果 */
.task-strip-card:hover {
  transform: translateY(-8rpx);
  box-shadow: 0 12rpx 40rpx rgba(2, 60, 153, 0.45);
  z-index: 10;
}

/* 任务卡片内容 */
.task-strip-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  position: relative;
  z-index: 2;
}

/* 任务条形头部 */
.task-strip-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16rpx;
}

/* 任务条形主要信息 */
.task-strip-main {
  flex: 1;
  min-width: 0;
}

/* 任务条形名称 */
.task-strip-name {
  font-size: 32rpx;
  font-weight: bold;
  color: white;
  line-height: 1.3;
  margin-bottom: 12rpx;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 任务条形标签容器 */
.task-strip-tags {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

/* 任务条形状态 */
.task-strip-state {
  padding: 8rpx 16rpx;
  border-radius: 50rpx;
  font-size: 24rpx;
  color: white;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10rpx);
}

/* 任务条形重要程度 */
.task-strip-severity {
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
  color: white;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10rpx);
}

/* 任务条形元信息 */
.task-strip-meta {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

/* 任务条形信息容器 */
.task-strip-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  flex-wrap: wrap;
}

/* 任务条形类型 */
.task-strip-type {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  gap: 8rpx;
}

/* 任务条形日期 */
.task-strip-date {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  gap: 8rpx;
}

/* 任务条形人员信息 */
.task-strip-people {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  gap: 8rpx;
}

/* 任务条形进度 */
.task-strip-progress {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 8rpx;
}

/* 任务条形进度文本 */
.task-strip-progress-text {
  font-size: 24rpx;
  font-weight: bold;
  color: white;
  min-width: 80rpx;
}

/* 任务条形进度条 */
.task-strip-progress-bar {
  flex: 1;
  height: 12rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6rpx;
  overflow: hidden;
  position: relative;
}

/* 任务条形进度填充 */
.task-strip-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  border-radius: 6rpx;
  transition: width 1.2s cubic-bezier(0.4, 0, 0.2, 1);
  animation: stripProgressAnimate 1.5s ease-out forwards;
  position: relative;
  overflow: hidden;
  box-shadow: 0 0 10rpx rgba(24, 144, 255, 0.5);
}

/* 进度填充动画 */
.task-strip-progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.6) 50%, transparent 100%);
  animation: shimmer 1.5s infinite;
}

/* 任务条形入场动画 */
@keyframes stripSlideUp {
  0% {
    opacity: 0;
    transform: translateY(40rpx);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 任务条形进度动画 */
@keyframes stripProgressAnimate {
  0% {
    width: 0;
    opacity: 0;
    transform: scaleX(0.8);
  }
  50% {
    opacity: 1;
    transform: scaleX(1.05);
  }
  100% {
    width: var(--progress-width, 0%);
    opacity: 1;
    transform: scaleX(1);
  }
}

/* 光泽动画 */
@keyframes shimmer {
  0% {
    transform: translateX(-100%) skewX(-20deg);
  }
  100% {
    transform: translateX(100%) skewX(-20deg);
  }
}

/* 不同重要程度的颜色样式 */
.task-strip-severity.severity-emergency {
  background-color: rgba(255, 77, 79, 0.9);
  border: 1rpx solid rgba(255, 204, 199, 0.5);
}

.task-strip-severity.severity-high {
  background-color: rgba(250, 140, 22, 0.9);
  border: 1rpx solid rgba(255, 213, 145, 0.5);
}

.task-strip-severity.severity-medium {
  background-color: rgba(82, 196, 26, 0.9);
  border: 1rpx solid rgba(183, 235, 143, 0.5);
}

.task-strip-severity.severity-low {
  background-color: rgba(24, 144, 255, 0.9);
  border: 1rpx solid rgba(145, 213, 255, 0.5);
}

/* 任务状态颜色 */
.task-strip-state.state-pending {
  background-color: rgba(24, 144, 255, 0.9);
}

.task-strip-state.state-progress {
  background-color: rgba(250, 173, 20, 0.9);
}

.task-strip-state.state-completed {
  background-color: rgba(82, 196, 26, 0.9);
}

.task-strip-state.state-overdue {
  background-color: rgba(255, 77, 79, 0.9);
}

.task-strip-state.state-default {
  background-color: rgba(144, 147, 153, 0.9);
}

/* 装饰效果 */
.task-strip-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 0%, rgba(255, 255, 255, 0.1) 50%, transparent 100%);
  animation: rotate 10s linear infinite;
  z-index: 1;
}

/* 旋转动画 */
@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 750rpx) {
  .task-strip-info {
    grid-template-columns: 1fr;
  }
  
  .task-strip-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .task-strip-name {
    font-size: 32rpx;
  }
  
  .tasks-container {
    padding: 20rpx;
    gap: 16rpx;
  }
  
  .task-strip-card {
    padding: 24rpx;
  }
}

/* 状态筛选滚动容器 */
.state-filter-scroll {
  white-space: nowrap;
  background-color: white;
  padding: 10rpx 20rpx;
  box-sizing: border-box;
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-bottom: 1rpx solid #e8e8e8;
}

/* 状态筛选按钮区域 */
.state-filter {
  display: flex;
  gap: 12rpx;
  min-height: 60rpx;
  align-items: center;
}

/* 状态筛选按钮 */
.state-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 20rpx;
  border: 2rpx solid #e4e7ed;
  border-radius: 50rpx;
  background-color: white;
  font-size: 28rpx;
  color: #606266;
  white-space: nowrap;
  transition: all 0.3s ease;
  height: 60rpx;
  box-sizing: border-box;
}

/* 按钮激活状态 */
.state-btn.active {
  background-color: #007aff;
  border-color: #007aff;
  color: white;
}

/* 按钮文本 */
.btn-text {
  font-weight: 500;
}

/* 按钮计数 */
.btn-count {
  font-size: 24rpx;
  padding: 2rpx 10rpx;
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 20rpx;
  min-width: 32rpx;
  text-align: center;
}

/* 激活状态的计数 */
.state-btn.active .btn-count {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 搜索栏 */
.search-bar {
  padding: 20rpx;
  background-color: white;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 50rpx;
  padding: 12rpx 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.search-icon {
  font-size: 32rpx;
  color: #909399;
  margin-right: 16rpx;
}

.search-input {
  flex: 1;
  height: 48rpx;
  font-size: 28rpx;
  background-color: transparent;
  color: #303133;
}

.clear-icon {
  font-size: 32rpx;
  color: #909399;
  margin-left: 16rpx;
  padding: 8rpx;
  border-radius: 50%;
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(144, 147, 153, 0.1);
}

/* 安全区域容器 */
.safe-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-top: 340rpx;
  padding-bottom: 30rpx;
  box-sizing: border-box;
  width: 100%;
}

/* 任务列表 */
.task-list {
  flex: 1;
  height: 0;
  padding: 60rpx 20rpx;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24rpx 30rpx;
  border-radius: 0;
  margin-bottom: 0;
  box-shadow: none;
  transition: all 0.3s ease;
  position: relative;
  border: none;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
  margin-right: 0;
  width: 100%;
  box-sizing: border-box;
}

.task-gray {
  background-color: #f8f8f8;
}

.task-beige {
  background-color: #fff8f0;
}

.task-info {
  flex: 1;
  margin-right: 20rpx;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15rpx;
  flex-wrap: wrap;
}

.task-name-container {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex: 1;
  margin-right: 20rpx;
  flex-wrap: wrap;
}

.task-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  line-height: 1.4;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 重要程度标识样式 */
.severity-tag {
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
  font-weight: 500;
  white-space: nowrap;
}

/* 不同重要程度的颜色样式 */
.severity-emergency {
  background-color: #fff1f0;
  color: #ff4d4f;
  border: 1rpx solid #ffccc7;
}

.severity-high {
  background-color: #fff7e6;
  color: #fa8c16;
  border: 1rpx solid #ffd591;
}

.severity-medium {
  background-color: #f6ffed;
  color: #52c41a;
  border: 1rpx solid #b7eb8f;
}

.severity-low {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1rpx solid #91d5ff;
}

.task-state {
  padding: 12rpx 24rpx;
  border-radius: 50rpx;
  font-size: 24rpx;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
}

.view-btn {
  padding: 15rpx 30rpx;
  background-color: #007aff;
  color: white;
  border-radius: 50rpx;
  font-size: 28rpx;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.task-type, .task-date, .task-progress {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.task-progress {
  width: 100%;
  display: flex;
  align-items: center;
  margin-top: 15rpx;
  gap: 8rpx;
}

.type-icon, .date-icon, .progress-icon {
  font-size: 24rpx;
}

/* 进度条样式 */
.progress-container {
  flex: 1;
  height: 40rpx;
  background-color: rgba(0, 0, 0, 0.08);
  border-radius: 20rpx;
  margin: 0 10rpx;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  border-radius: 20rpx;
  transition: width 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: absolute;
  left: 0;
  top: 0;
  animation: progressAnimate 1s ease-out forwards;
  width: 0;
  background-size: 200% 100%;
}

/* 进度条动画 */
@keyframes progressAnimate {
  0% {
    width: 0;
    background-position: 0% 0%;
  }
  50% {
    background-position: 100% 0%;
  }
  100% {
    background-position: 200% 0%;
  }
}

.progress-text-inside {
  font-size: 24rpx;
  color: black;
  font-weight: bold;
  position: relative;
  z-index: 1;
  min-width: 60rpx;
  text-align: center;
  animation: textFadeIn 0.5s ease-out forwards;
  animation-delay: 0.3s;
  opacity: 0;
}

/* 进度文本动画 */
@keyframes textFadeIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 任务项动画效果 */
.task-animate {
  opacity: 0;
  transform: translateY(20rpx);
  animation: taskFadeIn 0.5s ease-out forwards;
  animation-delay: calc(var(--item-index) * 0.1s);
}

@keyframes taskFadeIn {
  0% {
    opacity: 0;
    transform: translateY(20rpx);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 任务状态颜色 */
.state-pending {
  background-color: #e6f7ff;
  color: #1890ff;
}

.state-progress {
  background-color: #fffbe6;
  color: #faad14;
}

.state-completed {
  background-color: #f6ffed;
  color: #52c41a;
}

.state-overdue {
  background-color: #fff2f0;
  color: #ff4d4f;
}

.state-default {
  background-color: #f5f5f5;
  color: #595959;
}

.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 32rpx;
}



.btn-icon {
  color: white;
  font-size: 80rpx;
  font-weight: 300;
  line-height: 1;
}
</style>
