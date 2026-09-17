<template>
  <view class="container">
    <!-- AI助手悬浮入口（爱宝） -->
    <aibao-float />

    <!-- 顶部导航栏 -->
    <view class="top-nav">
      <text class="nav-title">{{ currentPage.title }}</text>
    </view>
    
    <!-- 内容区域 -->
    <view class="content">
      <!-- 用户信息页面 -->
      <view v-if="currentTab === 0" class="page-content">
        <!-- 搜索栏 -->
        <view class="search-container">
          <view class="search-box">
            <view class="search-inputs">
              <view class="input-wrapper">
                <input type="text" placeholder="用户ID" v-model="searchParams.id" class="search-input" />
                <text v-if="searchErrors.id" class="error-text">{{ searchErrors.id }}</text>
              </view>
              <view class="input-wrapper">
                <input type="text" placeholder="用户名" v-model="searchParams.name" class="search-input" />
                <text v-if="searchErrors.name" class="error-text">{{ searchErrors.name }}</text>
              </view>
              <view class="input-wrapper">
                <picker 
                :range="departmentList" 
                :range-key="'name'"
                class="search-select"
                @change="onDepartmentChange"

              >
                  <view class="picker-display">
                    {{ getSelectedDepartmentName() }}
                  </view>
                </picker>
                <text v-if="searchErrors.did" class="error-text">{{ searchErrors.did }}</text>
              </view>
            </view>
            <view class="search-buttons">
              <button @tap="handleSearch" class="search-btn">搜索</button>
              <button @tap="resetSearch" class="reset-btn">重置</button>
            </view>
          </view>
        </view>
        
        <!-- 用户列表 -->
        <view class="user-list">
          <view class="user-item" v-for="user in userList" :key="user.id">
            <view class="user-info">
              <text class="user-id">ID: {{ user.id }}</text>
              <text class="user-name">{{ user.name }}</text>
              <text class="user-department">{{ user.department }}</text>
              <text class="user-role">{{ user.role }}</text>
            </view>
            <!-- 状态显示 -->
            <view class="user-actions">
              <!-- 正常状态显示修改、删除和停用按钮 -->
              <template v-if="user.status !== '停用' && user.status !== 'disabled' && user.status !== '02'">
                <button @tap="editUser(user)" class="edit-btn">修改</button>
                <button @tap="deleteUser(user)" class="delete-btn">删除</button>
                <button @tap="disableUser(user)" class="disable-btn">停用</button>
              </template>
              <!-- 停用状态显示停用文本 -->
              <template v-else>
                <text class="disabled-text">停用</text>
              </template>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 科室信息页面 -->
      <view v-if="currentTab === 1" class="page-content">
        <view class="department-header">
          <text class="page-title">科室信息管理</text>
        </view>
        
        <!-- 科室列表 -->
        <view class="department-list">
          <view v-if="departmentList.length > 0">
            <view class="department-item" v-for="department in departmentList" :key="department.id">
              <view class="department-info">
                <text class="department-id">ID: {{ department.id }}</text>
                <text class="department-name">{{ department.name }}</text>
              </view>
              <view class="department-actions">
                <button @tap="showEditDepartmentDialog(department)" class="edit-btn">修改</button>
                <button @tap="deleteDepartment(department)" class="delete-btn">删除</button>
              </view>
            </view>
          </view>
          <view v-else class="empty-list">
            <text>暂无科室数据</text>
          </view>
        </view>
        
        <!-- 科室表单弹窗 -->
        <view v-if="departmentDialogVisible" class="dialog-overlay">
          <view class="popup-container">
            <text class="popup-title">{{ isEditDepartment ? '修改科室' : '新增科室' }}</text>
            <view class="form">
              <!-- 新增科室时隐藏科室ID输入框，修改时显示 -->
              <view v-if="isEditDepartment" class="form-item">
                <text class="form-label">科室ID</text>
                <input type="text" v-model="departmentForm.id" placeholder="请输入科室ID" class="form-input" disabled />
                <text v-if="departmentFormErrors.id" class="error-text">{{ departmentFormErrors.id }}</text>
              </view>
              <view class="form-item">
                <text class="form-label">科室名称 <text class="required">*</text></text>
                <input type="text" v-model="departmentForm.name" placeholder="请输入科室名称" class="form-input" />
                <text v-if="departmentFormErrors.name" class="error-text">{{ departmentFormErrors.name }}</text>
              </view>
            </view>
            <view class="popup-actions">
              <button @tap="closeDepartmentDialog" class="cancel-btn">取消</button>
              <button @tap="submitDepartmentForm" class="submit-btn">{{ isEditDepartment ? '修改' : '新增' }}</button>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 数据维护页面 -->
      <view v-if="currentTab === 2" class="page-content">
        <view class="dict-maintenance">
          <text class="page-title">数据维护管理</text>
          
          <!-- 字典类型选择 -->
          <view class="dict-type-selector">
            <text class="label">选择字典类型：</text>
            <picker 
              :range="dictFields" 
              :range-key="'name'"
              class="dict-type-picker"
              @change="onDictTypeChange"
            >
              <view class="picker-display">
                {{ selectedDictField && selectedDictField.name || '请选择字典类型' }}
              </view>
            </picker>
          </view>
          
          <!-- 字典数据树形展示 -->
          <view v-if="selectedDictField" class="dict-tree-container">
            <view class="dict-tree-header">
              <text class="tree-title">{{ selectedDictField.name }} - 字典数据</text>
            </view>
            
            <!-- 加载状态 -->
            <view v-if="loadingDict" class="loading-state">
              <text>加载中...</text>
            </view>
            
            <!-- 字典数据树 -->
            <view v-else-if="dictTreeData.length > 0" class="dict-tree">
              <view v-for="item in dictTreeData" :key="item.id" class="dict-tree-node">
                <!-- 字典树节点 -->
                <view class="dict-node">
                  <view class="node-content">
                    <!-- 展开/折叠图标 -->
                    <view v-if="item.children && item.children.length > 0" class="node-toggle">
                      <text class="toggle-icon">▼</text>
                    </view>
                    <view v-else class="node-toggle placeholder"></view>
                    
                    <!-- 节点内容 -->
                    <view class="node-main">
                      <text class="node-value">{{ item.value }}</text>
                      <view class="node-actions">
                        <text class="action-btn edit" @tap="editDictItem(item)">编辑</text>
                        <text class="action-btn add" @tap="addDictItem(item)">添加</text>
                        <text class="action-btn delete" @tap="deleteDictItem(item)">删除</text>
                      </view>
                    </view>
                  </view>
                  
                  <!-- 子节点 -->
                  <view v-if="item.children && item.children.length > 0" class="node-children">
                    <view v-for="child in item.children" :key="child.id" class="dict-tree-node">
                      <view class="dict-node">
                        <view class="node-content">
                          <view v-if="child.children && child.children.length > 0" class="node-toggle">
                            <text class="toggle-icon">▼</text>
                          </view>
                          <view v-else class="node-toggle placeholder"></view>
                          
                          <view class="node-main">
                            <text class="node-value">{{ child.value }}</text>
                            <view class="node-actions">
                              <text class="action-btn edit" @tap="editDictItem(child)">编辑</text>
                              <text class="action-btn add" @tap="addDictItem(child)">添加</text>
                              <text class="action-btn delete" @tap="deleteDictItem(child)">删除</text>
                            </view>
                          </view>
                        </view>
                        
                        <!-- 孙子节点 -->
                        <view v-if="child.children && child.children.length > 0" class="node-children">
                          <view v-for="grandchild in child.children" :key="grandchild.id" class="dict-tree-node">
                            <view class="dict-node">
                              <view class="node-content">
                                <view class="node-toggle placeholder"></view>
                                
                                <view class="node-main">
                                  <text class="node-value">{{ grandchild.value }}</text>
                                  <view class="node-actions">
                                    <text class="action-btn edit" @tap="editDictItem(grandchild)">编辑</text>
                                    <text class="action-btn add" @tap="addDictItem(grandchild)">添加</text>
                                    <text class="action-btn delete" @tap="deleteDictItem(grandchild)">删除</text>
                                  </view>
                                </view>
                              </view>
                            </view>
                          </view>
                        </view>
                      </view>
                    </view>
                  </view>
                </view>
              </view>
            </view>
            
            <!-- 空数据状态 -->
            <view v-else class="empty-state">
              <text>暂无数据</text>
            </view>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 新增按钮，在用户信息和科室信息界面显示 -->
    <view v-if="currentTab === 0 || currentTab === 1" class="add-user-btn" @tap="handleAddButtonClick">
      <text>+</text>
    </view>
    
    <!-- 退出登录按钮 -->
    <view class="logout-btn" @tap="handleLogout">
      <text class="logout-text">退出登录</text>
    </view>
    
    <!-- 底部导航栏 -->
    <view class="bottom-tabbar">
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 0 }" 
        @tap="switchTab(0)"
      >
        <view class="tab-icon">👤</view>
        <view class="tab-text">用户信息</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 1 }" 
        @tap="switchTab(1)"
      >
        <view class="tab-icon">🏢</view>
        <view class="tab-text">科室信息</view>
      </view>
      <view 
        class="tab-item" 
        :class="{ active: currentTab === 2 }" 
        @tap="switchTab(2)"
      >
        <view class="tab-icon">📊</view>
        <view class="tab-text">数据维护</view>
      </view>
    </view>
    
    <!-- 新增用户弹窗 -->
    <view v-if="addUserDialogVisible" class="dialog-overlay">
      <view class="popup-container">
        <text class="popup-title">新增用户</text>
        <view class="form">
          <view class="form-item">
            <text class="form-label">用户ID <text class="required">*</text></text>
            <input type="text" v-model="userForm.id" placeholder="请输入用户ID" class="form-input" />
            <text v-if="userFormErrors.id" class="error-text">{{ userFormErrors.id }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">用户名 <text class="required">*</text></text>
            <input type="text" v-model="userForm.name" placeholder="请输入用户名" class="form-input" />
            <text v-if="userFormErrors.name" class="error-text">{{ userFormErrors.name }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">密码 <text class="required">*</text></text>
            <input type="password" v-model="userForm.password" placeholder="请输入密码" class="form-input" />
            <text v-if="userFormErrors.password" class="error-text">{{ userFormErrors.password }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">学院 <text class="required">*</text></text>
            <input type="text" :value="getCollegeName()" placeholder="请输入学院ID" class="form-input" disabled />
            <text v-if="userFormErrors.tid" class="error-text">{{ userFormErrors.tid }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">科室 <text class="required">*</text></text>
            <picker 
              :range="departmentList" 
              :range-key="'name'"
              class="form-input"
              @change="onAddUserDepartmentChange"
            >
              <view class="picker-display">
                {{ getSelectedAddUserDepartmentName() }}
              </view>
            </picker>
            <text v-if="userFormErrors.did" class="error-text">{{ userFormErrors.did }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">角色 <text class="required">*</text></text>
            <picker 
              :range="roles" 
              :range-key="'name'"
              class="form-input"
              @change="onAddUserRoleChange"
            >
              <view class="picker-display">
                {{ getSelectedRoleName() }}
              </view>
            </picker>
            <text v-if="userFormErrors.rid" class="error-text">{{ userFormErrors.rid }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">电话</text>
            <input type="text" v-model="userForm.phone" placeholder="请输入电话" class="form-input" />
            <text v-if="userFormErrors.phone" class="error-text">{{ userFormErrors.phone }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">邮箱</text>
            <input type="text" v-model="userForm.email" placeholder="请输入邮箱" class="form-input" />
            <text v-if="userFormErrors.email" class="error-text">{{ userFormErrors.email }}</text>
          </view>
          <view class="form-item">
            <text class="form-label">状态 <text class="required">*</text></text>
            <picker 
              :range="statusOptions" 
              class="form-input"
              @change="onAddUserStatusChange"
            >
              <view class="picker-display">
                {{ userForm.status }}
              </view>
            </picker>
            <text v-if="userFormErrors.status" class="error-text">{{ userFormErrors.status }}</text>
          </view>
        </view>
        <view class="popup-actions">
          <button @tap="closeAddUserDialog" class="cancel-btn">取消</button>
          <button @tap="submitAddUserForm" class="submit-btn">创建</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { taskApi, logApi, userApi, fileApi, departmentApi, tenantApi, authApi, roleApi, dictApi } from '@/api'
import { SERVER_CONFIG } from '@/config/URLS'

export default {
  data() {
    return {
      currentTab: 0, // 当前选中的标签页
      pages: [
        { title: '用户信息管理', icon: '👤' },
        { title: '科室信息管理', icon: '🏢' },
        { title: '数据维护管理', icon: '📊' }
      ],
      // 用户相关数据
      userList: [],
      searchParams: {
        id: '',
        name: '',
        did: ''
      },
      searchErrors: {}, // 搜索表单验证错误
      userInfo: null,
      // 科室相关数据
      departmentList: [],
      departmentForm: {
        id: '',
        name: '',
        tid: ''
      },
      departmentDialogVisible: false,
      isEditDepartment: false,
      departmentFormErrors: {},
      // 新增用户相关数据
      addUserDialogVisible: false,
      userForm: {
        id: '',
        name: '',
        password: '',
        tid: '',
        did: '',
        rid: '',
        phone: '',
        email: '',
        status: '正常'
      },
      userFormErrors: {},
      statusOptions: ['正常', '停用'],
      // 角色列表
      roles: [],
      // 数据维护相关数据
      dictFields: [], // 字典类型列表
      selectedDictField: null, // 选中的字典类型
      dictTreeData: [], // 字典树形数据
      loadingDict: false // 加载状态
    }
  },
  
  computed: {
    currentPage() {
      return this.pages[this.currentTab]
    }
  },

  onLoad() {
    // 页面加载时的初始化操作
    console.log('页面加载，开始获取userInfo');
    this.userInfo = uni.getStorageSync('userInfo') || {};
    console.log('获取到的userInfo:', this.userInfo);
    this.getAllUsers();
    this.getAllDepartments();
    // 获取字典类型列表
    this.getDictFields();
  },

  methods: {
    // 切换标签页
    switchTab(index) {
      this.currentTab = index;
      // 切换到科室标签时获取科室列表
      if (index === 1) {
        console.log('切换到科室标签，尝试获取科室列表');
        this.getAllDepartments();
      } else if (index === 2) {
        console.log('切换到数据维护标签，尝试获取字典类型列表');
        this.getDictFields();
      }
    },
    
    // 获取所有科室
    async getAllDepartments() {
      console.log('开始获取科室列表，userInfo:', this.userInfo);
      
      // 尝试从后端获取真实数据
      if (!this.userInfo || !this.userInfo.tid) {
        console.error('获取真实科室列表失败：缺少tid');
        return;
      }
      
      try {
        // 使用正确的URL格式：带查询参数tid和token
        const token = uni.getStorageSync('token') || this.userInfo.token;
        console.log('请求参数：tid=', this.userInfo.tid, ', token=', token);

        const res = await departmentApi.getDepartmentByTid({
          tid: this.userInfo.tid
        });

        console.log('获取所有科室响应状态:', res.code);
        console.log('获取所有科室响应数据:', res.data);

        if (res.code === '200') {
            let departments = res.data || [];
            // 保留原始UUID，添加数字ID字段
            departments = departments.map((dept, index) => ({
              ...dept,
              originalId: dept.id, // 保存原始UUID
              id: (index + 1).toString() // 使用索引+1作为数字ID
            }));
            this.departmentList = departments;
            console.log('科室列表更新成功:', this.departmentList);
          } else {
          console.error('获取科室列表失败：后端返回错误', res);
          uni.showToast({ title: `获取科室列表失败：${res.msg || '未知错误'}`, icon: 'none' });
        }
      } catch (error) {
        console.error('获取科室列表失败：网络错误', error);
        uni.showToast({ title: '获取科室列表失败：网络错误', icon: 'none' });
      }
    },
    
    // 获取所有用户
    async getAllUsers() {
      try {
        const res = await userApi.getAllUsersByTid(this.userInfo.tid);
        console.log('获取所有用户响应:', res);
        if (res.code === '200') {
          this.userList = res.data || [];
        }
      } catch (error) {
        console.error('获取用户列表失败:', error);
        uni.showToast({ title: '获取用户列表失败', icon: 'none' });
      }
    },
    
    // 搜索用户
    async handleSearch() {
      // 表单验证
      if (!this.validateSearchForm()) {
        return;
      }
      
      try {
        console.log('开始搜索，当前searchParams:', this.searchParams);
        
        // 过滤掉空的搜索参数，只传递有值的字段
        const params = {
          tid: this.userInfo.tid
        };
        
        // 只添加有值的搜索字段
        if (this.searchParams.id) {
          params.id = this.searchParams.id;
          console.log('添加搜索参数：id=', params.id);
        }
        if (this.searchParams.name) {
          params.name = this.searchParams.name;
          console.log('添加搜索参数：name=', params.name);
        }
        if (this.searchParams.did) {
          params.did = this.searchParams.did;
          console.log('添加搜索参数：did=', params.did);
        }
        
        console.log('最终搜索请求参数:', JSON.stringify(params));

        const res = await userApi.getUserByCondition(params);

        console.log('搜索用户响应状态:', res.code);
        console.log('搜索用户响应数据:', JSON.stringify(res.data));

        if (res.code === '200') {
          this.userList = res.data || [];
          console.log('获取到的用户列表:', this.userList);
        }
      } catch (error) {
        console.error('搜索用户失败:', error);
        uni.showToast({ title: '搜索用户失败', icon: 'none' });
      }
    },
    
    // 验证搜索表单
    validateSearchForm() {
      const errors = {};
      
      // 不需要必填验证，支持根据用户id、用户名或科室来筛选用户
      // 至少需要填写一个字段
      const hasAtLeastOneField = this.searchParams.id || this.searchParams.name || this.searchParams.did;
      if (!hasAtLeastOneField) {
        // 可以选择不显示错误，直接允许搜索所有用户
      }
      
      // 更新错误信息
      this.searchErrors = errors;
      
      // 总是允许搜索，空条件返回所有用户
      return true;
    },
    
    // 重置搜索
    resetSearch() {
      this.searchParams = {
        id: '',
        name: '',
        did: ''
      };
      // 清除搜索错误
      this.searchErrors = {};
      this.getAllUsers();
    },
    
    // 科室选择变化事件
    onDepartmentChange(e) {
      console.log('科室选择变化事件详情:', JSON.stringify(e));
      // 小程序picker组件的change事件返回的是选中的索引，需要根据索引获取对应的值
      if (e.detail && e.detail.value !== undefined) {
        const selectedIndex = e.detail.value;
        console.log('选中的科室索引:', selectedIndex);
        console.log('科室列表:', this.departmentList);
        
        // 确保departmentList是数组且有值
        if (Array.isArray(this.departmentList) && this.departmentList.length > 0) {
          const selectedDepartment = this.departmentList[selectedIndex];
          if (selectedDepartment) {
              this.searchParams.did = selectedDepartment.originalId || selectedDepartment.id;
              console.log('选择的科室ID:', this.searchParams.did);
              console.log('选择的科室名称:', selectedDepartment.name);
              console.log('更新后的searchParams:', this.searchParams);
          } else {
            console.error('未找到选中索引对应的科室');
          }
        } else {
          console.error('科室列表为空或不是数组');
        }
      }
    },
    
    // 获取选中科室的名称
    getSelectedDepartmentName() {
      if (!this.searchParams.did) {
        // 没有选择科室时，返回空字符串，不显示提示
        return '科室';
      }
      const department = this.departmentList.find(dept => dept.id === this.searchParams.did);
      return department ? department.name : '科室';
    },
    
    // 编辑用户，跳转到修改页面
    editUser(user) {
      uni.navigateTo({
        url: `/pages/admin/user-edit?userId=${user.id}`
      });
    },
    
    // 停用用户（使用updateUser接口）
    disableUser(user) {
      uni.showModal({
        title: '确认停用',
        content: `确定要停用用户 ${user.name} 吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              // 准备所有必要参数
              const params = {
                id: user.id,
                name: user.name || '',
                password: user.password || '',
                tid: user.tid || this.userInfo.tid,
                did: user.did || '',
                rid: user.rid || '',
                phone: user.phone || '',
                email: user.email || '',
                status: '停用' // 设置为停用状态
              };
              
              console.log('准备停用的用户参数:', params);

              const result = await userApi.updateUser(params);

              console.log('停用用户响应:', result);

              if (result.code === '200') {
                uni.showToast({ title: '停用成功', icon: 'success' });
                // 重新获取用户列表，刷新页面
                this.getAllUsers();
              } else {
                // 显示后端返回的错误信息
                uni.showToast({
                  title: result.msg || '停用失败',
                  icon: 'none'
                });
              }
            } catch (error) {
              console.error('停用用户失败:', error);
              uni.showToast({ title: '网络错误，停用失败', icon: 'none' });
            }
          }
        }
      });
    },
    
    // 删除用户
    deleteUser(user) {
      uni.showModal({
        title: '确认删除',
        content: `确定要删除用户 ${user.name} 吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await userApi.deleteUser({
                uid: user.id,
                tid: this.userInfo.tid
              });
              console.log('删除用户响应:', result);
              if (result.code === '200') {
                uni.showToast({ title: '删除成功', icon: 'success' });
                // 重新获取用户列表，刷新页面
                this.getAllUsers();
              } else {
                // 显示后端返回的错误信息
                uni.showToast({
                  title: result.msg || '删除失败',
                  icon: 'none'
                });
              }
            } catch (error) {
              console.error('删除用户失败:', error);
              uni.showToast({ title: '网络错误，删除失败', icon: 'none' });
            }
          }
        }
      });
    },
    
    // 处理新增按钮点击事件
    handleAddButtonClick() {
      if (this.currentTab === 0) {
        // 用户信息界面，显示新增用户弹窗
        this.showAddUserDialog();
      } else if (this.currentTab === 1) {
        // 科室信息界面，显示新增科室弹窗
        this.showAddDepartmentDialog();
      }
    },
    
    // 显示新增用户弹窗
    async showAddUserDialog() {
      // 设置学院ID为当前管理员的学院ID
      this.userForm.tid = this.userInfo.tid;
      // 获取角色列表
      await this.getAllRoles();
      this.addUserDialogVisible = true;
    },
    
    // 获取所有角色
    async getAllRoles() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {};
        const token = uni.getStorageSync('token') || userInfo.token;
        
        console.log('开始获取角色列表');
        const res = await roleApi.getAllRoles();

        console.log('获取角色列表响应:', res);
        if (res.code === '200') {
          this.roles = res.data || [];
          console.log('角色列表更新成功:', this.roles);
        } else {
          console.error('获取角色列表失败:', res);
          uni.showToast({ title: '获取角色列表失败', icon: 'none' });
        }
      } catch (error) {
        console.error('获取角色列表失败:', error);
        uni.showToast({ title: '获取角色列表失败', icon: 'none' });
      }
    },
    
    // 新增用户时选择角色
    onAddUserRoleChange(e) {
      const selectedIndex = e.detail.value;
      if (this.roles[selectedIndex]) {
        this.userForm.rid = this.roles[selectedIndex].id; // 向后端传递角色ID
      }
    },
    
    // 获取新增用户时选中的角色名称
    getSelectedRoleName() {
      if (!this.userForm.rid) {
        return '请选择角色';
      }
      const role = this.roles.find(role => role.id === this.userForm.rid);
      return role ? role.name : '请选择角色';
    },
    
    // 数据维护相关方法
    
    // 获取字典类型列表
    async getDictFields() {
      console.log('开始获取字典类型列表');
      console.log('当前用户信息:', this.userInfo);
      try {
        // 检查是否有管理员信息和tid
        if (!this.userInfo || !this.userInfo.tid) {
          console.error('获取字典类型失败：缺少管理员tid');
          uni.showToast({ title: '获取字典类型失败：缺少管理员信息', icon: 'none' });
          return;
        }

        console.log('发送请求到获取字典类型');
        const res = await dictApi.getFields({ tid: this.userInfo.tid });

        console.log('获取字典类型响应完整信息:', res);
        console.log('response.code:', res.code);
        if (res.code === '200') {
          this.dictFields = res.data || [];
          console.log('字典类型列表更新成功:', this.dictFields);
          console.log('字典类型列表长度:', this.dictFields.length);
          if (this.dictFields.length > 0) {
            console.log('第一个字典类型:', this.dictFields[0]);
          }
        } else {
          console.error('获取字典类型失败：后端返回错误', res);
          uni.showToast({ title: `获取字典类型失败：${res.msg || '未知错误'}`, icon: 'none' });
        }
      } catch (error) {
        console.error('获取字典类型失败:', error);
        uni.showToast({ title: '获取字典类型失败', icon: 'none' });
      }
    },
    
    // 获取字典数据
    async getDictData(field) {
      console.log('开始获取字典数据，field:', field);
      try {
        this.loadingDict = true;
        console.log('发送请求到:', `${SERVER_CONFIG.URLS.devBaseURL}/sysdict/getAllSysdicByTid`);
        console.log('请求参数:', { tid: this.userInfo.tid, field: field });
        const res = await dictApi.getAllSysdicByTid({
          tid: this.userInfo.tid,
          field: field
        });

        console.log('获取字典数据响应完整信息:', res);
        console.log('response.code:', res.code);

        if (res.code === '200') {
          // 处理树形数据，只保留顶级节点
          const allData = res.data || [];
          console.log('获取到的所有字典数据:', allData);
          console.log('字典数据类型:', typeof allData);
          console.log('字典数据是否为数组:', Array.isArray(allData));

          // 构建完整的树形结构
          const treeData = this.buildTree(allData);
          this.dictTreeData = treeData;
          console.log('构建的树形数据:', this.dictTreeData);
          console.log('树形数据长度:', this.dictTreeData.length);
        } else {
          console.error('获取字典数据失败：后端返回错误', res);
          uni.showToast({ title: `获取字典数据失败：${res.msg || '未知错误'}`, icon: 'none' });
          this.dictTreeData = [];
        }
      } catch (error) {
        console.error('获取字典数据失败:', error);
        uni.showToast({ title: '获取字典数据失败', icon: 'none' });
        this.dictTreeData = [];
      } finally {
        this.loadingDict = false;
      }
    },
    
    // 构建树形结构
    buildTree(data) {
      console.log('开始构建树形结构，原始数据:', data);
      const tree = [];
      const map = {};
      
      // 首先创建所有节点的映射
      data.forEach(item => {
        map[item.id] = {
          ...item,
          children: []
        };
      });
      
      // 然后构建树形结构
      data.forEach(item => {
        const currentNode = map[item.id];
        if (item.fid === item.id || !map[item.fid]) {
          // 根节点
          tree.push(currentNode);
        } else {
          // 子节点
          map[item.fid].children.push(currentNode);
        }
      });
      
      console.log('构建完成的树形结构:', tree);
      return tree;
    },
    
    // 选择字典类型
    onDictTypeChange(e) {
      const selectedIndex = e.detail.value;
      console.log('选择的字典类型索引:', selectedIndex);
      console.log('当前字典类型列表:', this.dictFields);
      if (this.dictFields[selectedIndex]) {
        this.selectedDictField = this.dictFields[selectedIndex];
        console.log('选择的字典类型:', this.selectedDictField);
        // 获取对应字典数据
        this.getDictData(this.selectedDictField.field);
      }
    },
    
    // 编辑字典项
    editDictItem(node) {
      console.log('编辑字典项:', node);
      // 这里可以实现编辑字典项的逻辑
      uni.showToast({ title: '编辑功能待实现', icon: 'none' });
    },
    
    // 添加字典项
    addDictItem(parentNode) {
      console.log('添加字典项，父节点:', parentNode);
      // 这里可以实现添加字典项的逻辑
      uni.showToast({ title: '添加功能待实现', icon: 'none' });
    },
    
    // 删除字典项
    deleteDictItem(node) {
      console.log('删除字典项:', node);
      // 这里可以实现删除字典项的逻辑
      uni.showToast({ title: '删除功能待实现', icon: 'none' });
    },
    
    // 关闭新增用户弹窗
    closeAddUserDialog() {
      this.addUserDialogVisible = false;
      // 重置表单
      this.userForm = {
        id: '',
        name: '',
        password: '',
        tid: this.userInfo.tid,
        did: '',
        rid: '',
        phone: '',
        email: '',
        status: '正常'
      };
      this.userFormErrors = {};
    },
    
    // 新增用户时选择科室
    onAddUserDepartmentChange(e) {
      const selectedIndex = e.detail.value;
      if (this.departmentList[selectedIndex]) {
        this.userForm.did = this.departmentList[selectedIndex].originalId || this.departmentList[selectedIndex].id;
      }
    },
    
    // 获取新增用户时选中的科室名称
    getSelectedAddUserDepartmentName() {
      if (!this.userForm.did) {
        return '请选择科室';
      }
      const department = this.departmentList.find(dept => dept.originalId === this.userForm.did || dept.id === this.userForm.did);
      return department ? department.name : '请选择科室';
    },
    
    // 获取学院名称
    getCollegeName() {
      // 根据学院ID返回对应的学院名称，这里假设code为1代表计算机科学与技术学院
      if (this.userForm.tid === '1' || this.userForm.tid === 1) {
        return '计算机科学与技术学院';
      }
      return this.userForm.tid || '';
    },
    
    // 新增用户时选择状态
    onAddUserStatusChange(e) {
      const selectedIndex = e.detail.value;
      this.userForm.status = this.statusOptions[selectedIndex];
    },
    
    // 验证新增用户表单
    validateAddUserForm() {
      const errors = {};
      
      if (!this.userForm.id) {
        errors.id = '用户ID不能为空';
      }
      
      if (!this.userForm.name) {
        errors.name = '用户名不能为空';
      }
      
      if (!this.userForm.password) {
        errors.password = '密码不能为空';
      }
      
      if (!this.userForm.tid) {
        errors.tid = '学院ID不能为空';
      }
      
      if (!this.userForm.did) {
        errors.did = '科室不能为空';
      }
      
      if (!this.userForm.rid) {
        errors.rid = '角色ID不能为空';
      }
      
      if (!this.userForm.status) {
        errors.status = '状态不能为空';
      }
      
      this.userFormErrors = errors;
      return Object.keys(errors).length === 0;
    },
    
    // 提交新增用户表单
    async submitAddUserForm() {
      if (!this.validateAddUserForm()) {
        return;
      }
      
      try {
        console.log('准备提交的用户数据:', this.userForm);

        const res = await userApi.addUser(this.userForm);

        console.log('新增用户响应:', res);
        if (res.code === '200') {
          uni.showToast({
            title: '新增成功',
            icon: 'success'
          });
          this.closeAddUserDialog();
          // 刷新用户列表
          this.getAllUsers();
        } else {
          uni.showToast({
            title: res.msg || '新增失败',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('新增用户失败:', error);
        uni.showToast({ title: '网络错误，新增失败', icon: 'none' });
      }
    },
    
    // 显示新增科室弹窗
    showAddDepartmentDialog() {
      this.isEditDepartment = false;
      this.departmentForm = {
        id: '',
        name: '',
        tid: this.userInfo.tid
      };
      this.departmentFormErrors = {};
      this.departmentDialogVisible = true;
    },
    
    // 显示修改科室弹窗
    showEditDepartmentDialog(department) {
      this.isEditDepartment = true;
      this.departmentForm = {
        ...department,
        tid: this.userInfo.tid
      };
      this.departmentFormErrors = {};
      this.departmentDialogVisible = true;
    },
    
    // 关闭科室弹窗
    closeDepartmentDialog() {
      this.departmentDialogVisible = false;
    },
    
    // 验证科室表单
    validateDepartmentForm() {
      const errors = {};
      
      // 新增科室时不需要验证ID，修改时需要
      if (this.isEditDepartment && !this.departmentForm.id) {
        errors.id = '科室ID不能为空';
      }
      
      if (!this.departmentForm.name) {
        errors.name = '科室名称不能为空';
      }
      
      this.departmentFormErrors = errors;
      return Object.keys(errors).length === 0;
    },
    
    // 提交科室表单
    async submitDepartmentForm() {
      if (!this.validateDepartmentForm()) {
        return;
      }
      
      try {
        console.log('提交科室请求数据:', this.departmentForm);

        let res;
        if (this.isEditDepartment) {
          res = await departmentApi.updateDepartment(this.departmentForm);
        } else {
          res = await departmentApi.addDepartment(this.departmentForm);
        }

        console.log('提交科室响应:', res);
        if (res.code === '200') {
          uni.showToast({
            title: this.isEditDepartment ? '修改成功' : '新增成功',
            icon: 'success'
          });
          this.departmentDialogVisible = false;
          this.getAllDepartments();
        }
      } catch (error) {
        console.error('提交科室失败:', error);
        uni.showToast({ 
          title: this.isEditDepartment ? '修改失败' : '新增失败', 
          icon: 'none' 
        });
      }
    },
    
    // 删除科室
    deleteDepartment(department) {
      uni.showModal({
        title: '确认删除',
        content: `确定要删除科室 ${department.name} 吗？`,
        success: async (res) => {
          if (res.confirm) {
            try {
              const params = {
                id: department.originalId || department.id,
                tid: this.userInfo.tid
              };
              console.log('删除科室请求参数:', params);

              const result = await departmentApi.deleteDepartment(params);
              console.log('删除科室响应:', result);
              if (result.code === '200') {
                uni.showToast({ title: '删除成功', icon: 'success' });
                this.getAllDepartments();
              }
            } catch (error) {
              console.error('删除科室失败:', error);
              uni.showToast({ title: '删除科室失败', icon: 'none' });
            }
          }
        }
      });
    },
    
    // 退出登录
    handleLogout() {
      uni.showModal({
        title: '确认退出',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            try {
              // 清除本地存储的用户信息和token
              uni.removeStorageSync('userInfo');
              uni.removeStorageSync('token');
              
              // 跳转到登录页面
              uni.redirectTo({
                url: '/pages/login/login'
              });
            } catch (error) {
              console.error('退出登录失败:', error);
              uni.showToast({ title: '退出登录失败', icon: 'none' });
            }
          }
        }
      });
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
$bg-base: #f5f5f5;
$border-base: #e4e7ed;
$radius-base: 12rpx;
$radius-lg: 24rpx;
$spacing-base: 20rpx;
$spacing-lg: 32rpx;
$shadow-sm: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
$shadow-base: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);

.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 顶部导航样式 */
.top-nav {
  background-color: $primary-color;
  color: white;
  padding: 20rpx;
  text-align: center;
  font-size: 36rpx;
  font-weight: bold;
  box-shadow: $shadow-sm;
}

.nav-title {
  color: white;
}

/* 内容区域样式 */
.content {
  flex: 1;
  padding: 20rpx;
}

.page-content {
  background-color: white;
  border-radius: $radius-base;
  padding: 40rpx;
  box-shadow: $shadow-sm;
  min-height: 500rpx;
  text-align: center;
  font-size: 32rpx;
  color: $text-regular;
}

/* 底部导航栏样式 */
.bottom-tabbar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background-color: white;
  padding: 10rpx 0;
  box-shadow: $shadow-sm;
}

/* 标签项样式 */
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10rpx;
  flex: 1;
  text-align: center;
  transition: all 0.3s ease;
}

/* 激活状态样式 */
.tab-item.active {
  color: #1a73e8;
}

/* 标签图标样式 */
.tab-icon {
  font-size: 40rpx;
  margin-bottom: 5rpx;
}

/* 标签文本样式 */
.tab-text {
  font-size: 24rpx;
}

/* 搜索容器样式 */
.search-container {
  background-color: white;
  padding: 20rpx;
  border-radius: 10rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  margin-bottom: 20rpx;
}

.search-box {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding: 5rpx 0;
  width: 100%;
}

.search-inputs {
  display: flex;
  gap: 12rpx;
  align-items: center;
  overflow-x: auto;
  width: 100%;
}

.search-buttons {
  display: flex;
  gap: 8rpx;
  align-items: center;
  white-space: nowrap;
  justify-content: flex-end;
  width: 100%;
}

/* 输入框包装器，用于容纳输入框、必填标识和错误提示 */
.input-wrapper {
  position: relative;
  flex: 1;
  min-width: 150rpx;
  max-width: 220rpx;
  display: flex;
  flex-direction: column;
  gap: 5rpx;
}

.search-input {
  width: 100%;
  height: 64rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  background-color: #fafafa;
  transition: all 0.3s ease;
}

.search-input:focus {
  background-color: white;
  border-color: #1a73e8;
  box-shadow: 0 0 0 3rpx rgba(26, 115, 232, 0.1);
}

.search-select {
  width: 100%;
  height: 64rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  background-color: #fafafa;
  transition: all 0.3s ease;
}

.search-select:focus {
  background-color: white;
  border-color: #1a73e8;
  box-shadow: 0 0 0 3rpx rgba(26, 115, 232, 0.1);
}

.picker-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: #333;
  font-size: 28rpx;
  padding: 0 20rpx;
  box-sizing: border-box;
}

/* 为搜索选择器添加完整样式，使其与输入框一致 */
.search-select {
  width: 100%;
  height: 64rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  background-color: #fafafa;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  cursor: pointer;
}

.search-select:focus {
  background-color: white;
  border-color: #1a73e8;
  box-shadow: 0 0 0 3rpx rgba(26, 115, 232, 0.1);
}

/* 必填标识 */
.required {
  position: absolute;
  top: 50%;
  right: 10rpx;
  transform: translateY(-50%);
  color: #ff4d4f;
  font-size: 30rpx;
  font-weight: bold;
}

/* 错误提示文本 */
.error-text {
  font-size: 22rpx;
  color: #ff4d4f;
  margin-top: -5rpx;
  margin-left: 5rpx;
}

/* 状态包装器 */
.status-wrapper {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

/* 停用状态样式 */
.status-disabled {
  color: #909399;
  text-decoration: line-through;
}

/* 停用标志样式 */
.disabled-badge {
  background-color: #ff4d4f;
  color: white;
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 50rpx;
  font-weight: 600;
  white-space: nowrap;
}

.search-btn, .reset-btn {
  height: 64rpx;
  font-size: 28rpx;
  border-radius: 8rpx;
  line-height: 64rpx;
  min-width: 120rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  margin: 0;
}

.search-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
  box-shadow: 0 2rpx 8rpx rgba(26, 115, 232, 0.3);
}

.search-btn:hover {
  background-color: #1557b0;
  box-shadow: 0 4rpx 12rpx rgba(26, 115, 232, 0.4);
}

.reset-btn {
  background-color: #ffffff;
  color: #666;
  border: 1rpx solid #e0e0e0;
  margin-left: 8rpx;
}

.reset-btn:hover {
  background-color: #f5f5f5;
  border-color: #d0d0d0;
}

/* 用户列表样式 */
.user-list {
  background-color: white;
  border-radius: 10rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  transition: background-color 0.3s ease;
}

.user-item:last-child {
  border-bottom: none;
}

.user-item:hover {
  background-color: #fafafa;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 5rpx;
  flex: 1;
  min-width: 0;
  align-items: flex-start;
}

.user-id {
  font-size: 26rpx;
  color: #666;
}

.user-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.user-department, .user-role {
  font-size: 26rpx;
  color: #666;
}

.user-status {
  font-size: 26rpx;
  color: #666;
}

.user-actions {
  display: flex;
  gap: 10rpx;
  align-self: center;
  min-width: 180rpx;
  justify-content: flex-end;
}



.edit-btn, .delete-btn, .disable-btn {
  width: 80rpx;
  height: 50rpx;
  font-size: 26rpx;
  line-height: 50rpx;
  border-radius: 6rpx;
  margin: 0 4rpx;
}

.edit-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
}

.delete-btn {
  background-color: #f44336;
  color: white;
  border: none;
}

.disable-btn {
  background-color: #ff9800;
  color: white;
  border: none;
}

/* 停用文本样式 */
.disabled-text {
  font-size: 26rpx;
  color: #909399;
  text-decoration: line-through;
  padding: 8rpx 16rpx;
  border-radius: 6rpx;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50rpx;
  width: 80rpx;
  box-sizing: border-box;
}

/* 科室相关样式 */
.department-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20rpx;
  width: 100%;
  box-sizing: border-box;
  position: relative;
}

.page-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
}

.header-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
  padding-left: 200rpx; /* 为标题留出空间 */
  box-sizing: border-box;
}
/* 新增科室按钮 */
.add-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
  padding: 8rpx 16rpx;
  border-radius: 6rpx;
  font-size: 26rpx;
  margin: 0;
}

/* 底部悬浮新增用户按钮 */
.page-content .add-btn {
  position: fixed;
  bottom: 120rpx; /* 确保在底部导航栏之上 */
  right: 30rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background-color: #007aff;
  color: white;
  font-size: 80rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4rpx 20rpx rgba(0, 122, 255, 0.5);
  z-index: 999;
  text-align: center;
  line-height: 100rpx;
  padding: 0;
}

/* 新增用户弹窗中的表单样式 */
.form-item {
  margin-bottom: 20rpx;
}

.form-label {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 10rpx;
  display: block;
}

.form-input {
  width: 100%;
  height: 70rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  background-color: white;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3rpx rgba(26, 115, 232, 0.1);
}

/* 选择器显示文本居中 */
.picker-display {
  text-align: center;
  width: 100%;
}

.error-text {
  font-size: 22rpx;
  color: #ff4d4f;
  margin-top: 5rpx;
  display: block;
}

/* 新增用户按钮 */
.add-user-btn {
  position: fixed !important;
  bottom: 180rpx !important; /* 上移60rpx，避免覆盖底部导航栏 */
  right: 30rpx !important;
  width: 100rpx !important;
  height: 100rpx !important;
  border-radius: 50% !important;
  background-color: #007aff !important;
  color: white !important;
  font-size: 80rpx !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  box-shadow: 0 4rpx 20rpx rgba(0, 122, 255, 0.5) !important;
  z-index: 999 !important;
  text-align: center !important;
  line-height: 100rpx !important;
  padding: 0 !important;
  min-width: 100rpx !important;
  max-width: 100rpx !important;
  flex-shrink: 0 !important;
  cursor: pointer !important;
  border: none !important;
}

/* 退出登录按钮样式 */
.logout-btn {
  position: fixed;
  bottom: 150rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 200rpx;
  height: 60rpx;
  background-color: #ff4d4f;
  color: white;
  border-radius: 30rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2rpx 10rpx rgba(255, 77, 79, 0.3);
  z-index: 998;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background-color: #ff7875;
  box-shadow: 0 4rpx 15rpx rgba(255, 77, 79, 0.4);
}

.logout-text {
  font-size: 28rpx;
  font-weight: 600;
}

/* 确保科室选择下拉框与输入框大小一致 */
picker.form-input {
  width: 100% !important;
  height: 70rpx !important;
  border: 1rpx solid #e0e0e0 !important;
  border-radius: 8rpx !important;
  box-sizing: border-box !important;
  background-color: white !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 确保选择器内部显示容器与输入框大小一致 */
picker.form-input .picker-display {
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  text-align: center !important;
  padding: 0 20rpx !important;
  box-sizing: border-box !important;
}

.required {
  color: #ff4d4f;
}

.popup-container {
  width: 80%;
  max-height: 80vh;
  overflow-y: auto;
  background-color: white;
  border-radius: 16rpx;
  padding: 40rpx;
}

.popup-title {
  font-size: 36rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30rpx;
  color: #333;
}

.popup-actions {
  display: flex;
  justify-content: center;
  gap: 30rpx;
  margin-top: 40rpx;
}

.cancel-btn, .submit-btn {
  width: 150rpx;
  height: 60rpx;
  font-size: 28rpx;
  border-radius: 8rpx;
  line-height: 60rpx;
  margin: 0;
}

.cancel-btn {
  background-color: #ffffff;
  color: #666;
  border: 1rpx solid #e0e0e0;
}

.submit-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
}

/* 弹窗遮罩 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.department-list {
  background-color: white;
  border-radius: 10rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.department-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  transition: background-color 0.3s ease;
}

.department-item:last-child {
  border-bottom: none;
}

.department-item:hover {
  background-color: #fafafa;
}

.department-info {
  display: flex;
  flex-direction: column;
  gap: 5rpx;
  flex: 1;
  min-width: 0;
}

.department-id {
  font-size: 26rpx;
  color: #666;
}

.department-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.department-actions {
  display: flex;
  gap: 10rpx;
  align-self: center;
}

.empty-list {
  text-align: center;
  padding: 40rpx;
  color: #909399;
  font-size: 28rpx;
}

/* 弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.popup-container {
  width: 80%;
  background-color: white;
  border-radius: 16rpx;
  padding: 40rpx;
}

.popup-title {
  font-size: 36rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30rpx;
  color: #333;
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 25rpx;
}

.form-label {
  width: 120rpx;
  font-size: 28rpx;
  color: #666;
  text-align: right;
  margin-right: 20rpx;
}

.form-input {
  flex: 1;
  height: 60rpx;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  padding: 0 15rpx;
  font-size: 28rpx;
}

.popup-actions {
  display: flex;
  justify-content: center;
  gap: 30rpx;
  margin-top: 40rpx;
}

.cancel-btn, .submit-btn {
  width: 150rpx;
  height: 60rpx;
  font-size: 28rpx;
  border-radius: 8rpx;
  line-height: 60rpx;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
  border: 1rpx solid #ddd;
}

.submit-btn {
  background-color: #1a73e8;
  color: white;
  border: none;
}
/* 数据维护相关样式 */
.dict-maintenance {
  padding: 20rpx;
}

.dict-type-selector {
  display: flex;
  align-items: center;
  margin-bottom: 30rpx;
}

.dict-type-selector .label {
  font-size: 28rpx;
  color: #666;
  margin-right: 20rpx;
  width: 150rpx;
  text-align: right;
}

.dict-type-picker {
  flex: 1;
  height: 64rpx;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  background-color: #fafafa;
  display: flex;
  align-items: center;
}

.dict-type-picker .picker-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #333;
}

.dict-tree-container {
  background-color: white;
  border-radius: 10rpx;
  padding: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.dict-tree-header {
  margin-bottom: 20rpx;
  padding-bottom: 10rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.tree-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.loading-state {
  text-align: center;
  padding: 40rpx;
  color: #909399;
}

.empty-state {
  text-align: center;
  padding: 40rpx;
  color: #909399;
}

.dict-tree {
  margin: 10rpx 0;
}

.dict-tree-node {
  margin: 5rpx 0;
}

.dict-node {
  display: flex;
  flex-direction: column;
}

.node-content {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
}

.node-toggle {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 24rpx;
  color: #666;
}

.node-toggle.placeholder {
  cursor: default;
}

.toggle-icon {
  transition: transform 0.3s ease;
}

.node-main {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fafafa;
  padding: 10rpx 20rpx;
  border-radius: 8rpx;
  border: 1rpx solid #e0e0e0;
}

.node-value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

.node-actions {
  display: flex;
  gap: 15rpx;
}

.action-btn {
  font-size: 24rpx;
  padding: 5rpx 15rpx;
  border-radius: 5rpx;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn.edit {
  color: #1a73e8;
  background-color: rgba(26, 115, 232, 0.1);
}

.action-btn.edit:hover {
  background-color: rgba(26, 115, 232, 0.2);
}

.action-btn.add {
  color: #34a853;
  background-color: rgba(52, 168, 83, 0.1);
}

.action-btn.add:hover {
  background-color: rgba(52, 168, 83, 0.2);
}

.action-btn.delete {
  color: #ea4335;
  background-color: rgba(234, 67, 53, 0.1);
}

.action-btn.delete:hover {
  background-color: rgba(234, 67, 53, 0.2);
}

.node-children {
  margin-left: 40rpx;
  padding-left: 20rpx;
  border-left: 1rpx solid #e0e0e0;
}
</style>