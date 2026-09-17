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
          <text class="college-name">{{ displayDeptName }}</text>
        </view>
      </view>
    </view>

    <!-- 搜索和筛选区域 -->
    <view class="search-filter-section">
      <!-- 搜索框容器 -->
      <view class="search-container">
        <view class="search-bar-wrapper">
          <!-- 左侧部门图标 -->
          <view class="search-dept-icon" @tap="toggleSidebar">
            <view class="dept-badge">
              <text class="dept-badge-text">部</text>
            </view>
          </view>

          <!-- 分隔线 -->
          <view class="search-divider"></view>

          <!-- 右侧搜索框 -->
          <view class="search-input-wrapper">
            <text class="search-icon">🔍</text>
            <input
              class="search-input"
              type="text"
              placeholder="搜索工作名称、详情、创建人、负责人"
              v-model="searchKeyword"
              @confirm="handleSearch"
              @input="onSearchInput"
              placeholder-class="search-placeholder"
            />
            <!-- 清除按钮 -->
            <view
              v-if="searchKeyword"
              class="search-clear-btn"
              @tap="clearSearch"
            >
              <text class="clear-icon">×</text>
            </view>
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

      <!-- 工作状态筛选 -->
      <scroll-view
        class="state-filter-scroll"
        scroll-x
        scroll-with-animation
        :show-scrollbar="false"
      >
        <view class="state-filter">
          <!-- 全部按钮 -->
          <view
            class="state-btn"
            :class="{ 'active': selectedStatus === '' }"
            @tap="selectStatus('')"
          >
            <text class="btn-text">全部</text>
          </view>
          <!-- 动态生成状态按钮 -->
          <view
            v-for="status in statusOptions"
            :key="status.uuid || status.value"
            class="state-btn"
            :class="{ 'active': selectedStatus === status.value }"
            @tap="selectStatus(status.value)"
          >
            <text class="btn-text">{{ status.name }}</text>
          </view>
          <!-- 符合条件的工作总数（显示在已撤销状态后面） -->
          <view class="total-count-badge">
            <text class="total-count-text">{{ totalCount }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 左侧导航栏 -->
    <view class="sidebar" :class="{ 'show': showSidebar }">
      <view class="sidebar-overlay" @tap="closeSidebar"></view>
      <view class="sidebar-content">
        <view class="sidebar-header">
          <text class="sidebar-title">部门列表</text>
          <view class="sidebar-close" @tap="closeSidebar">
            <text class="close-icon">×</text>
          </view>
        </view>
        <scroll-view class="sidebar-scroll" scroll-y>
          <view class="dept-list">
            <view
              class="dept-item"
              :class="{ 'active': !selectedDept }"
              @tap="selectDept(null)"
            >
              <view class="dept-item-icon">📋</view>
              <text class="dept-item-name">全部工作</text>
              <view class="dept-item-badge">{{ totalTaskCount }}</view>
            </view>
            <view
              v-for="dept in departments"
              :key="dept.id"
              class="dept-item"
              :class="{ 'active': selectedDept && selectedDept.id === dept.id }"
              @tap="selectDept(dept)"
            >
              <view class="dept-item-icon">{{ getDeptIcon(dept.name) }}</view>
              <text class="dept-item-name">{{ dept.name }}</text>
              <view class="dept-item-badge">{{ getTaskCount(dept) }}</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 工作列表 -->
    <view class="tasks-section">
      <RefreshScrollView
        class="tasks-scroll"
        @refresh="onRefresh"
        @scroll="onScroll"
      >
        <view class="tasks-container">
        <view v-if="currentTasks.length === 0 && !isLoading" class="task-empty">
          <text class="empty-icon">📭</text>
          <text class="empty-text">暂无工作</text>
        </view>
        <view
          v-for="(item, index) in processedTasks"
          :key="item.id || index"
          class="task-item-wrapper"
        >
          <view
            class="task-card"
            @tap="goToTaskDetail(item)"
          >
            <!-- 左侧：工作名称和状态 -->
            <view class="task-left">
              <text class="task-name">{{item.taskName}}</text>
              <view class="task-tags">
                <view class="task-tag state-tag" :class="item.stateClass">
                  {{item.state}}
                </view>
              </view>
            </view>

            <!-- 右侧：详细信息 -->
            <view class="task-right">
              <view class="task-info-row">
                <text class="task-info-label">类型</text>
                <text class="task-info-value">{{item.taskType || '未知'}}</text>
              </view>
              <view class="task-info-row">
                <text class="task-info-label">创建人</text>
                <text class="task-info-value">{{item.createBy || '未知'}}</text>
              </view>
              <view class="task-info-row">
                <text class="task-info-label">负责人</text>
                <text class="task-info-value">{{item.head || '未知'}}</text>
              </view>
              <view class="task-info-row">
                <text class="task-info-label">截止</text>
                <text class="task-info-value">{{item.formattedEndDate || '待定'}}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 加载状态提示 -->
        <view v-if="isLoading" class="loading-more">
          <text class="loading-text">加载中...</text>
        </view>
        <view v-else-if="!hasMore && allTasks.length > 0" class="no-more">
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
    <view class="floating-add-btn" @tap="goToAddWork">
      <text class="add-icon">+</text>
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
          <!-- 时间范围筛选（后端暂不支持，已隐藏）-->
          <!-- <view class="filter-item date-range-filter">
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
    ></u-datetime-picker>
  </view>
</template>

<script>
import request from '@/utils/request'
import { workApi } from '@/api'
import RefreshScrollView from '@/components/RefreshScrollView.vue'
import refreshManager from '@/utils/refreshManager'
import { cleanupPendingRequests } from '@/utils/request'

export default {
  components: {
    RefreshScrollView
  },
  data() {
    return {
      pageId: 'works_page', // 页面唯一标识
      departments: [],
      user: {},
      collegeName: '', // 学院名称
      allTasks: [], // 存储所有工作（可能是筛选后的）
      allTasksOriginal: [], // 存储完整的原始工作列表（用于统计部门数量）
      showSidebar: false, // 侧边栏显示状态
      selectedDept: null, // 当前选中的部门
      allTaskCount: 0, // 全部工作总数
      searchKeyword: '', // 搜索关键词
      selectedStatus: '', // 选中的任务状态
      statusOptions: [], // 任务状态选项
      statusDict: {}, // 状态字典映射 {value: name}
      isLoading: false, // 是否正在加载
      isLoadingMore: false, // 是否正在加载更多
      // 自动刷新相关
      refreshTimer: null, // 定时器
      refreshInterval: 10 * 60 * 1000, // 10分钟（毫秒）
      lastRefreshTime: null, // 上次刷新时间
      // 定时器标记
      timerStarted: false, // 定时器是否已启动
      // 用户角色信息
      userRid: null, // 用户角色ID
      userDid: null, // 用户科室ID
      // 返回顶部按钮
      showBackToTop: false, // 是否显示返回顶部按钮
      // 页面加载标记
      isInitialized: false, // 是否已经初始化加载过数据
      // 部门工作数量缓存 {deptId: count}
      departmentTaskCounts: {},
      // 筛选弹窗相关
      showFilter: false, // 是否显示筛选弹窗
      filterStatus: '', // 筛选选中的状态
      filterSort: 'default', // 筛选选中的排序
      filterDept: null, // 筛选选中的部门
      expandedFilter: null, // 当前展开的筛选项
      filterStartDate: '', // 筛选开始日期
      filterEndDate: '', // 筛选结束日期
      appliedFilterStartDate: '', // 已应用的筛选开始日期
      appliedFilterEndDate: '', // 已应用的筛选结束日期
      showStartDatePickerFlag: false, // 是否显示开始日期选择器
      showEndDatePickerFlag: false, // 是否显示结束日期选择器
      startDateValue: new Date().getTime(), // 开始日期选择器的值（使用时间戳）
      endDateValue: new Date().getTime(), // 结束日期选择器的值（使用时间戳）
      startDatePickerMin: new Date().getTime() - 10 * 365 * 24 * 60 * 60 * 1000, // 开始日期最小值（10年前）
      endDatePickerMax: new Date().getTime() + 10 * 365 * 24 * 60 * 60 * 1000, // 结束日期最大值（10年后）
      sortOptions: [
        { value: 'default', name: '默认排序' },
        { value: 'createTime', name: '创建时间' },
        { value: 'endTime', name: '截止时间' },
        { value: 'priority', name: '优先级' }
      ],
      // 分页相关
      pageNum: 1, // 当前页码
      pageSize: 100, // 每页条数
      hasMore: true, // 是否还有更多数据
      totalCount: 0, // 总记录数
      // 保存所有工作的ID列表（用于状态统计）
      allTaskIds: [], // 所有工作的ID列表
      // 保存每个状态的任务数量缓存
      statusCountCache: {}, // 全部工作的状态统计 { statusValue: count }
      // 保存每个部门的状态统计
      departmentStatusCache: {}, // { deptId: { statusValue: count } }
      // 保存当前是否为部门视图
      isDepartmentView: false, // 标记当前是否显示部门工作
      // 保存当前是否为筛选视图
      isFilterView: false, // 标记当前是否使用筛选条件
      filterSortApplied: 'default' // 已应用的排序方式
    }
  },

  computed: {
    // 显示学院或部门名称
    displayDeptName() {
      return this.collegeName
    },

    // 所有工作总数（用于侧边栏显示）
    totalTaskCount() {
      return this.allTaskCount
    },

    // 当前显示的任务总数（根据选中的部门动态变化）
    currentDisplayedCount() {
      // 如果选择了部门，使用部门状态统计计算总数
      if (this.selectedDept && this.selectedDept.id && this.departmentStatusCache[this.selectedDept.id]) {
        const deptStatus = this.departmentStatusCache[this.selectedDept.id]
        // 只计算中文名称的键值（过滤掉UUID键）
        const count = Object.entries(deptStatus)
          .filter(([key]) => !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(key))
          .reduce((sum, [key, val]) => sum + (val || 0), 0)
        return count
      }
      // 全部工作使用 allTaskCount
      return this.allTaskCount
    },

    // 当前应该使用的状态统计（根据是否选择部门）
    currentStatusCount() {
      const cache = this.selectedDept && this.selectedDept.id && this.departmentStatusCache[this.selectedDept.id]
        ? this.departmentStatusCache[this.selectedDept.id]
        : this.statusCountCache

      return cache
    },

    // 当前显示的工作列表（支持搜索）
    currentTasks() {
      let tasks = this.allTasks

      console.log('📊 currentTasks计算: allTasks长度 =', this.allTasks.length)

      // 搜索过滤（前端搜索，在后端筛选结果中继续搜索）
      if (this.searchKeyword && this.searchKeyword.trim()) {
        const keyword = this.searchKeyword.trim().toLowerCase()
        tasks = tasks.filter(task => {
          // 搜索工作名称
          const taskName = (task.name || task.taskName || '').toLowerCase()
          // 搜索工作详情
          const taskDetail = (task.detail || '').toLowerCase()
          // 搜索创建人
          const creator = (task.username || task.createBy || '').toLowerCase()
          // 搜索负责人
          const head = (task.head || '').toLowerCase()

          return taskName.includes(keyword) ||
                 taskDetail.includes(keyword) ||
                 creator.includes(keyword) ||
                 head.includes(keyword)
        })
        console.log('🔍 搜索过滤后长度 =', tasks.length)
      }

      return tasks
    },

    // 处理工作数据，添加计算属性
    processedTasks() {
      try {
        // 如果没有任务，直接返回空数组
        if (!this.currentTasks || this.currentTasks.length === 0) {
          return []
        }

        const result = this.currentTasks.map(task => {
          // 格式化日期
          const formatDate = (dateStr) => {
            if (!dateStr) return ''
            try {
              const date = new Date(dateStr)
              if (isNaN(date.getTime())) return ''
              return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
            } catch (e) {
              console.error('日期格式化失败:', dateStr, e)
              return ''
            }
          }

          // 根据任务状态获取名称（直接使用明文）
          const getStateName = (stateName) => {
            return stateName || '未知状态'
          }

          // 根据任务重要程度获取名称（支持代码和明文）
          const getSeverityName = (severityValue) => {
            // 如果是代码值，进行映射
            const codeMap = {
              '04': '紧急',
              '03': '重要',
              '02': '常规',
              '01': '日常'
            }
            // 如果已经是中文名称，直接返回；否则查询代码映射
            if (codeMap[severityValue]) {
              return codeMap[severityValue]
            }
            return severityValue || '未知'
          }

          // 根据任务类型获取名称（直接使用明文）
          const getTaskTypeName = (taskTypeName) => {
            return taskTypeName || '未知类型'
          }

          // 根据任务重要程度获取颜色（根据明文）
          const getSeverityColor = (severityName) => {
            const colorMap = {
              '紧急': '#ff4d4f',
              '重要': '#ff7a45',
              '常规': '#faad14',
              '日常': '#52c41a',
              '不重要': '#52c41a'
            }
            return colorMap[severityName] || '#595959'
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

          // 根据任务类型获取图标（根据明文）
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

          // 根据重要程度获取对应的CSS类名（支持代码和名称）
          const getSeverityClassName = (severityValue) => {
            // 先尝试按名称映射
            const nameMap = {
              '紧急': 'severity-emergency',
              '重要': 'severity-high',
              '常规': 'severity-medium',
              '日常': 'severity-low',
              '不重要': 'severity-low'
            }
            // 再尝试按代码映射
            const codeMap = {
              '04': 'severity-emergency',
              '03': 'severity-high',
              '02': 'severity-medium',
              '01': 'severity-low'
            }
            return nameMap[severityValue] || codeMap[severityValue] || ''
          }

          // 清除HTML标签和URL，只保留纯文本
          const stripHtmlAndUrls = (html) => {
            if (!html) return ''
            let text = html.replace(/<[^>]*>/g, '')
            text = text.replace(/https?:\/\/[^\s]+/g, '')
            text = text.replace(/[\r\n\t]+/g, ' ')
            text = text.replace(/\s+/g, ' ')
            return text.trim()
          }

          // 获取正确的名称（使用后端返回的value字段，优先使用XXXValue字段）
          const workTypeName = getTaskTypeName(task.typeValue || task.type)
          const stateName = getStateName(task.statusValue || task.status)

          const creatorName = task.username || `用户${task.uid || task.createBy}`
          const headName = task.head || '未知'

          // 调试：检查原始工作数据
          console.log('原始工作数据字段检查:', {
            name: task.name,
            taskName: task.taskName,
            detail: task.detail,
            status: task.status,
            statusValue: task.statusValue,
            startTime: task.startTime,
            endTime: task.endTime
          })

          return {
            ...task,
            taskName: task.name || task.meetingName || task.taskName || '未命名工作',
            state: stateName,
            taskType: workTypeName,
            description: stripHtmlAndUrls(task.detail),
            formattedStartDate: formatDate(task.startTime || task.startDate),
            formattedEndDate: formatDate(task.endTime || task.endDate),
            stateClass: getStateClass(stateName),
            createBy: creatorName,
            head: headName
          }
        })

        // 应用排序
        const sortedResult = this.applySort(result)
        console.log('📋 处理后的工作列表:', sortedResult.length, '条')
        if (sortedResult.length > 0) {
          console.log('📋 第一条处理后的工作:', sortedResult[0])
        }
        return sortedResult
      } catch (error) {
        console.error('processedTasks处理失败:', error)
        return []
      }
    }
  },

  async onLoad(options) {
    // 清除旧的indexPageCache，防止加载任务数据
    uni.removeStorageSync('indexPageCache')

    const user = uni.getStorageSync('userInfo')
    if (!user) {
      uni.reLaunch({
        url: '/pages/login/login'
      })
      return
    }
    this.user = user
    // 保存用户角色和科室信息
    this.userRid = user.rid || user.role
    this.userDid = user.did || user.departmentId

    // 检查是否需要强制刷新（从登录页跳转过来）
    const forceRefresh = options.refresh === 'true'

    if (forceRefresh) {
      // 强制刷新，忽略缓存
      await this.initializeData()
    } else {
      // 尝试从缓存加载数据
      const cachedData = uni.getStorageSync('worksPageCache')

      if (cachedData && cachedData.timestamp) {
        // 检查缓存是否过期（5分钟）
        const cacheAge = Date.now() - cachedData.timestamp
        const isExpired = cacheAge > 5 * 60 * 1000

        if (!isExpired) {
          // 恢复基础数据（总是恢复）
          this.departments = cachedData.departments || []
          this.statusOptions = cachedData.statusOptions || []
          this.statusDict = cachedData.statusDict || {}
          this.collegeName = cachedData.collegeName || ''
          this.departmentTaskCounts = cachedData.departmentTaskCounts || {}
          this.statusCountCache = cachedData.statusCountCache || {} // 恢复状态统计缓存
          this.departmentStatusCache = cachedData.departmentStatusCache || {} // 恢复部门状态统计缓存

          // 检查是否为部门视图
          const wasDepartmentView = cachedData.isDepartmentView || false

          if (wasDepartmentView) {
            // 如果之前是部门视图，重置为全部工作视图并重新加载
            this.selectedDept = null
            this.isDepartmentView = false
            this.allTasks = []
            this.allTasksOriginal = []
            this.allTaskCount = 0
            this.totalCount = 0

            // 重新加载全部工作
            await this.loadAllTasks()

            // 更新部门工作数量
            await this.loadDepartmentTaskCounts()
          } else {
            // 如果是全部工作视图，恢复所有数据
            this.allTasks = cachedData.allTasks || []
            this.allTasksOriginal = cachedData.allTasksOriginal || []
            this.allTaskCount = cachedData.allTaskCount || 0
            this.totalCount = cachedData.allTaskCount || 0
            this.isDepartmentView = false
            this.selectedDept = null
          }

          this.isInitialized = true
        } else {
          console.log('缓存已过期，重新加载数据')
          await this.initializeData()
        }
      } else {
        console.log('无缓存数据，首次加载')
        await this.initializeData()
      }
    }

    // 启动自动刷新定时器（每10分钟刷新一次）
    this.startAutoRefresh()
  },

  async onUnload() {
    // 页面卸载时保存缓存、清除定时器和待处理请求
    this.saveDataToCache()
    this.stopAutoRefresh()
    cleanupPendingRequests() // 取消所有待处理的HTTP请求，防止ClientAbortException
  },

  async onHide() {
    // 页面隐藏时清除定时器和待处理请求
    this.stopAutoRefresh()
    cleanupPendingRequests() // 取消所有待处理的HTTP请求，防止ClientAbortException
  },

  async onShow() {
    console.log('工作页面显示，检查是否需要刷新数据')

    // 检查是否需要强制刷新（从操作页面返回）
    const pages_array = getCurrentPages()
    const currentPage = pages_array[pages_array.length - 1]
    const forceRefresh = currentPage.options && currentPage.options.forceRefresh === 'true'

    // 检查数据是否已存在且未过期（1分钟缓存）或者需要强制刷新
    const shouldRefresh = forceRefresh ||
                          !this.isInitialized ||
                          (Date.now() - (this.lastRefreshTime || 0) > 1 * 60 * 1000)

    if (shouldRefresh) {
      const refreshMsg = forceRefresh ? '检测到强制刷新参数，立即刷新数据' : '工作数据已过期或首次加载，开始刷新'
      console.log(refreshMsg)
      await this.loadAllTasks()

      // 同时更新部门任务数量缓存，确保显示最新的部门统计
      await this.loadDepartmentTaskCounts()
    } else {
      console.log('使用缓存工作数据，跳过刷新')
    }

    // 确保定时器只启动一次（检查是否已有定时器）
    if (!this.timerStarted) {
      console.log('重新启动自动刷新定时器')
      this.startAutoRefresh()
    }
  },

  methods: {
    // 获取状态数量（处理中文和UUID的映射）
    getStatusCountByValue(statusValue) {
      if (!statusValue) return 0

      const cache = this.selectedDept && this.selectedDept.id && this.departmentStatusCache[this.selectedDept.id]
        ? this.departmentStatusCache[this.selectedDept.id]
        : this.statusCountCache

      // 先尝试直接用 value 获取
      let count = cache[statusValue]

      // 如果没找到，尝试用 name 获取
      if (count === undefined) {
        const statusOption = this.statusOptions.find(s => s.value === statusValue)
        if (statusOption && statusOption.name) {
          count = cache[statusOption.name]
        }
      }

      return count || 0
    },

    // 显示筛选弹窗
    showFilterPopup() {
      this.showFilter = true
      // 初始化筛选条件为当前已应用的值
      this.filterStatus = this.selectedStatus
      this.filterDept = this.selectedDept
      this.filterSort = this.filterSortApplied || 'default' // 使用已应用的排序
      this.filterStartDate = this.appliedFilterStartDate
      this.filterEndDate = this.appliedFilterEndDate
      // 默认展开排序选项，方便用户查看当前排序
      this.expandedFilter = 'sort'
    },

    // 隐藏筛选弹窗
    hideFilterPopup() {
      this.showFilter = false
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

    // 获取筛选状态显示文本
    getFilterStatusText() {
      if (!this.filterStatus) return '全部'
      const status = this.statusOptions.find(s => s.value === this.filterStatus)
      return status ? status.name : '全部'
    },

    // 获取筛选排序显示文本
    getFilterSortText() {
      const sort = this.sortOptions.find(s => s.value === this.filterSort)
      return sort ? sort.name : '默认排序'
    },

    // 获取筛选部门显示文本
    getFilterDeptText() {
      if (!this.filterDept) return '全部部门'
      return this.filterDept.name
    },

    // 获取筛选日期范围显示文本
    getFilterDateRangeText() {
      if (!this.filterStartDate && !this.filterEndDate) return '不限'
      if (this.filterStartDate && this.filterEndDate) {
        return `${this.filterStartDate} 至 ${this.filterEndDate}`
      }
      if (this.filterStartDate) return `${this.filterStartDate} 起`
      if (this.filterEndDate) return `${this.filterEndDate} 止`
    },

    // 筛选开始日期变化
    onFilterStartDateChange(e) {
      this.filterStartDate = e.detail.value
    },

    // 筛选结束日期变化
    onFilterEndDateChange(e) {
      this.filterEndDate = e.detail.value
    },

    // 显示开始日期选择器
    showStartDatePicker() {
      // 如果已有选中的日期，使用它作为默认值
      if (this.filterStartDate) {
        this.startDateValue = new Date(this.filterStartDate).getTime()
      } else {
        this.startDateValue = new Date().getTime()
      }
      this.showStartDatePickerFlag = true
    },

    // 显示结束日期选择器
    showEndDatePicker() {
      // 如果已有选中的日期，使用它作为默认值
      if (this.filterEndDate) {
        this.endDateValue = new Date(this.filterEndDate).getTime()
      } else {
        this.endDateValue = new Date().getTime()
      }
      this.showEndDatePickerFlag = true
    },

    // 隐藏开始日期选择器
    hideStartDatePicker() {
      this.showStartDatePickerFlag = false
    },

    // 隐藏结束日期选择器
    hideEndDatePicker() {
      this.showEndDatePickerFlag = false
    },

    // 确认开始日期选择
    confirmStartDate(e) {
      console.log('确认开始日期，e:', e, '类型:', typeof e)

      // uView 的 datetime-picker 返回的是时间戳（数字）
      let timestamp = e
      if (e && e.value !== undefined) {
        timestamp = e.value
      }

      // 转换为 Date 对象
      let date
      if (typeof timestamp === 'number') {
        date = new Date(timestamp)
      } else if (typeof timestamp === 'string') {
        date = new Date(timestamp)
      } else if (timestamp instanceof Date) {
        date = timestamp
      } else {
        console.error('无法识别的日期格式:', timestamp)
        return
      }

      console.log('解析后的日期:', date)

      if (isNaN(date.getTime())) {
        console.error('日期格式错误:', date)
        return
      }

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      this.filterStartDate = `${year}-${month}-${day}`
      this.showStartDatePickerFlag = false
      this.startDateValue = timestamp // 更新为时间戳

      console.log('设置的开始日期:', this.filterStartDate)

      // 更新结束日期选择器的最小值
      this.startDatePickerMin = date.getTime()
    },

    // 确认结束日期选择
    confirmEndDate(e) {
      console.log('确认结束日期，e:', e, '类型:', typeof e)

      // uView 的 datetime-picker 返回的是时间戳（数字）
      let timestamp = e
      if (e && e.value !== undefined) {
        timestamp = e.value
      }

      // 转换为 Date 对象
      let date
      if (typeof timestamp === 'number') {
        date = new Date(timestamp)
      } else if (typeof timestamp === 'string') {
        date = new Date(timestamp)
      } else if (timestamp instanceof Date) {
        date = timestamp
      } else {
        console.error('无法识别的日期格式:', timestamp)
        return
      }

      console.log('解析后的日期:', date)

      if (isNaN(date.getTime())) {
        console.error('日期格式错误:', date)
        return
      }

      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      this.filterEndDate = `${year}-${month}-${day}`
      this.showEndDatePickerFlag = false
      this.endDateValue = timestamp // 更新为时间戳

      console.log('设置的结束日期:', this.filterEndDate)

      // 更新开始日期选择器的最大值
      this.endDatePickerMax = date.getTime()
    },

    // 清空日期范围筛选
    clearDateRangeFilter() {
      this.filterStartDate = ''
      this.filterEndDate = ''
    },

    // 选择筛选状态
    selectFilterStatus(status) {
      this.filterStatus = status
    },

    // 选择筛选排序
    selectFilterSort(sort) {
      this.filterSort = sort
    },

    // 应用排序到工作列表
    applySort(tasks) {
      if (!tasks || tasks.length === 0) {
        return tasks
      }

      // 创建排序后的副本
      const sortedTasks = [...tasks]

      switch (this.filterSort) {
        case 'createTime':
          // 按创建时间排序（最新的在前）
          sortedTasks.sort((a, b) => {
            const timeA = a.createTime ? new Date(a.createTime).getTime() : 0
            const timeB = b.createTime ? new Date(b.createTime).getTime() : 0
            return timeB - timeA // 降序
          })
          break

        case 'endTime':
          // 按截止时间排序（即将到期的在前）
          sortedTasks.sort((a, b) => {
            const timeA = a.endTime ? new Date(a.endTime).getTime() : Infinity
            const timeB = b.endTime ? new Date(b.endTime).getTime() : Infinity
            return timeA - timeB // 升序
          })
          break

        case 'priority':
          // 按优先级排序（紧急程度从高到低）
          sortedTasks.sort((a, b) => {
            // 优先级映射（使用importance字段）
            const priorityMap = {
              '04': 4,
              '03': 3,
              '02': 2,
              '01': 1,
              // 支持中文名称
              '紧急': 4,
              '重要': 3,
              '常规': 2,
              '日常': 1,
              '不重要': 0
            }

            const getPriorityValue = (task) => {
              const value = task.importance || task.imp || task.severityName
              return priorityMap[value] || 0
            }

            const priorityA = getPriorityValue(a)
            const priorityB = getPriorityValue(b)

            // 优先级高的在前（降序）
            if (priorityA !== priorityB) {
              return priorityB - priorityA
            }

            // 优先级相同时，按截止时间排序（即将到期的在前）
            const timeA = a.endTime ? new Date(a.endTime).getTime() : Infinity
            const timeB = b.endTime ? new Date(b.endTime).getTime() : Infinity
            return timeA - timeB
          })
          break

        case 'default':
        default:
          // 默认排序：按ID排序（最新的在前）
          sortedTasks.sort((a, b) => {
            const idA = a.id || 0
            const idB = b.id || 0
            return idB - idA // 降序
          })
          break
      }

      return sortedTasks
    },

    // 选择筛选部门
    selectFilterDept(dept) {
      this.filterDept = dept
    },

    // 重置筛选
    resetFilter() {
      this.filterStatus = ''
      this.filterSort = 'default'
      this.filterDept = null
      this.filterStartDate = ''
      this.filterEndDate = ''

      // 清除已应用的筛选（注意：不清除左侧导航栏选择的部门）
      this.selectedStatus = ''
      this.appliedFilterStartDate = ''
      this.appliedFilterEndDate = ''
      this.isFilterView = false
    },

    // 应用筛选
    async applyFilter() {
      console.log('应用筛选:', {
        status: this.filterStatus,
        dept: this.filterDept,
        startDate: this.filterStartDate,
        endDate: this.filterEndDate,
        sort: this.filterSort
      })

      // 应用筛选条件
      this.selectedStatus = this.filterStatus
      this.selectedDept = this.filterDept
      this.appliedFilterStartDate = this.filterStartDate
      this.appliedFilterEndDate = this.filterEndDate
      this.filterSortApplied = this.filterSort

      // 重置分页
      this.pageNum = 1
      this.hasMore = true

      // 判断是否需要调用后端API
      const hasFilterCondition = this.filterStartDate || this.filterEndDate || this.filterStatus || this.filterDept

      if (hasFilterCondition) {
        // 有筛选条件（日期、部门、状态），调用后端API
        await this.loadFilteredTasks()
      } else {
        // 只有排序条件，需要确保加载所有数据
        console.log('仅前端排序，检查是否需要加载所有数据')

        // 检查当前数据是否完整
        const currentTotal = this.allTasksOriginal.length
        const expectedTotal = this.allTaskCount || this.totalCount

        if (currentTotal < expectedTotal) {
          console.log(`数据不完整：当前 ${currentTotal} 条，总共 ${expectedTotal} 条，开始加载所有数据...`)
          // 加载所有数据（不分页）
          await this.loadAllTasksWithoutPagination()
        } else {
          console.log(`数据已完整：${currentTotal} 条工作`)
          // 数据完整，直接使用现有数据
          this.allTasks = [...this.allTasksOriginal]
          this.totalCount = this.allTasks.length
          this.hasMore = false
        }
      }

      this.hideFilterPopup()

      // 显示筛选结果提示
      if (hasFilterCondition) {
        uni.showToast({
          title: '已应用筛选',
          icon: 'success',
          duration: 2000
        })
      } else {
        // 仅排序时也显示提示
        uni.showToast({
          title: '已应用排序',
          icon: 'success',
          duration: 1500
        })
      }
    },

    // 加载筛选后的任务（综合筛选：部门+状态+时间）
    async loadFilteredTasks(isLoadMore = false) {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        if (!tid) {
          console.error('用户信息中tid为空，无法加载筛选工作')
          return
        }

        if (isLoadMore) {
          this.isLoadingMore = true
        } else {
          this.isLoading = true
          if (!isLoadMore) {
            this.pageNum = 1
          }
        }

        console.log('=== 加载筛选工作 ===')
        console.log('pageNum:', this.pageNum)
        console.log('当前筛选条件:', {
          dept: this.selectedDept,
          status: this.selectedStatus
        })

        // 构建查询参数
        const params = {
          tid: tid,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }

        // 添加部门筛选
        if (this.selectedDept && this.selectedDept.id) {
          params.did = this.selectedDept.id
          console.log('✓ 添加部门筛选 did:', this.selectedDept.id)
        }

        // 添加状态筛选
        if (this.selectedStatus) {
          // Works实体的status字段存储的是UUID，statusValue字段存储的是中文名称
          // 需要将中文名称转换为UUID后再传递给后端
          const statusUuid = this.statusDict[this.selectedStatus]

          if (statusUuid) {
            params.status = statusUuid
            console.log('✓ 添加状态筛选:', this.selectedStatus, '→ UUID:', statusUuid)
          } else {
            console.warn('⚠️ 无法找到状态对应的UUID:', this.selectedStatus)
            console.warn('statusDict:', this.statusDict)
          }
        }

        // 添加日期范围筛选
        if (this.appliedFilterStartDate) {
          params.startTime = this.appliedFilterStartDate
          console.log('✓ 添加开始时间筛选 startTime:', this.appliedFilterStartDate)
        }

        if (this.appliedFilterEndDate) {
          params.endTime = this.appliedFilterEndDate
          console.log('✓ 添加结束时间筛选 endTime:', this.appliedFilterEndDate)
        }

        console.log('最终筛选参数:', JSON.stringify(params, null, 2))

        console.log('正在调用接口...')
        const res = await workApi.searchWorksByConditionPage(params)
        console.log('接口调用完成')

        console.log('接口响应:', res.code, res.data)

        if (res.code === '200') {
          const pageData = res.data
          let newTasks = pageData.list || []

          console.log('✓ 获取到筛选工作数:', newTasks.length, '总记录数:', pageData.total)

          // 调试：输出前3条工作的详细信息
          if (newTasks.length > 0) {
            console.log('📋 前3条工作的详细信息:', newTasks.slice(0, 3).map(task => ({
              id: task.id,
              name: task.name,
              status: task.status,
              statusValue: task.statusValue,
              type: task.type,
              did: task.did,
              tid: task.tid
            })))

            // 🔧 如果字典中缺少UUID映射，从实际数据中提取
            if (Object.keys(this.statusDict).length === 0 || !this.statusDict['未开始']) {
              console.log('📝 从实际数据中提取状态UUID映射')
              newTasks.forEach(task => {
                if (task.status && task.statusValue && !this.statusDict[task.statusValue]) {
                  this.statusDict[task.statusValue] = task.status
                  console.log(`  添加映射: ${task.statusValue} → ${task.status}`)
                }
              })
            }
          } else {
            console.log('⚠️ 没有获取到任何工作数据')
            console.log('💡 提示：请检查数据库中是否有工作记录，以及status字段的实际值')
          }

          // 添加部门名称
          if (this.departments.length > 0) {
            newTasks = newTasks.map(task => {
              const dept = this.departments.find(d => d.id === task.did)
              return {
                ...task,
                deptName: dept ? dept.name : ''
              }
            })
          }

          if (isLoadMore) {
            this.allTasks = [...this.allTasks, ...newTasks]
            this.allTasksOriginal = [...this.allTasksOriginal, ...newTasks]
          } else {
            // 先清空数组，避免Vue检测到重复key
            this.allTasks = []
            this.allTasksOriginal = []
            // 使用nextTick确保Vue已经处理了清空操作
            await this.$nextTick()
            this.allTasks = newTasks
            this.allTasksOriginal = newTasks
          }

          this.totalCount = pageData.total
          this.allTaskCount = pageData.total
          this.hasMore = this.allTasks.length < pageData.total

          // 如果有任何筛选条件，标记为筛选视图
          this.isFilterView = !!(this.selectedDept || this.selectedStatus || this.appliedFilterStartDate || this.appliedFilterEndDate)

          console.log('✓ 筛选工作加载完成，当前显示:', this.allTasks.length, '总数:', this.totalCount)
        } else {
          console.error('✗ 获取筛选工作失败，响应码:', res.code)
          uni.showToast({
            title: '获取筛选工作失败',
            icon: 'none'
          })
        }
      } catch (error) {
        console.error('✗ 加载筛选工作失败:', error)
        uni.showToast({
          title: '加载筛选工作失败',
          icon: 'none'
        })
      } finally {
        this.isLoading = false
        this.isLoadingMore = false
      }
    },

    // 初始化数据方法
    async initializeData() {
      console.log('🚀 开始初始化数据...')

      try {
        // 获取学院名称（轻量级）
        await this.loadCollegeName()

        // 加载状态字典（轻量级）
        await this.loadStatusDict()

        // 加载部门数据（轻量级）
        await this.loadDepartments()

        // 加载状态统计（获取所有工作的状态，用于准确显示状态数量）
        await this.loadStatusStatistics()

        // 加载工作列表（分页，只加载第一页）
        await this.loadAllTasks()

        // 加载各部门工作数量统计
        await this.loadDepartmentTaskCounts()

        // 标记为已初始化
        this.isInitialized = true
        this.lastRefreshTime = Date.now()

        console.log('✅ 页面初始化完成，共加载', this.allTasks.length, '条任务')
        console.log('📊 任务总数:', this.totalCount)
      } catch (error) {
        console.error('❌ 初始化数据失败:', error)
      }
    },

    // 保存数据到缓存
    saveDataToCache() {
      const cacheData = {
        departments: this.departments,
        allTasks: this.allTasks,
        allTasksOriginal: this.allTasksOriginal,
        statusOptions: this.statusOptions,
        statusDict: this.statusDict,
        collegeName: this.collegeName,
        allTaskCount: this.allTaskCount,
        departmentTaskCounts: this.departmentTaskCounts,
        statusCountCache: this.statusCountCache, // 保存状态统计缓存
        departmentStatusCache: this.departmentStatusCache, // 保存部门状态统计缓存
        isDepartmentView: this.isDepartmentView, // 保存是否为部门视图
        timestamp: Date.now()
      }

      uni.setStorageSync('worksPageCache', cacheData)
      console.log('数据已保存到缓存，工作数:', this.allTasks.length)
    },

    // 启动自动刷新定时器
    startAutoRefresh() {
      // 防止重复启动
      if (this.timerStarted) {
        console.log('定时器已在运行，跳过重复启动')
        return
      }

      // 先清除已有的定时器
      this.stopAutoRefresh()

      console.log('启动自动刷新定时器，间隔:', this.refreshInterval / 1000 / 60, '分钟')

      // 标记定时器已启动
      this.timerStarted = true

      // 立即执行一次时间戳记录
      this.lastRefreshTime = new Date()

      this.refreshTimer = setInterval(async () => {
        const now = new Date()
        const elapsed = now - this.lastRefreshTime

        console.log('定时器触发，距离上次刷新:', Math.floor(elapsed / 1000), '秒')

        // 更新最后刷新时间
        this.lastRefreshTime = now

        // 刷新工作数据
        await this.refreshTasks()
      }, this.refreshInterval)
    },

    // 停止自动刷新定时器
    stopAutoRefresh() {
      if (this.refreshTimer) {
        console.log('清除自动刷新定时器')
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
        this.timerStarted = false
      }
    },

    // 刷新工作数据（静默刷新，只刷新状态统计，不刷新工作列表）
    async refreshTasks() {
      if (this.isLoading || this.isLoadingMore) {
        console.log('正在加载中，跳过此次刷新')
        return
      }

      try {
        console.log('开始自动刷新...')

        // 只刷新状态统计（轻量级）
        await this.loadStatusStatistics()

        // 只刷新部门工作数量统计
        await this.loadDepartmentTaskCounts()

        console.log('自动刷新完成')
      } catch (error) {
        console.error('自动刷新失败:', error)
      }
    },

    // 切换侧边栏显示状态
    toggleSidebar() {
      this.showSidebar = !this.showSidebar
      console.log('切换侧边栏，showSidebar:', this.showSidebar)
      console.log('部门列表:', this.departments)
      console.log('部门数量:', this.departments.length)
      console.log('当前选中部门:', this.selectedDept)

      // 如果部门列表为空，尝试加载
      if (this.departments.length === 0) {
        console.log('部门列表为空，尝试加载部门数据')
        this.loadDepartments()
      }
    },

    // 关闭侧边栏
    closeSidebar() {
      this.showSidebar = false
    },

    // 选择部门
    async selectDept(dept) {
      console.log('选择部门:', dept)
      this.selectedDept = dept
      this.closeSidebar()

      // 选择部门后，应用所有筛选条件（部门+状态+时间）
      // 清除筛选视图标记，使用综合筛选
      this.isFilterView = false

      // 重置分页
      this.pageNum = 1
      this.hasMore = true

      // 调用综合筛选接口
      await this.loadFilteredTasks()
    },

    // 返回顶部（修复：针对RefreshScrollView组件）
    backToTop() {
      // 由于使用了RefreshScrollView组件，需要滚动到组件内部顶部
      // 通过查询选择器找到scroll-view并滚动到顶部
      const query = uni.createSelectorQuery()
      query.select('.tasks-scroll').boundingClientRect()
      query.exec((res) => {
        if (res && res[0]) {
          // 使用scrollTo方法滚动到顶部
          uni.pageScrollTo({
            scrollTop: 0,
            duration: 300
          })
        }
      })
    },

    // 触底加载更多
    onReachBottom() {
      console.log('触底加载更多')

      // 如果正在加载或没有更多数据，直接返回
      if (this.isLoadingMore || this.isLoading || !this.hasMore) {
        console.log('跳过触底加载:', {
          isLoadingMore: this.isLoadingMore,
          isLoading: this.isLoading,
          hasMore: this.hasMore
        })
        return
      }

      // 加载更多数据
      this.loadMoreTasks()
    },

    // 加载状态统计（用于准确显示各状态任务数量）
    async loadStatusStatistics() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        if (!tid) {
          console.error('用户信息中tid为空，无法加载状态统计')
          return
        }

        console.log('📊 开始加载状态统计...')

        // 使用不分页接口获取所有工作（只用于状态统计）
        const res = await workApi.getAllWorksByTenant({ tid: tid })

        if (res.code === '200') {
          const allTasks = res.data || []

          // 清空并重新统计
          this.statusCountCache = {}
          this.departmentStatusCache = {}

          // 为每个部门初始化状态统计
          this.departments.forEach(dept => {
            this.departmentStatusCache[dept.id] = {}
          })

          allTasks.forEach(task => {
            // 获取工作状态
            let taskStatusValue = task.statusValue || task.status
            let taskStatusName = task.status || task.statusValue

            // 统计全部工作的状态
            if (taskStatusValue) {
              if (!this.statusCountCache[taskStatusValue]) {
                this.statusCountCache[taskStatusValue] = 0
              }
              this.statusCountCache[taskStatusValue]++
            }

            if (taskStatusName && taskStatusName !== taskStatusValue) {
              if (!this.statusCountCache[taskStatusName]) {
                this.statusCountCache[taskStatusName] = 0
              }
              this.statusCountCache[taskStatusName]++
            }

            // 统计各部门的任务状态
            if (task.did && this.departmentStatusCache[task.did]) {
              if (taskStatusValue) {
                if (!this.departmentStatusCache[task.did][taskStatusValue]) {
                  this.departmentStatusCache[task.did][taskStatusValue] = 0
                }
                this.departmentStatusCache[task.did][taskStatusValue]++
              }

              if (taskStatusName && taskStatusName !== taskStatusValue) {
                if (!this.departmentStatusCache[task.did][taskStatusName]) {
                  this.departmentStatusCache[task.did][taskStatusName] = 0
                }
                this.departmentStatusCache[task.did][taskStatusName]++
              }
            }
          })

          console.log('✅ 状态统计完成')
          console.log('📊 全部工作统计:', this.statusCountCache)
          console.log('📊 各部门统计:', this.departmentStatusCache)
        } else {
          console.error('❌ 获取状态统计失败，响应码:', res.code)
        }
      } catch (error) {
        console.error('❌ 加载状态统计失败:', error)
        // 即使失败也要初始化为空对象，避免后续出错
        if (!this.statusCountCache) {
          this.statusCountCache = {}
        }
        if (!this.departmentStatusCache) {
          this.departmentStatusCache = {}
        }
      }
    },

    // 加载学院名称
    async loadCollegeName() {
      const userInfo = uni.getStorageSync('userInfo') || {}
      if (userInfo.tenant) {
        this.collegeName = userInfo.tenant
      }
    },

    // 加载状态字典
    async loadStatusDict() {
      try {
        // Works表的status字段存储的是UUID，statusValue字段存储的是中文名称
        // 需要从字典接口获取状态数据，建立中文名称→UUID的映射
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        if (!tid) {
          console.warn('无法加载状态字典：tid为空，使用默认配置')
          this.initDefaultStatusOptions()
          return
        }

        // 调用字典接口获取状态数据
        const dictRes = await authApi.getDictData(tid)

        if (dictRes.code === '200' && dictRes.data) {
          console.log('获取到的字典数据:', dictRes.data)

          // 字典数据是树形结构，需要找到field='status'的节点
          let statusDictList = []

          const findStatusItems = (items) => {
            if (!Array.isArray(items)) return
            items.forEach(item => {
              // 检查是否是状态字典项（可能有不同的field值或没有field）
              if (item.field === 'status' || (!item.field && item.name && ['未开始', '进行中', '已完成'].includes(item.name))) {
                statusDictList.push(item)
              }
              // 递归查找子节点
              if (item.children && item.children.length > 0) {
                findStatusItems(item.children)
              }
            })
          }

          findStatusItems(dictRes.data)

          console.log('找到的状态字典项:', statusDictList)

          if (statusDictList.length > 0) {
            // 去重：按UUID或名称去重，确保没有重复的状态项
            const uniqueStatusList = []
            const seenUuids = new Set()
            const seenNames = new Set()

            statusDictList.forEach(item => {
              // 如果有UUID且未见过，或没有UUID但名称未见过，则添加
              if (item.id && !seenUuids.has(item.id)) {
                uniqueStatusList.push(item)
                seenUuids.add(item.id)
                seenNames.add(item.name)
              } else if (!item.id && !seenNames.has(item.name)) {
                uniqueStatusList.push(item)
                seenNames.add(item.name)
              }
            })

            console.log('去重后的状态字典项:', uniqueStatusList)

            // 构建状态选项和中文名称→UUID的映射（使用UUID作为唯一标识）
            this.statusOptions = uniqueStatusList.map((item, index) => ({
              id: item.id || `status_${index}`, // 使用UUID或生成唯一ID
              value: item.name, // 中文名称
              name: item.name,
              uuid: item.id // UUID
            }))

            // 构建中文名称→UUID的映射字典
            this.statusDict = {}
            uniqueStatusList.forEach(item => {
              this.statusDict[item.name] = item.id
            })

            console.log('✓ 状态选项:', this.statusOptions)
            console.log('✓ 状态字典（中文名→UUID）:', this.statusDict)
            console.log('✓ 检查重复key:', {
              optionsLength: this.statusOptions.length,
              uniqueKeys: new Set(this.statusOptions.map(s => s.uuid || s.value)).size
            })
          } else {
            console.warn('未找到状态字典，使用默认配置')
            this.initDefaultStatusOptions()
          }
        } else {
          console.warn('获取字典数据失败，使用默认配置')
          this.initDefaultStatusOptions()
        }
      } catch (error) {
        console.error('加载状态字典失败:', error)
        console.warn('使用默认状态配置')
        this.initDefaultStatusOptions()
      }
    },

    // 初始化默认状态选项（当无法获取字典数据时使用）
    initDefaultStatusOptions() {
      // 注意：这是fallback方案，需要从实际数据中提取UUID
      this.statusOptions = [
        { id: '1', value: '未开始', name: '未开始', uuid: '' },
        { id: '2', value: '进行中', name: '进行中', uuid: '' },
        { id: '3', value: '已完成', name: '已完成', uuid: '' }
      ]

      this.statusDict = {
        '未开始': '',
        '进行中': '',
        '已完成': ''
      }
    },

    // 选择任务状态
    async selectStatus(statusValue) {
      console.log('=== 点击状态按钮 ===')
      console.log('接收到的 statusValue:', statusValue)
      console.log('statusValue 类型:', typeof statusValue)

      // 如果点击的是当前已选中的状态，不做处理
      if (this.selectedStatus === statusValue) {
        console.log('状态未改变:', statusValue)
        return
      }

      // 检查状态选项的数据结构
      console.log('当前状态选项:', this.statusOptions)
      console.log('当前状态字典:', this.statusDict)

      const oldStatusName = this.selectedStatus ? (this.statusDict[this.selectedStatus] || this.selectedStatus) : '全部'
      const newStatusName = statusValue ? (this.statusDict[statusValue] || statusValue) : '全部'

      console.log('筛选状态改变:', oldStatusName, '->', newStatusName)
      this.selectedStatus = statusValue

      // 状态改变后，应用所有筛选条件（部门+状态+时间）
      // 重置分页
      this.pageNum = 1
      this.hasMore = true

      console.log('准备调用筛选接口，当前条件:', {
        dept: this.selectedDept,
        status: this.selectedStatus,
        statusType: typeof this.selectedStatus,
        startDate: this.appliedFilterStartDate,
        endDate: this.appliedFilterEndDate
      })

      // 调用综合筛选接口
      await this.loadFilteredTasks()

      console.log('状态筛选完成，当前显示任务数:', this.currentTasks.length)
    },

    // 获取指定状态的任务数量（只使用缓存数据，不受分页影响）
    getStatusCount(statusValue) {
      if (!statusValue) return 0

      let count = 0

      // 如果选择了特定部门，使用该部门的统计
      if (this.selectedDept && this.selectedDept.id) {
        const deptCache = this.departmentStatusCache[this.selectedDept.id]
        count = deptCache && deptCache[statusValue] ? deptCache[statusValue] : 0
      } else {
        // 使用全部工作的统计
        count = this.statusCountCache[statusValue] || 0
      }

      // 添加调试日志
      console.log('🔍 获取状态数量:', {
        statusValue,
        selectedDept: this.selectedDept?.name,
        count,
        allCache: this.statusCountCache,
        deptCache: this.selectedDept ? this.departmentStatusCache[this.selectedDept.id] : 'N/A'
      })

      return count
    },

    // 搜索任务
    handleSearch() {
      const keyword = this.searchKeyword ? this.searchKeyword.trim() : ''

      if (keyword) {
        console.log('执行搜索，关键词:', keyword)
      } else {
        console.log('清除搜索')
      }

      // 搜索通过 currentTasks 计算属性自动处理
      // 这里只是记录日志，便于调试
    },

    // 搜索输入事件（实时搜索）
    onSearchInput() {
      // 实时搜索，通过 currentTasks 计算属性自动处理
      if (this.searchKeyword && this.searchKeyword.trim()) {
        console.log('搜索关键词:', this.searchKeyword.trim())
      }
    },

    // 清除搜索
    clearSearch() {
      this.searchKeyword = ''
      console.log('搜索已清除')
    },
    // 获取部门工作数量
    getTaskCount(dept) {
      const deptId = dept.id || dept.did
      // 优先使用缓存的部门工作数量
      if (this.departmentTaskCounts[deptId] !== undefined) {
        return this.departmentTaskCounts[deptId]
      }
      // 如果没有缓存，返回0
      return 0
    },

    // 跳转到工作详情
    async goToTaskDetail(task) {
      const workId = task.id
      if (!workId) {
        uni.showToast({ title: '工作ID不存在', icon: 'none' })
        return
      }

      try {
        uni.showLoading({ title: '加载工作数据...' })

        const res = await request({
          url: '/works/getWorksDetail',
          method: 'GET',
          params: {
            id: workId
          }
        })

        uni.hideLoading()

        if (res.code === '200') {
          const workList = res.data
          if (workList && workList.length > 0) {
            const workData = workList[0] // getWorksDetail returns a list, get first item
            const encodedWork = encodeURIComponent(JSON.stringify(workData))
            uni.navigateTo({
              url: `/pages/work-detail/work-detail?work=${encodedWork}`
            })
          } else {
            uni.showToast({ title: '未找到工作数据', icon: 'none' })
          }
        } else {
          const encodedTask = encodeURIComponent(JSON.stringify(task))
          uni.navigateTo({
            url: `/pages/task-detail/task-detail?task=${encodedTask}`
          })
        }
      } catch (error) {
        uni.hideLoading()
        console.error('获取工作详情失败:', error)
        const encodedTask = encodeURIComponent(JSON.stringify(task))
        uni.navigateTo({
          url: `/pages/task-detail/task-detail?task=${encodedTask}`
        })
      }
    },

    // 初始化部门数据
    async loadDepartments() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        console.log('开始加载部门, tid:', tid)

        if (!tid) {
          console.error('用户信息中tid为空，无法获取部门列表')
          uni.showToast({
            title: '获取部门列表失败',
            icon: 'none'
          })
          return
        }

        const res = await request({
          url: '/department/getDepartmentByTid',
          method: 'GET',
          params: {
            tid: tid
          }
        })

        console.log('部门接口响应:', res)

        if (res.code === '200') {
          const depts = res.data || []
          this.departments = depts.map(dept => ({
            ...dept,
            did: dept.id
          }))
          console.log('✓ 部门加载完成, 数量:', this.departments.length)
          console.log('✓ 部门列表:', this.departments)

          // 保存到缓存
          this.saveDataToCache()
        } else {
          console.error('获取部门失败, code:', res.data?.code, 'msg:', res.data?.msg)
        }
      } catch (error) {
        console.error('加载部门数据失败:', error)
      }
    },

    // 获取所有工作（不分页，一次性加载所有数据）
    async loadAllTasks(isLoadMore = false) {
      // 直接调用不分页加载方法
      await this.loadAllTasksWithoutPagination()
    },

    // 不分页加载所有工作（用于排序功能）
    async loadAllTasksWithoutPagination() {
      this.isLoading = true
      this.pageNum = 1
      this.allTasks = []
      this.allTasksOriginal = []

      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        if (!tid) {
          console.error('❌ 用户信息中tid为空，无法加载工作')
          uni.showToast({
            title: '加载失败',
            icon: 'none'
          })
          return
        }

        let hasMore = true
        let page = 1
        let allTasks = []

        // 循环加载所有页
        while (hasMore) {
          const res = await workApi.searchWorksByConditionPage({
            tid: tid,
            pageNum: page,
            pageSize: 100  // 每页加载更多数据，减少请求次数
          })

          console.log(`📋 第${page}页API原始响应:`, res)

          if (res.code === '200' || res.code === 200) {
              const pageData = res.data
              let newTasks = []

              // 处理不同的数据结构
              if (Array.isArray(pageData)) {
                // 如果直接返回数组
                newTasks = pageData
              } else if (pageData && typeof pageData === 'object') {
                // 如果是对象，尝试获取list或records字段
                newTasks = pageData.list || pageData.records || pageData.data || []
              } else {
                console.warn('❓ 未知的数据结构:', pageData)
                hasMore = false
                break
              }

              console.log(`📋 第${page}页数据处理:`, {
                原始数据类型: typeof pageData,
                是否数组: Array.isArray(pageData),
                提取任务数: newTasks.length,
                示例任务: newTasks.length > 0 ? newTasks[0] : null
              })

            // 添加部门名称
            if (this.departments.length > 0) {
              newTasks = newTasks.map(task => {
                const dept = this.departments.find(d => d.id === task.did)
                return {
                  ...task,
                  deptName: dept ? dept.name : ''
                }
              })
            }

            allTasks = [...allTasks, ...newTasks]

            // 检查是否还有更多数据
            let totalPages = 0
            let total = 0

            if (typeof pageData === 'object' && pageData !== null && !Array.isArray(pageData)) {
              totalPages = pageData.pages || pageData.totalPages || 0
              total = pageData.total || pageData.totalCount || 0
            }

            hasMore = page < totalPages && newTasks.length > 0
            page++

            console.log(`✅ 第${page - 1}页加载成功:`, {
              当前页任务数: newTasks.length,
              累计任务数: allTasks.length,
              总页数: totalPages,
              总任务数: total,
              是否继续: hasMore
            })
          } else {
            console.error('❌ 获取工作失败, code:', res.code, 'msg:', res.msg, 'data:', res.data)
            hasMore = false

            // 如果是接口不存在错误，显示友好提示
            if (res.code === '404' || res.code === 404 || res.msg?.includes('不存在')) {
              console.error('🚨 接口不存在，检查后端服务是否正常运行')
              uni.showToast({
                title: '工作接口异常，请联系管理员',
                icon: 'none',
                duration: 3000
              })
            } else {
              uni.showToast({
                title: `加载失败: ${res.msg || '未知错误'}`,
                icon: 'none',
                duration: 2000
              })
            }
          }
        }

        // 保存所有工作
        this.allTasks = allTasks
        this.allTasksOriginal = allTasks
        this.totalCount = allTasks.length
        this.allTaskCount = allTasks.length
        this.hasMore = false

        console.log(`✅ 加载完成，共 ${allTasks.length} 条工作`)
        if (allTasks.length > 0) {
          console.log('📋 第一条工作数据:', allTasks[0])
        }
      } catch (error) {
        console.error('❌ 加载工作失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        this.isLoading = false
      }
    },

    // 加载指定部门的任务（使用分页接口）
    async loadDepartmentTasks(dept) {
      try {
        const deptId = dept.id || dept.did
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        console.log('加载部门任务, deptId:', deptId, 'deptName:', dept.name)

        // 重置分页状态
        this.pageNum = 1
        this.hasMore = true

        // 使用条件搜索分页接口获取部门任务
        const res = await workApi.searchWorksByConditionPage({
          tid: tid,
          did: deptId,
          pageNum: 1,
          pageSize: this.pageSize
        })

        console.log('部门任务接口返回:', {
          code: res.code,
          hasData: !!res.data,
          dataType: typeof res.data
        })

        if (res.code === '200') {
          const pageData = res.data
          const deptTasks = pageData.list || []

          console.log('分页数据:', {
            listLength: deptTasks.length,
            total: pageData.total,
            pageNum: pageData.pageNum,
            pageSize: pageData.pageSize,
            pages: pageData.pages
          })

          // 添加部门名称
          const tasksWithDeptName = deptTasks.map(task => ({
            ...task,
            deptName: dept.name
          }))

          this.allTasks = tasksWithDeptName
          this.allTasksOriginal = tasksWithDeptName

          // 使用分页返回的总数，而不是当前页的数量
          this.allTaskCount = pageData.total
          this.totalCount = pageData.total
          this.hasMore = deptTasks.length < pageData.total

          console.log('部门任务加载完成:', {
            currentTasks: this.allTasks.length,
            totalTasks: this.allTaskCount,
            hasMore: this.hasMore
          })
        } else {
          console.error('❌ 获取部门任务失败，响应码:', res.code)
          this.allTasks = []
          this.allTasksOriginal = []
          this.allTaskCount = 0
          this.totalCount = 0
        }
      } catch (error) {
        console.error('❌ 加载部门任务失败:', error)
        this.allTasks = []
        this.allTasksOriginal = []
        this.allTaskCount = 0
        this.totalCount = 0
      }
    },

    // 分页加载部门任务（用于触底加载更多）
    async loadDepartmentTasksPage(dept) {
      try {
        this.isLoadingMore = true
        const deptId = dept.id || dept.did
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        console.log('分页加载部门任务, deptId:', deptId, 'pageNum:', this.pageNum)

        // 使用条件搜索分页接口获取部门任务
        const res = await workApi.searchWorksByConditionPage({
          tid: tid,
          did: deptId,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        })

        if (res.code === '200') {
          const pageData = res.data
          const deptTasks = pageData.list || []

          // 添加部门名称
          const tasksWithDeptName = deptTasks.map(task => ({
            ...task,
            deptName: dept.name
          }))

          // 追加到现有工作列表
          this.allTasks = [...this.allTasks, ...tasksWithDeptName]
          this.allTasksOriginal = [...this.allTasksOriginal, ...tasksWithDeptName]

          // 更新是否有更多数据
          this.hasMore = this.allTasks.length < pageData.total

          console.log('部门任务加载完成, 当前总任务数:', this.allTasks.length)
        } else {
          console.error('获取部门任务失败，响应码:', res.code)
          this.hasMore = false
        }
      } catch (error) {
        console.error('加载部门任务失败:', error)
        this.hasMore = false
      } finally {
        this.isLoadingMore = false
      }
    },

    // 加载各部门工作数量
    async loadDepartmentTaskCounts() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const tid = userInfo.tid || ''

        console.log('开始加载各部门工作数量')

        // 并行加载所有部门的任务数量
        const promises = this.departments.map(async (dept) => {
          try {
            const res = await workApi.getWorksByDepartment({
              tid: tid,
              did: dept.id
            })

            if (res.code === '200') {
              const count = (res.data || []).length
              this.$set(this.departmentTaskCounts, dept.id, count)
              console.log('部门', dept.name, '任务数:', count)
            }
          } catch (error) {
            console.error('加载部门工作数量失败:', dept.name, error)
            this.$set(this.departmentTaskCounts, dept.id, 0)
          }
        })

        await Promise.all(promises)

        console.log('各部门工作数量加载完成:', this.departmentTaskCounts)

        // 保存到缓存
        this.saveDataToCache()
      } catch (error) {
        console.error('加载各部门工作数量失败:', error)
      }
    },

    // 根据部门名称获取对应的图标
    getDeptIcon(deptName) {
      const iconMap = {
        '院办': '🏢',
        '研办': '📚',
        '科研办': '🔬',
        '学生部': '👨‍🎓',
        '财务部': '💰',
        '人力资源部': '👥',
        '信息技术部': '💻',
        '工办': '🛠️'
      }
      return iconMap[deptName] || '📋'
    },

    // 监听滚动事件
    onScroll(e) {
      const scrollTop = e.detail.scrollTop

      // 返回顶部按钮显示逻辑
      this.showBackToTop = scrollTop > 300
    },

    // 下拉刷新（已禁用）
    async onRefresh() {
      try {
        console.log('🔄 开始刷新工作数据')

        // 重新加载所有工作数据
        await this.loadAllTasks()

        // 重新加载状态统计
        await this.loadStatusStatistics()

        // 重新加载部门工作数量
        await this.loadDepartmentTaskCounts()

        console.log('✅ 工作数据刷新完成')

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

    // 跳转到新增工作页面
    goToAddWork() {
      uni.navigateTo({
        url: '/pages/works-add/works-add'
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
  width: 0;
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


/* 左侧导航栏样式 */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2000;
  visibility: hidden;
  transition: visibility 0.3s ease;

  &.show {
    visibility: visible;

    .sidebar-content {
      transform: translateX(0);
    }

    .sidebar-overlay {
      opacity: 1;
    }
  }
}

.sidebar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.sidebar-content {
  position: absolute;
  top: 0;
  left: 0;
  width: 70%;
  max-width: 500rpx;
  height: 100%;
  background: #ffffff;
  box-shadow: 8rpx 0 30rpx rgba(0, 0, 0, 0.15);
  transform: translateX(-100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 60rpx 32rpx 32rpx;
  border-bottom: 1rpx solid $border-base;
  background: linear-gradient(135deg, $primary-color 0%, #0247b3 100%);
}

.sidebar-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
}

.sidebar-close {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;

  &:active {
    background: rgba(255, 255, 255, 0.3);
    transform: scale(0.9);
  }
}

.close-icon {
  font-size: 48rpx;
  color: #ffffff;
  line-height: 1;
  font-weight: 300;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
}

.dept-list {
  padding: 20rpx 0;
}

.dept-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  transition: all 0.3s ease;
  cursor: pointer;
  border-left: 6rpx solid transparent;

  &:active {
    background: $bg-base;
  }

  &.active {
    background: $primary-light;
    border-left-color: $primary-color;

    .dept-item-name {
      color: $primary-color;
      font-weight: 600;
    }
  }
}

.dept-item-icon {
  font-size: 44rpx;
  margin-right: 20rpx;
}

.dept-item-name {
  flex: 1;
  font-size: 30rpx;
  color: $text-primary;
  transition: all 0.3s ease;
}

.dept-item-badge {
  padding: 4rpx 16rpx;
  background: $bg-base;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: $text-regular;
  font-weight: 600;
}

/* 头部区域 */
.header-section {
  padding: 30rpx 40rpx;
}

/* 部门图标按钮 - 只显示图标 */
.dept-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  background: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  flex-shrink: 0;
  align-self: center;

  &:active {
    transform: scale(0.95);
    background: #f5f7fa;
  }
}

.dept-icon-text {
  font-size: 32rpx;
}

.dept-icon-label {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
}

.dept-icon-arrow {
  font-size: 40rpx;
  color: $text-secondary;
  font-weight: 300;
}

/* 搜索和筛选区域 */
.search-filter-section {
  position: fixed;
  top: 100rpx;
  left: 0;
  right: 0;
  z-index: 998;
  background: #ffffff;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

/* 搜索容器 */
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
  padding: 0 8rpx;
  padding-left: 20rpx;
  overflow: hidden;
}

/* 左侧部门图标 - 融入搜索框 */
.search-dept-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68rpx;
  height: 68rpx;
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-right: 8rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
  border: none;

  &:active {
    transform: scale(0.95);
    background: #e8e8e8;
  }
}

.dept-icon {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dept-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f5f5;
  border-radius: 36rpx;
}

.dept-badge-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #666666;
  line-height: 1;
  text-shadow: 0 1rpx 2rpx rgba(255, 255, 255, 0.5);
}

.line-1 {
  width: 36rpx;
}

.line-2 {
  width: 28rpx;
}

.line-3 {
  width: 20rpx;
}

/* 分隔线 */
.search-divider {
  width: 2rpx;
  height: 40rpx;
  background: #e0e0e0;
  margin: 0 4rpx;
  flex-shrink: 0;
}

/* 搜索输入框包装器 */
.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 16rpx 0 4rpx;
  gap: 8rpx;
  min-width: 0;
  max-width: calc(100% - 160rpx);
}

/* 搜索图标 */
.search-icon {
  font-size: 32rpx;
  color: #999;
  flex-shrink: 0;
  line-height: 1;
}

/* 搜索输入框 */
.search-input {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: #333;
  background: transparent;
  border: none;
  padding: 0;
  line-height: 1;
}

/* 搜索框占位符样式 */
.search-placeholder {
  color: #999;
  font-size: 28rpx;
}

/* 搜索清除按钮 */
.search-clear-btn {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 8rpx;
  border-radius: 50%;
  background: #e0e0e0;
  transition: all 0.3s ease;
}

.search-clear-btn:active {
  transform: scale(0.9);
  background: #d0d0d0;
}

.clear-icon {
  font-size: 32rpx;
  color: #666;
  line-height: 1;
  font-weight: 300;
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

/* 筛选弹窗 - 从底部弹出 */
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
  font-weight: bold;
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

/* 状态筛选区域 - 淘宝风格 */
.state-filter-scroll {
  position: fixed;
  top: 182rpx;
  left: 0;
  right: 0;
  z-index: 997;
  background: #ffffff;
  padding: 12rpx 24rpx 8rpx;
  white-space: nowrap;
}

.state-filter {
  display: flex;
  gap: 12rpx;
  min-height: 60rpx;
  align-items: center;
}

.state-btn {
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

.state-btn:active {
  transform: scale(0.95);
}

.btn-text {
  font-size: 26rpx;
  color: #023c99;
  white-space: nowrap;
  font-weight: 600;
}

.btn-count {
  font-size: 22rpx;
  color: #023c99;
  background: rgba(2, 60, 153, 0.08);
  padding: 4rpx 10rpx;
  border-radius: 14rpx;
  min-width: 32rpx;
  text-align: center;
  font-weight: 600;
}

.state-btn.active {
  background: #023c99;
  border-color: #023c99;
}

.state-btn.active .btn-text {
  color: white;
}

.state-btn.active .btn-count {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

/* 符合条件的工作总数徽章 */
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

/* 工作列表区域 */
.tasks-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tasks-scroll {
  height: 100%;
}

/* 任务容器 */
.tasks-container {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 4rpx 32rpx 32rpx;
  width: 100%;
  box-sizing: border-box;
}

/* 任务项包装器 */
.task-item-wrapper {
  width: 100%;
  margin-bottom: 24rpx;
}

/* 任务卡片样式 - 新设计 */
.task-card {
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

/* 卡片左侧区域 */
.task-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding-right: 20rpx;
}

/* 工作名称 */
.task-name {
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

/* 任务标签容器 */
.task-tags {
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
}

/* 任务标签 */
.task-tag {
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

/* 重要程度标签 */
.severity-tag {
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.15);
}

/* 卡片右侧区域 */
.task-right {
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

/* 任务信息行 */
.task-info-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  line-height: 1.4;
}

/* 信息标签 */
.task-info-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  min-width: 70rpx;
  flex-shrink: 0;
}

/* 信息值 */
.task-info-value {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 1);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 不同重要程度的颜色样式 */
.task-tag.severity-tag.severity-emergency {
  background-color: rgba(255, 77, 79, 0.9);
  border: 1rpx solid rgba(255, 204, 199, 0.5);
}

.task-tag.severity-tag.severity-high {
  background-color: rgba(250, 140, 22, 0.9);
  border: 1rpx solid rgba(255, 213, 145, 0.5);
}

.task-tag.severity-tag.severity-medium {
  background-color: rgba(82, 196, 26, 0.9);
  border: 1rpx solid rgba(183, 235, 143, 0.5);
}

.task-tag.severity-tag.severity-low {
  background-color: rgba(24, 144, 255, 0.9);
  border: 1rpx solid rgba(145, 213, 255, 0.5);
}

/* 任务状态颜色 */
.task-tag.state-tag.state-pending {
  background-color: rgba(24, 144, 255, 0.9);
}

.task-tag.state-tag.state-progress {
  background-color: rgba(250, 173, 20, 0.9);
}

.task-tag.state-tag.state-completed {
  background-color: rgba(82, 196, 26, 0.9);
}

.task-tag.state-tag.state-overdue {
  background-color: rgba(255, 77, 79, 0.9);
}

.task-tag.state-tag.state-default {
  background-color: rgba(144, 147, 153, 0.9);
}

/* 空状态 */
.task-empty {
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
  .task-card {
    flex-direction: column;
  }

  .task-left {
    padding-right: 0;
    padding-bottom: 12rpx;
    border-bottom: 1rpx solid rgba(255, 255, 255, 0.2);
  }

  .task-right {
    padding-left: 0;
    padding-top: 12rpx;
    border-left: none;
    min-width: auto;
    max-width: none;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .task-info-row {
    flex: 1;
    min-width: 45%;
  }

  .task-name {
    font-size: 30rpx;
  }

  .tasks-container {
    padding: 20rpx;
    gap: 16rpx;
  }

  .task-card {
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