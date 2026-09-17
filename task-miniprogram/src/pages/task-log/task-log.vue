<template>
  <view class="container">
    <!-- 使用uView UI的navbar组件，带返回功能 -->
    <u-navbar
      title="任务日志"
      :bgColor="'#1a73e8'"
      :titleStyle="{ color: '#fff', fontWeight: 'bold' }"
      :autoBack="true"
      backText="返回"
    >
    </u-navbar>
    
    <!-- 安全区域容器，所有内容都放在里面 -->
    <view class="safe-content">
      <!-- 历史日志列表区域（顶部显示） -->
      <view class="history-logs-section" v-if="selectedTaskLogs.length > 0">
        <view class="section-title">历史日志</view>
        <scroll-view scroll-y class="history-logs-list">
          <view class="log-item" v-for="(log, index) in selectedTaskLogs" :key="index">
            <view class="log-header">
              <text class="log-person-name">{{log.logPerson}}</text>
              <text class="log-date">{{log.createTime}}</text>
            </view>
            <view class="log-content">{{log.content}}</view>
          </view>
        </scroll-view>
      </view>

      <!-- 任务列表和日志表单区域 -->
      <view class="main-content">
        <!-- 任务列表 -->
        <scroll-view scroll-y class="task-list">
          <view 
            v-for="(item, index) in processedTasks" 
            :key="index"
            class="task-item"
            :class="{
              'task-gray': index % 2 === 0,
              'task-beige': index % 2 !== 0,
              'task-animate': true
            }"
            :style="{
              borderLeft: '12rpx solid ' + item.severityColor,
              '--item-index': index
            }"
            @tap="selectTask(item)"
          >
            <view class="task-info">
              <view class="task-header">
              <view class="task-name-container">
                <text class="task-name">{{item.taskName}}</text>
                <view class="severity-tag" :class="item.severityClassName">
                  {{item.severityName}}
                </view>
              </view>
              <view class="task-state" :class="item.stateClass">
                {{item.state}}
              </view>
            </view>
              <view class="task-meta">
                <view class="task-type">
                  <text class="type-icon">{{item.typeIcon}}</text>
                  {{item.taskType}}
                </view>
                <view class="task-date">
                  <text class="date-icon">日期</text>
                  {{item.formattedStartDate}} - {{item.formattedEndDate}}
                </view>
                <view class="task-person">
                  <text class="person-icon">负责人:</text>
                  {{item.head || '未知'}}
                </view>
                <view class="task-progress">
                  <text class="progress-icon">进度</text>
                  <view class="progress-container">
                    <view class="progress-bar" :style="{width: item.progress}"></view>
                    <text class="progress-text-inside">{{item.progress}}</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <view class="empty-tip" v-if="processedTasks.length === 0">
            <text>{{searchKeyword ? '未找到相关任务' : '暂无任务'}}</text>
          </view>
        </scroll-view>
        
        <!-- 日志填写表单 -->
        <view class="log-form" v-if="selectedTask">
          <view class="form-card">
            <view class="form-header">
              <text class="form-title">记录日志</text>
              <text class="selected-task-name">{{selectedTask.taskName}}</text>
            </view>
            
            <!-- 日志内容 -->
            <view class="form-item">
              <text class="label">日志内容 <text class="required">*</text></text>
              <textarea 
                class="textarea" 
                placeholder="请输入日志内容"
                v-model="logForm.content"
                rows="6"
                maxlength="500"
              ></textarea>
              <text class="word-count">{{logForm.content.length}}/500</text>
            </view>
            
            <!-- 日志人 -->
            <view class="form-item">
              <text class="label">日志人</text>
              <input 
                class="input" 
                placeholder="请输入日志人姓名"
                v-model="logForm.logPerson"
                maxlength="20"
              />
            </view>
            
            <!-- 操作按钮 -->
            <view class="form-actions">
              <view class="btn-cancel" @tap="cancelLog">取消</view>
              <view class="btn-save" @tap="saveLog">保存日志</view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
// 导入mock数据
import { mockTaskTypeDict } from '@/mock/dict.js'
import { taskApi, logApi } from '@/api'

export default {
  data() {
    return {
      // 任务相关
      tasks: [],
      searchKeyword: '',
      selectedTask: null,
      selectedTaskLogs: [],
      
      // 任务类型字典
      taskTypeDictionary: [],
      
      // 日志表单数据
      logForm: {
        content: '',
        logPerson: '',
        taskId: ''
      },
      
      // 用户信息
      userInfo: uni.getStorageSync('userInfo') || {}
    }
  },
  
  async onLoad(options) {
    // 加载任务类型字典
    try {
      this.taskTypeDictionary = await mockTaskTypeDict()
    } catch (error) {
      console.error('加载任务类型字典失败:', error)
    }

    // 获取用户信息
    this.userInfo = uni.getStorageSync('userInfo') || {}

    // 设置默认日志人
    if (this.userInfo.name) {
      this.logForm.logPerson = this.userInfo.name
    }

    // 检查是否从task页面传递了任务数据
    if (options.task) {
      try {
        const task = JSON.parse(decodeURIComponent(options.task))

        // 设置选中的任务
        this.selectedTask = task
        this.logForm.taskId = task.id || task.pid
        this.logForm.content = ''

        // 获取该任务的历史日志
        this.getTaskLogs(task.id || task.pid)
      } catch (error) {
        console.error('解析任务数据失败:', error)
      }
    } else {
      // 如果没有传递任务数据，获取任务列表供用户选择
      this.getTasks()
    }
  },
  
  methods: {
    // 获取任务列表
    async getTasks() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const { uid, tid } = userInfo

        // 调用后端接口获取任务列表
        const res = await taskApi.getPlanByPerson({
          uid: uid || '',
          tid: tid || ''
        })

        if (res.code === '200') {
          this.tasks = res.data || []
        }
      } catch (error) {
        console.error('获取任务列表失败:', error)
      }
    },
    
    // 选择任务
    selectTask(task) {
      this.selectedTask = task
      this.logForm.taskId = task.id
      this.logForm.content = ''
      
      // 获取该任务的历史日志
      this.getTaskLogs(task.id)
    },
    
    // 获取任务日志
    async getTaskLogs(taskId) {
      try {
        // 调用后端接口获取任务日志
        const res = await logApi.getPlanLogs({
          pid: taskId
        })

        if (res.code === '200') {
          this.selectedTaskLogs = res.data || []
        } else {
          this.selectedTaskLogs = []
        }
      } catch (error) {
        console.error('获取任务日志失败:', error)
        this.selectedTaskLogs = []
      }
    },
    
    // 保存日志
    async saveLog() {
      if (!this.validateLogForm()) {
        return
      }
      
      try {
        const userInfo = uni.getStorageSync('userInfo') || {}
        const uid = userInfo.id || userInfo.uid || ''
        
        // 准备提交的数据
        const logData = {
          pid: this.logForm.taskId,
          content: this.logForm.content,
          logPerson: this.logForm.logPerson,
          uid: uid
        }
        
        // 调用后端接口保存日志
        const res = await logApi.addPlanLog(logData)

        if (res.code === '200') {
          uni.showToast({
            title: '保存成功',
            icon: 'success'
          })

          // 清空表单并重新获取日志
          this.logForm.content = ''
          this.getTaskLogs(this.logForm.taskId)
        }
      } catch (error) {
        console.error('保存日志失败:', error)
      }
    },
    
    // 取消日志
    cancelLog() {
      this.selectedTask = null
      this.selectedTaskLogs = []
      this.logForm.content = ''
    },
    
    // 验证日志表单
    validateLogForm() {
      if (!this.logForm.content.trim()) {
        uni.showToast({ title: '请输入日志内容', icon: 'none' })
        return false
      }
      
      if (!this.logForm.logPerson.trim()) {
        uni.showToast({ title: '请输入日志人姓名', icon: 'none' })
        return false
      }
      
      return true
    },
    
    // 搜索输入
    onSearchInput() {
      // 可以添加防抖处理
      this.searchTasks()
    },
    
    // 清除搜索
    onClearSearch() {
      this.searchKeyword = ''
      this.getTasks()
    },
    
    // 搜索任务
    async searchTasks() {
      if (!this.searchKeyword) {
        this.getTasks()
        return
      }

      try {
        // 调用后端搜索接口
        const res = await taskApi.searchPlanByCondition({
          name: this.searchKeyword
        })

        if (res.code === '200') {
          this.tasks = res.data || []
        }
      } catch (error) {
        console.error('搜索任务失败:', error)
      }
    }
  },
  
  computed: {
    // 筛选任务，支持关键词搜索
    filteredTasks() {
      const { tasks, searchKeyword } = this
      
      return tasks.filter(task => {
        // 关键词筛选
        const keywordMatch = !searchKeyword || 
          (task.name && task.name.toLowerCase().includes(searchKeyword.toLowerCase()))
        
        return keywordMatch
      })
    },
    
    // 处理任务数据，添加计算属性
    processedTasks() {
      return this.filteredTasks.map(task => {
        // 格式化日期
        const formatDate = (dateStr) => {
          if (!dateStr) return ''
          const date = new Date(dateStr)
          return `${date.getMonth() + 1}月${date.getDate()}日`
        }
        
        // 根据任务状态获取名称
        const getStateName = (stateName) => {
          return stateName || '未知状态'
        }
        
        // 根据任务重要程度获取名称
        const getSeverityName = (severityName) => {
          return severityName || '未知'
        }
        
        // 根据任务类型获取名称
        const getTaskTypeName = (taskTypeName) => {
          return taskTypeName || '未知类型'
        }
        
        // 根据任务重要程度获取颜色
        const getSeverityColor = (severityName) => {
          const colorMap = {
            '紧急': '#ff4d4f', // 紧急
            '重要': '#ff7a45', // 重要
            '常规': '#faad14', // 常规
            '日常': '#52c41a', // 日常
            '不重要': '#52c41a' // 不重要
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
        
        // 根据重要程度获取对应的CSS类名
        const getSeverityClassName = (severityName) => {
          const classMap = {
            '紧急': 'severity-emergency', // 紧急
            '重要': 'severity-high',      // 重要
            '常规': 'severity-medium',    // 常规
            '日常': 'severity-low',       // 日常
            '不重要': 'severity-low'      // 不重要
          }
          return classMap[severityName] || ''
        }
        
        // 获取正确的名称
        const taskTypeName = getTaskTypeName(task.type) // 后端直接传递type为明文
        const stateName = getStateName(task.status) // 后端直接传递status为明文
        const severityName = getSeverityName(task.imp) // 后端直接传递imp为明文
        
        return {
          ...task,
          taskName: task.name, // 后端返回的是name，前端模板使用的是taskName
          state: stateName, // 后端返回的是status（明文）
          taskType: taskTypeName, // 后端返回的是type（明文）
          formattedStartDate: formatDate(task.startTime),
          formattedEndDate: formatDate(task.endTime),
          severityColor: getSeverityColor(severityName), // 使用明文名称获取颜色
          stateClass: getStateClass(stateName),
          typeIcon: getTypeIcon(taskTypeName),
          severityClassName: getSeverityClassName(severityName), // 使用明文名称获取CSS类
          severityName: severityName, // 直接使用明文名称
          progress: `${task.progress || 0}%` // 确保进度值带百分比符号
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
  width: 100%;
  box-sizing: border-box;
}

/* 安全区域容器，所有内容都放在里面 */
.safe-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  /* 顶部边距，确保内容在导航栏之下 */
  padding-top: 100rpx;
  /* 底部边距，确保内容在底部手势区域之上 */
  padding-bottom: 30rpx;
  box-sizing: border-box;
  width: 100%;
  overflow: hidden;
}

/* 历史日志区域样式 */
.history-logs-section {
  background-color: white;
  border-radius: 20rpx;
  padding: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  border: 1rpx solid #f0f2f5;
  margin-bottom: 20rpx;
  max-height: 300rpx;
  flex-shrink: 0;
}

.section-title {
  font-size: 32rpx;
  color: #303133;
  font-weight: 600;
  margin-bottom: 15rpx;
  padding-bottom: 10rpx;
  border-bottom: 1rpx solid #f0f2f5;
}

.history-logs-list {
  max-height: 240rpx;
  overflow-y: auto;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 20rpx;
  padding: 0 20rpx;
  overflow: hidden;
}

/* 任务列表 */
.task-list {
  flex: 1;
  background-color: white;
  border-radius: 20rpx;
  padding: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  border: 1rpx solid #f0f2f5;
  overflow-y: auto;
}

/* 任务项样式 */
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 30rpx;
  border-radius: 16rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12), 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  position: relative;
  border: 1rpx solid rgba(0, 0, 0, 0.08);
  cursor: pointer;
}

.task-item:active {
  transform: scale(0.98);
}

.task-gray {
  background-color: #f8f8f8;
}

.task-beige {
  background-color: #fff8f0;
}

.task-info {
  flex: 1;
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
  padding: 8rpx 20rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  font-weight: bold;
  white-space: nowrap;
  margin-left: 10rpx;
}

/* 不同重要程度的颜色样式 */
.severity-emergency {
  background-color: #fff1f0;
  color: #ff4d4f;
  border: 2rpx solid #ffccc7;
  animation: pulse 2s infinite;
}

.severity-high {
  background-color: #fff7e6;
  color: #fa8c16;
  border: 2rpx solid #ffd591;
  animation: pulse 2s infinite;
}

.severity-medium {
  background-color: #f6ffed;
  color: #52c41a;
  border: 2rpx solid #b7eb8f;
}

.severity-low {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 2rpx solid #91d5ff;
}

/* 任务状态样式 */
.task-state {
  padding: 8rpx 16rpx;
  border-radius: 40rpx;
  font-size: 20rpx;
  font-weight: 500;
  white-space: nowrap;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.1);
  border: 1rpx solid rgba(0, 0, 0, 0.08);
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

/* 空任务提示 */
.empty-tip {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 32rpx;
}

/* 日志表单样式 */
.log-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  min-width: 500rpx;
  max-width: 600rpx;
}

.form-card {
  background-color: white;
  border-radius: 20rpx;
  padding: 35rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  border: 1rpx solid #f0f2f5;
}

.form-header {
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f2f5;
}

.form-title {
  font-size: 36rpx;
  color: #303133;
  font-weight: 600;
  margin-bottom: 10rpx;
  display: block;
}

.selected-task-name {
  font-size: 28rpx;
  color: #606266;
  font-weight: 500;
}

.form-item {
  margin-bottom: 35rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.label {
  font-size: 32rpx;
  color: #303133;
  font-weight: 600;
}

.required {
  color: #ff4d4f;
}

.input {
  width: 100%;
  height: 90rpx;
  padding: 0 20rpx;
  border: 1rpx solid #dcdfe6;
  border-radius: 10rpx;
  font-size: 32rpx;
  box-sizing: border-box;
  background-color: #fafafa;
}

.input:focus {
  border-color: #409eff;
  outline: none;
}

.textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 20rpx;
  border: 1rpx solid #dcdfe6;
  border-radius: 10rpx;
  font-size: 32rpx;
  box-sizing: border-box;
  background-color: #fafafa;
  resize: none;
}

.textarea:focus {
  border-color: #409eff;
  outline: none;
}

.word-count {
  font-size: 24rpx;
  color: #909399;
  align-self: flex-end;
  margin-top: 5rpx;
}

.form-actions {
  display: flex;
  gap: 20rpx;
  justify-content: flex-end;
  margin-top: 40rpx;
}

.btn-cancel, .btn-save {
  padding: 20rpx 40rpx;
  border-radius: 10rpx;
  font-size: 32rpx;
  font-weight: 600;
  transition: all 0.3s ease;
  cursor: pointer;
}

.btn-cancel {
  background-color: #f5f7fa;
  color: #606266;
  border: 1rpx solid #dcdfe6;
}

.btn-cancel:active {
  background-color: #e4e7ed;
}

.btn-save {
  background-color: #1a73e8;
  color: white;
  box-shadow: 0 4rpx 12rpx rgba(26, 115, 232, 0.3);
}

.btn-save:active {
  background-color: #1557b0;
  transform: scale(0.98);
}

/* 日志项样式 */
.log-item {
  padding: 20rpx;
  border-bottom: 1rpx solid #f0f2f5;
  margin-bottom: 20rpx;
}

.log-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.log-person-name {
  font-size: 30rpx;
  color: #303133;
  font-weight: 600;
}

.log-date {
  font-size: 24rpx;
  color: #909399;
}

.log-content {
  font-size: 28rpx;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  background-color: #fafafa;
  padding: 20rpx;
  border-radius: 8rpx;
  border: 1rpx solid #f0f2f5;
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

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  
  .log-form {
    min-width: auto;
    max-width: none;
  }
}
</style>