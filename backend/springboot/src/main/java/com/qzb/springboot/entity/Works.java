package com.qzb.springboot.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class Works implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务ID*/
    private String id;
    /** 任务名称*/
    private String name;
    /** 任务详情*/
    private String detail;
    /** 任务性质*/
//    private String attribute;
    /** 开始时间*/
    private String startTime;
    /** 结束时间*/
    private String endTime;
    /** 申报人ID*/
    private String uid;
    /** 任务类型ID*/
    private String type;
    /** 负责人*/
    private String head;

    /** 重要程度*/
//    private String imp;
    /** 任务状态*/
    private String status;
    /** 任务成果*/
    private String achievement;

    private String username;

    private String tid;

    private String tenant;

    private String did;
    private String department;

    private String createTime;
    private String isdelete;

//    private String attributeValue;

    private String typeValue;

//    private String impValue;

    private String statusValue;
    private String isopen;
    private String updateTime;
    private String isshow;
}
