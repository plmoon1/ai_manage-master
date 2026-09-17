<template>
  <view class="container">
    <u-navbar
      title="任务创建"
      :autoBack="true"
    >
    </u-navbar>
    <view class="header">
      <text class="title">任务创建</text>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">任务名称 <text class="required">*</text></text>
        <input class="input" placeholder="请输入任务名称" v-model="formData.taskName" @input="onTaskNameInput"/>
      </view>

      <view class="form-item">
        <text class="label">任务类型 <text class="required">*</text></text>
        <view class="picker" @tap="showTypePicker = true">
          {{selectedTypeText || '请选择任务类型'}}
          <text class="arrow">›</text>
        </view>
      </view>

      <!-- 学院选择 -->
      <view class="form-item">
        <text class="label">学院 <text class="required">*</text></text>
        <!-- role为2和1时，学院固定；role为3时，学院也固定（根据需求） -->
        <view class="picker" style="background-color: #ffffff; border: 1rpx solid #ddd; border-radius: 8rpx; padding: 0 20rpx; height: 80rpx; line-height: 80rpx;">
          {{colleges[collegeIndex] ? colleges[collegeIndex].name : userInfo.tenant || '请选择学院'}}
        </view>
      </view>

      <!-- 科室选择 -->
      <view class="form-item">
        <text class="label">科室 <text class="required">*</text></text>
        <!-- 学院领导可以选择科室；科室职员和科室领导时科室固定 -->
        <template v-if="userInfo.role === '学院领导'">
          <picker 
            @change="onDepartmentChange" 
            :value="departmentIndex" 
            :range="departmentsNames"
            style="width: 100%; box-sizing: border-box;"
          >
            <view class="picker" style="width: 100%; box-sizing: border-box;">
              {{departments[departmentIndex] ? departments[departmentIndex].name : '请选择科室'}}
              <text class="arrow">›</text>
            </view>
          </picker>
        </template>
        <template v-else>
          <view class="picker" style="background-color: #ffffff; border: 1rpx solid #ddd; border-radius: 8rpx; padding: 0 20rpx; height: 80rpx; line-height: 80rpx; width: 100%; ">
            {{departments[departmentIndex] ? departments[departmentIndex].name : '请选择科室'}}
          </view>
        </template>
      </view>

      <view class="form-item">
        <text class="label">重要程度 <text class="required">*</text></text>
        <picker @change="onSeverityChange" :value="severityIndex" :range="severityNames">
          <view class="picker">
            {{severity[severityIndex] ? severity[severityIndex].name : '请选择重要程度'}}
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
	  
	  <view class="form-item">
        <text class="label">任务状态 <text class="required">*</text></text>
        <picker @change="onStateChange" :value="stateIndex" :range="stateNames">
          <view class="picker">
            {{state[stateIndex] ? state[stateIndex].name : '请选择任务状态'}}
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">开始时间 <text class="required">*</text></text>
        <picker mode="date" @change="onStartDateChange" :value="formData.startDate">
          <view class="picker">
            {{formData.startDate || '请选择开始时间'}}
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">结束时间 <text class="required">*</text></text>
        <picker mode="date" @change="onEndDateChange" :value="formData.endDate">
          <view class="picker">
            {{formData.endDate || '请选择结束时间'}}
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">负责人 <text class="required">*</text></text>
        <input class="input" placeholder="请输入负责人，多人用逗号分隔" v-model="formData.head" @input="onHeadInput"/>
      </view>

      <view class="form-item">
        <text class="label">计划周期 <text class="required">*</text></text>
        <picker @change="onCycleChange" :value="cycleIndex" :range="cycleNames">
          <view class="picker">
            {{cycleList[cycleIndex] ? cycleList[cycleIndex].name : '请选择计划周期'}}
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">
			任务详情 
			<text class="required">*</text>
		</text>
		<view class="editor-toolbar">
		    <button size="mini" @click="uploadFile('detail')">上传文件</button>
		    <button size="mini" @click="clearEditor">清空</button>
		</view>
        <editor
            id="editor"
            class="editor"
            placeholder="请填写任务详情"
            @input="onEditorInput"
            @ready="onEditorReady"
          />
          
        <!-- 附件列表 -->
        <view class="attachments-container" v-if="attachments.detail.length > 0">
          <view class="attachments-title">附件列表</view>
          <view class="attachment-item" v-for="(file, index) in attachments.detail" :key="index">
            <view class="attachment-info">
              <text class="attachment-icon">📎</text>
              <text class="attachment-name">{{ file.fileName }}</text>
              <text class="attachment-size">({{ file.size }})</text>
            </view>
            <view class="attachment-delete" @click="deleteAttachment('detail', index)">
              ✕
            </view>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">
      			任务结果 
      		<text class="required">*</text>
      		</text>
      		<view class="editor-toolbar">
      		    <button size="mini" @click="uploadFile('result')">上传文件</button>
      		    <button size="mini" @click="clearResultEditor">清空</button>
      		</view>
        <editor
            id="editor-result"
            class="editor"
            placeholder="请填写任务结果"
            @input="onResultEditorInput"
            @ready="onResultEditorReady"
          />
          
        <!-- 附件列表 -->
        <view class="attachments-container" v-if="attachments.result.length > 0">
          <view class="attachments-title">附件列表</view>
          <view class="attachment-item" v-for="(file, index) in attachments.result" :key="index">
            <view class="attachment-info">
              <text class="attachment-icon">📎</text>
              <text class="attachment-name">{{ file.fileName }}</text>
              <text class="attachment-size">({{ file.size }})</text>
            </view>
            <view class="attachment-delete" @click="deleteAttachment('result', index)">
              ✕
            </view>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">任务进度</text>
        <input type="number"
          v-model="formData.progress"
          placeholder="请输入进度(0-100)"
          @input="onProgressInput"
          @blur="onProgressBlur" />
      </view>

      <view class="form-item">
        <text class="label">任务属性 <text class="required">*</text></text>
        <picker @change="onAttributeChange" :value="attributeIndex" :range="attributeNames">
          <view class="picker">
            {{attributeList[attributeIndex] ? attributeList[attributeIndex].name : '请选择任务属性'}}
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">备注</text>
        <textarea class="textarea" placeholder="其他需要说明的事项..." v-model="formData.remark" @input="onRemarkInput"/>
      </view>

      <view class="btn-submit" @tap="submitTask">提交申报</view>
    </view>

    <!-- 任务类型多级选择器 -->
    <u-picker
      :show="showTypePicker"
      :columns="typeColumns"
      keyName="name"
      @confirm="onTypeConfirm"
      @cancel="showTypePicker = false"
      @change="onTypeColumnChange"
    ></u-picker>
  </view>
</template>

<script>
import { taskApi, logApi, userApi, fileApi, departmentApi, tenantApi, authApi, roleApi, dictApi } from '@/api'
import { arrayToMap } from '@/utils/dictHelper.js'
import config from '@/config'

export default {
  data() {
    return {
      formData: {
        id: '', // 任务ID（UUID）
        taskName: '',
        taskType: '',
	severity:'',
        startDate: '',
        endDate: '',
        head: '', // 负责人，对应后端head字段
        cycle: '', // 计划周期
        description: '',
        progress: '',
        taskResult: '',
	state:'',
        remark: '',
        // 新增属性字段
        attribute: '',
        // 新增科室和学院字段
        college: '',
        department: '',
        isshow: '0', // 是否公开
        isopen: '0'  // 是否开放
      },
      // 附件列表数据，按类型区分
      attachments: {
        detail: [], // 任务详情附件
        result: []  // 任务结果附件
      },
      typesIndex: -1,
      taskTypes: [],
      severityIndex:-1,
      severity: [],
      editorCtx:null,
      resultEditorCtx:null,
      stateIndex:-1,
      state: [],
      // 属性相关
      attributeIndex: -1,
      attributeList: [],
      // 计划周期相关
      cycleIndex: 0,
      cycleList: [],
      // 新增科室和学院数据
      colleges: [],
      collegeIndex: -1,
      departments: [],
      departmentIndex: -1,
      // 用户信息
      userInfo: uni.getStorageSync('userInfo') || {},
      // 任务类型多级选择器相关
      showTypePicker: false,
      selectedTypeText: '',
      typeColumns: [], // 多级选择器列数据
      typeSelectedIndex: [0, 0, 0] // 各列选中的索引
    }
  },
  
  async onLoad(options) {
    try {
      // 生成UUID
      this.formData.id = this.generateUUID();

      // 获取用户信息
      this.userInfo = uni.getStorageSync('userInfo') || {};

      console.log('当前用户信息:', this.userInfo);

      // 调用后端接口获取字典数据，传递tid参数
      // 确保tid存在，否则使用默认值1
      const tid = this.userInfo.tid || '1';
      const dictRes = await dictApi.getAllSysdicByTid({ tid });

      console.log('========== 字典接口响应详情 ==========');
      console.log('完整响应:', dictRes);
      console.log('statusCode:', dictRes.statusCode);
      console.log('response.data:', dictRes.data);
      console.log('response.data.code:', dictRes.data?.code);
      console.log('response.data.data:', dictRes.data?.data);
      console.log('response.data.data的类型:', typeof dictRes.data?.data);
      console.log('=========================================');

      let taskTypesList = [];
      let severityList = [];
      let stateList = [];
      let attributeList = [];
      let cycleList = [];

      // 直接写死任务状态选项
      stateList = [
        { code: '1', name: '已完成' },
        { code: '2', name: '进行中' },
        { code: '3', name: '已撤销' },
        { code: '4', name: '待开始' }
      ];
      console.log('任务状态列表（写死）:', stateList);

      if (dictRes.statusCode === 200 && (dictRes.data.code === '200' || dictRes.data.code === 200)) {
        const dictData = dictRes.data.data || [];

        console.log('后端返回的字典原始数据:', dictData);
        console.log('dictData类型:', typeof dictData);
        console.log('dictData是否为数组:', Array.isArray(dictData));

        // 如果dictData不是数组，尝试转换为数组或处理对象结构
        let dataArray = [];
        if (Array.isArray(dictData)) {
          dataArray = dictData;
        } else if (typeof dictData === 'object' && dictData !== null) {
          // 如果是对象，可能是按类型分组的数据结构
          // 尝试将所有值合并到一个数组
          dataArray = Object.values(dictData).flat();
          console.log('dictData转换为数组:', dataArray);
        } else {
          console.warn('dictData不是有效的数据格式');
        }

        // 检查数组中第一项的结构，了解字段名
        if (dataArray.length > 0) {
          console.log('数组第一项结构:', dataArray[0]);
          console.log('数组第一项的所有字段:', Object.keys(dataArray[0]));
        }

        // 处理任务类型（多级结构）
        taskTypesList = this.buildTaskTypeTree(dataArray);
        console.log('任务类型列表:', taskTypesList);

        // 处理重要程度
        const impItems = dataArray.filter(item => item.field === 'imp');
        console.log('field===imp的项:', impItems);
        severityList = impItems.map(item => ({ code: item.id, name: item.value }));
        console.log('重要程度列表:', severityList);

        // 处理任务属性（计划属性）
        const attributeItems = dataArray.filter(item => item.field === 'attribute');
        console.log('field===attribute的项:', attributeItems);
        attributeList = attributeItems.map(item => ({ code: item.id, name: item.value }));
        console.log('任务属性列表:', attributeList);

        // 处理计划周期
        const cycleItems = dataArray.filter(item => item.field === 'cycle');
        console.log('field===cycle的项:', cycleItems);
        cycleList = cycleItems.map(item => ({ code: item.id, name: item.value }));
        console.log('计划周期列表:', cycleList);

        console.log('解析后的字典数据:', {
          taskTypes: taskTypesList,
          severity: severityList,
          state: stateList,
          attribute: attributeList,
          cycle: cycleList
        });
      } else {
        console.error('获取字典数据失败:', dictRes);
        console.error('statusCode:', dictRes.statusCode);
        console.error('response.data:', dictRes.data);
        uni.showToast({
          title: dictRes.data?.msg || '获取字典数据失败',
          icon: 'none'
        });
      }

      this.taskTypes = taskTypesList;
      this.taskTypesMap = arrayToMap(taskTypesList);

      this.severity = severityList;
      this.severityMap = arrayToMap(severityList);

      this.state = stateList;
      this.stateMap = arrayToMap(stateList);

      this.attributeList = attributeList;
      this.cycleList = cycleList;

      // 加载科室和学院数据
      await this.loadCollegesAndDepartments();

      // 初始化任务类型多级选择器数据
      this.initTypeColumns();

      // 如果有数据，默认选中第一项
      if (this.taskTypes.length > 0) {
        this.typesIndex = 0;
        this.formData.taskType = this.taskTypes[0].code;
      }
      if (this.severity.length > 0) {
        this.severityIndex = 0;
        this.formData.severity = this.severity[0].code;
      }
      if (this.state.length > 0) {
        this.stateIndex = 0;
        this.formData.state = this.state[0].name;
      }
      if (this.attributeList.length > 0) {
        this.attributeIndex = 0;
        this.formData.attribute = this.attributeList[0].code;
      }
      if (this.cycleList.length > 0) {
        this.cycleIndex = 0;
        this.formData.cycle = this.cycleList[0].name;
      }

    } catch (error) {
      console.error('加载字典失败:', error);
      uni.showToast({
        title: '加载字典失败',
        icon: 'none'
      });
    }
  },

  computed: {
    taskTypesNames() {
      return this.taskTypes.map(t => t.name)
    },
    severityNames() {
      return this.severity.map(s => s.name)
    },
	stateNames() {
	  return this.state.map(s => s.name)
	},
	// 新增学院和科室名称列表
	collegesNames() {
	  return this.colleges.map(c => c.name)
	},
	departmentsNames() {
	  return this.departments.map(d => d.name)
	},
	// 属性名称列表
	attributeNames() {
	  return this.attributeList.map(a => a.name)
	},
	// 计划周期名称列表
	cycleNames() {
	  return this.cycleList.map(c => c.name)
	},
},
	methods: {
	// 构建任务类型树形结构
	buildTaskTypeTree(dictData) {
		console.log('buildTaskTypeTree 输入数据:', dictData);

		// 过滤出任务类型（field === 'type'）
		const taskTypeItems = dictData.filter(item => item.field === 'type');

		console.log('过滤后的任务类型项:', taskTypeItems);

		// 找出顶层节点（没有fid、fid等于自己或者fid不存在于taskTypeItems中的，即一级分类）
		const topLevelItems = taskTypeItems.filter(item => {
			// 如果没有fid，或者fid等于自己，或者fid不存在于taskTypeItems中，就是顶层节点
			if (!item.fid || item.fid === item.id) {
				return true;
			}
			// 检查fid是否存在于taskTypeItems中
			const parentExists = taskTypeItems.some(parentItem => parentItem.id === item.fid);
			return !parentExists;
		});

		console.log('顶层节点（一级分类）:', topLevelItems);

		// 递归构建树形结构，使用后端提供的children字段
		const buildNode = (item) => {
			// 直接使用后端返回的数据结构，确保保留所有必要字段
			const node = {
				code: item.id,
				name: item.value,
				children: []
			};

			// 如果有children，递归处理
			if (item.children && Array.isArray(item.children) && item.children.length > 0) {
				node.children = item.children.map(child => buildNode(child));
			}

			return node;
		};

		const result = topLevelItems.map(item => buildNode(item));

		console.log('构建后的树形结构:', result);

		// 如果没有构建成功，返回扁平数据
		if (result.length === 0) {
			return taskTypeItems.map(item => ({
				code: item.id,
				name: item.value,
				children: []
			}));
		}

		return result;
	},

	rightClick() {
		console.log('rightClick');
	},
	leftClick() {
	    console.log('leftClick');
	},
	// 生成UUID (8-4-4-4-12格式)
	generateUUID() {
		const chars = '0123456789abcdef';
		const uuid = [];
		// 8-4-4-4-12 format
		const format = [8, 4, 4, 4, 12];
		
		format.forEach((length, index) => {
			for (let i = 0; i < length; i++) {
				uuid.push(chars[Math.floor(Math.random() * 16)]);
			}
			if (index < format.length - 1) {
				uuid.push('-');
			}
		});
		
		return uuid.join('');
	},
	
	// 上传文件（支持图片和其他类型文件，附件列表模式）
	async uploadFile(attachType) {
		const that = this;
		
		// 检查是否为H5环境
		const isH5 = typeof window !== 'undefined' && typeof document !== 'undefined';
		
		if (isH5) {
			// H5环境，使用HTML5文件输入
			const input = document.createElement('input');
			input.type = 'file';
			input.accept = '*/*';
			input.style.display = 'none';
			
			input.onchange = (e) => {
				const file = e.target.files[0];
				if (file) {
					// 格式化文件大小
					const formattedSize = that.formatFileSize(file.size);
					
					try {
						// 显示加载提示
						uni.showLoading({
							title: '上传中...'
						});
						
						// 创建FormData对象
						const formData = new FormData();
						formData.append('file', file);
						
						// 获取token
						const token = uni.getStorageSync('token') || '';

						// 构建上传URL
						const uploadUrl = (config.baseURL || '') + '/files/upload';

						// 使用fetch API上传文件（H5环境更可靠）
						fetch(uploadUrl, {
							method: 'POST',
							body: formData,
							headers: {
								...(token ? { token } : {})
							}
						})
						.then(response => response.json())
						.then(result => {
							uni.hideLoading();
							if (result.code === '200') {
								const url = result.data;
								
								// 将文件添加到附件列表
								that.attachments[attachType].push({
									id: Date.now() + Math.random().toString(36).substr(2, 9), // 生成临时ID
									fileName: file.name,
									size: formattedSize,
									originalSize: file.size,
									tempFilePath: file.name, // H5环境下使用文件名作为临时路径
									url: url,
									type: attachType
								});
								
								uni.showToast({ title: '文件上传成功', icon: 'success' });
							} else {
								uni.showToast({ title: result.msg || '文件上传失败', icon: 'none' });
							}
						})
						.catch(error => {
							uni.hideLoading();
							console.error('文件上传失败:', error);
							uni.showToast({ title: '文件上传失败', icon: 'none' });
						});
					} catch (error) {
						uni.hideLoading();
						console.error('文件上传失败:', error);
						uni.showToast({ title: '文件上传失败', icon: 'none' });
					}
				}
				// 移除临时元素
				document.body.removeChild(input);
			};
			
			// 添加到DOM并触发选择
			document.body.appendChild(input);
			input.click();
		} else {
			// 非H5环境，使用uni.chooseMessageFile
			uni.chooseMessageFile({
				count: 1,
				type: 'file',
				success: async function(chooseRes) {
					const tempFile = chooseRes.tempFiles[0];
					const tempFilePath = tempFile.path;
					const fileName = tempFile.name;
					const fileSize = tempFile.size;
					
					// 格式化文件大小
					const formattedSize = that.formatFileSize(fileSize);
					
					try {
						// 显示加载提示
						uni.showLoading({
							title: '上传中...'
						});
						
						// 上传文件到后端
						// 构建上传URL
						const uploadUrl = (config.baseURL || '') + '/files/upload';

						uni.uploadFile({
							url: uploadUrl,
							filePath: tempFilePath,
							name: 'file',
							success: async (uploadRes) => {
								uni.hideLoading();
								const result = JSON.parse(uploadRes.data);
								if (result.code === '200') {
									const url = result.data;
									
									// 将文件添加到附件列表
									that.attachments[attachType].push({
										id: Date.now() + Math.random().toString(36).substr(2, 9), // 生成临时ID
										fileName: fileName,
										size: formattedSize,
										originalSize: fileSize,
										tempFilePath: tempFilePath,
										url: url,
										type: attachType
									});
									
									uni.showToast({ title: '文件上传成功', icon: 'success' });
								} else {
									uni.showToast({ title: '文件上传失败', icon: 'none' });
								}
							},
							fail: (error) => {
								uni.hideLoading();
								console.error('文件上传失败:', error);
								uni.showToast({ title: '文件上传失败', icon: 'none' });
							}
						});
					} catch (error) {
						uni.hideLoading();
						console.error('文件上传失败:', error);
						uni.showToast({ title: '文件上传失败', icon: 'none' });
					}
				}
			});
		}
	},
	
	// 删除附件
	deleteAttachment(attachType, index) {
		uni.showModal({
			title: '确认删除',
			content: '确定要删除此附件吗？',
			success: (res) => {
				if (res.confirm) {
					this.attachments[attachType].splice(index, 1);
					uni.showToast({ title: '删除成功', icon: 'success' });
				}
			}
		});
	},
	
	// 格式化文件大小
	formatFileSize(bytes) {
		if (bytes === 0) return '0 Bytes';
		const k = 1024;
		const sizes = ['Bytes', 'KB', 'MB', 'GB'];
		const i = Math.floor(Math.log(bytes) / Math.log(k));
		return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
	},
	
	// 清空任务详情编辑器
	clearEditor() {
		if (!this.editorCtx) return
		this.editorCtx.clear()
		this.formData.description = ''
	},
	
	// 清空任务结果编辑器
	clearResultEditor() {
		if (!this.resultEditorCtx) return
		this.resultEditorCtx.clear()
		this.formData.taskResult = ''
	},
	
	// 加载学院和科室数据
	async loadCollegesAndDepartments() {
		try {
			const { tid, did, role } = this.userInfo
			
			console.log('当前角色名称:', role)
			
			// 调用后端接口获取学院列表
			const collegesRes = await tenantApi.getAllTenants({ token: this.userInfo.token })

			if (collegesRes.code === '200') {
				this.colleges = collegesRes.data || []
			}

			// 调用后端接口获取科室列表
			const deptsRes = await departmentApi.getDepartmentByTid({
				tid: tid,
				token: this.userInfo.token
			})

			if (deptsRes.code === '200') {
				this.departments = deptsRes.data || []
			}
			
			// 根据角色名称设置默认值
			// 学院领导：可以选择科室
			// 科室职员、科室领导：科室固定
			
			// 所有角色学院都固定为用户所属的学院，只有在formData.college为空时设置
			if (!this.formData.college) {
				this.formData.college = tid || ''
			}
			// 将tid转换为字符串，确保与item.id类型一致
			this.collegeIndex = this.colleges.findIndex(item => item.id == tid)
			console.log('学院默认值设置:', {
				tid: tid,
				collegeIndex: this.collegeIndex,
				colleges: this.colleges
			})
			
			// 设置科室默认值，只有在formData.department为空时才设置
			if (!this.formData.department) {
				if (role === '学院领导') {
					// 学院领导可以选择科室，默认选中第一个
					if (this.departments.length > 0) {
						this.formData.department = this.departments[0].id
						this.departmentIndex = 0
					}
				} else {
					// 科室职员、科室领导，科室固定
					this.formData.department = did || ''
					// 将did转换为字符串，确保与item.id类型一致
					this.departmentIndex = this.departments.findIndex(item => item.id == did)
				}
			}
			console.log('科室默认值设置:', {
				did: did,
				role: role,
				departmentIndex: this.departmentIndex,
				departments: this.departments
			})
			
			console.log('学院列表:', this.colleges)
			console.log('科室列表:', this.departments)
		} catch (error) {
			console.error('加载学院和科室数据失败:', error)
		}
	},
	onEditorReady(){
		uni.createSelectorQuery()
		.select('#editor')
		.context(res=>{
			this.editorCtx = res.context
		})
		.exec()
	},
	onResultEditorReady(){
		uni.createSelectorQuery()
		.select('#editor-result')
		.context(res=>{
			this.resultEditorCtx = res.context
		})
		.exec()
	},
	onEditorInput(e){
		this.formData.description = e.detail.html
	},
	onResultEditorInput(e){
		this.formData.taskResult = e.detail.html
	},
    onTaskNameInput() {
      // 使用v-model自动绑定，无需额外处理
    },

    onTypeChange(e) {
      this.typesIndex = e.detail.value
      this.formData.taskType = this.taskTypes[e.detail.value].code
    },

    // 初始化任务类型多级选择器数据
    initTypeColumns() {
      // 第一列：一级分类
      const level1 = this.taskTypes.map(item => ({ code: item.code, name: item.name }));

      // 第二列：二级分类（根据第一列第一个选项）
      let level2 = [];
      let level3 = [];
      if (this.taskTypes[0]?.children) {
        level2 = this.taskTypes[0].children.map(item => ({ code: item.code, name: item.name }));

        // 第三列：三级分类（根据第二列第一个选项）
        if (level2[0]?.children) {
          level3 = level2[0].children.map(item => ({ code: item.code, name: item.name }));
        }
      }

      // 初始化三列数据
      this.typeColumns = [level1, level2, level3];

      // 默认选择第一项
      if (level1.length > 0) {
        this.selectedTypeText = this.getFullTypeName(level1[0], level2[0], level3[0]);
        this.formData.taskType = level3[0]?.code || level2[0]?.code || level1[0]?.code || '';
      }
    },

    // 多级选择器列变化
    onTypeColumnChange(e) {
      const { columnIndex, index } = e;
      const columns = this.typeColumns;

      // 保存当前选择的索引
      this.typeSelectedIndex[columnIndex] = index;

      // 如果第一列变化，更新第二列
      if (columnIndex === 0) {
        const selectedLevel1 = this.taskTypes[index];
        if (selectedLevel1?.children) {
          columns[1] = selectedLevel1.children.map(item => ({ code: item.code, name: item.name }));
          columns[2] = []; // 清空第三列

          // 如果第二列有数据，默认选中第一项并更新第三列
          if (columns[1].length > 0) {
            const selectedLevel2 = selectedLevel1.children[0];
            if (selectedLevel2?.children) {
              columns[2] = selectedLevel2.children.map(item => ({ code: item.code, name: item.name }));
            }
          }
        } else {
          columns[1] = [];
          columns[2] = [];
        }
      }
      // 如果第二列变化，更新第三列
      else if (columnIndex === 1) {
        const selectedLevel1 = this.taskTypes[this.typeSelectedIndex[0]];
        const selectedLevel2 = selectedLevel1?.children?.[index];
        if (selectedLevel2?.children) {
          columns[2] = selectedLevel2.children.map(item => ({ code: item.code, name: item.name }));
        } else {
          columns[2] = [];
        }
      }

      this.typeColumns = [...columns];
    },

    // 多级选择器确认选择
    onTypeConfirm(e) {
      const values = e.value;
      const indexes = e.indexs;

      // 保存选择的索引
      this.typeSelectedIndex = indexes;

      // 获取选中的各级数据
      const level1 = values[0] || {};
      const level2 = values[1] || {};
      const level3 = values[2] || {};

      // 设置显示文本（使用最深级别的名称）
      this.selectedTypeText = this.getFullTypeName(level1, level2, level3);

      // 设置任务类型代码（使用最深级别的代码）
      this.formData.taskType = level3.code || level2.code || level1.code || '';

      this.showTypePicker = false;
    },

    // 获取完整的类型名称
    getFullTypeName(level1, level2, level3) {
      const parts = [];
      if (level1?.name) parts.push(level1.name);
      if (level2?.name && level2.code !== level1?.code) parts.push(level2.name);
      if (level3?.name && level3.code !== level2?.code) parts.push(level3.name);
      return parts.join(' / ') || '';
    },

    onSeverityChange(e) {
      this.severityIndex = e.detail.value
      this.formData.severity = this.severity[e.detail.value].code
    },
	onStateChange(e){
      this.stateIndex = e.detail.value
      this.formData.state = this.state[e.detail.value].name
    },
    
    // 属性选择事件
    onAttributeChange(e) {
      this.attributeIndex = e.detail.value
      this.formData.attribute = this.attributeList[e.detail.value].code
    },
    
    // 学院选择事件
    onCollegeChange(e) {
      this.collegeIndex = e.detail.value
      this.formData.college = this.colleges[e.detail.value].id
    },
    
    // 科室选择事件
    onDepartmentChange(e) {
      this.departmentIndex = e.detail.value
      this.formData.department = this.departments[e.detail.value].id
    },

    onStartDateChange(e) {
      this.formData.startDate = e.detail.value
    },

    onEndDateChange(e) {
      this.formData.endDate = e.detail.value
    },

    onHeadInput() {
      // 使用v-model自动绑定，无需额外处理
    },

    onCycleChange(e) {
      this.cycleIndex = e.detail.value
      this.formData.cycle = this.cycleList[e.detail.value].name
    },

    onDescriptionInput() {
      // 使用v-model自动绑定，无需额外处理
    },

    onExpectedResultInput() {
      // 使用v-model自动绑定，无需额外处理
    },

    onBudgetInput() {
      // 使用v-model自动绑定，无需额外处理
    },
    
    onProgressInput(e) {
      // 验证任务进度，确保只能填写0-100的数字
      let value = e.detail.value;

    // 只保留数字
    value = value.replace(/\D/g, '');

    this.formData.progress = value;
    },
     onProgressBlur() {
    let value = Number(this.formData.progress);

    if (isNaN(value) || value === '') {
      this.formData.progress = '';
      return;
    }

    if (value < 0) {
      this.formData.progress = '0';
    } else if (value > 100) {
      this.formData.progress = '100';
    } else {
      this.formData.progress = String(value);
    }
  },

    onRemarkInput() {
      // 使用v-model自动绑定，无需额外处理
    },

    validateForm() {
      const { taskName, taskType, startDate, state, endDate, description, college, department, head, cycle, progress } = this.formData
      
      if (!taskName) {
        uni.showToast({ title: '请输入任务名称', icon: 'none' })
        return false
      }
      
      if (!taskType) {
        uni.showToast({ title: '请选择任务类型', icon: 'none' })
        return false
      }
      
      if (!college) {
        uni.showToast({ title: '请选择学院', icon: 'none' })
        return false
      }
      
      if (!department) {
        uni.showToast({ title: '请选择科室', icon: 'none' })
        return false
      }
      
      if (!startDate) {
        uni.showToast({ title: '请选择开始时间', icon: 'none' })
        return false
      }
      
      if (!endDate) {
        uni.showToast({ title: '请选择结束时间', icon: 'none' })
        return false
      }
      
      if (startDate && endDate && startDate > endDate) {
        uni.showToast({ title: '开始时间不能晚于结束时间', icon: 'none' })
        return false
      }
      
      if (!state) {
        uni.showToast({ title: '请选择任务状态', icon: 'none' })
        return false
      }
      
      if (!description) {
        uni.showToast({ title: '请输入任务详情', icon: 'none' })
        return false
      }
      
      if (!head) {
        uni.showToast({ title: '请输入负责人', icon: 'none' })
        return false
      }
      
      if (!cycle) {
        uni.showToast({ title: '请选择计划周期', icon: 'none' })
        return false
      }
      
      if (!this.formData.attribute) {
        uni.showToast({ title: '请选择任务属性', icon: 'none' })
        return false
      }
      
      // 验证任务进度
      if (progress && (isNaN(progress) || progress < 0 || progress > 100)) {
        uni.showToast({ title: '任务进度只能填写0-100的数字', icon: 'none' })
        return false
      }
      
      return true
    },

    async submitTask() {
      if (!this.validateForm()) return
      
      try {
        const userInfo = uni.getStorageSync('userInfo') || {};
        const uid = userInfo.id || userInfo.uid || ''; // 使用id字段作为uid，兼容userInfo中的不同字段名
        const { tid, did } = userInfo
        
        console.log('当前用户信息:', userInfo)
        console.log('准备提交的任务数据:', {
          taskName: this.formData.taskName,
          taskType: this.formData.taskType,
          severity: this.formData.severity,
          state: this.formData.state,
          attribute: this.formData.attribute,
          uid: uid,
          tid: this.formData.college || tid,
          did: this.formData.department || did
        })
        
        // 准备提交的数据
        // 科室传代码，任务类型、任务重要性、任务属性、任务状态传明文
        // 任务类型名称从selectedTypeText获取（已经包含了多级名称）
        const taskTypeName = this.selectedTypeText || this.formData.taskType;
        const severityName = this.severity.find(item => item.code === this.formData.severity)?.name || this.formData.severity;
        const attributeName = this.attributeList.find(item => item.code === this.formData.attribute)?.name || this.formData.attribute;

        const taskData = {
          id: this.generateUUID(), // 生成UUID作为任务ID
          name: this.formData.taskName,
          type: taskTypeName, // 任务类型明文
          imp: severityName, // 重要程度明文
          status: this.formData.state, // 任务状态明文
          startTime: this.formData.startDate,
          endTime: this.formData.endDate,
          head: this.formData.head, // 负责人
          cycle: this.formData.cycle, // 计划周期
          detail: this.formData.description,
          progress: this.formData.progress || 0, // 确保进度有默认值
          achievement: this.formData.taskResult,
          uid: uid || '', // 确保uid正确传递
          // 使用表单中选择的学院和科室，而不是用户信息中的
          tid: this.formData.college || tid || '',
          did: this.formData.department || did || '', // 科室代码
          attribute: attributeName, // 任务属性明文
          isshow: this.formData.isshow || '0', // 是否公开
          isopen: this.formData.isopen || '0' // 是否开放
        }
        
        // 新增任务，调用addPlan接口
        const res = await taskApi.createPlan(taskData)

        if (res.code === '200') {
          // 提交任务成功后，处理所有附件
          await this.processAttachments(taskData.id);

          uni.showToast({
            title: '提交成功',
            icon: 'success'
          })

          // 发送任务更新事件，通知任务列表刷新
          uni.$emit('taskUpdated');

          setTimeout(() => {
            uni.switchTab({
              url: '/pages/task/task'
            })
          }, 1500)
        } else {
          uni.showToast({
            title: res.msg || '提交失败',
            icon: 'none'
          })
        }
      } catch (error) {
        console.error('提交任务失败:', error)
        uni.showToast({
          title: '网络错误',
          icon: 'none'
        })
      }
    },
    
    // 处理附件，调用addPlanFile接口
    async processAttachments(pid) {
      // 合并所有附件
      const allAttachments = [...this.attachments.detail, ...this.attachments.result];
      
      if (allAttachments.length === 0) {
        return; // 没有附件，直接返回
      }
      
      // 显示加载提示
      uni.showLoading({
        title: '处理附件中...'
      });
      
      try {
        // 遍历所有附件，调用addPlanFile接口
        for (const file of allAttachments) {
          await taskApi.uploadPlanFile({
            pid: pid,
            name: file.url,
            type: file.type
          });
        }
        
        uni.hideLoading();
      } catch (error) {
        uni.hideLoading();
        console.error('处理附件失败:', error);
        // 附件处理失败不影响任务提交成功的结果
      }
    }
  }
}
</script>

<style lang="scss">
/* 统一样式变量 */
$primary-gradient: linear-gradient(135deg, #1890ff 0%, #52c41a 100%);
$primary-color: #1890ff;
$primary-light: rgba(24, 144, 255, 0.1);
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$bg-primary: #ffffff;
$bg-base: #f5f5f5;
$border-base: #e4e7ed;
$radius-base: 12rpx;
$radius-lg: 24rpx;
$spacing-base: 20rpx;
$spacing-lg: 32rpx;
$shadow-sm: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
$shadow-base: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);

.container {
  background-color: #f5f5f5;
  min-height: 100vh;
  padding: 120rpx 20rpx 20rpx;
}

.header {
  text-align: center;
  margin-bottom: 30rpx;
}

.title {
  font-size: 40rpx;
  font-weight: bold;
  color: $text-primary;
}

.form-card {
  background-color: white;
  border-radius: $radius-base;
  padding: 30rpx;
  box-shadow: $shadow-sm;
}

.form-item {
  margin-bottom: 30rpx;
}

.label {
  display: block;
  font-size: 32rpx;
  color: #333;
  margin-bottom: 10rpx;
}

.required {
  color: #ff4d4f;
}

.input {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 32rpx;
  box-sizing: border-box;
}

.picker {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
}

.arrow {
  color: #999;
  font-size: 40rpx;
}

.serverity-options {
  display: flex;
  gap: 20rpx;
}

.serverity-item {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 32rpx;
}

.serverity-item.active {
  background-color: #007aff;
  color: white;
  border-color: #007aff;
}

.editor {
  width: 100%;
  min-height: 150rpx;
  padding: 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 32rpx;
  box-sizing: border-box;
}

.textarea {
  width: 100%;
  min-height: 150rpx;
  padding: 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  font-size: 32rpx;
  box-sizing: border-box;
  resize: none;
}

.btn-submit {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  background-color: #007aff;
  color: white;
  text-align: center;
  border-radius: 10rpx;
  font-size: 36rpx;
  margin-top: 40rpx;
}
.editor-toolbar {
  display: flex;
  gap: 20rpx;
  margin: 10rpx 0;
}

.editor-toolbar button {
  padding: 0 20rpx;
  font-size: 24rpx;
}

/* 附件列表样式 */
.attachments-container {
  margin-top: 20rpx;
  padding: 20rpx;
  background-color: #f8f9fa;
  border-radius: 8rpx;
  border: 1rpx dashed #ddd;
}

.attachments-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: white;
  padding: 20rpx;
  border-radius: 8rpx;
  margin-bottom: 15rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  position: relative;
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

.attachment-size {
  font-size: 24rpx;
  color: #999;
  flex-shrink: 0;
}

.attachment-delete {
  font-size: 32rpx;
  color: #ff4d4f;
  background-color: rgba(255, 77, 79, 0.1);
  width: 50rpx;
  height: 50rpx;
  line-height: 50rpx;
  text-align: center;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.3s;
  margin-left: 15rpx;
}

.attachment-delete:hover {
  background-color: rgba(255, 77, 79, 0.2);
  transform: scale(1.1);
}
</style>
