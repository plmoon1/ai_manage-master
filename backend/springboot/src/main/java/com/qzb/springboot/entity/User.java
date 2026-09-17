package com.qzb.springboot.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class User extends Account implements Serializable, UserDetails {
    private static final long serialVersionUID = 1L;
    /** 用户ID*/
    private String id;
    /** 用户名称*/
    private String name;
    /** 用户密码*/
    private String password;
    /** 租户ID*/
    private String tid;
    /** 科室ID*/
    private String did;
    /** 角色ID*/
    private String rid;
    /** 电话*/
    private String phone;
    /** 邮箱*/
    private String email;
    /** 角色名称*/
    private String role;
    private String token;
    private String department;
    private String tenant;
    private String status;
    private String createTime;
    private String updateTime;

    private List<Permission> permissions;
    // ========== Spring Security 认证方法 ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将权限标识封装为Security权限对象
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (permissions != null && !permissions.isEmpty()) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getPermission())));
        }
        return authorities;
    }
    @Override
    public String getUsername() { return name; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return "正常".equals(status); }
}
