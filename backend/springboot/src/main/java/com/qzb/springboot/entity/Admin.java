package com.qzb.springboot.entity;

import java.io.Serializable;


public class Admin extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 管理员ID*/
    private String id;
    /** 管理员昵称*/
    private String name;
    /** 租户ID*/
    private String tid;
    /** 管理员密码*/
    private String password;
    /** 邮箱*/
    private String email;
    private String role;
    private String token;
    private String tenant;

    private String rid;

    @Override
    public String getRid() {
        return rid;
    }

    @Override
    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
