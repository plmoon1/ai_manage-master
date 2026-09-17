package com.qzb.springboot.common;

public interface WorkConstants {
    String FILE_INSERT_ERROR_CODE = "501";
    String USER_ERROR = "导入时读取当前用户信息失败，请重新登录！";

    String DEPARTMENT_ERROR = "所填写的部门不存在！";

    String ATTRIBUTE_ERROR = "工作性质列所填数据不存在！";

    String TYPE_ERROR = "工作类型列所填数据不存在！";

    String IMP_ERROR = "重要程度列所填数据不存在！";
    String STATUS_ERROR = "工作状态列所填数据不存在！";

    String HEAD_ERROR = "负责人未填写！";
    String NAME_ERROR = "工作名称未填写！";
    String DETAIL_ERROR = "工作详情未填写！";

    String ISOPEN_ERROR = "可见范围填写错误！";
    String ISSHOW_ERROR = "大屏展示填写错误！";

    String DID_MISSING = "工作所属部门未填写！";

    String ATTRIBUTE_MISSING = "工作性质未填写！";

    String IMP_MISSING = "重要程度未填写！";

    String STATUS_MISSING = "工作状态未填写！";

    String ISOPEN_MISSING = "可见范围未填写！";
    String ISSSHOW_MISSING = "大屏展示未填写！";

    String START_TIME_MISSING = "工作开始时间未填写";
    String END_TIME_MISSING = "工作结束时间未填写";

    String TYPE_MISSING = "工作类型未填写！";
}
