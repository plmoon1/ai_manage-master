package com.qzb.springboot.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.qzb.springboot.utils.ExcelTimeToStringConverter;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MeetingExcel implements Serializable {
    private static final long serialVersionUID = 1L;
    @ExcelProperty("日期")
    private String startDate;
    @ExcelProperty(value = "时间", converter = ExcelTimeToStringConverter.class)
    private String startStartTime;
    @ExcelProperty("结束日期")
    private String endDate;
    @ExcelProperty(value = "结束时间", converter = ExcelTimeToStringConverter.class)
    private String endEndTime;
    @ExcelProperty("会议名称")
    private String meetingName;
    @ExcelProperty("主持人")
    private String host;
    @ExcelProperty("参加对象")
    private String participants;
    @ExcelProperty("会议地点")
    private String location;
    @ExcelProperty("会议状态")
    private String status;
    @ExcelProperty("大屏展示")
    private String isshow;
//    @ExcelProperty("会议类型")
//    private String meetingType;
//    @ExcelProperty("起始星期")
//    private String startWeek;
//    @ExcelProperty("起始日结束时间")
//    private String startEndTime;
//    @ExcelProperty("结束星期")
//    private String endWeek;
//    @ExcelProperty("结束日开始时间")
//    private String endStartTime;
}
