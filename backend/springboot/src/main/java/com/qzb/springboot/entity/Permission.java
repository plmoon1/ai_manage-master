package com.qzb.springboot.entity;

import lombok.Data;

import java.util.List;

@Data
public class Permission {
    private String id;
    private String tid;
    private String name;
    private String permission;
    private String type;
    private String parentId; // 新增
    private List<Permission> children; // 新增：子权限列表，用于树形结构
}
