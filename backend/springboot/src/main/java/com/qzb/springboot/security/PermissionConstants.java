package com.qzb.springboot.security;

public interface PermissionConstants {
    // 接口权限标识示例（按需扩展）
    String USER_LIST = "user:list";      // 用户列表
    String USER_ADD = "user:add";        // 新增用户
    String ROLE_LIST = "role:list";      // 角色列表
    String ROLE_ASSIGN = "role:assign";  // 角色分配权限
}
