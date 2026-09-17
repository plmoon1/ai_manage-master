package com.qzb.springboot.common.enums;

public enum OpenEnum {

    OPEN("1","公开"),
    SECRETE("0","私密"),
    ;
    public String status;
    public String code;

    OpenEnum(String code, String status) {
        this.status = status;
        this.code = code;
    }
}
