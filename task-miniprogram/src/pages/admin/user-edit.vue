<template>
  <view class="container">
    <!-- 顶部导航栏 -->
    <view class="top-nav">
      <view class="nav-back" @tap="onBack">
        <text class="back-icon">←</text>
        <text class="back-text">返回</text>
      </view>
      <text class="nav-title">修改用户信息</text>
      <view class="nav-right"></view>
    </view>
    
    <!-- 内容区域 -->
    <view class="content">
      <!-- 用户信息表单 -->
      <view class="form-container">
        <view class="form-item">
          <text class="form-label">用户ID:</text>
          <input type="text" v-model="userForm.id" disabled class="form-input" />
        </view>
        
        <view class="form-item">
          <text class="form-label">用户名:</text>
          <input type="text" v-model="userForm.name" class="form-input" placeholder="请输入用户名" />
        </view>
        
        <view class="form-item">
          <text class="form-label">密码:</text>
          <input type="password" v-model="userForm.password" class="form-input" placeholder="不修改请留空" />
        </view>
        
        <view class="form-item">
          <text class="form-label">学院:</text>
          <view class="form-value">{{ collegeName }}</view>
        </view>
        
        <view class="form-item">
          <text class="form-label">科室:</text>
          <view class="select-container">
            <view class="select-value" @tap="showDepartmentPicker = true">
              <text>{{ getDepartmentName(userForm.did) || '请选择科室' }}</text>
              <text class="select-arrow">▼</text>
            </view>
            <!-- 科室选择器 -->
            <view v-if="showDepartmentPicker" class="picker-container">
              <view class="picker-header">
                <text class="picker-title">选择科室</text>
                <text class="picker-close" @tap="showDepartmentPicker = false">×</text>
              </view>
              <view class="picker-content">
                <scroll-view scroll-y class="department-list">
                  <view 
                    class="department-item" 
                    v-for="dept in departments" 
                    :key="dept.id"
                    :class="{ 'selected': userForm.did === dept.id }"
                    @tap="selectDepartment(dept)"
                  >
                    {{ dept.name }}
                  </view>
                </scroll-view>
              </view>
            </view>
          </view>
        </view>
        
        <view class="form-item">
          <text class="form-label">角色ID:</text>
          <view class="select-container">
            <view class="select-value" @tap="showRolePicker = true">
              <text>{{ getRoleName(userForm.rid) || '请选择角色' }}</text>
              <text class="select-arrow">▼</text>
            </view>
            <!-- 角色选择器 -->
            <view v-if="showRolePicker" class="picker-container">
              <view class="picker-header">
                <text class="picker-title">选择角色</text>
                <text class="picker-close" @tap="showRolePicker = false">×</text>
              </view>
              <view class="picker-content">
                <scroll-view scroll-y class="role-list">
                  <view 
                    class="role-item" 
                    v-for="role in roles" 
                    :key="role.id"
                    :class="{ 'selected': userForm.rid === role.id }"
                    @tap="selectRole(role)"
                  >
                    {{ role.name }}
                  </view>
                </scroll-view>
              </view>
            </view>
          </view>
        </view>
        
        <view class="form-item">
          <text class="form-label">手机号:</text>
          <input type="text" v-model="userForm.phone" class="form-input" placeholder="请输入手机号" />
        </view>
        
        <view class="form-item">
          <text class="form-label">邮箱:</text>
          <input type="text" v-model="userForm.email" class="form-input" placeholder="请输入邮箱" />
        </view>
        
        <view class="form-item">
          <text class="form-label">状态:</text>
          <input type="text" v-model="userForm.status" class="form-input" placeholder="请输入状态" />
        </view>
        
        <!-- 保存按钮 -->
        <view class="button-group">
          <button @tap="onSave" class="save-btn">保存修改</button>
          <button @tap="onBack" class="cancel-btn">取消</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { taskApi, logApi, userApi, fileApi, departmentApi, tenantApi, authApi, roleApi, dictApi } from '@/api'

export default {
  data() {
    return {
      userForm: {
        id: '',
        name: '',
        password: '',
        tid: '',
        did: '',
        rid: '',
        phone: '',
        email: '',
        status: ''
      },
      collegeName: '', // 学院名字
      departments: [], // 科室列表
      showDepartmentPicker: false, // 是否显示科室选择器
      roles: [], // 角色列表
      showRolePicker: false // 是否显示角色选择器
    }
  },
  
  async onLoad(options) {
    // 获取传递过来的用户ID
    const { userId } = options;
    if (userId) {
      await this.getUserDetail(userId);
      // 获取科室列表
      await this.getAllDepartments();
      // 获取角色列表
      await this.getAllRoles();
      // 设置学院名字
      this.setCollegeName();
    }
  },
  
  methods: {
    // 返回上一页
    onBack() {
      uni.navigateBack();
    },
    
    // 获取用户详情
    async getUserDetail(userId) {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {};
        const res = await userApi.getUserByCondition({
          id: userId,
          tid: userInfo.tid
        });

        if (res.code === '200') {
          const data = res.data || [];
          if (data.length > 0) {
            const user = data[0];
            this.userForm = {
              id: user.id || '',
              name: user.name || '',
              password: '',
              tid: user.tid || '',
              did: user.did || '',
              rid: user.rid || '',
              phone: user.phone || '',
              email: user.email || '',
              status: user.status || ''
            };
            // 设置学院名字
            this.collegeName = user.tenant || '未知学院';
          }
        }
      } catch (error) {
        console.error('获取用户详情失败:', error);
        uni.showToast({ title: '获取用户详情失败', icon: 'none' });
      }
    },
    
    // 获取所有科室
    async getAllDepartments() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {};
        const tid = this.userForm.tid || userInfo.tid;

        const res = await departmentApi.getDepartmentByTid({
          tid: tid,
          token: userInfo.token
        });

        if (res.code === '200') {
          this.departments = res.data || [];
        }
      } catch (error) {
        console.error('获取科室列表失败:', error);
        uni.showToast({ title: '获取科室列表失败', icon: 'none' });
      }
    },
    
    // 设置学院名字
    setCollegeName() {
      // 如果用户详情中没有返回tenant字段，可以通过tid查询学院信息
      // 这里暂时使用已有的collegeName
      if (!this.collegeName) {
        this.collegeName = '未知学院';
      }
    },
    
    // 根据科室ID获取科室名称
    getDepartmentName(did) {
      const department = this.departments.find(dept => dept.id === did);
      return department ? department.name : '';
    },
    
    // 选择科室
    selectDepartment(dept) {
      this.userForm.did = dept.id;
      this.showDepartmentPicker = false;
    },
    
    // 获取所有角色
    async getAllRoles() {
      try {
        const userInfo = uni.getStorageSync('userInfo') || {};
        const token = uni.getStorageSync('token') || userInfo.token;

        const res = await roleApi.getAllRoles();

        if (res.code === '200') {
          this.roles = res.data || [];
        }
      } catch (error) {
        console.error('获取角色列表失败:', error);
        uni.showToast({ title: '获取角色列表失败', icon: 'none' });
      }
    },
    
    // 根据角色ID获取角色名称
    getRoleName(rid) {
      const role = this.roles.find(role => role.id === rid);
      return role ? role.name : '';
    },
    
    // 选择角色
    selectRole(role) {
      this.userForm.rid = role.id; // 向后端传递角色ID
      this.showRolePicker = false;
    },
    
    // 保存用户信息
    async onSave() {
      try {
        // 验证表单
        if (!this.userForm.name) {
          uni.showToast({ title: '请输入用户名', icon: 'none' });
          return;
        }

        const res = await userApi.updateUser(this.userForm);

        if (res.code === '200') {
          uni.showToast({ title: '修改成功', icon: 'success' });
          // 延迟返回，让用户看到成功提示
          setTimeout(() => {
            uni.navigateBack();
          }, 1500);
        } else {
          uni.showToast({ title: '修改失败', icon: 'none' });
        }
      } catch (error) {
        console.error('修改用户失败:', error);
        uni.showToast({ title: '修改失败', icon: 'none' });
      }
    }
  }
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f5f5f5;
  width: 100%;
  box-sizing: border-box;
}

/* 顶部导航样式 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #1a73e8;
  color: white;
  padding: 20rpx 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.nav-back {
  display: flex;
  align-items: center;
  gap: 10rpx;
  cursor: pointer;
}

.back-icon {
  font-size: 36rpx;
  font-weight: bold;
}

.back-text {
  font-size: 28rpx;
}

.nav-title {
  font-size: 36rpx;
  font-weight: bold;
}

.nav-right {
  width: 60rpx;
}

/* 内容区域样式 */
.content {
  flex: 1;
  padding: 30rpx;
  box-sizing: border-box;
}

/* 表单容器样式 */
.form-container {
  background-color: white;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.08);
}

/* 表单项样式 */
.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 35rpx;
  gap: 20rpx;
}

/* 表单标签样式 */
.form-label {
  width: 140rpx;
  font-size: 30rpx;
  color: #333;
  font-weight: 600;
  text-align: right;
  flex-shrink: 0;
}

/* 表单值显示样式（用于学院名称） */
.form-value {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  background-color: #f5f7fa;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  color: #606266;
  box-sizing: border-box;
}

/* 表单输入框样式 */
.form-input {
  flex: 1;
  height: 80rpx;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  background-color: #fafafa;
  box-sizing: border-box;
}

.form-input:disabled {
  background-color: #f5f7fa;
  color: #909399;
}

/* 选择容器样式 */
.select-container {
  flex: 1;
  position: relative;
}

/* 选择值显示 */
.select-value {
  height: 80rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20rpx;
  font-size: 28rpx;
  background-color: #fafafa;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  color: #606266;
  box-sizing: border-box;
  cursor: pointer;
  transition: all 0.3s ease;
}

.select-value:active {
  background-color: #e6e8eb;
}

/* 选择箭头 */
.select-arrow {
  font-size: 20rpx;
  color: #909399;
}

/* 选择器容器样式 */
.picker-container {
  position: absolute;
  top: 85rpx;
  left: 0;
  right: 0;
  background-color: white;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 400rpx;
  overflow: hidden;
}

/* 选择器头部 */
.picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  background-color: #fafafa;
}

/* 选择器标题 */
.picker-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

/* 选择器关闭按钮 */
.picker-close {
  font-size: 36rpx;
  color: #909399;
  cursor: pointer;
  padding: 5rpx;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.picker-close:active {
  background-color: #e6e8eb;
  color: #606266;
}

/* 选择器内容 */
.picker-content {
  max-height: 320rpx;
  overflow: hidden;
}

/* 科室列表 */
.department-list {
  max-height: 320rpx;
}

/* 科室列表项 */
.department-item {
  padding: 25rpx 30rpx;
  font-size: 28rpx;
  color: #606266;
  transition: all 0.3s ease;
  cursor: pointer;
}

.department-item:hover {
  background-color: #f0f9ff;
  color: #1890ff;
}

/* 选中的科室项 */
.department-item.selected {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.department-item.selected::before {
  content: '✓';
  margin-right: 10rpx;
  font-size: 24rpx;
}

/* 角色列表 */
.role-list {
  max-height: 320rpx;
}

/* 角色列表项 */
.role-item {
  padding: 25rpx 30rpx;
  font-size: 28rpx;
  color: #606266;
  transition: all 0.3s ease;
  cursor: pointer;
}

.role-item:hover {
  background-color: #f0f9ff;
  color: #1890ff;
}

/* 选中的角色项 */
.role-item.selected {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.role-item.selected::before {
  content: '✓';
  margin-right: 10rpx;
  font-size: 24rpx;
}

/* 按钮组样式 */
.button-group {
  display: flex;
  gap: 20rpx;
  margin-top: 50rpx;
}

/* 保存按钮样式 */
.save-btn {
  flex: 1;
  height: 80rpx;
  background-color: #1a73e8;
  color: white;
  border: none;
  border-radius: 8rpx;
  font-size: 30rpx;
  font-weight: 600;
  transition: all 0.3s ease;
}

.save-btn:active {
  background-color: #1557b0;
  transform: scale(0.98);
}

/* 取消按钮样式 */
.cancel-btn {
  flex: 1;
  height: 80rpx;
  background-color: #f5f7fa;
  color: #606266;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  font-size: 30rpx;
  font-weight: 600;
  transition: all 0.3s ease;
}

.cancel-btn:active {
  background-color: #e6e8eb;
  transform: scale(0.98);
}
</style>