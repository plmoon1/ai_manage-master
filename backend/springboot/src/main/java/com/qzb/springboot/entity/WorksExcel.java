package com.qzb.springboot.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
@Data
public class WorksExcel implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 工作名称 - 对应name */
    @ExcelProperty("工作名称")
    private String name;

    /** 工作详情 - 对应detail */
    @ExcelProperty("工作详情")
    private String detail;

    /** 工作所属部门 - 对应did */
    @ExcelProperty("工作所属部门")
    private String did;

    /** 工作性质 - 对应attribute */
//    @ExcelProperty("工作性质")
//    private String attribute;

//    /** 计划周期 - 对应cycle */
//    @ExcelProperty("计划周期")
//    private String cycle;

    /** 开始时间 - 对应startTime */
    @ExcelProperty("开始时间")
    private String startTime;

    /** 结束时间 - 对应endTime */
    @ExcelProperty("结束时间")
    private String endTime;

    /** 工作类型 - 对应type */
    @ExcelProperty("工作类型")
    private String type;

    /** 可见范围 - 对应isopen */
    @ExcelProperty("可见范围")
    private String isopen;

    /** 可见范围 - 对应open */
    @ExcelProperty("大屏展示")
    private String isshow;

    /** 负责人 - 对应head */
    @ExcelProperty("负责人")
    private String head;

    /** 重要程度 - 对应imp */
//    @ExcelProperty("重要程度")
//    private String imp;

    /** 工作状态 - 对应status */
    @ExcelProperty("工作状态")
    private String status;

    /** 工作成果 - 对应achievement */
    @ExcelProperty("工作成果")
    private String achievement;
}
