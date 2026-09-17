<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      <view class="nav-content">
        <view class="nav-title-wrapper">
          <view class="nav-title">
            <text class="title-text">{{ pageTitle }}</text>
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

        <!-- 任务名称 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">任务名称</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <input
              class="form-input"
              type="text"
              v-model="formData.name"
              placeholder="请输入任务名称"
              maxlength="100"
            />
          </view>
        </view>

        <!-- 任务详情 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">任务详情</text>
          </view>
          <view class="item-content">
            <textarea
              class="form-textarea"
              v-model="formData.detail"
              placeholder="请输入任务详情"
              maxlength="1000"
              :show-confirm-bar="false"
            ></textarea>
            <text class="char-count">{{ formData.detail.length }}/1000</text>
          </view>
        </view>

        <!-- 任务性质 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">任务性质</text>
          </view>
          <view class="item-content">
            <picker
              mode="selector"
              :range="attributeOptions"
              range-key="name"
              :value="attributeIndex"
              @change="onAttributeChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !attributeName }">
                  {{ attributeName || '请选择任务性质' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>
      </view>

      <!-- 时间设置卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">📅</text>
          <text class="title-text">时间设置</text>
        </view>

        <!-- 开始时间 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">开始时间</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <picker
              mode="date"
              :value="formData.startTime"
              @change="onStartTimeChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.startTime }">
                  {{ formData.startTime || '请选择开始时间' }}
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
              mode="date"
              :value="formData.endTime"
              :start="formData.endTime"
              @change="onEndTimeChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.endTime }">
                  {{ formData.endTime || '请选择结束时间' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>
      </view>

      <!-- 人员设置卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">👥</text>
          <text class="title-text">人员设置</text>
        </view>

        <!-- 负责人 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">负责人</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <input
              class="form-input"
              type="text"
              v-model="formData.head"
              placeholder="请输入负责人姓名或ID"
            />
          </view>
        </view>

        <!-- 申报人 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">申报人</text>
          </view>
          <view class="item-content">
            <view class="info-display">
              <text class="info-text">{{ currentUser.username || '当前用户' }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 分类设置卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">🏷️</text>
          <text class="title-text">分类设置</text>
        </view>

        <!-- 任务类型（级联选择） -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">任务类型</text>
          </view>
          <view class="item-content">
            <view class="cascade-selector">
              <!-- 一级分类 -->
              <picker
                mode="selector"
                :range="firstLevelTypes"
                range-key="name"
                :value="firstLevelIndex"
                @change="onFirstLevelChange"
                class="cascade-picker"
              >
                <view class="picker-view" :class="{ 'has-value': selectedFirstLevel }">
                  <text class="picker-text" :class="{ 'placeholder': !selectedFirstLevel }">
                    {{ selectedFirstLevel || '请选择一级分类' }}
                  </text>
                  <text class="picker-arrow" v-if="secondLevelTypes.length > 0">›</text>
                </view>
              </picker>

              <!-- 二级分类 -->
              <picker
                v-if="secondLevelTypes.length > 0"
                mode="selector"
                :range="secondLevelTypes"
                range-key="name"
                :value="secondLevelIndex"
                @change="onSecondLevelChange"
                class="cascade-picker"
              >
                <view class="picker-view" :class="{ 'has-value': selectedSecondLevel }">
                  <text class="picker-text" :class="{ 'placeholder': !selectedSecondLevel }">
                    {{ selectedSecondLevel || '请选择二级分类' }}
                  </text>
                  <text class="picker-arrow" v-if="thirdLevelTypes.length > 0">›</text>
                </view>
              </picker>

              <!-- 三级分类 -->
              <picker
                v-if="thirdLevelTypes.length > 0"
                mode="selector"
                :range="thirdLevelTypes"
                range-key="name"
                :value="thirdLevelIndex"
                @change="onThirdLevelChange"
                class="cascade-picker"
              >
                <view class="picker-view" :class="{ 'has-value': selectedThirdLevel }">
                  <text class="picker-text" :class="{ 'placeholder': !selectedThirdLevel }">
                    {{ selectedThirdLevel || '请选择三级分类' }}
                  </text>
                </view>
              </picker>
            </view>
          </view>
        </view>

        <!-- 重要程度 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">重要程度</text>
          </view>
          <view class="item-content">
            <picker
              mode="selector"
              :range="impOptions"
              range-key="name"
              :value="impIndex"
              @change="onImpChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.imp }">
                  {{ impName || '请选择重要程度' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 任务状态 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">任务状态</text>
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
                  {{ statusName || '请选择任务状态' }}
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

        <!-- 所属部门 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">所属部门</text>
          </view>
          <view class="item-content">
            <picker
              mode="selector"
              :range="departmentOptions"
              range-key="name"
              :value="departmentIndex"
              @change="onDepartmentChange"
            >
              <view class="picker-view">
                <text class="picker-text" :class="{ 'placeholder': !formData.did }">
                  {{ departmentName || '请选择所属部门' }}
                </text>
                <text class="picker-arrow">›</text>
              </view>
            </picker>
          </view>
        </view>

        <!-- 是否公开 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">是否公开</text>
          </view>
          <view class="item-content">
            <switch
              :checked="isPublic"
              @change="onPublicChange"
              color="#023c99"
            />
          </view>
        </view>

        <!-- 任务成果 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">任务成果</text>
          </view>
          <view class="item-content">
            <textarea
              class="form-textarea"
              v-model="formData.achievement"
              placeholder="请输入任务成果"
              maxlength="500"
              :show-confirm-bar="false"
            ></textarea>
            <text class="char-count">{{ formData.achievement.length }}/500</text>
          </view>
        </view>
      </view>

      <!-- 附件上传卡片 -->
      <view class="form-card">
        <view class="card-title">
          <text class="title-icon">📎</text>
          <text class="title-text">附件上传</text>
        </view>

        <view class="form-item">
          <view class="item-content">
            <view class="upload-btn" @tap="chooseFile">
              <text class="upload-icon">+</text>
              <text class="upload-text">选择文件</text>
            </view>
          </view>
        </view>

        <!-- 已存在附件列表（编辑模式） -->
        <view class="file-list" v-if="isEditMode && (attachments.detail.length > 0 || attachments.result.length > 0)">
          <view class="file-section-title">已存在附件</view>
          <!-- 详情附件 -->
          <view v-if="attachments.detail.length > 0">
            <view class="file-section-subtitle">详情附件</view>
            <view
              class="file-item"
              v-for="(file, index) in attachments.detail"
              :key="'existing-detail-' + index"
            >
              <text class="file-icon">📎</text>
              <view class="file-info">
                <text class="file-name">{{ file.fileName }}</text>
                <text class="file-size">{{ file.size }}</text>
              </view>
              <view class="file-delete" @tap="deleteExistingAttachment('detail', index, file.id)">
                <text class="delete-icon">×</text>
              </view>
            </view>
          </view>
          <!-- 结果附件 -->
          <view v-if="attachments.result.length > 0">
            <view class="file-section-subtitle">结果附件</view>
            <view
              class="file-item"
              v-for="(file, index) in attachments.result"
              :key="'existing-result-' + index"
            >
              <text class="file-icon">📎</text>
              <view class="file-info">
                <text class="file-name">{{ file.fileName }}</text>
                <text class="file-size">{{ file.size }}</text>
              </view>
              <view class="file-delete" @tap="deleteExistingAttachment('result', index, file.id)">
                <text class="delete-icon">×</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 新上传文件列表 -->
        <view class="file-list" v-if="uploadedFiles.length > 0">
          <view class="file-section-title">新上传文件</view>
          <view
            class="file-item"
            v-for="(file, index) in uploadedFiles"
            :key="'new-' + index"
          >
            <text class="file-icon">📄</text>
            <view class="file-info">
              <text class="file-name">{{ file.fileName }}</text>
            </view>
            <view class="file-delete" @tap="deleteFile(index)">
              <text class="delete-icon">×</text>
            </view>
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
      <view class="confirm-btn" @tap="saveTask">
        <text class="confirm-text">确认保存</text>
      </view>
    </view>
  </view>
</template>

<script>
import { taskApi, userApi, departmentApi, fileApi } from '@/api'
import config from '@/config'

export default {
  data() {
    return {
      // 表单数据
      formData: {
        id: '',             // 任务ID（UUID）
        name: '',           // 任务名称
        detail: '',         // 任务详情
        attribute: '',      // 任务性质
        startTime: '',      // 开始时间
        endTime: '',        // 结束时间
        head: '',           // 负责人ID
        type: '',           // 任务类型ID
        imp: '',            // 重要程度
        status: '',         // 任务状态
        did: '',            // 部门ID
        isshow: '0',        // 是否公开
        isopen: '0',        // 是否开放
        achievement: '',    // 任务成果
        uid: '',            // 申报人ID
        tid: ''             // 租户ID
      },

      // 用户信息
      currentUser: {},
      userInfo: {},

      // 模式相关字段
      isEditMode: false,        // 是否为编辑模式
      pageTitle: '新增任务',    // 页面标题

      // 选项数据
      departmentOptions: [],   // 部门列表
      typeOptions: [],         // 任务类型（原始数据）
      attributeOptions: [],    // 任务性质（从字典加载）
      impOptions: [],          // 重要程度
      statusOptions: [],       // 任务状态

      // 级联选择相关
      firstLevelTypes: [],     // 一级分类列表
      secondLevelTypes: [],    // 二级分类列表
      thirdLevelTypes: [],     // 三级分类列表
      firstLevelIndex: -1,     // 一级分类选中索引
      secondLevelIndex: -1,    // 二级分类选中索引
      thirdLevelIndex: -1,     // 三级分类选中索引
      selectedFirstLevel: '',  // 已选择的一级分类
      selectedSecondLevel: '', // 已选择的二级分类
      selectedThirdLevel: '',  // 已选择的三级分类

      // 附件数据
      attachments: {
        detail: [],    // 详情附件
        result: []     // 结果附件
      },

      // 选择器索引
      departmentIndex: -1,
      impIndex: -1,
      statusIndex: -1,
      attributeIndex: -1,

      // 开关状态
      isPublic: false,

      // 文件上传
      uploadedFiles: [],        // 已上传的文件

      // 加载状态
      isLoading: false
    }
  },

  computed: {
    // 部门名称
    departmentName() {
      if (this.departmentIndex >= 0 && this.departmentOptions[this.departmentIndex]) {
        return this.departmentOptions[this.departmentIndex].name
      }
      return ''
    },

    // 任务性质名称
    attributeName() {
      if (this.attributeIndex >= 0 && this.attributeOptions[this.attributeIndex]) {
        return this.attributeOptions[this.attributeIndex].name
      }
      return ''
    },

    // 重要程度名称
    impName() {
      if (this.impIndex >= 0 && this.impOptions[this.impIndex]) {
        return this.impOptions[this.impIndex].name
      }
      return ''
    },

    // 任务状态名称
    statusName() {
      if (this.statusIndex >= 0 && this.statusOptions[this.statusIndex]) {
        return this.statusOptions[this.statusIndex].name
      }
      return ''
    },

    // 任务类型完整路径
    taskTypePath() {
      let path = ''
      if (this.selectedFirstLevel) {
        path = this.selectedFirstLevel
      }
      if (this.selectedSecondLevel) {
        path += ' > ' + this.selectedSecondLevel
      }
      if (this.selectedThirdLevel) {
        path += ' > ' + this.selectedThirdLevel
      }
      return path
    }
  },

  async onLoad(options) {
    // 检测模式
    const mode = options.mode || 'add'
    const taskId = options.taskId

    if (mode === 'edit' && taskId) {
      this.isEditMode = true
      this.pageTitle = '修改任务'
      console.log('编辑模式，任务ID:', taskId)

      // 加载任务数据
      try {
        uni.showLoading({ title: '加载中...' })
        const res = await taskApi.getPlanDetail({ id: taskId })
        uni.hideLoading()

        if (res.code === '200' && res.data && res.data.length > 0) {
          const task = res.data[0]

          // 设置表单数据
          this.formData = {
            id: task.id || '',
            taskName: task.name || '',
            taskType: task.typeValue || task.type || '',
            severity: task.impValue || task.imp || '',
            state: task.statusValue || task.status || '',
            startDate: task.startDate || task.startTime || '',
            endDate: task.endDate || task.endTime || '',
            head: task.head || '',
            cycle: task.cycle || '',
            description: task.description || task.detail || '',
            taskResult: task.achievement || task.taskResult || '',
            progress: task.progress || '',
            remark: task.remark || '',
            attribute: task.attribute || '',
            college: task.tid || '',
            department: task.did || ''
          }

          // 加载附件数据
          await this.loadTaskAttachments(taskId)

          console.log('编辑模式数据加载成功')
        } else {
          throw new Error(res.msg || '获取任务详情失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('编辑模式加载数据失败:', error)
        uni.showToast({
          title: '加载任务失败',
          icon: 'none'
        })
      }
    } else {
      this.isEditMode = false
      this.pageTitle = '新增任务'
    }

    // 现有的用户信息检查逻辑保持不变
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

    // 加载所需数据
    await this.loadInitialData()

    // 如果是编辑模式，加载任务数据
    if (this.isEditMode && taskId) {
      await this.loadTaskData(taskId)
    }
  },

  methods: {
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
          this.loadDepartments(),
          this.loadDictData()
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

    // 加载部门列表
    async loadDepartments() {
      try {
        const res = await departmentApi.getDepartmentByTid({
          tid: this.userInfo.tid
        })

        if (res.code === '200' && res.data) {
          this.departmentOptions = res.data

          // 设置当前用户部门为默认部门
          if (this.userInfo.did) {
            const currentDeptIndex = this.departmentOptions.findIndex(
              dept => dept.id === this.userInfo.did
            )
            if (currentDeptIndex >= 0) {
              this.departmentIndex = currentDeptIndex
              this.formData.did = this.departmentOptions[currentDeptIndex].id
            }
          }
        }
      } catch (error) {
        console.error('加载部门列表失败:', error)
      }
    },

    // 加载字典数据
    async loadDictData() {
      try {
        // 使用原始字典数据，保留完整树形结构
        const dictData = uni.getStorageSync('dictDataRaw')
        if (!dictData) {
          console.warn('原始字典数据不存在，尝试使用处理后的数据')
          // 回退到处理后的数据
          const dictDataProcessed = uni.getStorageSync('dictData')
          if (!dictDataProcessed) {
            console.warn('字典数据不存在')
            return
          }
        }

        console.log('字典数据原始结构:', dictData)

        // 加载任务类型（级联分类）- 从原始数据中提取
        const typeData = dictData.filter(item => item.field === 'type')
        if (typeData.length > 0) {
          this.typeOptions = typeData
          // 提取一级分类 - 使用value字段作为显示名称
          this.firstLevelTypes = typeData.map(item => ({
            id: item.id,
            name: item.value, // 使用value字段作为显示名称
            children: item.children || []
          }))
          console.log('一级分类:', this.firstLevelTypes)
        }

        // 加载任务性质 (单层结构)
        const attributeData = dictData.filter(item => item.field === 'attribute')
        if (attributeData.length > 0) {
          this.attributeOptions = attributeData.map(item => ({
            id: item.id,
            value: item.id,
            name: item.value // 使用value字段作为显示名称
          }))
          console.log('任务性质:', this.attributeOptions)
        }

        // 加载重要程度 (单层结构)
        const impData = dictData.filter(item => item.field === 'imp')
        if (impData.length > 0) {
          this.impOptions = impData.map(item => ({
            id: item.id,
            value: item.id,
            name: item.value // 使用value字段作为显示名称
          }))
          console.log('重要程度:', this.impOptions)
        }

        // 加载任务状态 (单层结构)
        const statusData = dictData.filter(item => item.field === 'status')
        if (statusData.length > 0) {
          this.statusOptions = statusData.map(item => ({
            id: item.id,
            value: item.id,
            name: item.value // 使用value字段作为显示名称
          }))
          console.log('任务状态:', this.statusOptions)
        }

        console.log('字典数据加载完成')
      } catch (error) {
        console.error('加载字典数据失败:', error)
      }
    },

    // 加载任务数据（编辑模式）
    async loadTaskData(taskId) {
      try {
        uni.showLoading({
          title: '加载中...'
        })

        const res = await taskApi.getPlanDetail({
          id: taskId
        })

        if (res.code === '200' && res.data && res.data.length > 0) {
          const task = res.data[0]

          // 回填表单数据
          this.formData = {
            id: task.id || '',
            name: task.name || '',
            detail: task.detail || '',
            attribute: task.attribute || '',
            startTime: task.startTime || task.startDate || '',
            endTime: task.endTime || task.endDate || '',
            head: task.head || '',
            type: task.type || '',
            imp: task.imp || '',
            status: task.status || '',
            did: task.did || '',
            isshow: task.isshow || '0',
            isopen: task.isopen || '0',
            achievement: task.achievement || '',
            uid: task.uid || this.userInfo.id,
            tid: task.tid || this.userInfo.tid
          }

          // 设置选择器索引
          this.setSelectorIndexes(task)

          console.log('任务数据加载成功')
        } else {
          throw new Error(res.msg || '获取任务详情失败')
        }
      } catch (error) {
        console.error('加载任务数据失败:', error)
        uni.showToast({
          title: error.message || '加载失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    // 设置选择器索引
    setSelectorIndexes(task) {
      // 1. 处理任务类型级联选择
      this.setCascadeSelectorIndexes(task)

      // 2. 处理其他单层选择器
      this.setSingleSelectorIndexes(task)
    },

    // 设置级联选择器索引
    setCascadeSelectorIndexes(task) {
      if (!task.type) return

      // 查找type对应的路径
      const findTypePath = (types, targetId, path = []) => {
        for (const type of types) {
          const currentPath = [...path, type]

          if (type.id === targetId) {
            return currentPath
          }

          if (type.children && type.children.length > 0) {
            const result = findTypePath(type.children, targetId, currentPath)
            if (result) return result
          }
        }
        return null
      }

      const typePath = findTypePath(this.firstLevelTypes, task.type)

      if (typePath && typePath.length >= 1) {
        // 设置一级分类
        this.firstLevelIndex = this.firstLevelTypes.findIndex(t => t.id === typePath[0].id)
        this.selectedFirstLevel = typePath[0].name
        this.formData.type = typePath[0].id

        // 模拟触发一级分类变化以加载二级分类
        if (typePath[0].children && typePath[0].children.length > 0) {
          this.secondLevelTypes = typePath[0].children.map(child => ({
            id: child.id,
            name: child.value,
            children: child.children || []
          }))

          // 设置二级分类
          if (typePath.length >= 2) {
            this.secondLevelIndex = this.secondLevelTypes.findIndex(t => t.id === typePath[1].id)
            this.selectedSecondLevel = typePath[1].name
            this.formData.type = typePath[1].id

            // 加载三级分类
            if (typePath[1].children && typePath[1].children.length > 0) {
              this.thirdLevelTypes = typePath[1].children.map(child => ({
                id: child.id,
                name: child.value
              }))

              // 设置三级分类
              if (typePath.length >= 3) {
                this.thirdLevelIndex = this.thirdLevelTypes.findIndex(t => t.id === typePath[2].id)
                this.selectedThirdLevel = typePath[2].name
                this.formData.type = typePath[2].id
              }
            }
          }
        }
      }
    },

    // 设置单层选择器索引
    setSingleSelectorIndexes(task) {
      // 任务性质
      if (task.attribute) {
        const attrIndex = this.attributeOptions.findIndex(opt => opt.id === task.attribute)
        if (attrIndex >= 0) {
          this.attributeIndex = attrIndex
        }
      }

      // 部门选择
      if (task.did) {
        const deptIndex = this.departmentOptions.findIndex(opt => opt.id === task.did)
        if (deptIndex >= 0) {
          this.departmentIndex = deptIndex
        }
      }

      // 重要程度
      if (task.imp) {
        const impIndex = this.impOptions.findIndex(opt => opt.id === task.imp)
        if (impIndex >= 0) {
          this.impIndex = impIndex
        }
      }

      // 任务状态
      if (task.status) {
        const statusIndex = this.statusOptions.findIndex(opt => opt.id === task.status)
        if (statusIndex >= 0) {
          this.statusIndex = statusIndex
        }
      }

      // 是否公开
      this.isPublic = task.isshow === '1'
    },

    // 任务性质选择
    onAttributeChange(e) {
      this.attributeIndex = e.detail.value
      const selected = this.attributeOptions[this.attributeIndex]
      this.formData.attribute = selected.id // 使用ID而不是字符串
    },

    // 开始时间选择
    onStartTimeChange(e) {
      this.formData.startTime = e.detail.value
      // 如果结束时间早于开始时间，重置结束时间
      if (this.formData.endTime && this.formData.endTime < this.formData.startTime) {
        this.formData.endTime = ''
      }
    },

    // 结束时间选择
    onEndTimeChange(e) {
      this.formData.endTime = e.detail.value
      // 验证时间逻辑
      if (this.formData.startTime && this.formData.endTime < this.formData.startTime) {
        uni.showToast({
          title: '结束时间不能早于开始时间',
          icon: 'none'
        })
        this.formData.endTime = ''
      }
    },

    // 一级分类选择
    onFirstLevelChange(e) {
      this.firstLevelIndex = e.detail.value
      const selected = this.firstLevelTypes[this.firstLevelIndex]
      this.selectedFirstLevel = selected.name

      // 重置下级选择
      this.selectedSecondLevel = ''
      this.selectedThirdLevel = ''
      this.secondLevelIndex = -1
      this.thirdLevelIndex = -1
      this.formData.type = selected.id

      // 加载二级分类 - 使用value字段作为显示名称
      if (selected.children && selected.children.length > 0) {
        this.secondLevelTypes = selected.children.map(child => ({
          id: child.id,
          name: child.value, // 使用value字段作为显示名称
          children: child.children || []
        }))
        console.log('二级分类:', this.secondLevelTypes)
      } else {
        this.secondLevelTypes = []
        this.thirdLevelTypes = []
      }
    },

    // 二级分类选择
    onSecondLevelChange(e) {
      this.secondLevelIndex = e.detail.value
      const selected = this.secondLevelTypes[this.secondLevelIndex]
      this.selectedSecondLevel = selected.name

      // 重置下级选择
      this.selectedThirdLevel = ''
      this.thirdLevelIndex = -1
      this.formData.type = selected.id

      // 加载三级分类 - 使用value字段作为显示名称
      if (selected.children && selected.children.length > 0) {
        this.thirdLevelTypes = selected.children.map(child => ({
          id: child.id,
          name: child.value // 使用value字段作为显示名称
        }))
        console.log('三级分类:', this.thirdLevelTypes)
      } else {
        this.thirdLevelTypes = []
      }
    },

    // 三级分类选择
    onThirdLevelChange(e) {
      this.thirdLevelIndex = e.detail.value
      const selected = this.thirdLevelTypes[this.thirdLevelIndex]
      this.selectedThirdLevel = selected.name
      this.formData.type = selected.id
      console.log('最终选择的任务类型ID:', this.formData.type)
    },

    // 重要程度选择
    onImpChange(e) {
      this.impIndex = e.detail.value
      this.formData.imp = this.impOptions[this.impIndex].id
    },

    // 任务状态选择
    onStatusChange(e) {
      this.statusIndex = e.detail.value
      this.formData.status = this.statusOptions[this.statusIndex].id
    },

    // 部门选择
    onDepartmentChange(e) {
      this.departmentIndex = e.detail.value
      this.formData.did = this.departmentOptions[this.departmentIndex].id
    },

    // 是否公开切换
    onPublicChange(e) {
      this.isPublic = e.detail.value
      this.formData.isshow = this.isPublic ? '1' : '0'
    },

    // 选择文件
    async chooseFile() {
      try {
        // #ifdef MP-WEIXIN || MP-ALIPAY || MP-BAIDU
        // 小程序环境：使用 chooseMessageFile
        const res = await uni.chooseMessageFile({
          count: 5, // 最多选择5个文件
          type: 'file', // 选择文件
          extension: ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt', '.jpg', '.png']
        })
        // #endif

        // #ifndef MP
        // H5环境：使用 chooseFile
        const res = await uni.chooseFile({
          count: 5, // 最多选择5个文件
          extension: ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt', '.jpg', '.png']
        })
        // #endif

        if (res.tempFilePaths && res.tempFilePaths.length > 0) {
          // 上传文件
          for (let i = 0; i < res.tempFilePaths.length; i++) {
            await this.uploadFile(res.tempFilePaths[i], res.tempFiles[i].name)
          }
        }
      } catch (error) {
        console.error('选择文件失败:', error)
        uni.showToast({
          title: '选择文件失败',
          icon: 'none'
        })
      }
    },

    // 上传文件
    async uploadFile(filePath, fileName) {
      try {
        uni.showLoading({
          title: '上传中...'
        })

        console.log('上传文件参数:', {
          fileName: fileName,
          tid: this.userInfo.tid,
          uid: this.userInfo.id,
          type: 'task'
        })

        // 使用uni.uploadFile替代FormData，确保兼容性
        const uploadRes = await new Promise((resolve, reject) => {
          uni.uploadFile({
            url: `${config.baseURL}/files/upload`,
            filePath: filePath,
            name: 'file',
            formData: {
              tid: this.userInfo.tid || '',
              uid: this.userInfo.id || '',
              type: 'task'
            },
            header: {
              token: uni.getStorageSync('token') || ''
            },
            success: (res) => {
              try {
                const data = JSON.parse(res.data)
                resolve(data)
              } catch (e) {
                reject(new Error('响应解析失败'))
              }
            },
            fail: (err) => {
              reject(err)
            }
          })
        })

        console.log('上传文件响应:', uploadRes)
        const res = uploadRes

        uni.hideLoading()

        if (res.code === '200') {
          // 后端直接返回文件访问路径字符串，不是对象
          const fileUrl = res.data // 直接使用res.data，不是res.data.url

          this.uploadedFiles.push({
            fileName: fileName,
            filePath: fileUrl,        // 修正：直接使用文件路径
            fileSize: res.data.fileSize || 0 // 如果后端提供了大小信息
          })

          uni.showToast({
            title: '上传成功',
            icon: 'success'
          })
        } else {
          console.error('上传失败，响应码:', res.code, '响应信息:', res.msg || res.data)
          throw new Error(res.msg || '上传失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('上传文件失败:', error)

        // 提供更详细的错误信息
        let errorMessage = '上传失败'
        if (error.message && error.message.includes('null')) {
          errorMessage = '文件上传功能暂时不可用，请先保存任务后再上传附件'
        } else if (error.message && !error.message.includes('cancel')) {
          errorMessage = `上传失败: ${error.message}`
        }

        uni.showToast({
          title: errorMessage,
          icon: 'none',
          duration: 3000
        })
      }
    },

    // 删除文件
    deleteFile(index) {
      this.uploadedFiles.splice(index, 1)
    },

    // 格式化文件大小

    // 验证表单
    validateForm() {
      if (!this.formData.name.trim()) {
        uni.showToast({
          title: '请输入任务名称',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.startTime) {
        uni.showToast({
          title: '请选择开始时间',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.endTime) {
        uni.showToast({
          title: '请选择结束时间',
          icon: 'none'
        })
        return false
      }

      if (this.formData.endTime < this.formData.startTime) {
        uni.showToast({
          title: '结束时间不能早于开始时间',
          icon: 'none'
        })
        return false
      }

      if (!this.formData.head || !this.formData.head.trim()) {
        uni.showToast({
          title: '请输入负责人',
          icon: 'none'
        })
        return false
      }

      return true
    },

    // 保存任务
    async saveTask() {
      if (!this.validateForm()) {
        return
      }

      if (this.isLoading) {
        return
      }

      this.isLoading = true

      try {
        const loadingTitle = this.isEditMode ? '更新中...' : '保存中...'
        uni.showLoading({
          title: loadingTitle
        })

        // 构建保存数据
        const saveData = {
          ...this.formData,
          // 编辑模式不需要重新生成ID
          id: this.isEditMode ? this.formData.id : this.generateUUID(),
          // 确保必要字段存在
          uid: this.formData.uid || this.userInfo.id,
          tid: this.formData.tid || this.userInfo.tid,
          username: this.userInfo.username,
          tenant: this.userInfo.tenant,
          department: this.departmentName
        }

        let res
        let taskId
        if (this.isEditMode) {
          // 编辑模式：调用更新接口
          res = await taskApi.updatePlan(saveData)
          taskId = this.formData.id
        } else {
          // 新增模式：调用创建接口
          res = await taskApi.createPlan(saveData)
          taskId = res.data || this.formData.id
        }

        uni.hideLoading()

        if (res.code === '200' || res.code === 200) {
          // 保存附件关联到数据库（异步处理，不阻塞主流程）
          if (this.uploadedFiles.length > 0 && taskId) {
            // 异步保存附件，不阻塞用户操作
            setTimeout(async () => {
              try {
                console.log('开始异步保存附件关联...')

                for (const file of this.uploadedFiles) {
                  try {
                    await taskApi.uploadPlanFile({
                      pid: taskId,
                      name: file.filePath,        // 文件路径（如："/files/uuid-timestamp-file.pdf"）
                      type: 'detail'               // 附件类型
                    })
                    console.log('附件关联保存成功:', file.fileName)
                  } catch (error) {
                    console.error('单个附件保存失败:', file.fileName, error)
                  }
                }

                console.log('所有附件关联保存完成')
              } catch (error) {
                console.error('批量保存附件关联失败:', error)
              }
            }, 100) // 延迟100ms执行，确保主流程先完成
          }

          const successTitle = this.isEditMode ? '更新成功' : '保存成功'
          uni.showToast({
            title: successTitle,
            icon: 'success',
            duration: 1500
          })

          setTimeout(() => {
            // 无论是新增还是编辑模式，都跳转到任务主页并强制刷新
            uni.redirectTo({
              url: '/pages/index/index?forceRefresh=true',
              success: function() {
                console.log('跳转到任务主页成功，页面将自动刷新')
              },
              fail: function(err) {
                console.error('跳转到任务主页失败:', err)
              }
            })
          }, 1500)
        } else if (res.code === '401' || res.code === 403) {
          throw new Error('您没有权限进行此操作')
        } else {
          throw new Error(res.msg || '操作失败')
        }
      } catch (error) {
        uni.hideLoading()
        console.error('保存任务失败:', error)
        uni.showToast({
          title: error.message || '操作失败',
          icon: 'none',
          duration: 2000
        })
      } finally {
        this.isLoading = false
      }
    },

    // 返回上一页
    goBack() {
      // 总是跳转到任务主页并强制刷新
      uni.redirectTo({
        url: '/pages/index/index?forceRefresh=true',
        success: function() {
          console.log('返回任务主页成功，页面将自动刷新')
        },
        fail: function(err) {
          console.error('返回任务主页失败:', err)
        }
      })
    },

    // 删除已存在的附件
    async deleteExistingAttachment(type, index, pfid) {
      if (!pfid) {
        uni.showToast({ title: '附件ID不存在', icon: 'none' })
        return
      }

      try {
        const confirm = await new Promise((resolve) => {
          uni.showModal({
            title: '确认删除',
            content: '确定要删除这个附件吗？',
            success: (res) => resolve(res.confirm)
          })
        })

        if (!confirm) return

        uni.showLoading({ title: '删除中...' })

        // 1. 先删除任务文件关联
        const deleteRes = await taskApi.deletePlanFiles({ pfid })
        if (deleteRes.code !== '200') {
          throw new Error(deleteRes.msg || '删除文件关联失败')
        }

        // 2. 再删除服务器上的实际文件
        const fileToDelete = this.attachments[type][index]
        if (fileToDelete.url) {
          const deleteFileRes = await fileApi.delete(fileToDelete.url)
          if (deleteFileRes.code !== '200') {
            console.warn('删除实际文件失败，但关联已删除:', deleteFileRes.msg)
          }
        }

        uni.hideLoading()

        // 3. 前端删除附件
        this.attachments[type].splice(index, 1)

        uni.showToast({ title: '删除成功', icon: 'success' })
      } catch (error) {
        uni.hideLoading()
        console.error('删除附件失败:', error)
        uni.showToast({
          title: error.message || '删除失败',
          icon: 'none'
        })
      }
    },

    // 加载任务附件
    async loadTaskAttachments(taskId) {
      if (!taskId) {
        console.warn('loadTaskAttachments - 任务ID为空')
        return
      }

      console.log('loadTaskAttachments - 开始加载任务附件，taskId:', taskId)

      try {
        const res = await taskApi.getPlanFiles({ pid: taskId })
        console.log('loadTaskAttachments - API响应:', res)

        if (res.code === '200') {
          const files = res.data || []
          console.log('loadTaskAttachments - 获取到的附件数量:', files.length)
          console.log('loadTaskAttachments - 附件详情:', files)

          // 清空现有附件
          this.attachments.detail = []
          this.attachments.result = []

          // 根据type字段分类附件
          files.forEach((file, index) => {
            console.log(`loadTaskAttachments - 处理附件 ${index + 1}:`, file)

            const attachment = {
              id: file.id,
              fileName: this.getFileName(file.name),
              url: file.name,
              type: file.type,
              size: file.size || '未知大小'
            }

            const resultTypes = ['achievement', 'result', '成果']
            if (resultTypes.includes(file.type)) {
              console.log(`loadTaskAttachments - 附件 ${index + 1} 分类为结果附件`)
              this.attachments.result.push(attachment)
            } else {
              console.log(`loadTaskAttachments - 附件 ${index + 1} 分类为详情附件`)
              this.attachments.detail.push(attachment)
            }
          })

          console.log('loadTaskAttachments - 附件分类完成:', {
            detail: this.attachments.detail.length,
            result: this.attachments.result.length,
            detailFiles: this.attachments.detail.map(f => f.fileName),
            resultFiles: this.attachments.result.map(f => f.fileName)
          })
        } else {
          console.warn('loadTaskAttachments - API返回失败，响应码:', res.code, '消息:', res.msg)
          uni.showToast({
            title: '获取附件失败: ' + (res.msg || '未知错误'),
            icon: 'none',
            duration: 2000
          })
        }
      } catch (error) {
        console.error('loadTaskAttachments - 加载附件异常:', error)
        console.error('loadTaskAttachments - 异常详情:', error.message, error.stack)
        uni.showToast({
          title: '获取附件异常: ' + error.message,
          icon: 'none',
          duration: 2000
        })
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
    }
  }
}
</script>

<style lang="scss" scoped>
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

/* 文件上传 */
.upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160rpx;
  background: #f5f7fa;
  border: 2rpx dashed $border-color;
  border-radius: 12rpx;
  transition: all 0.3s ease;

  &:active {
    background: $primary-light;
    border-color: $primary-color;
  }
}

.upload-icon {
  font-size: 48rpx;
  color: $text-secondary;
  margin-right: 12rpx;
  font-weight: 300;
}

.upload-text {
  font-size: 28rpx;
  color: $text-secondary;
}

/* 文件列表 */
.file-list {
  margin-top: 24rpx;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  margin-bottom: 16rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.file-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.file-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

.file-size {
  font-size: 24rpx;
  color: $text-secondary;
}

.file-delete {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;

  &:active {
    background: rgba(0, 0, 0, 0.1);
  }
}

.delete-icon {
  font-size: 40rpx;
  color: $text-secondary;
  line-height: 1;
}

/* 附件分组样式 */
.file-section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $text-primary;
  margin: 24rpx 0 16rpx 0;
  padding-bottom: 12rpx;
  border-bottom: 2rpx solid #e4e7ed;
}

.file-section-subtitle {
  font-size: 26rpx;
  font-weight: 500;
  color: $text-regular;
  margin: 16rpx 0 12rpx 0;
  padding-left: 8rpx;
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

/* 级联选择器样式 */
.cascade-selector {
  display: flex;
  gap: 16rpx;
}

.cascade-picker {
  flex: 1;
  min-width: 0;
}

.cascade-picker .picker-view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  padding: 0 24rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;

  &:active {
    background: #e6f0ff;
    border-color: $primary-color;
  }

  &.has-value {
    background: #ffffff;
    border-color: $primary-color;
  }
}

.cascade-picker .picker-text {
  font-size: 26rpx;
  color: $text-primary;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.placeholder {
    color: $text-secondary;
  }
}

.cascade-picker .picker-arrow {
  font-size: 32rpx;
  color: $text-secondary;
  margin-left: 8rpx;
  flex-shrink: 0;
}
</style>