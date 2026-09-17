package com.qzb.springboot.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class SysDict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务类型ID*/
    private String id;
    private String fid;
    private String tid;
    private String field;
    private String name;
    private String value;
    /** 核心新增：是否为当前查询的目标节点（true=突出显示） */
    private boolean isCurrent;
    private List<SysDict> children;



}
