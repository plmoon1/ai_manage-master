package com.qzb.springboot.common.enums;

public interface MeetingConstants {
    String FILE_INSERT_ERROR_CODE = "501";
    String USER_ERROR = "导入时读取当前用户信息失败，请重新登录！";
    String START_DATA_ERROR = "日期填写错误！";
    String START_TIME_ERROR = "时间填写错误！";
    String START_DATA_MISSING = "日期未填写！";
    String END_DATA_ERROR = "结束日期填写错误！";
    String END_TIME_ERROR = "结束时间填写错误！";
    String START_TIME_MISSING = "起始日期未填写！";

    String STATUS_MISSING = "会议状态未填写！";

    String STATUS_ERROR = "会议状态数据在数据字典中不存在！";

    String ISSHOW_MISSING = "大屏展示未填写！";
    String ISSHOW_ERROR = "大屏展示填写错误！";
}
