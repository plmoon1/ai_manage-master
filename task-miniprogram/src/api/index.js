/**
 * API 接口统一管理
 * 所有业务接口都在这里定义
 */

import request from '@/utils/request'

/**
 * 用户认证相关接口
 */
export const authApi = {
  // 用户登录
  login(data) {
    return request({
      url: '/login',
      method: 'POST',
      data
    })
  },

  // 获取字典数据
  getDictData(tid) {
    return request({
      url: '/sysdict/getAllSysdicByTid',
      method: 'GET',
      params: { tid }
    })
  }
}

/**
 * 任务相关接口
 */
export const taskApi = {
  // 创建任务
  createPlan(data) {
    return request({
      url: '/plan/addPlan',
      method: 'POST',
      data
    })
  },

  // 上传任务文件（建立任务与文件的关联）
  uploadPlanFile(planFileData) {
    return request({
      url: '/plan/addPlanFile',
      method: 'POST',
      data: planFileData,  // JSON格式的PlanFiles对象
      headers: {
        'Content-Type': 'application/json'  // 后端使用@RequestBody，期望JSON
      }
    })
  },

  // 获取科室任务列表
  getPlanByDepartment(params) {
    return request({
      url: '/plan/getAllPlanByDepartment',
      method: 'GET',
      params
    })
  },

  // 获取租户所有任务（不分页）
  getAllPlanByTenant(params) {
    return request({
      url: '/plan/getAllPlanByTenant',
      method: 'GET',
      params
    })
  },

  // 分页获取租户所有任务（使用条件查询接口）
  getAllPlanByTenantPage(params) {
    return request({
      url: '/plan/searchPlanByConditionPage',
      method: 'GET',
      params
    })
  },

  // 获取已完成任务数
  getCompletedPlanCount(params) {
    return request({
      url: '/plan/getPlanNumCompleteByDepartment',
      method: 'GET',
      params
    })
  },

  // 获取进行中任务数
  getDoingPlanCount(params) {
    return request({
      url: '/plan/getPlanNumDoingByDepartment',
      method: 'GET',
      params
    })
  },

  // 获取已取消任务数
  getCancelledPlanCount(params) {
    return request({
      url: '/plan/getPlanNumCancelByDepartment',
      method: 'GET',
      params
    })
  },

  // 获取准备中任务数
  getPreparePlanCount(params) {
    return request({
      url: '/plan/getPlanNumPrepareByDepartment',
      method: 'GET',
      params
    })
  },

  // 根据条件搜索任务
  searchPlanByCondition(params) {
    return request({
      url: '/plan/searchPlanByCondition',
      method: 'GET',
      params
    })
  },

  // 更新任务
  updatePlan(data) {
    return request({
      url: '/plan/updatePlan',
      method: 'PUT',
      data
    })
  },

  // 获取任务详情
  getPlanDetail(params) {
    return request({
      url: '/plan/getPlanDetail',
      method: 'GET',
      params
    })
  },

  // 获取任务文件列表
  getPlanFiles(params) {
    return request({
      url: '/plan/getPlanFiles',
      method: 'GET',
      params
    })
  },

  // 删除任务文件
  deletePlanFiles(params) {
    return request({
      url: '/plan/deletePlanFiles',
      method: 'DELETE',
      params
    })
  },

  // 获取个人任务列表
  getPlanByPerson(params) {
    return request({
      url: '/plan/getAllPlanByPerson',
      method: 'GET',
      params
    })
  },

  // 分页获取科室任务列表
  getPlanByDepartmentPage(params) {
    return request({
      url: '/plan/getAllPlanByDepartmentPage',
      method: 'GET',
      params
    })
  },

  // 条件搜索分页（用于获取部门任务）
  searchPlanByConditionPage(params) {
    return request({
      url: '/plan/searchPlanByConditionPage',
      method: 'GET',
      params
    })
  }
}

/**
 * 重要工作相关接口
 */
export const workApi = {
  // 获取租户所有工作（不分页）
  getAllWorksByTenant(params) {
    return request({
      url: '/works/getAllWorksByTenant',
      method: 'GET',
      params
    })
  },

  // 分页获取租户所有工作
  getAllWorksByTenantPage(params) {
    return request({
      url: '/works/getAllWorksByTenantPage',
      method: 'GET',
      params
    })
  },

  // 条件搜索分页
  searchWorksByConditionPage(params) {
    return request({
      url: '/works/searchWorksByConditionPage',
      method: 'GET',
      params
    })
  },

  // 获取科室工作列表
  getWorksByDepartment(params) {
    return request({
      url: '/works/getAllWorksByDepartment',
      method: 'GET',
      params
    })
  },

  // 添加工作
  addWorks(data) {
    return request({
      url: '/works/addWorks',
      method: 'POST',
      data
    })
  },

  // 更新工作
  updateWorks(data) {
    return request({
      url: '/works/updateWorks',
      method: 'PUT',
      data
    })
  },

  // 获取工作详情
  getWorksDetail(params) {
    return request({
      url: '/works/getWorksDetail',
      method: 'GET',
      params
    })
  },

  // 条件搜索工作
  searchWorksByCondition(params) {
    return request({
      url: '/works/searchWorksByCondition',
      method: 'GET',
      params
    })
  },

  // 获取工作文件列表
  getWorksFiles(params) {
    return request({
      url: '/works/getWorksFiles',
      method: 'GET',
      params
    })
  },

  // 添加工作文件（建立工作与文件的关联）
  addWorksFile(worksFileData) {
    return request({
      url: '/works/addWorksFile',
      method: 'POST',
      data: worksFileData,
      headers: {
        'Content-Type': 'application/json'
      }
    })
  },

  // 删除工作文件
  deleteWorksFiles(params) {
    return request({
      url: '/works/deleteWorksFiles',
      method: 'DELETE',
      params
    })
  }
}

/**
 * 文件上传接口
 */
export const fileApi = {
  // 上传文件
  upload(formData) {
    return request({
      url: '/files/upload',
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 删除文件
  delete(file) {
    return request({
      url: '/files/delete',
      method: 'DELETE',
      params: { file }
    })
  }
}

/**
 * 租户相关接口
 */
export const tenantApi = {
  // 获取所有租户
  getAllTenants() {
    return request({
      url: '/tenant/getAlltenant',
      method: 'GET'
    })
  }
}

/**
 * 部门相关接口
 */
export const departmentApi = {
  // 根据租户ID获取部门
  getDepartmentByTid(params) {
    return request({
      url: '/department/getDepartmentByTid',
      method: 'GET',
      params
    })
  },

  // 删除部门
  deleteDepartment(data) {
    return request({
      url: '/department/deleteDepartment',
      method: 'DELETE',
      data
    })
  }
}

/**
 * 用户相关接口
 */
export const userApi = {
  // 更新用户信息（管理员）
  updateUser(data) {
    return request({
      url: '/user/updateUser',
      method: 'PUT',
      data
    })
  },

  // 更新用户信息（自己）
  updateUserBySelf(data) {
    return request({
      url: '/user/updateUserBySelf',
      method: 'PUT',
      data
    })
  },

  // 根据条件查询用户
  getUserByCondition(params) {
    return request({
      url: '/user/getUserByCondition',
      method: 'GET',
      params
    })
  },

  // 添加用户
  addUser(data) {
    return request({
      url: '/user/addUser',
      method: 'POST',
      data
    })
  }
}

/**
 * 日志相关接口
 */
export const logApi = {
  // 获取任务日志列表
  getPlanLogs(params) {
    return request({
      url: '/log/getPlanLogs',
      method: 'GET',
      params
    })
  },

  // 添加任务日志
  addPlanLog(data) {
    return request({
      url: '/log/addPlanLog',
      method: 'POST',
      data
    })
  }
}

/**
 * 角色相关接口
 */
export const roleApi = {
  // 获取所有角色
  getAllRoles() {
    return request({
      url: '/role/getAllRole',
      method: 'GET'
    })
  }
}

/**
 * 字典相关接口
 */
export const dictApi = {
  // 获取字典字段
  getFields(params) {
    return request({
      url: '/sysdict/getFields',
      method: 'GET',
      params
    })
  }
}

/**
 * 学院相关接口
 */
export const collegeApi = {
  // 根据租户ID获取所有学院
  getAllCollegeByTid(params) {
    return request({
      url: '/college/getAllCollegeByTid',
      method: 'GET',
      params
    })
  }
}

/**
 * 会议相关接口
 */
export const meetingApi = {
  // 根据租户ID分页获取会议列表
  getMeetingByTenantPage(params) {
    return request({
      url: '/meeting/getMeetingByTenantPage',
      method: 'GET',
      params
    })
  },

  // 创建会议
  createMeeting(data) {
    return request({
      url: '/meeting/addMeeting',
      method: 'POST',
      data
    })
  },

  // 更新会议
  updateMeeting(data) {
    return request({
      url: '/meeting/updateMeeting',
      method: 'PUT',
      data
    })
  },

  // 删除会议
  deleteMeeting(params) {
    return request({
      url: '/meeting/deleteMeeting',
      method: 'DELETE',
      params
    })
  },

  // 根据ID获取会议详情
  getMeetingById(params) {
    return request({
      url: '/meeting/getMeetingById',
      method: 'GET',
      params
    })
  },

  // 根据条件分页获取会议列表（后端已有接口）
  getMeetingByConditionPage(params) {
    return request({
      url: '/meeting/getMeetingByConditionPage',
      method: 'GET',
      params
    })
  }
}

/**
 * AI 助手相关接口
 */
export const aiApi = {
  // AI 对话（文字输入 / 语音识别后的文字均走此接口）
  // data: { message: '用户问题', history: [{role:'user|assistant', content:'...'}] }
  chat(data) {
    return request({
      url: '/ai/chat',
      method: 'POST',
      data,
      timeout: 60000 // AI 生成较慢，放宽超时
    })
  },

  // 语音识别：上传录音(wav)，返回识别文本
  asr(formData) {
    return request({
      url: '/ai/asr',
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000 // 上传+识别较慢，且后端调GLM本身有30s超时，放宽到60s
    })
  },

  // 我的会话列表（按最近活跃倒序）
  sessions(params) {
    return request({
      url: '/ai/sessions',
      method: 'GET',
      params
    })
  },

  // 加载某个会话的全部消息
  messages(sid) {
    return request({
      url: '/ai/messages',
      method: 'GET',
      params: { sid }
    })
  },

  // 删除会话
  deleteSession(sid) {
    return request({
      url: '/ai/session',
      method: 'DELETE',
      params: { sid }
    })
  },

  // 删除某一轮问答（mid=该轮用户消息id，连带AI回答一起删除）
  deleteMessage(sid, mid) {
    return request({
      url: '/ai/message',
      method: 'DELETE',
      params: { sid, mid }
    })
  }
}
