package com.qzb.springboot.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

//import java.io.Serial;
import java.io.Serializable;

/**
 * Excel导入数据传输对象
 * 映射Excel表头与数据库字段
 */
@Data
public class PlanExcel implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 任务名称 - 对应name */
    @ExcelProperty("任务名称")
    private String name;

    /** 任务详情 - 对应detail */
    @ExcelProperty("任务详情")
    private String detail;

    /** 任务所属部门 - 对应did */
    @ExcelProperty("任务所属部门")
    private String did;

    /** 任务性质 - 对应attribute */
    @ExcelProperty("任务性质")
    private String attribute;

    /** 开始时间 - 对应startTime */
    @ExcelProperty("开始时间")
    private String startTime;

    /** 结束时间 - 对应endTime */
    @ExcelProperty("结束时间")
    private String endTime;

    /** 任务类型 - 对应type */
    @ExcelProperty("任务类型")
    private String type;

    /** 可见范围 - 对应isopen */
    @ExcelProperty("可见范围")
    private String isopen;

    /** 可见范围 - 对应isshow */
    @ExcelProperty("大屏展示")
    private String isshow;

    /** 负责人 - 对应head */
    @ExcelProperty("负责人")
    private String head;

    /** 重要程度 - 对应imp */
    @ExcelProperty("重要程度")
    private String imp;

    /** 任务状态 - 对应status */
    @ExcelProperty("任务状态")
    private String status;

    /** 任务成果 - 对应achievement */
    @ExcelProperty("任务成果")
    private String achievement;
}
