package com.qzb.springboot.entity;

import java.io.Serializable;

public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 租户ID*/
    private String id;
    /** 租户名称*/
    private String name;

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
}
