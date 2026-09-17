<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      <view class="nav-content">
        <view class="nav-back-wrapper">
          <view class="nav-back" @tap.stop="goBack" @click.stop="goBack">
            <text class="back-icon">←</text>
          </view>
        </view>
        <view class="nav-title-wrapper">
          <view class="nav-title">
            <text class="title-text">任务详情</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 内容区域 -->
    <view class="content">
      <!-- 任务标题卡片 -->
      <view class="task-title-card">
        <text class="task-title">{{ task.taskName || '暂无任务名称' }}</text>
        <view class="task-tags">
          <view class="task-tag state-tag" :class="task.stateClass">
            {{ task.state || '暂无状态' }}
          </view>
          <view
            class="task-tag severity-tag"
            :style="{
              background: severityBackground,
              boxShadow: severityShadow
            }"
          >
            {{ task.severityName || '暂无' }}
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
            <text class="info-value">{{ task.taskType || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">计划周期</text>
            <text class="info-value">{{ task.cycle || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">申报人</text>
            <text class="info-value">{{ task.createBy || task.uid || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">负责人</text>
            <text class="info-value">{{ task.head || '暂无' }}</text>
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
            <text class="info-value">{{ task.startDate || '暂无' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">结束时间</text>
            <text class="info-value">{{ task.endDate || '暂无' }}</text>
          </view>
          <view class="info-item full-width">
            <text class="info-label">创建时间</text>
            <text class="info-value">{{ task.createTime || '暂无' }}</text>
          </view>
        </view>
      </view>

      <!-- 任务详情卡片 -->
      <view class="content-card">
        <view class="card-title">
          <text class="title-icon">📝</text>
          <text class="title-text">任务详情</text>
        </view>
        <view class="card-content">{{ task.description || '暂无任务详情' }}</view>

        <!-- 任务详情附件 -->
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

      <!-- 任务结果卡片 -->
      <view class="content-card">
        <view class="card-title">
          <text class="title-icon">✅</text>
          <text class="title-text">任务结果</text>
        </view>
        <view class="card-content">{{ task.taskResult || '暂无任务结果' }}</view>

        <!-- 任务结果附件 -->
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
      <view class="content-card" v-if="task.remark">
        <view class="card-title">
          <text class="title-icon">💡</text>
          <text class="title-text">备注</text>
        </view>
        <view class="card-content">{{ task.remark || '暂无备注' }}</view>
      </view>

      <!-- 底部操作栏 -->
      <view class="bottom-action-bar">
        <view class="edit-btn" @tap="editTask">
          <text class="btn-text">修改任务</text>
        </view>
        <view class="delete-btn" @tap="deleteTask">
          <text class="btn-text">删除任务</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { taskApi, logApi, userApi, fileApi, tenantApi, authApi, roleApi, dictApi } from '@/api'
import { mockTaskTypeDict } from '@/mock/dict.js'
import config from '@/config'

export default {
  data() {
    return {
      task: {},
      taskTypeDictionary: [],
      attachments: {
        detail: [], // 任务详情附件
        result: []  // 任务结果附件
      }
    }
  },

  computed: {
    // 根据重要性名称获取对应的背景色
    severityBackground() {
      const colorMap = {
        '紧急': 'linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%)',
        '重要': 'linear-gradient(135deg, #fa8c16 0%, #d46b08 100%)',
        '常规': 'linear-gradient(135deg, #52c41a 0%, #389e0d 100%)',
        '日常': 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)'
      }
      return colorMap[this.task.severityName] || 'rgba(144, 147, 153, 0.9)'
    },
    // 根据重要性名称获取对应的阴影色
    severityShadow() {
      const shadowMap = {
        '紧急': '0 2rpx 8rpx rgba(255, 77, 79, 0.4)',
        '重要': '0 2rpx 8rpx rgba(250, 140, 22, 0.4)',
        '常规': '0 2rpx 8rpx rgba(82, 196, 26, 0.4)',
        '日常': '0 2rpx 8rpx rgba(24, 144, 255, 0.4)'
      }
      return shadowMap[this.task.severityName] || '0 2rpx 8rpx rgba(0, 0, 0, 0.1)'
    }
  },

  async onLoad(options) {
    // 加载任务类型字典
    try {
      this.taskTypeDictionary = await mockTaskTypeDict()
    } catch (error) {
      console.error('加载任务类型字典失败:', error)
    }

    // 接收从dept页面传递的任务数据
    if (options.task) {
      try {
        let task = JSON.parse(decodeURIComponent(options.task))

        // 直接使用传递的数据初始化页面
        this.initTaskData(task)

        // 异步获取任务附件，不阻塞页面显示和导航
        this.getTaskAttachments(task.id || task.pid)
      } catch (e) {
        console.error('解析任务数据失败:', e)
      }
    }
  },

  methods: {
    // 初始化任务数据
    initTaskData(task) {

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

      // 根据任务类型code获取名称
      const getTaskTypeName = (typeCode) => {
        const typeItem = this.taskTypeDictionary.find(item => item.code === typeCode)
        return typeItem ? typeItem.name : typeCode
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

      // 处理任务数据，确保所有模板中使用的属性都有值
      const processedTask = {
        ...task,
        // 基本信息
        taskName: task.name || task.taskName || '暂无任务名称',
        taskType: getTaskTypeName(task.typeValue || task.taskType || task.type),
        cycle: task.cycleValue || task.cycle || '暂无',

        // 状态和重要程度
        state: getStateName(task.statusValue || task.state || task.status),
        stateClass: getStateClass(getStateName(task.statusValue || task.state || task.status)),
        severityName: getSeverityName(task.impValue || task.imp || task.severity),
        severityClassName: getSeverityClassName(task.impValue || task.imp || task.severity),

        // 人员信息
        createBy: task.username || `用户${task.uid || task.createBy || '未知'}`,
        head: task.head || '暂无',

        // 时间信息
        startDate: task.startDate || task.startTime || '暂无',
        endDate: task.endDate || task.endTime || '暂无',
        createTime: task.createTime || '暂无',

        // 其他信息
        progress: task.progress || '0%',
        description: stripHtmlAndUrls(task.description || task.detail || ''),
        taskResult: stripHtmlAndUrls(task.taskResult || task.achievement || ''),
        remark: task.remark || '暂无',

        // 确保taskId存在
        id: task.id || task.pid || ''
      }

      this.task = processedTask

      // 处理原始任务数据中的attachments字段
      if (task.attachments && Array.isArray(task.attachments) && task.attachments.length > 0) {
        console.log('处理传递的任务附件数据:', task.attachments.length)

        // 清空现有附件
        this.attachments.detail = []
        this.attachments.result = []

        // 根据type字段分类附件
        task.attachments.forEach((file, index) => {
          console.log(`处理传递的附件 ${index + 1}:`, {
            id: file.id,
            name: file.name || file.url,
            type: file.type,
            size: file.size
          })

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
            console.log('传递的附件分类为结果附件')
            this.attachments.result.push(attachment)
          } else {
            console.log('传递的附件分类为详情附件')
            this.attachments.detail.push(attachment)
          }
        })

        console.log('传递的附件分类完成:', {
          detail: this.attachments.detail.length,
          result: this.attachments.result.length
        })

      } else {
        console.log('传递的任务数据中没有附件，将尝试从API获取')
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
      // 找到最后一个 '-' 后面的内容，即真实文件名
      const lastDashIndex = fileName.lastIndexOf('-')
      if (lastDashIndex > 0 && lastDashIndex < fileName.length - 1) {
        // 检查是否匹配UUID-timestamp-文件名的格式
        const afterLastDash = fileName.substring(lastDashIndex + 1)
        // 如果后面部分看起来像真实文件名（包含文件扩展名），则使用它
        if (afterLastDash.includes('.') && afterLastDash.length > 4) {
          return afterLastDash  // 返回真实文件名
        }
      }

      return fileName  // 如果格式不匹配，返回完整文件名
    },

    // 获取任务附件
    async getTaskAttachments(pid) {
      if (!pid) {
        console.warn('任务ID为空，跳过附件加载')
        return
      }

      console.log('开始获取任务附件，任务ID:', pid)

      try {
        // 使用Promise.race设置10秒超时，给予更多时间
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('Attachment load timeout')), 10000)
        })

        const res = await Promise.race([
          taskApi.getPlanFiles({ pid: pid }),
          timeoutPromise
        ])

        console.log('获取任务附件响应:', res)

        if (res.code === '200' || res.code === 200) {
          const files = res.data || []
          console.log('获取到的附件文件数量:', files.length)
          console.log('附件数据详情:', files)

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
              size: file.size || '未知大小'
            }

            // 根据type字段添加到不同的附件列表
            // 支持多种可能的结果类型值
            const resultTypes = ['achievement', 'result', '成果'];
            if (resultTypes.includes(file.type)) {
              console.log('附件分类为结果附件')
              this.attachments.result.push(attachment)
            } else {
              console.log('附件分类为详情附件')
              this.attachments.detail.push(attachment)
            }
          })

          console.log('附件分类完成:', {
            detail: this.attachments.detail.length,
            result: this.attachments.result.length
          })

          if (files.length === 0) {
            console.log('任务没有附件文件')
          }

        } else {
          console.warn('获取任务附件返回非200状态:', res.code)
          if (res.code === '401' || res.code === 403) {
            console.warn('没有权限获取附件')
            uni.showToast({
              title: '没有权限查看附件',
              icon: 'none',
              duration: 2000
            })
          } else {
            uni.showToast({
              title: '获取附件失败: ' + (res.msg || res.message || '未知错误'),
              icon: 'none',
              duration: 2000
            })
          }
        }
      } catch (error) {
        console.error('获取任务附件异常:', error)
        // 不影响页面显示，附件加载失败时页面其他功能正常
        if (error.message === 'Attachment load timeout') {
          console.log('附件加载超时(10秒)，页面其他功能正常')
          // 显示友好提示但不阻塞页面
          setTimeout(() => {
            uni.showToast({
              title: '附件加载超时，请稍后刷新',
              icon: 'none',
              duration: 1500
            })
          }, 1000)
        } else {
          console.log('附件加载失败:', error.message)
        }
      }
    },

    // 下载附件
    downloadAttachment(fileUrl) {
      if (!fileUrl) return

      console.log('下载附件，原始URL:', fileUrl)

      try {
        // 构建下载URL - fileUrl格式应为 /files/UUID-timestamp-filename
        const downloadUrl = `${config.baseURL}/files/download?file=${encodeURIComponent(fileUrl)}`

        console.log('完整下载URL:', downloadUrl)

        // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU
        // 小程序环境：使用uni.downloadFile + uni.openDocument
        // 获取token用于认证
        const token = uni.getStorageSync('token')

        uni.downloadFile({
          url: downloadUrl,
          header: {
            'token': token  // 添加认证token
          },
          success: (res) => {
            if (res.statusCode === 200) {
              console.log('文件下载成功，临时文件路径:', res.tempFilePath)

              // 打开文档（不同文件类型使用不同方式）
              const filePath = res.tempFilePath
              const fileName = this.getFileName(fileUrl)

              // 判断文件类型
              if (fileName.match(/\.(jpg|jpeg|png|gif|bmp)$/i)) {
                // 图片类型：预览
                uni.previewImage({
                  urls: [filePath],
                  current: 0,
                  fail: (err) => {
                    console.error('图片预览失败:', err)
                    uni.showToast({ title: '无法预览图片', icon: 'none' })
                  }
                })
              } else if (fileName.match(/\.(doc|docx|xls|xlsx|pdf|txt|ppt|pptx)$/i)) {
                // 文档类型：打开文档
                uni.openDocument({
                  filePath: filePath,
                  showMenu: true,
                  success: () => {
                    console.log('文档打开成功')
                  },
                  fail: (err) => {
                    console.error('文档打开失败:', err)
                    uni.showToast({ title: '无法打开文档', icon: 'none' })
                  }
                })
              } else {
                // 其他类型：提示用户
                uni.showToast({
                  title: '文件已下载到临时目录',
                  icon: 'success',
                  duration: 2000
                })
              }
            } else if (res.statusCode === 401) {
              uni.showToast({
                title: '登录已过期，请重新登录',
                icon: 'none'
              })
              setTimeout(() => {
                uni.reLaunch({
                  url: '/pages/login/login'
                })
              }, 1500)
            } else {
              uni.showToast({
                title: '下载失败，状态码:' + res.statusCode,
                icon: 'none'
              })
            }
          },
          fail: (err) => {
            console.error('uni.downloadFile失败:', err)
            uni.showToast({
              title: '下载失败，请重试',
              icon: 'none'
            })
          }
        })
        // #endif

        // #ifndef MP
        // H5环境：创建a标签下载
        const token = uni.getStorageSync('token')
        const a = document.createElement('a')
        a.style.display = 'none'

        // H5环境下需要添加token到URL或header
        // 由于浏览器限制，我们使用URL参数方式传递token
        const urlWithToken = downloadUrl.includes('?')
          ? `${downloadUrl}&token=${encodeURIComponent(token)}`
          : `${downloadUrl}?token=${encodeURIComponent(token)}`

        a.href = urlWithToken
        a.download = this.getFileName(fileUrl)
        a.target = '_blank'  // 在新标签页打开
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        uni.showToast({
          title: '开始下载...',
          icon: 'loading'
        })
        // #endif

      } catch (error) {
        console.error('下载附件异常:', error)
        uni.showToast({
          title: '下载失败，请重试',
          icon: 'none'
        })
      }
    },

// 返回上一页
    goBack() {
      console.log('执行返回操作')

      // 显示加载提示
      uni.showLoading({
        title: '返回中...',
        mask: true
      })

      // 先尝试navigateBack
      uni.navigateBack({
        delta: 1,
        success: function() {
          uni.hideLoading()
          console.log('navigateBack返回成功')
        },
        fail: function(err) {
          console.error('navigateBack失败，使用redirectTo:', err)
          uni.hideLoading()

          // 如果navigateBack失败，使用redirectTo返回主页
          uni.redirectTo({
            url: '/pages/index/index',
            success: function() {
              console.log('redirectTo返回主页成功')
              uni.showToast({
                title: '已返回任务列表',
                duration: 1000
              })
            },
            fail: function(err) {
              console.error('redirectTo也失败:', err)
              uni.showToast({
                title: '返回失败，请重试',
                icon: 'none'
              })
            }
          })
        }
      })
    },

    // 编辑任务
    editTask() {
      if (!this.task.id) {
        uni.showToast({
          title: '任务ID不存在',
          icon: 'none'
        })
        return
      }

      uni.navigateTo({
        url: `/pages/task-add/task-add?mode=edit&taskId=${this.task.id}`
      })
    },

    // 删除任务
    async deleteTask() {
      if (!this.task.id) {
        uni.showToast({
          title: '任务ID不存在',
          icon: 'none'
        })
        return
      }

      try {
        const res = await uni.showModal({
          title: '确认删除',
          content: '删除后将无法恢复，是否确认删除该任务？',
          confirmText: '确认删除',
          confirmColor: '#ff4d4f',
          cancelText: '取消'
        })

        if (res.confirm) {
          await this.performDelete()
        }
      } catch (error) {
        console.error('删除操作失败:', error)
        uni.showToast({
          title: '删除操作失败',
          icon: 'none'
        })
      }
    },

    // 执行删除操作
    async performDelete() {
      try {
        uni.showLoading({
          title: '删除中...'
        })

        // 调用API，使用软删除方式
        const res = await taskApi.updatePlan({
          id: this.task.id,
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
            // 返回任务列表并强制刷新
            uni.redirectTo({
              url: '/pages/index/index?forceRefresh=true'
            })
          }, 1500)
        } else if (res.code === '401' || res.code === 403) {
          throw new Error('您没有权限进行此操作')
        } else {
          throw new Error(res.msg || '删除失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('删除任务失败:', error)
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

.back-icon {
  font-size: 36rpx;
  color: #ffffff;
  font-weight: bold;
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
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.1);
  z-index: 999;
}

.edit-btn,
.delete-btn {
  flex: 1;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  transition: all 0.3s ease;
}

.edit-btn:active,
.delete-btn:active {
  transform: scale(0.98);
}

.edit-btn {
  background: linear-gradient(135deg, #023c99 0%, #1890ff 100%);
  box-shadow: 0 4rpx 12rpx rgba(2, 60, 153, 0.3);
}

.delete-btn {
  background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);
  box-shadow: 0 4rpx 12rpx rgba(255, 77, 79, 0.3);
}

.btn-text {
  font-size: 28rpx;
  color: #ffffff;
  font-weight: 600;
}
</style>