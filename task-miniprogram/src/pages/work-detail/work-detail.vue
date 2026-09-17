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
            <text class="title-text">工作详情</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 内容区域 -->
    <view class="content">
      <!-- 工作标题卡片 -->
      <view class="task-title-card">
        <text class="task-title">{{ work.taskName || '暂无工作名称' }}</text>
        <view class="task-tags">
          <view class="task-tag state-tag" :class="work.stateClass">
            {{ work.state || '暂无状态' }}
          </view>
          <view
            class="task-tag severity-tag"
            :style="{
              background: severityBackground,
              boxShadow: severityShadow
            }"
          >
            {{ work.severityName || '暂无' }}
          </view>
        </view>
      </view>

      <!-- 基本信息卡片 -->
      <view class="info-card">
        <view class="card-title">
          <text class="title-icon">📋</text>
          <text class="title-text">基本信息</text>
        </view>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">任务类型</text>
            <text class="info-value">{{ work.taskType || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">计划周期</text>
            <text class="info-value">{{ work.cycle || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">申报人</text>
            <text class="info-value">{{ work.createBy || work.uid || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">负责人</text>
            <text class="info-value">{{ work.head || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">所属部门</text>
            <text class="info-value">{{ work.deptName || '暂无' }}</text>
          </view>
        </view>
      </view>

      <!-- 时间信息卡片 -->
      <view class="info-card">
        <view class="card-title">
          <text class="title-icon">📅</text>
          <text class="title-text">时间信息</text>
        </view>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">开始时间</text>
            <text class="info-value">{{ work.startDate || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">结束时间</text>
            <text class="info-value">{{ work.endDate || '暂无' }}</text>
          </view>
          <view class="info-item full-width">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ work.createTime || '暂无' }}</text>
          </view>
        </view>
      </view>

      <!-- 工作详情卡片 -->
      <view class="content-card">
        <view class="card-title">
          <text class="title-icon">📝</text>
          <text class="title-text">工作详情</text>
        </view>
        <view class="card-content">{{ work.description || '暂无工作详情' }}</view>

        <!-- 工作详情附件 -->
        <view class="attachments-container" v-if="attachments.detail.length > 0">
          <view class="attachment-title">附件列表</view>
          <view class="attachment-list">
            <view
              class="attachment-item"
              v-for="(file, index) in attachments.detail"
              :key="index"
              @click="downloadAttachment(file.url)"
            >
              <text class="attachment-icon">📄</text>
              <text class="attachment-name">{{ file.fileName }}</text>
              <text class="download-tip">点击下载</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 工作成果卡片 -->
      <view class="content-card">
        <view class="card-title">
          <text class="title-icon">✅</text>
          <text class="title-text">工作成果</text>
        </view>
        <view class="card-content">{{ work.taskResult || '暂无工作成果' }}</view>

        <!-- 工作成果附件 -->
        <view class="attachments-container" v-if="attachments.result.length > 0">
          <view class="attachment-title">附件列表</view>
          <view class="attachment-list">
            <view
              class="attachment-item"
              v-for="(file, index) in attachments.result"
              :key="index"
              @click="downloadAttachment(file.url)"
            >
              <text class="attachment-icon">📄</text>
              <text class="attachment-name">{{ file.fileName }}</text>
              <text class="download-tip">点击下载</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 备注卡片 -->
      <view class="content-card" v-if="work.remark">
        <view class="card-title">
          <text class="title-icon">💡</text>
          <text class="title-text">备注</text>
        </view>
        <view class="card-content">{{ work.remark || '暂无备注' }}</view>
      </view>

    <!-- 底部操作栏 -->
    <view class="bottom-action-bar">
      <view class="action-buttons">
        <view class="action-btn edit-btn" @tap="editWork">
          <text class="btn-text">修改工作</text>
        </view>
        <view class="action-btn delete-btn" @tap="deleteWork">
          <text class="btn-text">删除工作</text>
        </view>
      </view>
    </view>
    </view>
  </view>
</template>

<script>
import { workApi, departmentApi } from '@/api'
import config from '@/config'

export default {
  data() {
    return {
      work: {},
      rawWork: {},  // 保存原始工作数据，用于编辑
      departments: [],
      attachments: {
        detail: [],
        result: []
      }
    }
  },

  computed: {
    severityBackground() {
      const colorMap = {
        '紧急': 'linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%)',
        '重要': 'linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)',
        '常规': 'linear-gradient(135deg, #52c41a 0%, #389e0d 100%)',
        '日常': 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)'
      }
      return colorMap[this.work.severityName] || 'rgba(144, 147, 153, 0.9)'
    },
    severityShadow() {
      const shadowMap = {
        '紧急': '0 2rpx 8rpx rgba(255, 77, 79, 0.4)',
        '重要': '0 2rpx 8rpx rgba(250, 140, 22, 0.4)',
        '常规': '0 2rpx 8rpx rgba(82, 196, 26, 0.4)',
        '日常': '0 2rpx 8rpx rgba(24, 144, 255, 0.4)'
      }
      return shadowMap[this.work.severityName] || '0 2rpx 8rpx rgba(0, 0, 0, 0.1)'
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

    if (options.work) {
      try {
        let work = JSON.parse(decodeURIComponent(options.work))
        console.log('原始工作数据:', work)
        this.initWorkData(work)
        await this.getWorkAttachments(work.id)
      } catch (e) {
        console.error('解析工作数据失败:', e)
      }
    }
  },

  methods: {
    // 初始化工作数据
    initWorkData(work) {
      console.log('initWorkData - 输入:', work) // 调试日志

      // 保存原始工作数据，用于编辑
      this.rawWork = work

      // 根据任务重要程度获取对应的CSS类名
      const getSeverityClassName = (severityCode) => {
        const classMap = {
          '04': 'severity-emergency', // 紧急
          '03': 'severity-high',      // 重要
          '02': 'severity-medium',    // 常规
          '01': 'severity-low'        // 日常
        }
        return classMap[severityCode] || ''
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

      // 根据任务状态code获取名称
      const getStateName = (stateCode) => {
        const stateMap = {
          '01': '已完成',
          '02': '进行中',
          '03': '已撤销'
        }
        return stateMap[stateCode] || stateCode
      }

      // 根据任务重要程度code获取名称
      const getSeverityName = (severityCode) => {
        const severityMap = {
          '04': '紧急',
          '03': '重要',
          '02': '常规',
          '01': '日常'
        }
        return severityMap[severityCode] || severityCode
      }

      // 根据工作类型获取名称
      const getTaskTypeName = (typeCode) => {
        // 对于工作，type字段直接就是类型名称，不需要从字典查找
        return typeCode || '暂无'
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

      // 处理工作数据，确保所有模板中使用的属性都有值
      const processedWork = {
        ...work,
        // 基本信息
        taskName: work.name || work.taskName || '暂无工作名称',
        taskType: getTaskTypeName(work.typeValue || work.taskType || work.type),
        cycle: work.cycleValue || work.cycle || '暂无',

        // 状态和重要程度
        state: getStateName(work.statusValue || work.state || work.status),
        stateClass: getStateClass(getStateName(work.statusValue || work.state || work.status)),
        severityName: getSeverityName(work.impValue || work.imp || work.severity),
        severityClassName: getSeverityClassName(work.impValue || work.imp || work.severity),

        // 人员信息
        createBy: work.username || `用户${work.uid || work.createBy || '未知'}`,
        head: work.head || '暂无',
        deptName: work.deptName || (work.did && this.departments.length > 0 ? (this.departments.find(d => d.id === work.did)?.name || '') : '') || '暂无',

        // 时间信息
        startDate: work.startDate || work.startTime || '暂无',
        endDate: work.endDate || work.endTime || '暂无',
        createTime: work.createTime || '暂无',

        // 其他信息
        progress: work.progress || '0%',
        description: stripHtmlAndUrls(work.description || work.detail || ''),
        taskResult: stripHtmlAndUrls(work.taskResult || work.achievement || ''),
        remark: work.remark || '暂无',

        // 确保taskId存在
        id: work.id || work.pid || ''
      }

      console.log('initWorkData - 输出:', processedWork) // 调试日志

      // 调试重要性信息
      console.log('重要性详细信息:', {
        原始值: work.impValue || work.imp || work.severity,
        severityName: processedWork.severityName,
        severityClassName: processedWork.severityClassName,
        '是否为空': !processedWork.severityClassName
      })

      this.work = processedWork
      console.log('this.work:', this.work) // 调试日志

      // 处理原始任务数据中的attachments字段
      if (work.attachments && Array.isArray(work.attachments)) {
        console.log('处理原始任务数据中的attachments:', work.attachments)
        // 清空现有附件
        this.attachments.detail = []
        this.attachments.result = []

        // 根据type字段分类附件
        work.attachments.forEach(file => {
          const attachment = {
            id: file.id,
            fileName: this.getFileName(file.name || file.url),
            url: file.name || file.url,
            type: file.type,
            size: file.size
          }

          // 根据type字段添加到不同的附件列表
          // 支持多种可能的结果类型值
          const resultTypes = ['achievement', 'result', '成果'];
          if (resultTypes.includes(file.type)) {
            this.attachments.result.push(attachment)
          } else {
            this.attachments.detail.push(attachment)
          }
        })

        console.log('处理后的附件数据:', this.attachments)
      }
    },

    // 从URL中提取文件名（只显示真实文件名，去掉UUID和时间戳）
    getFileName(fileUrl) {
      if (!fileUrl) return '未知文件'
      const urlStr = typeof fileUrl === 'string' ? fileUrl : ''
      if (!urlStr) return '未知文件'

      // 先从URL中提取完整文件名
      const parts = urlStr.split('/')
      let fileName = parts[parts.length - 1] || '未知文件'
      // 移除可能的查询参数
      fileName = fileName.split('?')[0]

      // 处理文件名格式：uuid-timestamp-真实文件名
      const lastDashIndex = fileName.lastIndexOf('-')
      if (lastDashIndex > 0 && lastDashIndex < fileName.length - 1) {
        const afterLastDash = fileName.substring(lastDashIndex + 1)
        // 如果后面部分看起来像真实文件名（包含文件扩展名），则使用它
        if (afterLastDash.includes('.') && afterLastDash.length > 4) {
          return afterLastDash
        }
      }

      return fileName
    },

    // 获取任务附件
    async getWorkAttachments(pid) {
      if (!pid) return

      try {
        // 调用后端接口获取工作附件
        const res = await workApi.getWorksFiles({ pid: pid })

        console.log('获取工作附件响应:', res)

        if (res.code === '200') {
          const files = res.data || []

          console.log('原始附件数据详情:', files)

          // 清空现有附件
          this.attachments.detail = []
          this.attachments.result = []

          // 根据type字段分类附件
          files.forEach((file, index) => {
            console.log(`处理附件 ${index + 1}:`, {
              id: file.id,
              name: file.name,
              type: file.type,
              createTime: file.createTime,
              size: file.size
            })

            const attachment = {
              id: file.id,
              fileName: this.getFileName(file.name),
              url: file.name,
              type: file.type,
              createTime: file.createTime,
              size: file.size
            }

            // 根据type字段添加到不同的附件列表
            // 支持多种可能的结果类型值
            const resultTypes = ['achievement', 'result', '成果'];
            if (resultTypes.includes(file.type)) {
              this.attachments.result.push(attachment)
            } else {
              this.attachments.detail.push(attachment)
            }
          })

          console.log('处理后的附件数据:', this.attachments)
        }
      } catch (error) {
        console.error('获取任务附件失败:', error)
      }
    },

    // 下载附件
    downloadAttachment(fileUrl) {
      if (!fileUrl) return

      const token = uni.getStorageSync('token') || ''
      if (!token) {
        uni.showToast({
          title: '请先登录',
          icon: 'none'
        })
        return
      }

      console.log('downloadAttachment - 文件路径:', fileUrl)
      console.log('downloadAttachment - baseURL:', config.baseURL)

      try {

        // ===== H5：浏览器直接下载 =====
        // #ifdef H5
        // H5环境使用header传递token，不放在URL中
        const downloadUrl = `${config.baseURL}/files/download?file=${encodeURIComponent(fileUrl)}`

        // 创建一个隐藏的iframe来触发下载，这样可以携带cookie中的token信息
        const iframe = document.createElement('iframe')
        iframe.style.display = 'none'
        iframe.src = downloadUrl

        // 由于axios已经设置了token拦截器，我们需要先设置cookie
        document.body.appendChild(iframe)

        uni.showToast({
          title: '开始下载',
          icon: 'none'
        })

        // 5秒后移除iframe
        setTimeout(() => {
          document.body.removeChild(iframe)
        }, 5000)
        return
        // #endif


        // ===== 小程序 / App =====
        // 使用header传递token，而不是URL参数
        uni.downloadFile({
          url: `${config.baseURL}/files/download?file=${encodeURIComponent(fileUrl)}`,
          header: {
            'token': token  // 使用与axios相同的token header名称
          },
          success: (res) => {
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

            } else if (res.statusCode === 401) {
              uni.showToast({
                title: '登录已过期，请重新登录',
                icon: 'none'
              })
              // 跳转到登录页
              setTimeout(() => {
                uni.reLaunch({
                  url: '/pages/login/login'
                })
              }, 1500)
            } else {
              console.error('下载失败，状态码:', res.statusCode)
              uni.showToast({
                title: `下载失败: ${res.statusCode}`,
                icon: 'none'
              })
            }
          },
          fail: (error) => {
            console.error('下载文件失败:', error)
            uni.showToast({
              title: '文件下载失败',
              icon: 'none'
            })
          }
        })

      } catch (error) {
        console.error('下载附件异常:', error)
      }
    },

// 返回上一页
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
            // 如果 navigateBack 失败，尝试直接跳转到工作页面
            uni.redirectTo({
              url: '/pages/works/works',
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
          url: '/pages/works/works',
          fail: () => {
            console.log('redirectTo 失败')
          }
        })
      }
    },

    // 编辑工作
    editWork() {
      if (!this.rawWork.id) {
        uni.showToast({
          title: '工作ID不存在',
          icon: 'none'
        })
        return
      }

      // 将原始工作数据传递给编辑页面
      const encodedWork = encodeURIComponent(JSON.stringify(this.rawWork))

      // 跳转到工作编辑页面
      uni.navigateTo({
        url: `/pages/works-add/works-add?mode=edit&work=${encodedWork}`
      })
    },

    // 删除工作
    deleteWork() {
      if (!this.work.id) {
        uni.showToast({
          title: '工作ID不存在',
          icon: 'none'
        })
        return
      }

      // 显示确认对话框
      uni.showModal({
        title: '确认删除',
        content: '删除后将无法恢复，是否确认删除该工作？',
        confirmText: '确认删除',
        confirmColor: '#ff4d4f',
        success: async (res) => {
          if (res.confirm) {
            await this.confirmDeleteWork()
          }
        }
      })
    },

    // 确认删除工作
    async confirmDeleteWork() {
      uni.showLoading({
        title: '删除中...',
        mask: true
      })

      try {
        const res = await workApi.updateWorks({
          ...this.work,
          isdelete: '1'
        })

        uni.hideLoading()

        if (res.code === '200' || res.code === 200) {
          uni.showToast({
            title: '删除成功',
            icon: 'success',
            duration: 1500
          })

          setTimeout(() => {
            // 返回工作主页并强制刷新
            uni.redirectTo({
              url: '/pages/works/works?forceRefresh=true'
            })
          }, 1500)
        } else if (res.code === '401' || res.code === 403) {
          throw new Error('您没有权限进行此操作')
        } else {
          throw new Error(res.msg || '删除失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('删除工作失败:', error)
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

<style>
page {
  --status-bar-height: 44px;
}
</style>

<style scoped>
.title-icon {
  font-size: 40rpx;
}

.title-text {
  flex: 1;
}

/* 内容样式 */
.content {
  font-size: 32rpx;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  background-color: #fafafa;
  padding: 25rpx;
  border-radius: 12rpx;
  border: 1rpx solid #f0f2f5;
  min-height: 200rpx;
}

/* 附件列表容器样式 */
.attachments-container {
  margin-top: 30rpx;
  padding: 20rpx;
  background-color: #f8f9fa;
  border-radius: 12rpx;
  border: 1rpx solid #e9ecef;
}

/* 附件项样式 */
.attachment-item {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background-color: white;
  border-radius: 8rpx;
  margin-bottom: 15rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1rpx solid #e9ecef;
}

.attachment-item:last-child {
  margin-bottom: 0;
}

.attachment-item:active {
  transform: scale(0.98);
  background-color: #f0f0f0;
}

/* 附件信息样式 */
.attachment-info {
  display: flex;
  align-items: center;
  gap: 15rpx;
  width: 100%;
}

/* 附件图标样式 */
.attachment-icon {
  font-size: 36rpx;
  flex-shrink: 0;
}

/* 附件名称样式 */
.attachment-name {
  font-size: 30rpx;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 下载按钮样式 */
.attachment-download {
  font-size: 26rpx;
  color: #1890ff;
  padding: 8rpx 16rpx;
  background-color: rgba(24, 144, 255, 0.1);
  border-radius: 6rpx;
  flex-shrink: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .info-row {
    flex-direction: column;
  }
  
  .info-item {
    min-width: 100%;
  }
  
  .task-title {
    font-size: 42rpx;
  }
}

/* 附件列表样式 */
.attachments-container {
  margin-top: 20rpx;
  padding: 20rpx;
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx dashed #ddd;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background-color: white;
  border-radius: 8rpx;
  margin-bottom: 15rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  border: 1rpx solid #e9ecef;
}

.attachment-item:last-child {
  margin-bottom: 0;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 15rpx;
  flex: 1;
  overflow: hidden;
}

.attachment-icon {
  font-size: 32rpx;
  flex-shrink: 0;
}

.attachment-name {
  font-size: 28rpx;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}



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

/* 工作标题卡片 */
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
  margin-bottom: 16rpx;
  display: block;
}

.task-tags {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

.task-tag {
  padding: 8rpx 16rpx;
  border-radius: 50rpx;
  font-size: 24rpx;
  color: white;
  font-weight: 500;
  backdrop-filter: blur(10rpx);
}

.state-tag {
  background: rgba(255, 255, 255, 0.2);
}

.severity-tag {
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  background: rgba(144, 147, 153, 0.9);
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

/* 内容卡片 */
.content-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.08);
}

.card-content {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 附件区域 */
.attachments-container {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $border-base;
}

.attachment-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 16rpx;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx;
  background: $bg-base;
  border-radius: 12rpx;
  transition: all 0.3s ease;

  &:active {
    background: darken($bg-base, 5%);
    transform: scale(0.98);
  }
}

.attachment-icon {
  font-size: 32rpx;
  flex-shrink: 0;
}

.attachment-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.download-tip {
  font-size: 24rpx;
  color: $text-secondary;
  flex-shrink: 0;
}

/* 状态标签颜色 */
.task-tag.state-tag.state-completed {
  background-color: rgba(82, 196, 26, 0.9);
}

.task-tag.state-tag.state-progress {
  background-color: rgba(250, 173, 20, 0.9);
}

.task-tag.state-tag.state-overdue {
  background-color: rgba(255, 77, 79, 0.9);
}

.task-tag.state-tag.state-pending {
  background-color: rgba(24, 144, 255, 0.9);
}

.task-tag.state-tag.state-default {
  background-color: rgba(144, 147, 153, 0.9);
}

/* 重要程度标签颜色 - 增强对比度 */
.task-tag.severity-tag.severity-emergency {
  background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%) !important;
  box-shadow: 0 2rpx 8rpx rgba(255, 77, 79, 0.4) !important;
  font-weight: 600 !important;
  border: 1rpx solid rgba(255, 77, 79, 0.3) !important;
}

.task-tag.severity-tag.severity-high {
  background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%) !important;
  box-shadow: 0 2rpx 8rpx rgba(250, 140, 22, 0.4) !important;
  font-weight: 600 !important;
  border: 1rpx solid rgba(250, 140, 22, 0.3) !important;
}

.task-tag.severity-tag.severity-medium {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%) !important;
  box-shadow: 0 2rpx 8rpx rgba(82, 196, 26, 0.4) !important;
  font-weight: 600 !important;
  border: 1rpx solid rgba(82, 196, 26, 0.3) !important;
}

.task-tag.severity-tag.severity-low {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%) !important;
  box-shadow: 0 2rpx 8rpx rgba(24, 144, 255, 0.4) !important;
  font-weight: 600 !important;
  border: 1rpx solid rgba(24, 144, 255, 0.3) !important;
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