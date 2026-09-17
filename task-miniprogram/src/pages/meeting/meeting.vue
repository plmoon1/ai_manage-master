<template>
  <view class="page">
    <!-- 顶部导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      <view class="nav-content">
        <view class="nav-logo-wrapper">
          <image class="nav-logo" src="/static/logo.png" mode="aspectFit"></image>
        </view>
        <view class="nav-text-wrapper">
          <text class="college-name">{{ collegeName }}</text>
        </view>
      </view>
    </view>

    <!-- 搜索和筛选区域 -->
    <view class="search-filter-section">
      <!-- 搜索框容器 -->
      <view class="search-container">
        <view class="search-bar-wrapper">
          <!-- 搜索框 -->
          <view class="search-input-wrapper">
            <text class="search-icon">🔍</text>
            <input
              class="search-input"
              type="text"
              placeholder="搜索会议名称"
              v-model="searchKeyword"
              @confirm="handleSearch"
              placeholder-class="search-placeholder"
            />
          </view>

          <!-- 筛选按钮 -->
          <view class="filter-btn" @tap="showFilterPopup">
            <view class="filter-icon">
              <view class="filter-line line-1"></view>
              <view class="filter-line line-2"></view>
              <view class="filter-line line-3"></view>
            </view>
          </view>
        </view>
      </view>

      <!-- 日期筛选 -->
      <scroll-view
        class="state-filter-scroll"
        scroll-x
        scroll-with-animation
        :show-scrollbar="false"
      >
        <view class="filter-row">
          <!-- 全部会议按钮 -->
          <view
            class="today-meeting-btn small-btn"
            :class="{ 'active': !isTodayMeeting && !isTomorrowMeeting && !isWeekMeeting && !isMonthMeeting }"
            @tap="selectAllMeetings"
          >
            <text class="today-text">全部</text>
          </view>

          <view class="filter-divider"></view>

          <!-- 今日会议按钮 -->
          <view
            class="today-meeting-btn small-btn"
            :class="{ 'active': isTodayMeeting }"
            @tap="selectTodayMeeting"
          >
            <text class="today-text">今日</text>
          </view>

          <view class="filter-divider"></view>

          <!-- 明日会议按钮 -->
          <view
            class="today-meeting-btn small-btn"
            :class="{ 'active': isTomorrowMeeting }"
            @tap="selectTomorrowMeeting"
          >
            <text class="today-text">明日</text>
          </view>

          <view class="filter-divider"></view>

          <!-- 本周会议按钮 -->
          <view
            class="today-meeting-btn small-btn"
            :class="{ 'active': isWeekMeeting }"
            @tap="selectWeekMeeting"
          >
            <text class="today-text">本周</text>
          </view>

          <view class="filter-divider"></view>

          <!-- 本月会议按钮 -->
          <view
            class="today-meeting-btn small-btn"
            :class="{ 'active': isMonthMeeting }"
            @tap="selectMonthMeeting"
          >
            <text class="today-text">本月</text>
          </view>

          <!-- 会议总数徽章 -->
          <view class="total-count-badge">
            <text class="total-count-text">{{ currentMeetings.length }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 筛选弹窗 -->
    <view class="filter-popup" :class="{ 'show': showFilter }">
      <view class="filter-overlay" @tap="hideFilterPopup"></view>
      <view class="filter-content">
        <view class="filter-header">
          <text class="filter-title">筛选</text>
          <text class="filter-reset" @tap="resetFilter">重置</text>
        </view>

        <scroll-view class="filter-body" scroll-y>
          <!-- 时间范围 -->
          <view class="filter-item date-range-filter">
            <text class="filter-label">时间范围</text>
            <view class="date-range-pickers">
              <view class="date-range-picker" @tap="showStartDatePicker">
                <view class="date-range-value" :class="{ 'has-value': filterStartDate }">
                  {{ filterStartDate || '开始日期' }}
                </view>
              </view>
              <text class="date-separator">-</text>
              <view class="date-range-picker" @tap="showEndDatePicker">
                <view class="date-range-value" :class="{ 'has-value': filterEndDate }">
                  {{ filterEndDate || '结束日期' }}
                </view>
              </view>
              <text v-if="filterStartDate || filterEndDate" class="date-clear" @tap.stop="clearDateRangeFilter">×</text>
            </view>
          </view>

          <!-- 排序方式 -->
          <view class="filter-item" @tap="toggleFilterItem('sort')">
            <text class="filter-label">排序方式</text>
            <view class="filter-value-row">
              <text class="filter-value">{{ getFilterSortText() }}</text>
              <text class="filter-arrow">{{ expandedFilter === 'sort' ? '︿' : '︾' }}</text>
            </view>
          </view>
          <view class="filter-options" :class="{ 'show': expandedFilter === 'sort' }">
            <view
              v-for="sort in sortOptions"
              :key="sort.value"
              class="filter-option-item"
              :class="{ 'active': filterSort === sort.value }"
              @tap="selectFilterSort(sort.value)"
            >
              <text class="option-item-text">{{ sort.name }}</text>
              <text v-if="filterSort === sort.value" class="option-item-check">✓</text>
            </view>
          </view>
        </scroll-view>

        <view class="filter-footer">
          <view class="filter-confirm-btn" @tap="applyFilter">
            <text class="confirm-btn-text">确定</text>
          </view>
        </view>
      </view>
    </view>

    <!-- uView 日期时间选择器 -->
    <u-datetime-picker
      :show="showStartDatePickerFlag"
      v-model="startDateValue"
      mode="date"
      :max-date="endDatePickerMax"
      @cancel="hideStartDatePicker"
      @confirm="confirmStartDate"
      :closeOnClickOverlay="true"
      @close="hideStartDatePicker"
      :z-index="999999"
    ></u-datetime-picker>

    <u-datetime-picker
      :show="showEndDatePickerFlag"
      v-model="endDateValue"
      mode="date"
      :min-date="startDatePickerMin"
      @cancel="hideEndDatePicker"
      @confirm="confirmEndDate"
      :closeOnClickOverlay="true"
      @close="hideEndDatePicker"
      :z-index="999999"
    ></u-datetime-picker>

    <!-- 会议列表 -->
    <view class="meetings-section">
      <RefreshScrollView
        class="meetings-scroll"
        @refresh="onRefresh"
      >
        <view class="meetings-container">
        <view v-if="currentMeetings.length === 0 && !isLoading" class="meeting-empty">
          <text class="empty-icon">📅</text>
          <text class="empty-text">暂无会议</text>
        </view>
        <view
          v-for="(item, index) in processedMeetings"
          :key="item.id || index"
          class="meeting-item-wrapper"
        >
          <view
            class="meeting-card"
            @tap="goToMeetingDetail(item)"
          >
            <!-- 左侧：会议名称和状态 -->
            <view class="meeting-left">
              <text class="meeting-name">{{ item.meetingName || '未命名会议' }}</text>
              <view class="meeting-tags">
                <view class="meeting-tag state-tag" :class="item.stateClass">
                  {{ item.state }}
                </view>
                <view
                  v-for="(tag, tagIndex) in item.typeTags"
                  :key="tagIndex"
                  class="meeting-tag type-tag"
                  :class="tag.class"
                >
                  {{ tag.text }}
                </view>
              </view>
            </view>

            <!-- 右侧：详细信息 -->
            <view class="meeting-right">
              <view class="meeting-info-row">
                <text class="meeting-info-label">日期</text>
                <text class="meeting-info-value">{{ item.meetingDate || '待定' }}</text>
              </view>
              <view class="meeting-info-row">
                <text class="meeting-info-label">开始</text>
                <text class="meeting-info-value">{{ item.startTime || '待定' }}</text>
              </view>
              <view class="meeting-info-row">
                <text class="meeting-info-label">结束</text>
                <text class="meeting-info-value">{{ item.endTime || '待定' }}</text>
              </view>
              <view class="meeting-info-row">
                <text class="meeting-info-label">地点</text>
                <text class="meeting-info-value">{{ item.location || '待定' }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载状态提示 -->
        <view v-if="isLoading" class="loading-more">
          <text class="loading-text">加载中...</text>
        </view>
        <view v-else-if="!hasMore && allMeetings.length > 0" class="no-more">
          <text class="no-more-text">没有更多了</text>
        </view>
      </view>
      </RefreshScrollView>
    </view>

    <!-- 返回顶部按钮 -->
    <view
      v-if="showBackToTop"
      class="back-to-top"
      @tap="backToTop"
    >
      <text class="back-to-top-icon">↑</text>
    </view>

    <!-- 底部导航栏 -->
    <BottomNav />

    <!-- 浮动新增按钮 -->
    <view class="floating-add-btn" @tap="goToAddMeeting">
      <text class="add-icon">+</text>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'
import { meetingApi } from '@/api/index.js'
import { getAllStoredDates } from '@/utils/dateHelper.js'
import RefreshScrollView from '@/components/RefreshScrollView.vue'
import refreshManager from '@/utils/refreshManager'
import { cleanupPendingRequests } from '@/utils/request'

export default {
  components: {
    RefreshScrollView
  },
  data() {
    return {
      pageId: 'meeting_page', // 页面唯一标识
      collegeName: '会议管理',
      searchKeyword: '',
      selectedStatus: '',
      isTodayMeeting: false, // 是否选中今日会议
      isTomorrowMeeting: false, // 是否选中明日会议
      isWeekMeeting: false, // 是否选中本周会议
      isMonthMeeting: false, // 是否选中本月会议
      appliedFilterStartDate: '', // 应用的筛选开始日期
      appliedFilterEndDate: '', // 应用的筛选结束日期
      todayDateStr: '', // 今天的日期字符串
      tomorrowDateStr: '', // 明天的日期字符串
      weekStartStr: '', // 本周开始日期字符串
      weekEndStr: '', // 本周结束日期字符串
      monthStartStr: '', // 本月开始日期字符串
      monthEndStr: '', // 本月结束日期字符串
      statusOptions: [],
      allMeetings: [],
      isLoading: false,
      hasMore: true,
      currentPage: 1,
      pageSize: 100,
      showBackToTop: false,
      // 是否是初始化加载
      isInitialLoad: true,
      // 优化：防止过度刷新
      isInitialized: false,    // 是否已初始化加载过数据
      lastRefreshTime: null,    // 上次刷新时间
      // 后端API相关
      totalPages: 1,
      totalCount: 0,
      // 筛选弹窗相关
      showFilter: false,
      filterStartDate: '',
      filterEndDate: '',
      showStartDatePickerFlag: false,
      showEndDatePickerFlag: false,
      startDateValue: new Date().getTime(),
      endDateValue: new Date().getTime(),
      startDatePickerMin: new Date().getTime(),
      endDatePickerMin: new Date().getTime(),
      endDatePickerMax: new Date().getTime() + 365 * 24 * 60 * 60 * 1000,
      // 排序方式
      sortOptions: [
        { value: 'default', name: '默认排序' },
        { value: 'createTime', name: '创建时间' },
        { value: 'startTime', name: '开始时间' }
      ],
      filterSort: 'default',
      expandedFilter: null
    }
  },

  computed: {
    // 用于显示的会议列表（仅支持前端搜索和状态筛选）
    currentMeetings() {
      let meetings = [...this.allMeetings]

      // 关键词搜索
      if (this.searchKeyword && this.searchKeyword.trim()) {
        const keyword = this.searchKeyword.trim().toLowerCase()
        meetings = meetings.filter(meeting =>
          (meeting.meetingName && meeting.meetingName.toLowerCase().includes(keyword)) ||
          (meeting.location && meeting.location.toLowerCase().includes(keyword)) ||
          (meeting.host && meeting.host.toLowerCase().includes(keyword))
        )
      }

      // 状态筛选
      if (this.selectedStatus) {
        meetings = meetings.filter(meeting => meeting.status === this.selectedStatus)
      }

      return meetings
    },

    processedMeetings() {
      if (!this.currentMeetings || this.currentMeetings.length === 0) {
        return []
      }

      return this.currentMeetings.map(meeting => {
        // 格式化日期
        const formatMeetingDate = (dateStr) => {
          if (!dateStr) return '待定'
          try {
            const date = new Date(dateStr)
            if (isNaN(date.getTime())) return dateStr // 如果无法解析，直接返回原字符串
            const month = date.getMonth() + 1
            const day = date.getDate()
            return `${month}月${day}日`
          } catch (e) {
            return dateStr || '待定'
          }
        }

        // 格式化时间
        const formatMeetingTime = (startTime, endTime) => {
          if (!startTime) return '待定'

          // 处理时间格式 HH:mm:ss 或 HH:mm
          const formatTime = (timeStr) => {
            if (!timeStr) return ''
            // 如果已经是简洁格式，直接返回
            if (timeStr.match(/^\d{1,2}:\d{2}$/)) {
              return timeStr
            }
            // 如果包含秒，去掉秒
            if (timeStr.match(/^\d{1,2}:\d{2}:\d{2}$/)) {
              return timeStr.substring(0, 5)
            }
            return timeStr
          }

          const start = formatTime(startTime)
          const end = endTime ? formatTime(endTime) : ''

          return end ? `${start}-${end}` : start
        }

        // 格式化单个时间
        const formatSingleTime = (timeStr) => {
          if (!timeStr) return '待定'

          // 如果是"上午08:30"或"下午14:30"这种格式，提取时间部分
          const timeMatch = timeStr.match(/(\d{1,2}:\d{2})/)
          if (timeMatch) {
            return timeMatch[1]
          }

          // 如果已经是简洁格式，直接返回
          if (timeStr.match(/^\d{1,2}:\d{2}$/)) {
            return timeStr
          }

          // 如果包含秒，去掉秒
          if (timeStr.match(/^\d{1,2}:\d{2}:\d{2}$/)) {
            return timeStr.substring(0, 5)
          }

          return timeStr
        }

        const getStateName = (status, statusValue) => {
          // 优先使用 statusValue
          if (statusValue) return statusValue

          const stateMap = {
            '0': '未开始',
            '1': '进行中',
            '2': '已结束',
            '3': '已取消'
          }
          return stateMap[status] || status || '未开始'
        }

        const getStateClass = (status) => {
          const classMap = {
            '0': 'state-pending',
            '1': 'state-ongoing',
            '2': 'state-completed',
            '3': 'state-cancelled'
          }
          return classMap[status] || 'state-pending'
        }

        // 判断会议类型标志
        const getMeetingTypeTags = (meeting) => {
          const tags = []

          if (!meeting.startDate) {
            return tags
          }

          const startDate = this.formatMeetingDate(meeting.startDate)
          let endDate = startDate

          if (meeting.endDate) {
            endDate = this.formatMeetingDate(meeting.endDate)
          }

          // 判断跨月
          if (this.isCrossMonthMeeting(startDate, endDate)) {
            tags.push({
              text: '跨月',
              class: 'type-cross-month'
            })
          }
          // 判断跨周（如果不是跨月）
          else if (this.isCrossWeekMeeting(startDate, endDate)) {
            tags.push({
              text: '跨周',
              class: 'type-cross-week'
            })
          }
          // 判断跨天（如果不是跨周跨月）
          else if (startDate !== endDate) {
            tags.push({
              text: '跨天',
              class: 'type-cross-day'
            })
          }

          return tags
        }

        const typeTags = getMeetingTypeTags(meeting)

        return {
          ...meeting,
          meetingName: meeting.meetingName || meeting.title || meeting.name || '未命名会议',
          meetingDate: formatMeetingDate(meeting.startDate),
          meetingTime: formatMeetingTime(meeting.startStartTime, meeting.endEndTime),
          startTime: formatSingleTime(meeting.startStartTime),
          endTime: formatSingleTime(meeting.endEndTime),
          state: getStateName(meeting.status, meeting.statusValue),
          stateClass: getStateClass(meeting.status),
          host: meeting.host || '未知',
          location: meeting.location || '待定',
          typeTags: typeTags
        }
      })

      // 应用排序
      const sortedResult = this.applySort(result)
      return sortedResult
    }
  },

  async onLoad() {
    const user = uni.getStorageSync('userInfo')
    if (!user) {
      uni.reLaunch({
        url: '/pages/login/login'
      })
      return
    }

    // 从全局工具获取存储的日期值
    const dates = getAllStoredDates()
    this.todayDateStr = dates.today
    this.tomorrowDateStr = dates.tomorrow
    this.weekStartStr = dates.weekStart
    this.weekEndStr = dates.weekEnd
    this.monthStartStr = dates.monthStart
    this.monthEndStr = dates.monthEnd


    await this.loadCollegeName()
    await this.loadAllMeetings()

    // 标记为已初始化
    this.isInitialized = true
    this.lastRefreshTime = Date.now()
  },

  async onShow() {
    console.log('会议页面显示，检查是否需要刷新数据')

    // 检查是否需要强制刷新（从操作页面返回）
    const pages_array = getCurrentPages()
    const currentPage = pages_array[pages_array.length - 1]
    const forceRefresh = currentPage.options && currentPage.options.forceRefresh === 'true'

    // 检查数据是否已存在且未过期（1分钟缓存）或者需要强制刷新
    const shouldRefresh = forceRefresh ||
                          !this.isInitialized ||
                          (Date.now() - (this.lastRefreshTime || 0) > 1 * 60 * 1000)

    if (shouldRefresh) {
      const refreshMsg = forceRefresh ? '检测到强制刷新参数，立即刷新数据' : '会议数据已过期或首次加载，开始刷新'
      console.log(refreshMsg)
      await this.loadAllMeetings()
      this.lastRefreshTime = Date.now()
    } else {
      console.log('使用缓存会议数据，跳过刷新')
    }
  },

  async onHide() {
    // 页面隐藏时清除待处理请求，防止ClientAbortException
    cleanupPendingRequests()
  },

  async onUnload() {
    // 页面卸载时清除待处理请求，防止ClientAbortException
    cleanupPendingRequests()
  },

  methods: {
    async loadCollegeName() {
      const userInfo = uni.getStorageSync('userInfo') || {}
      if (userInfo.tenant) {
        this.collegeName = userInfo.tenant
      }
    },

    // 选择全部会议
    async selectAllMeetings() {
      this.isTodayMeeting = false
      this.isTomorrowMeeting = false
      this.isWeekMeeting = false
      this.isMonthMeeting = false
      this.appliedFilterStartDate = ''
      this.appliedFilterEndDate = ''
      // 重新加载全部会议（不分页）
      this.isInitialLoad = true
      await this.loadAllMeetings()
    },

    // 显示筛选弹窗
    showFilterPopup() {
      this.showFilter = true
      // 初始化筛选条件为当前已应用的值
      this.filterStartDate = this.appliedFilterStartDate
      this.filterEndDate = this.appliedFilterEndDate
      // 默认展开排序选项
      this.expandedFilter = 'sort'
    },

    // 隐藏筛选弹窗
    hideFilterPopup() {
      this.showFilter = false
    },

    // 重置筛选
    resetFilter() {
      this.filterStartDate = ''
      this.filterEndDate = ''
      this.filterSort = 'default'
      this.expandedFilter = null
    },

    // 切换筛选项展开/收起
    toggleFilterItem(filterType) {
      if (this.expandedFilter === filterType) {
        this.expandedFilter = null
      } else {
        this.expandedFilter = filterType
      }
    },

    // 获取筛选排序显示文本
    getFilterSortText() {
      const sort = this.sortOptions.find(s => s.value === this.filterSort)
      return sort ? sort.name : '默认排序'
    },

    // 选择筛选排序
    selectFilterSort(sort) {
      this.filterSort = sort
    },

    // 显示开始日期选择器
    showStartDatePicker() {
      this.showStartDatePickerFlag = true
      if (this.filterStartDate) {
        this.startDateValue = new Date(this.filterStartDate).getTime()
      } else {
        this.startDateValue = new Date().getTime()
      }
    },

    // 隐藏开始日期选择器
    hideStartDatePicker() {
      this.showStartDatePickerFlag = false
    },

    // 确认开始日期
    confirmStartDate(e) {
      const date = new Date(e.value)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      this.filterStartDate = `${year}-${month}-${day}`
      this.showStartDatePickerFlag = false
      this.startDateValue = e.value
      this.startDatePickerMin = date.getTime()
      this.endDatePickerMin = date.getTime()
    },

    // 显示结束日期选择器
    showEndDatePicker() {
      if (this.filterStartDate) {
        const startDate = new Date(this.filterStartDate)
        this.endDatePickerMin = startDate.getTime()
      }
      this.showEndDatePickerFlag = true
      if (this.filterEndDate) {
        this.endDateValue = new Date(this.filterEndDate).getTime()
      } else {
        this.endDateValue = new Date().getTime()
      }
    },

    // 隐藏结束日期选择器
    hideEndDatePicker() {
      this.showEndDatePickerFlag = false
    },

    // 确认结束日期
    confirmEndDate(e) {
      const date = new Date(e.value)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      this.filterEndDate = `${year}-${month}-${day}`
      this.showEndDatePickerFlag = false
      this.endDateValue = e.value
    },

    // 清除日期范围筛选
    clearDateRangeFilter() {
      this.filterStartDate = ''
      this.filterEndDate = ''
    },

    // 应用筛选
    async applyFilter() {
      // 验证日期范围
      if (this.filterStartDate && this.filterEndDate) {
        if (this.filterStartDate > this.filterEndDate) {
          uni.showToast({
            title: '开始日期不能晚于结束日期',
            icon: 'none'
          })
          return
        }
      }

      // 保存筛选条件
      this.appliedFilterStartDate = this.filterStartDate
      this.appliedFilterEndDate = this.filterEndDate

      // 取消所有快捷筛选按钮的选中状态
      this.isTodayMeeting = false
      this.isTomorrowMeeting = false
      this.isWeekMeeting = false
      this.isMonthMeeting = false

      // 判断是否有日期筛选条件
      const hasDateFilter = this.filterStartDate || this.filterEndDate

      if (hasDateFilter) {
        // 有日期筛选条件，调用后端API
        await this.loadFilteredMeetings(this.filterStartDate, this.filterEndDate)
      } else {
        // 只有排序条件，不调用API，直接使用现有数据
        console.log('仅前端排序，不调用后端API')
        // 重新加载所有会议（确保数据完整）
        this.isInitialLoad = true
        await this.loadAllMeetings()
      }

      // 关闭筛选弹窗
      this.showFilter = false

      // 显示提示
      uni.showToast({
        title: '已应用筛选',
        icon: 'success',
        duration: 1500
      })
    },

    // 应用排序到会议列表
    applySort(meetings) {
      if (!meetings || meetings.length === 0) {
        return meetings
      }

      // 创建排序后的副本
      const sortedMeetings = [...meetings]

      switch (this.filterSort) {
        case 'createTime':
          // 按创建时间排序（最新的在前）
          sortedMeetings.sort((a, b) => {
            const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
            const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
            return timeB - timeA // 降序
          })
          break

        case 'startTime':
          // 按开始时间排序（即将开始的在前）
          sortedMeetings.sort((a, b) => {
            const timeA = a.startDate ? new Date(a.startDate).getTime() : Infinity
            const timeB = b.startDate ? new Date(b.startDate).getTime() : Infinity
            return timeA - timeB // 升序
          })
          break

        case 'default':
        default:
          // 默认排序（使用原有的复杂排序逻辑）
          return this.sortMeetings(sortedMeetings)
      }

      return sortedMeetings
    },

    // 加载筛选后的会议（调用后端接口）
    async loadFilteredMeetings(startDate, endDate) {
      try {

        this.isLoading = true
        this.currentPage = 1
        this.hasMore = true

        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        // 构建查询参数
        const params = {
          tid: tid,
          pageNum: this.currentPage,
          pageSize: 100  // 加载更多数据
        }

        // 后端期望的参数名是 startTime 和 endTime（不是 startDate 和 endDate）
        // 后端筛选逻辑：
        // - 多日会议(meeting_type='1'): end_date >= startTime AND start_date <= endTime（有重叠即可）
        // - 单日会议(meeting_type='0'): start_date >= startTime AND start_date <= endTime（startDate在范围内）
        if (startDate && endDate) {
          params.startTime = startDate
          params.endTime = endDate

        } else if (startDate) {
          // 如果只有开始日期（单日筛选）
          params.startTime = startDate
          params.endTime = startDate
        }

        // 添加状态筛选
        if (this.selectedStatus) {
          params.status = this.selectedStatus
        }


        const res = await meetingApi.getMeetingByConditionPage(params)


        if (res.code === '200' || res.code === 200) {
          const pageData = res.data
          let meetings = pageData.list || []


          this.allMeetings = meetings
          this.totalCount = pageData.total || meetings.length
          this.totalPages = pageData.pages || 1
          this.hasMore = this.currentPage < this.totalPages && meetings.length > 0

        } else {
          console.error('❌ API返回错误, code:', res.code, 'msg:', res.msg)
          uni.showToast({
            title: res.msg || '加载失败',
            icon: 'none'
          })
        }
      } catch (error) {
        console.error('✗ 加载筛选会议失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.isLoading = false
      }
    },

    async loadAllMeetings() {
      // 如果是初始化加载，加载所有数据
      if (this.isInitialLoad) {
        await this.loadAllMeetingsWithoutPagination()
        this.isInitialLoad = false
        return
      }

      // 分页加载
      await this.loadMeetingsByPage()
    },

    // 不分页加载所有会议
    async loadAllMeetingsWithoutPagination() {

      this.isLoading = true
      this.currentPage = 1
      this.allMeetings = []

      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        if (!tid) {
          console.error('❌ 用户信息中tid为空，无法加载会议')
          uni.showToast({
            title: '获取会议列表失败',
            icon: 'none'
          })
          return
        }

        let hasMore = true
        let page = 1
        let allMeetings = []

        // 循环加载所有页
        while (hasMore) {

          const res = await meetingApi.getMeetingByTenantPage({
            tid: tid,
            pageNum: page,
            pageSize: 100  // 每页加载更多数据，减少请求次数
          })


          if (res.code === '200') {
            const pageData = res.data
            const meetings = pageData.list || []


            allMeetings = [...allMeetings, ...meetings]

            // 检查是否还有更多数据
            const totalPages = pageData.pages || 0
            hasMore = page < totalPages && meetings.length > 0
            page++

          } else {
            console.error('❌ 获取会议失败, code:', res.code, 'msg:', res.msg)
            hasMore = false
          }
        }


        // 对会议进行排序
        allMeetings = this.sortMeetings(allMeetings)

        // 调试：打印所有会议的日期信息

        allMeetings.forEach(m => {
        })

        this.allMeetings = allMeetings
        this.totalPages = 1
        this.hasMore = false

      } catch (error) {
        console.error('❌ 加载会议列表失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.isLoading = false
      }
    },

    // 分页加载会议
    async loadMeetingsByPage() {
      if (this.isLoading || !this.hasMore) return

      this.isLoading = true
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        if (!tid) {
          console.error('用户信息中tid为空，无法加载会议')
          return
        }

        const res = await meetingApi.getMeetingByTenantPage({
          tid: tid,
          pageNum: this.currentPage,
          pageSize: this.pageSize
        })

        if (res.code === '200') {
          const pageData = res.data
          const meetings = pageData.list || []

          if (this.currentPage === 1) {
            this.allMeetings = meetings
          } else {
            this.allMeetings = [...this.allMeetings, ...meetings]
          }

          this.totalPages = pageData.pages || 0
          this.hasMore = this.currentPage < this.totalPages && meetings.length > 0

        } else {
          console.error('获取会议失败:', res.msg)
          this.hasMore = false
        }
      } catch (error) {
        console.error('加载会议列表失败:', error)
        this.hasMore = false
      } finally {
        this.isLoading = false
      }
    },

    // 加载更多会议（上拉加载）
    loadMoreMeetings() {
      // 如果是初始化加载或者没有更多数据，不执行
      if (this.isInitialLoad || !this.hasMore || this.isLoading) {
        return
      }
      this.currentPage++
      this.loadAllMeetings()
    },

    selectStatus(status) {
      this.selectedStatus = status
    },

    // 选择今日会议
    async selectTodayMeeting() {
      this.isTodayMeeting = !this.isTodayMeeting
      if (this.isTodayMeeting) {
        this.isTomorrowMeeting = false
        this.isWeekMeeting = false
        this.isMonthMeeting = false
        this.appliedFilterStartDate = ''
        this.appliedFilterEndDate = ''
        // 调用后端接口加载今日会议
        await this.loadFilteredMeetings(this.todayDateStr, this.todayDateStr)
      } else {
        // 取消今日筛选，重新加载全部会议
        await this.loadAllMeetings()
      }
    },

    // 选择明日会议
    async selectTomorrowMeeting() {
      this.isTomorrowMeeting = !this.isTomorrowMeeting
      if (this.isTomorrowMeeting) {
        this.isTodayMeeting = false
        this.isWeekMeeting = false
        this.isMonthMeeting = false
        this.appliedFilterStartDate = ''
        this.appliedFilterEndDate = ''
        // 调用后端接口加载明日会议
        await this.loadFilteredMeetings(this.tomorrowDateStr, this.tomorrowDateStr)
      } else {
        // 取消明日筛选，重新加载全部会议
        await this.loadAllMeetings()
      }
    },

    // 选择本周会议
    async selectWeekMeeting() {
      this.isWeekMeeting = !this.isWeekMeeting
      if (this.isWeekMeeting) {
        this.isTodayMeeting = false
        this.isTomorrowMeeting = false
        this.isMonthMeeting = false
        this.appliedFilterStartDate = ''
        this.appliedFilterEndDate = ''
        // 调用后端接口加载本周会议
        await this.loadFilteredMeetings(this.weekStartStr, this.weekEndStr)
      } else {
        // 取消本周筛选，重新加载全部会议
        await this.loadAllMeetings()
      }
    },

    // 选择本月会议
    async selectMonthMeeting() {
      this.isMonthMeeting = !this.isMonthMeeting
      if (this.isMonthMeeting) {
        this.isTodayMeeting = false
        this.isTomorrowMeeting = false
        this.isWeekMeeting = false
        this.appliedFilterStartDate = ''
        this.appliedFilterEndDate = ''
        // 调用后端接口加载本月会议
        await this.loadFilteredMeetings(this.monthStartStr, this.monthEndStr)
      } else {
        // 取消本月筛选，重新加载全部会议
        await this.loadAllMeetings()
      }
    },

    handleSearch() {

    },

    formatDate(date) {
      if (!date) return ''
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    formatDateShort(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      if (isNaN(date.getTime())) return dateStr
      const month = date.getMonth() + 1
      const day = date.getDate()
      return `${month}月${day}日`
    },

    formatMeetingDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      if (isNaN(date.getTime())) return ''
      // 返回 YYYY-MM-DD 格式，与 picker 返回格式一致
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    goToMeetingDetail(meeting) {
      const encodedMeeting = encodeURIComponent(JSON.stringify(meeting))
      uni.navigateTo({
        url: `/pages/meeting-detail/meeting-detail?meeting=${encodedMeeting}`
      })
    },

    backToTop() {
      uni.pageScrollTo({
        scrollTop: 0,
        duration: 300
      })
    },

    onPageScroll(e) {
      this.showBackToTop = e.scrollTop > 300
    },

    // 触底加载更多（已禁用，因为一次性加载100条数据足够）
    onReachBottom() {
      // 不再加载更多数据
      return
    },

    // 会议排序
    sortMeetings(meetings) {
      return meetings.sort((a, b) => {
        const dateA = a.startDate ? new Date(a.startDate) : null
        const dateB = b.startDate ? new Date(b.startDate) : null

        // 判断是否是跨月会议
        const endDateA = a.endDate ? new Date(a.endDate) : dateA
        const endDateB = b.endDate ? new Date(b.endDate) : dateB
        const isCrossMonthA = dateA && endDateA && this.isCrossMonthMeeting(this.formatDate(dateA), this.formatDate(endDateA))
        const isCrossMonthB = dateB && endDateB && this.isCrossMonthMeeting(this.formatDate(dateB), this.formatDate(endDateB))

        // 判断是否是跨周会议
        const isCrossWeekA = dateA && endDateA && this.isCrossWeekMeeting(this.formatDate(dateA), this.formatDate(endDateA))
        const isCrossWeekB = dateB && endDateB && this.isCrossWeekMeeting(this.formatDate(dateB), this.formatDate(endDateB))

        // 判断是否是跨天会议
        const isMultiDayA = dateA && endDateA && dateA.getTime() !== endDateA.getTime()
        const isMultiDayB = dateB && endDateB && dateB.getTime() !== endDateB.getTime()

        // 判断是否是全天会议
        const isAllDayA = !a.startTime || a.startTime === '00:00:00'
        const isAllDayB = !b.startTime || b.startTime === '00:00:00'

        // 优先级1：跨月会议优先于单月会议
        if (isCrossMonthA && !isCrossMonthB) return -1
        if (!isCrossMonthA && isCrossMonthB) return 1

        // 优先级2：跨月会议按开始时间排序
        if (isCrossMonthA && isCrossMonthB) {
          return dateA - dateB
        }

        // 优先级3：跨周会议优先于单周会议
        if (isCrossWeekA && !isCrossWeekB) return -1
        if (!isCrossWeekA && isCrossWeekB) return 1

        // 优先级4：跨周会议按开始时间排序
        if (isCrossWeekA && isCrossWeekB) {
          return dateA - dateB
        }

        // 优先级5：跨天会议优先于单天会议
        if (isMultiDayA && !isMultiDayB) return -1
        if (!isMultiDayA && isMultiDayB) return 1

        // 优先级6：跨天会议按开始时间排序
        if (isMultiDayA && isMultiDayB) {
          return dateA - dateB
        }

        // 优先级7：全天会议优先于小时会议
        if (isAllDayA && !isAllDayB) return -1
        if (!isAllDayA && isAllDayB) return 1

        // 优先级8：小时会议按时间排序
        if (!isAllDayA && !isAllDayB && a.startTime && b.startTime) {
          return a.startTime.localeCompare(b.startTime)
        }

        // 优先级9：同类型按日期排序
        if (dateA && dateB) {
          return dateA - dateB
        }

        return 0
      })
    },

    // 判断是否是跨周会议（持续时间超过7天或跨越不同的周）
    isCrossWeekMeeting(startDate, endDate) {
      if (!startDate || !endDate) return false

      const start = new Date(startDate)
      const end = new Date(endDate)

      // 计算天数差
      const daysDiff = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1

      // 如果持续时间超过7天，肯定是跨周会议
      if (daysDiff > 7) return true

      // 检查是否跨越不同的周（一年中的周数）
      const startWeek = this.getWeekNumber(start)
      const endWeek = this.getWeekNumber(end)

      return startWeek !== endWeek
    },

    // 判断是否是跨月会议
    isCrossMonthMeeting(startDate, endDate) {
      if (!startDate || !endDate) return false

      const start = new Date(startDate)
      const end = new Date(endDate)

      // 比较年月
      const startMonth = start.getFullYear() * 12 + start.getMonth()
      const endMonth = end.getFullYear() * 12 + end.getMonth()

      return startMonth !== endMonth
    },

    // 获取日期是当年的第几周
    getWeekNumber(date) {
      const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
      const dayNum = d.getUTCDay() || 7
      d.setUTCDate(d.getUTCDate() + 4 - dayNum)
      const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
      return Math.ceil((((d - yearStart) / 86400000) + 1) / 7)
    },

    // 下拉刷新
    async onRefresh() {
      try {
        console.log('🔄 开始刷新会议数据')

        // 重新加载会议数据
        await this.loadAllMeetings()

        console.log('✅ 会议数据刷新完成')

        // 刷新成功提示
        uni.showToast({
          title: '刷新成功',
          icon: 'success',
          duration: 1500
        })
      } catch (error) {
        console.error('❌ 刷新失败:', error)
        uni.showToast({
          title: '刷新失败',
          icon: 'none',
          duration: 1500
        })
      }
    },

    // 跳转到新增会议页面
    goToAddMeeting() {
      uni.navigateTo({
        url: '/pages/meeting-add/meeting-add'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 统一样式变量 */
$primary-color: #023c99;
$primary-light: #e6f0ff;
$accent-color: #1890ff;
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$bg-primary: #ffffff;
$bg-base: #f5f7fa;
$border-base: #e4e7ed;
$radius-lg: 24rpx;
$radius-base: 12rpx;
$shadow-base: 0 4rpx 20rpx rgba(0, 0, 0, 0.08);
$shadow-sm: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

.page {
  background: linear-gradient(180deg, #f8f9fb 0%, #ffffff 100%);
  min-height: 100vh;
  padding-top: calc(env(safe-area-inset-top) + 274rpx); // 精确计算：筛选区域底部262rpx + 12rpx间距
  padding-bottom: 160rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  position: relative;
}

/* 自定义导航栏样式 */
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
  justify-content: flex-start;
  position: relative;
}

.nav-logo-wrapper {
  position: absolute;
  left: -30rpx;
  top: 20rpx;
  transform: translateY(-50%);
}

.nav-text-wrapper {
  position: absolute;
  left: 90rpx;
  top: 20rpx;
  transform: translateY(-50%);
}

.nav-right {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.calendar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  backdrop-filter: blur(10rpx);
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.95);
    background: rgba(255, 255, 255, 0.3);
  }
}

.calendar-icon {
  font-size: 32rpx;
  line-height: 1;
}

.college-name {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 600;
  letter-spacing: 1rpx;
  text-shadow: 0 2rpx 4rpx rgba(0, 0, 0, 0.1);
  line-height: 1;
  white-space: nowrap;
}

.nav-logo {
  width: 100rpx;
  height: 100rpx;
  border-radius: 8rpx;
}

/* 搜索和筛选区域 */
.search-filter-section {
  position: fixed;
  top: 100rpx;
  left: 0;
  right: 0;
  z-index: 999;
  background: #ffffff;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.search-container {
  padding: 16rpx 24rpx 8rpx;
}

/* 搜索框包装器 - 淘宝风格 */
.search-bar-wrapper {
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 24rpx;
  overflow: hidden;
}

/* 搜索输入框包装器 */
.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 12rpx;
  gap: 8rpx;
}

/* 搜索图标 */
.search-icon {
  font-size: 32rpx;
  color: #999;
  flex-shrink: 0;
  line-height: 1;
}

.search-input {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: #333;
  background: transparent;
  border: none;
  padding: 0;
}

.search-placeholder {
  color: #999;
  font-size: 28rpx;
}

/* 筛选按钮 - 三条横线设计 */
.filter-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8rpx;
  flex-shrink: 0;
  background: transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  border-radius: 12rpx;

  &:active {
    transform: scale(0.95);
    background: rgba(0, 0, 0, 0.05);
  }
}

.filter-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7rpx;
  line-height: 1;
  height: 100%;
}

.filter-line {
  height: 6rpx;
  background: #999999;
  border-radius: 3rpx;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.filter-line.line-1 {
  width: 36rpx;
}

.filter-line.line-2 {
  width: 36rpx;
}

.filter-line.line-3 {
  width: 36rpx;
}

.filter-btn:active .filter-line {
  background: #666666;
}

/* 筛选弹窗 */
.filter-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  visibility: hidden;
  pointer-events: none;
}

.filter-popup.show {
  visibility: visible;
  pointer-events: auto;
}

.filter-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.filter-popup.show .filter-overlay {
  opacity: 1;
}

.filter-content {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  transform: translateY(100%);
  transition: transform 0.3s ease;
  border-radius: 24rpx 24rpx 0 0;
  z-index: 2001;
}

.filter-popup.show .filter-content {
  transform: translateY(0);
}

/* 确保 picker 在筛选器之上 */
.date-range-picker {
  position: relative;
  z-index: 99999;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.filter-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.filter-reset {
  font-size: 28rpx;
  color: #666;
  padding: 8rpx 16rpx;
}

.filter-body {
  flex: 1;
  overflow-y: auto;
}

.filter-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
  background: #ffffff;
}

.filter-label {
  font-size: 28rpx;
  color: #333;
}

.filter-value-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.filter-value {
  font-size: 28rpx;
  color: #666;
}

.filter-arrow {
  font-size: 24rpx;
  color: #999;
}

.filter-options {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
  background: #f9f9f9;
}

.filter-options.show {
  max-height: 600rpx;
  overflow-y: auto;
}

.filter-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f0f0f0;
  transition: background 0.2s ease;
}

.filter-option-item:last-child {
  border-bottom: none;
}

.filter-option-item:active {
  background: #f0f0f0;
}

.filter-option-item.active {
  background: #e6f7ff;
}

.option-item-text {
  font-size: 28rpx;
  color: #333;
}

.option-item-check {
  font-size: 32rpx;
  color: #1890ff;
}

.filter-footer {
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #f0f0f0;
  background: #ffffff;
}

.filter-confirm-btn {
  width: 100%;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1890ff;
  border-radius: 12rpx;
  transition: background 0.3s ease;
}

.filter-confirm-btn:active {
  background: #1677d9;
}

.confirm-btn-text {
  font-size: 32rpx;
  color: #ffffff;
  font-weight: 600;
}

/* 时间范围筛选样式 - 内联式 */
.date-range-filter {
  flex-direction: column;
  align-items: flex-start !important;
  padding: 24rpx 32rpx !important;
}

.date-range-pickers {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
  width: 100%;
}

.date-range-picker {
  flex: 1;
  min-width: 0;
}

.date-range-value {
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  color: #999;
  text-align: center;
  background: #f5f5f5;
  border-radius: 8rpx;
  transition: all 0.3s ease;
}

.date-range-value.has-value {
  color: #333;
  background: #e6f7ff;
}

.date-separator {
  font-size: 28rpx;
  color: #999;
  flex-shrink: 0;
}

.date-clear {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  color: #999;
  flex-shrink: 0;
  margin-left: 8rpx;
}

.date-clear:active {
  color: #666;
}

/* 状态筛选区域 */
.state-filter-scroll {
  position: fixed;
  top: 182rpx;
  left: 0;
  right: 0;
  z-index: 998;
  background: #ffffff;
  padding: 12rpx 24rpx 8rpx;
  white-space: nowrap;
}

.filter-row {
  display: flex;
  gap: 12rpx;
  min-height: 60rpx;
  align-items: center;
}

/* 今日会议按钮 */
.today-meeting-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 18rpx;
  border: 2rpx solid #023c99;
  border-radius: 24rpx;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.today-meeting-btn.small-btn {
  padding: 10rpx 18rpx;
}

.today-meeting-btn.active {
  background: #023c99;
  border-color: #023c99;
}

.today-icon {
  font-size: 26rpx;
}

.today-text {
  font-size: 26rpx;
  color: #023c99;
  font-weight: 600;
  white-space: nowrap;
}

.today-meeting-btn.active .today-text {
  color: #ffffff;
}

/* 会议总数徽章 */
.total-count-badge {
  margin-left: 8rpx;
  padding: 6rpx 16rpx;
  background: rgba(24, 144, 255, 0.1);
  border-radius: 20rpx;
  border: 2rpx solid #1890ff;
  flex-shrink: 0;
}

.total-count-text {
  font-size: 24rpx;
  color: #1890ff;
  font-weight: 600;
}

/* 日期查询按钮 */
.date-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10rpx 20rpx;
  background: linear-gradient(135deg, #023c99 0%, #1890ff 100%);
  border-radius: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(2, 60, 153, 0.2);
  flex-shrink: 0;
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.95);
  }
}

.search-btn-text {
  font-size: 26rpx;
  color: #ffffff;
  font-weight: 600;
  white-space: nowrap;
}

.filter-divider {
  width: 2rpx;
  height: 40rpx;
  background: #e0e0e0;
  flex-shrink: 0;
}

.state-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 20rpx;
  border: 2rpx solid #e4e7ed;
  border-radius: 20rpx;
  background-color: #ffffff;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.state-btn:active {
  transform: scale(0.95);
}

.btn-text {
  font-size: 26rpx;
  color: #606266;
  white-space: nowrap;
}

.btn-count {
  font-size: 22rpx;
  color: #909399;
  background-color: #f5f7fa;
  padding: 2rpx 8rpx;
  border-radius: 12rpx;
  min-width: 32rpx;
  text-align: center;
}

.state-btn.active {
  background-color: #007aff;
  border-color: #007aff;
  color: white;
}

.state-btn.active .btn-text {
  color: white;
}

.state-btn.active .btn-count {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}

/* 会议列表区域 */
.meetings-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.meetings-scroll {
  height: 100%;
}

.meetings-container {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 4rpx 32rpx 32rpx;
  width: 100%;
  box-sizing: border-box;
}

.meeting-item-wrapper {
  width: 100%;
  margin-bottom: 24rpx;
}

/* 会议卡片样式 */
.meeting-card {
  position: relative;
  background: linear-gradient(135deg, rgba(2, 60, 153, 0.8) 0%, rgba(0, 86, 179, 0.7) 100%);
  border-radius: 24rpx;
  width: 95%;
  margin: 0 auto;
  min-height: 180rpx;
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  box-shadow: 0 4rpx 20rpx rgba(2, 60, 153, 0.2);
  cursor: pointer;
  padding: 24rpx;
  box-sizing: border-box;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  backdrop-filter: blur(10rpx);
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

.meeting-card:active {
  transform: scale(0.98);
}

/* 卡片左侧区域 */
.meeting-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding-right: 20rpx;
}

/* 会议名称 */
.meeting-name {
  font-size: 32rpx;
  font-weight: bold;
  color: white;
  line-height: 1.4;
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 会议标签容器 */
.meeting-tags {
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
}

/* 会议标签 */
.meeting-tag {
  padding: 8rpx 16rpx;
  border-radius: 50rpx;
  font-size: 24rpx;
  color: white;
  font-weight: 500;
  backdrop-filter: blur(10rpx);
}

/* 状态标签 */
.state-tag {
  background: rgba(255, 255, 255, 0.2);
}

.state-tag.state-pending {
  background-color: rgba(24, 144, 255, 0.9);
}

.state-tag.state-ongoing {
  background-color: rgba(250, 173, 20, 0.9);
}

.state-tag.state-completed {
  background-color: rgba(82, 196, 26, 0.9);
}

.state-tag.state-cancelled {
  background-color: rgba(255, 77, 79, 0.9);
}

/* 类型标签 */
.type-tag {
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.15);
}

.type-tag.type-emergency {
  background-color: rgba(255, 77, 79, 0.9);
  border: 1rpx solid rgba(255, 204, 199, 0.5);
}

.type-tag.type-department {
  background-color: rgba(114, 46, 209, 0.9);
  border: 1rpx solid rgba(199, 157, 255, 0.5);
}

.type-tag.type-training {
  background-color: rgba(82, 196, 26, 0.9);
  border: 1rpx solid rgba(183, 235, 143, 0.5);
}

.type-tag.type-regular {
  background-color: rgba(24, 144, 255, 0.9);
  border: 1rpx solid rgba(145, 213, 255, 0.5);
}

/* 跨天标签 */
.type-tag.type-cross-day {
  background-color: rgba(24, 144, 255, 0.85);
  border: 1rpx solid rgba(145, 213, 255, 0.6);
}

/* 跨周标签 */
.type-tag.type-cross-week {
  background-color: rgba(114, 46, 209, 0.85);
  border: 1rpx solid rgba(199, 157, 255, 0.6);
}

/* 跨月标签 */
.type-tag.type-cross-month {
  background-color: rgba(255, 77, 79, 0.85);
  border: 1rpx solid rgba(255, 204, 199, 0.6);
}

/* 卡片右侧区域 */
.meeting-right {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10rpx;
  min-width: 200rpx;
  max-width: 280rpx;
  padding-left: 20rpx;
  border-left: 1rpx solid rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

/* 会议信息行 */
.meeting-info-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  line-height: 1.4;
}

/* 信息标签 */
.meeting-info-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  min-width: 70rpx;
  flex-shrink: 0;
}

/* 信息值 */
.meeting-info-value {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 1);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 空状态 */
.meeting-empty {
  text-align: center;
  padding: 200rpx 40rpx;

  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 20rpx;
    opacity: 0.4;
  }

  .empty-text {
    font-size: 28rpx;
    color: $text-secondary;
  }
}

/* 响应式设计 */
@media (max-width: 750rpx) {
  .meeting-card {
    flex-direction: column;
  }

  .meeting-left {
    padding-right: 0;
    padding-bottom: 12rpx;
    border-bottom: 1rpx solid rgba(255, 255, 255, 0.2);
  }

  .meeting-right {
    padding-left: 0;
    padding-top: 12rpx;
    border-left: none;
    min-width: auto;
    max-width: none;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .meeting-info-row {
    flex: 1;
    min-width: 45%;
  }

  .meeting-name {
    font-size: 30rpx;
  }

  .meetings-container {
    padding: 20rpx;
    gap: 16rpx;
  }

  .meeting-card {
    padding: 20rpx;
  }
}

/* 加载状态提示 */
.loading-more {
  text-align: center;
  padding: 30rpx 0;
}

.loading-text {
  font-size: 26rpx;
  color: $text-secondary;
}

.no-more {
  text-align: center;
  padding: 30rpx 0;
}

.no-more-text {
  font-size: 26rpx;
  color: $text-secondary;
}

/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  right: 30rpx;
  bottom: 280rpx;  /* 上移避免与添加按钮重叠 */
  width: 90rpx;
  height: 90rpx;
  background: linear-gradient(135deg, rgba(2, 60, 153, 0.9) 0%, rgba(0, 86, 179, 0.9) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  z-index: 997;
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(20rpx);
  animation: fadeInUp 0.3s ease forwards;
}

.back-to-top:active {
  transform: scale(0.95);
}

.back-to-top-icon {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: bold;
  line-height: 1;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 浮动新增按钮 */
.floating-add-btn {
  position: fixed;
  right: 30rpx;
  bottom: 180rpx;
  width: 90rpx;
  height: 90rpx;
  background: linear-gradient(135deg, rgba(2, 60, 153, 0.9) 0%, rgba(0, 86, 179, 0.9) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  z-index: 998;
  transition: all 0.3s ease;
  animation: fadeInUp 0.3s ease forwards;
}

.floating-add-btn:active {
  transform: scale(0.95);
}

.add-icon {
  font-size: 48rpx;
  color: #ffffff;
  font-weight: bold;
  line-height: 1;
}
</style>
