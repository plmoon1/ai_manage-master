package com.qzb.springboot.exception;

import com.qzb.springboot.common.enums.ResultCodeEnum;

public class TaskException extends RuntimeException {
    private String code;
    private String msg;

    public TaskException(ResultCodeEnum resultCodeEnum) {
        this.code = resultCodeEnum.code;
        this.msg = resultCodeEnum.msg;
    }

    public TaskException(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
