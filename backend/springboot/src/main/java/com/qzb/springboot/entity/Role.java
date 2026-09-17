package com.qzb.springboot.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 角色ID*/
    private String id;
    /** 角色名称*/
    private String name;
    private String tid;
}
