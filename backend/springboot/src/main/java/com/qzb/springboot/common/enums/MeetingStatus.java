package com.qzb.springboot.common.enums;

public enum MeetingStatus {

    MEETING_TIME_ERROR("501","时间格式错误，当前支持的格式包括yyyy-MM-dd、yyyy/MM/dd、yyyy.MM.dd、yyyyMMdd等，请查看模板数据格式要求！"),
    SECRETE("407","私密"),
    ;
    public String code;
    public String msg;


    MeetingStatus(String code, String msg) {
        this.msg = msg;
        this.code = code;
    }
}
