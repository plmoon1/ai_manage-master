package com.qzb.springboot.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会议安排实体类
 * 对应数据库表：meeting
 */
@Data
public class Meeting {
    /**
     * 主键ID
     */
    private String id;
    private String meetingType;
    private String startWeek;
    private String endWeek;
    private String startDate;
    private String endDate;
    private String startStartTime;
//    private String startEndTime;
//    private String endStartTime;
    private String endEndTime;
    //存储uuid
    private String status;
    //存储对应的value
    private String statusValue;
    /**
     * 会议名称
     */
    private String meetingName;

    /**
     * 主持人
     */
    private String host;

    /**
     * 参加对象
     */
    private String participants;

    /**
     * 会议地点
     */
    private String location;


    private String tid;

    private String uid;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    private String isdelete;
    /**
     * 展示大屏
     */
    private String isshow;
    /**
     * 辅助变量
     * */
    private String startTime;
    private String endTime;

}
