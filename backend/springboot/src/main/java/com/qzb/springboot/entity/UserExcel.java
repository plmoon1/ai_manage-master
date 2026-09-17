package com.qzb.springboot.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
@Data
public class UserExcel implements Serializable {
    private static final long serialVersionUID = 1L;

    @ExcelProperty("教职工号")
    private String id;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("所属科室")
    private String did;
    @ExcelProperty("角色")
    private String rid;
    @ExcelProperty("电话")
    private String phone;
    @ExcelProperty("邮箱")
    private String email;
}
