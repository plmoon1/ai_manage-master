<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="custom-nav">
      <view class="status-bar"></view>
      <view class="nav-content">
        <view class="nav-title-wrapper">
          <view class="nav-title">
            <text class="title-text">{{ isEditMode ? '修改工作' : '新增工作' }}</text>
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

        <!-- 工作名称 -->
        <view class="form-item required">
          <view class="item-label">
            <text class="label-text">工作名称</text>
            <text class="required-mark">*</text>
          </view>
          <view class="item-content">
            <input
              class="form-input"
              type="text"
              v-model="formData.name"
              placeholder="请输入工作名称"
              maxlength="100"
            />
          </view>
        </view>

        <!-- 工作详情 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">工作详情</text>
          </view>
          <view class="item-content">
            <textarea
              class="form-textarea"
              v-model="formData.detail"
              placeholder="请输入工作详情"
              maxlength="1000"
              :show-confirm-bar="false"
            ></textarea>
            <text class="char-count">{{ formData.detail.length }}/1000</text>
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
              :start="formData.startTime"
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

        <!-- 工作类型（级联选择） -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">工作类型</text>
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

        <!-- 工作状态 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">工作状态</text>
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
                  {{ statusName || '请选择工作状态' }}
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

        <!-- 工作成果 -->
        <view class="form-item">
          <view class="item-label">
            <text class="label-text">工作成果</text>
          </view>
          <view class="item-content">
            <textarea
              class="form-textarea"
              v-model="formData.achievement"
              placeholder="请输入工作成果"
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
      <view class="confirm-btn" @tap="saveWork">
        <text class="confirm-text">确认保存</text>
      </view>
    </view>
  </view>
</template>

<script>
import { workApi, departmentApi, fileApi } from '@/api'
import config from '@/config'

export default {
  data() {
    return {
      // 编辑模式标识
      isEditMode: false,
      editingWorkId: '',
      editingWorkData: null,  // 编辑的工作数据

      // 表单数据
      formData: {
        id: '',             // 工作ID（UUID）
        name: '',           // 工作名称
        detail: '',         // 工作详情
        startTime: '',      // 开始时间
        endTime: '',        // 结束时间
        head: '',           // 负责人
        type: '',           // 工作类型ID
        status: '',         // 工作状态
        did: '',            // 部门ID
        isshow: '0',        // 是否公开
        isopen: '0',        // 是否开放
        achievement: '',    // 工作成果
        uid: '',            // 申报人ID
        tid: ''             // 租户ID
      },

      // 用户信息
      currentUser: {},
      userInfo: {},

      // 选项数据
      departmentOptions: [],   // 部门列表
      typeOptions: [],         // 工作类型（原始数据）
      statusOptions: [],       // 工作状态

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

      // 选择器索引
      departmentIndex: -1,
      statusIndex: -1,

      // 开关状态
      isPublic: false,

      // 文件上传
      uploadedFiles: [],        // 已上传的文件

      // 附件数据
      attachments: {
        detail: [],    // 详情附件
        result: []     // 结果附件
      },

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

    // 工作状态名称
    statusName() {
      if (this.statusIndex >= 0 && this.statusOptions[this.statusIndex]) {
        return this.statusOptions[this.statusIndex].name
      }
      return ''
    },

    // 工作类型完整路径
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
    // 检测是否为编辑模式
    if (options.mode === 'edit' && options.work) {
      this.isEditMode = true
      try {
        const workData = JSON.parse(decodeURIComponent(options.work))
        this.editingWorkId = workData.id
        this.editingWorkData = workData
      } catch (error) {
        console.error('解析工作数据失败:', error)
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

    // 加载所需数据
    await this.loadInitialData()

    // 如果是编辑模式，加载数据
    if (this.isEditMode && this.editingWorkData) {
      this.loadWorkData()
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
          const dictDataProcessed = uni.getStorageSync('dictData')
          if (!dictDataProcessed) {
            console.warn('字典数据不存在')
            return
          }
        }

        console.log('字典数据原始结构:', dictData)

        // 加载工作类型（级联分类，与任务类型相同）
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

        // 加载工作状态
        const statusData = dictData.filter(item => item.field === 'status')
        if (statusData.length > 0) {
          this.statusOptions = statusData.map(item => ({
            id: item.id,
            value: item.id,
            name: item.value // 使用value字段作为显示名称
          }))
          console.log('工作状态:', this.statusOptions)

          // 排序：未开始、进行中、已完成、已撤销
          const statusOrder = ['未开始', '进行中', '已完成', '已撤销']
          this.statusOptions.sort((a, b) => {
            const indexA = statusOrder.indexOf(a.name)
            const indexB = statusOrder.indexOf(b.name)
            if (indexA !== -1 && indexB !== -1) return indexA - indexB
            if (indexA !== -1) return -1
            if (indexB !== -1) return 1
            return 0
          })

          // 默认设置为未开始
          const defaultIndex = this.statusOptions.findIndex(s => s.name === '未开始')
          if (defaultIndex >= 0) {
            this.statusIndex = defaultIndex
            this.formData.status = this.statusOptions[defaultIndex].value
          }
        }

        console.log('字典数据加载完成')
      } catch (error) {
        console.error('加载字典数据失败:', error)
      }
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
      console.log('最终选择的工作类型ID:', this.formData.type)
    },

    // 工作状态选择
    onStatusChange(e) {
      this.statusIndex = e.detail.value
      this.formData.status = this.statusOptions[this.statusIndex].value
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
        // 选择文件
        const res = await uni.chooseFile({
          count: 5, // 最多选择5个文件
          extension: ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt', '.jpg', '.png']
        })

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
          type: 'works'
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
              type: 'works'
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
          // 确保获取到正确的文件路径
          // 后端返回的格式应该是：/resource/task_manage/UUID-timestamp-filename
          const serverFilePath = res.data || res.data.url || res.data.filePath

          console.log('服务器返回的文件路径:', serverFilePath)
          console.log('原始文件名:', fileName)

          this.uploadedFiles.push({
            fileName: fileName,              // 原始文件名（用于显示）
            filePath: serverFilePath,        // 服务器完整路径（用于下载和存储）
            fileSize: res.data.fileSize || 0
          })

          console.log('保存的文件信息:', {
            fileName: fileName,
            filePath: serverFilePath
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
          errorMessage = '文件上传功能暂时不可用，请先保存工作后再上传附件'
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

    // 加载工作附件
    async loadWorkAttachments(workId) {
      if (!workId) {
        console.warn('loadWorkAttachments - 工作ID为空')
        return
      }

      console.log('loadWorkAttachments - 开始加载工作附件，workId:', workId)

      try {
        const res = await workApi.getWorksFiles({ pid: workId })
        console.log('loadWorkAttachments - API响应:', res)

        if (res.code === '200') {
          const files = res.data || []
          console.log('loadWorkAttachments - 获取到的附件数量:', files.length)
          console.log('loadWorkAttachments - 附件详情:', files)

          // 清空现有附件
          this.attachments.detail = []
          this.attachments.result = []

          // 根据type字段分类附件
          files.forEach((file, index) => {
            console.log(`loadWorkAttachments - 处理附件 ${index + 1}:`, file)

            const attachment = {
              id: file.id,
              fileName: this.getFileName(file.name),
              url: file.name,
              type: file.type,
              size: file.size || '未知大小'
            }

            const resultTypes = ['achievement', 'result', '成果']
            if (resultTypes.includes(file.type)) {
              console.log(`loadWorkAttachments - 附件 ${index + 1} 分类为结果附件`)
              this.attachments.result.push(attachment)
            } else {
              console.log(`loadWorkAttachments - 附件 ${index + 1} 分类为详情附件`)
              this.attachments.detail.push(attachment)
            }
          })

          console.log('loadWorkAttachments - 附件分类完成:', {
            detail: this.attachments.detail.length,
            result: this.attachments.result.length,
            detailFiles: this.attachments.detail.map(f => f.fileName),
            resultFiles: this.attachments.result.map(f => f.fileName)
          })
        } else {
          console.warn('loadWorkAttachments - API返回失败，响应码:', res.code, '消息:', res.msg)
        }
      } catch (error) {
        console.error('loadWorkAttachments - 加载附件异常:', error)
        console.error('loadWorkAttachments - 异常详情:', error.message, error.stack)
      }
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

        // 1. 先删除工作文件关联（注意：workApi可能没有deleteWorksFiles方法，需要确认）
        try {
          const deleteRes = await workApi.deleteWorksFiles({ pfid })
          if (deleteRes.code !== '200') {
            throw new Error(deleteRes.msg || '删除文件关联失败')
          }
        } catch (error) {
          console.warn('删除文件关联接口可能不存在，跳过:', error.message)
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

    // 验证表单
    validateForm() {
      if (!this.formData.name.trim()) {
        uni.showToast({
          title: '请输入工作名称',
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

      if (!this.formData.head.trim()) {
        uni.showToast({
          title: '请输入负责人姓名或ID',
          icon: 'none'
        })
        return false
      }

      return true
    },

    // 加载工作数据（编辑模式）
    async loadWorkData() {
      if (!this.editingWorkData) {
        uni.showToast({
          title: '工作数据不存在',
          icon: 'none'
        })
        return
      }

      try {
        const workData = this.editingWorkData

        // 回填表单数据
        this.formData.id = workData.id || ''
        this.formData.name = workData.name || ''
        this.formData.detail = workData.detail || ''
        this.formData.startTime = workData.startTime || ''
        this.formData.endTime = workData.endTime || ''
        this.formData.head = workData.head || ''
        this.formData.type = workData.type || ''
        this.formData.status = workData.status || ''
        this.formData.did = workData.did || ''
        this.formData.isshow = workData.isshow || '0'
        this.formData.isopen = workData.isopen || '0'
        this.formData.achievement = workData.achievement || ''
        this.formData.tid = workData.tid || this.userInfo.tid
        this.formData.uid = workData.uid || this.userInfo.id

        // 回填公开开关
        this.isPublic = this.formData.isshow === '1'

        // 回填部门选择器
        if (workData.did && this.departmentOptions.length > 0) {
          const deptIndex = this.departmentOptions.findIndex(dept => dept.id === workData.did)
          if (deptIndex >= 0) {
            this.departmentIndex = deptIndex
            this.formData.did = this.departmentOptions[deptIndex].id
          }
        }

        // 回填状态选择器
        if (workData.status && this.statusOptions.length > 0) {
          const statusIndex = this.statusOptions.findIndex(status => status.value === workData.status)
          if (statusIndex >= 0) {
            this.statusIndex = statusIndex
            this.formData.status = this.statusOptions[statusIndex].value
          }
        }

        // 回填级联选择器（工作类型）
        if (workData.type && this.typeOptions.length > 0) {
          this.fillCascadeSelectors(workData.type)
        }

        // 加载附件数据
        await this.loadWorkAttachments(this.editingWorkId)

        console.log('工作数据加载完成:', this.formData)
      } catch (error) {
        console.error('加载工作数据失败:', error)
        uni.showToast({
          title: error.message || '加载工作数据失败',
          icon: 'none'
        })
      }
    },

    // 回填级联选择器（递归查找并回填）
    fillCascadeSelectors(typeId) {
      try {
        // 在一级分类中查找
        for (let i = 0; i < this.firstLevelTypes.length; i++) {
          const firstLevel = this.firstLevelTypes[i]
          if (firstLevel.id === typeId) {
            // 找到了，设置一级分类
            this.firstLevelIndex = i
            this.selectedFirstLevel = firstLevel.name
            this.formData.type = firstLevel.id
            return
          }

          // 在二级分类中查找
          if (firstLevel.children && firstLevel.children.length > 0) {
            for (let j = 0; j < firstLevel.children.length; j++) {
              const secondLevel = firstLevel.children[j]
              if (secondLevel.id === typeId) {
                // 找到了，设置一级和二级分类
                this.firstLevelIndex = i
                this.selectedFirstLevel = firstLevel.name

                // 加载二级分类
                this.secondLevelTypes = firstLevel.children.map(child => ({
                  id: child.id,
                  name: child.value,
                  children: child.children || []
                }))

                this.secondLevelIndex = j
                this.selectedSecondLevel = secondLevel.value
                this.formData.type = secondLevel.id
                return
              }

              // 在三级分类中查找
              if (secondLevel.children && secondLevel.children.length > 0) {
                for (let k = 0; k < secondLevel.children.length; k++) {
                  const thirdLevel = secondLevel.children[k]
                  if (thirdLevel.id === typeId) {
                    // 找到了，设置一级、二级和三级分类
                    this.firstLevelIndex = i
                    this.selectedFirstLevel = firstLevel.name

                    // 加载二级分类
                    this.secondLevelTypes = firstLevel.children.map(child => ({
                      id: child.id,
                      name: child.value,
                      children: child.children || []
                    }))

                    this.secondLevelIndex = j
                    this.selectedSecondLevel = secondLevel.value

                    // 加载三级分类
                    this.thirdLevelTypes = secondLevel.children.map(child => ({
                      id: child.id,
                      name: child.value
                    }))

                    this.thirdLevelIndex = k
                    this.selectedThirdLevel = thirdLevel.value
                    this.formData.type = thirdLevel.id
                    return
                  }
                }
              }
            }
          }
        }
      } catch (error) {
        console.error('回填级联选择器失败:', error)
      }
    },

    // 保存工作
    async saveWork() {
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
          // 确保必要字段存在
          uid: this.formData.uid || this.userInfo.id,
          tid: this.formData.tid || this.userInfo.tid,
          username: this.userInfo.username,
          tenant: this.userInfo.tenant,
          department: this.departmentName
        }

        // 根据模式选择API
        let res
        if (this.isEditMode) {
          // 编辑模式：使用现有ID，调用更新接口
          saveData.id = this.editingWorkId
          res = await workApi.updateWorks(saveData)
        } else {
          // 新增模式：生成新ID，调用创建接口
          saveData.id = this.generateUUID()
          res = await workApi.addWorks(saveData)
        }

        uni.hideLoading()

        if (res.code === '200' || res.code === 200) {
          // 保存附件关联到数据库（异步处理，不阻塞主流程）
          const workId = this.isEditMode ? this.editingWorkId : saveData.id
          console.log('保存成功，工作ID:', workId, '是否有附件需要保存:', this.uploadedFiles.length > 0)

          if (this.uploadedFiles.length > 0 && workId) {
            // 显示附件保存提示
            uni.showToast({
              title: '正在保存附件...',
              icon: 'loading',
              duration: 2000
            })

            // 异步保存附件，不阻塞用户操作
            setTimeout(async () => {
              try {
                console.log('开始异步保存工作附件关联...')
                console.log('工作ID:', workId)
                console.log('待保存的附件数量:', this.uploadedFiles.length)
                console.log('待保存的附件详情:', this.uploadedFiles)

                for (const file of this.uploadedFiles) {
                  try {
                    // 修复：必须使用服务器返回的完整路径，不能使用原始文件名
                    // file.filePath 格式应为：/resource/task_manage/UUID-timestamp-filename
                    const fileName = file.filePath || file.fileName || 'unknown';

                    console.log('保存附件文件路径详情:', {
                      'file.filePath': file.filePath,
                      'file.fileName': file.fileName,
                      '最终使用的fileName': fileName,
                      '是否为完整路径': fileName.includes('/resource/')
                    })

                    const fileData = {
                      pid: workId,
                      name: fileName,              // 必须是完整路径，包含UUID-timestamp前缀
                      type: 'detail'                 // 附件类型
                    }
                    console.log('准备保存附件关联:', fileData)
                    console.log('原文件信息:', file)

                    const saveRes = await workApi.addWorksFile(fileData)
                    console.log('addWorksFile API响应:', saveRes)

                    if (saveRes.code === '200' || saveRes.code === 200) {
                      console.log('工作附件关联保存成功:', file.fileName)
                    } else {
                      console.error('工作附件关联保存失败:', file.fileName, '响应码:', saveRes.code, '消息:', saveRes.msg)
                    }
                  } catch (error) {
                    console.error('单个工作附件保存失败:', file.fileName, error)
                  }
                }

                console.log('所有工作附件关联保存完成')
              } catch (error) {
                console.error('批量保存工作附件关联失败:', error)
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
            // 跳转到工作主页并强制刷新
            uni.redirectTo({
              url: '/pages/works/works?forceRefresh=true',
              success: function() {
                console.log('跳转到工作主页成功，页面将自动刷新')
              },
              fail: function(err) {
                console.error('跳转到工作主页失败:', err)
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
        console.error('保存工作失败:', error)
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
      // 总是跳转到工作主页并强制刷新
      uni.redirectTo({
        url: '/pages/works/works?forceRefresh=true',
        success: function() {
          console.log('返回工作主页成功，页面将自动刷新')
        },
        fail: function(err) {
          console.error('返回工作主页失败:', err)
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

  &:hover {
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