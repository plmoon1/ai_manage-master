package com.qzb.springboot.common.enums;

public enum ResultCodeEnum {
    SUCCESS("200", "成功"),

    PARAM_ERROR("400", "参数异常"),
    TOKEN_INVALID_ERROR("401", "无效的token"),
    TOKEN_CHECK_ERROR("401", "token验证失败，请重新登录"),

    NOT_FOUND("404", "数据不存在"),
    Authority_ERROR("403", "权限不足"),
    DUPLICATE_ERROR("409", "数据已存在"),

    PARAM_LOST_ERROR("4001", "参数缺失"),

    SYSTEM_ERROR("500", "系统异常"),
    FILE_INSERT_ERROR("501", "数据导入失败，请检查数据是否重复或错误"),
    USER_EXIST_ERROR("5001", "用户已存在"),
    USER_NOT_LOGIN("5002", "用户未登录"),
    USER_ACCOUNT_ERROR("5003", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("5004", "用户不存在"),
    PARAM_PASSWORD_ERROR("5005", "原密码输入错误"),
    COLLECT_ALREADY_ERROR("5006","商品已收藏"),
    ;


    public String code;
    public String msg;

    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
