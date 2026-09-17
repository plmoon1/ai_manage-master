package com.qzb.springboot.entity;

import lombok.Data;

import java.util.List;

@Data
public class RolePermission {
    private String rid;
    private List<String> pids;
    private String tid;
}
