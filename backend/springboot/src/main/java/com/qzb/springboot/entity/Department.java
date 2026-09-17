package com.qzb.springboot.entity;

import java.io.Serializable;

public class Department implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 科室ID */
    private String id;
    /** 科室名称*/
    private String name;
    /** 租户ID*/
    private String tid;

    private String tenant;

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }
}
