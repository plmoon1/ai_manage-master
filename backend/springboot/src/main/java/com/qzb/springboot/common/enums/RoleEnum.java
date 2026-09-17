package com.qzb.springboot.common.enums;

public enum RoleEnum {
    ADMIN("ADMIN","系统管理员"),// 系统管理员
    SAAS("SAAS","超级管理员"),// 超级管理员
    USER("USER","普通用户"),// 普通用户


    SUPER_ADMIN("5","超级管理员"),
    SYSTEM_ADMIN("4","系统管理员"),
    COLLEGE_LEADER("3","分管领导"),
    DEPARTMENT_LEADER("2","科室领导"),
    DEPARTMENT_ORDINARY("1","科室人员"),
    ;
    //自定义属性
    public String code; //角色code
    public String role; //角色名

    //自定义构造函数
    RoleEnum(String code,String role){
        this.code = code;
        this.role = role;
    }

//    public String getRoleName() {
//        return roleName;
//    }
//
//    public void setRoleName(String roleName) {
//        this.roleName = roleName;
//    }
//
//    public String getRole() {
//        return role;
//    }
//
//    public void setRoleCode(String roleCode) {
//        this.role = role;
//    }
//
//    //通过code来返回name的值
//    public static String getRoleName(String role) {
//        for(RoleEnum enums:RoleEnum.values()) {
//            if(enums.getRole().equals(role)) {
//                return enums.getRoleName();
//            }
//        }
//        return null;
//    }



}
