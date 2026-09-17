package com.qzb.springboot.service;

import com.qzb.springboot.entity.Role;
import com.qzb.springboot.mapper.RoleMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RoleService {
    @Resource
    private RoleMapper roleMapper;
    public List<Role> getAllRole() {
        return this.roleMapper.getAllRole();
    }

    public List<Role> getAllRoleByTid(String tid) {
        return this.roleMapper.getAllRoleByTid(tid);
    }

    public void addRole(Role role) {
        this.roleMapper.addRole(role);
    }

    public void updateRole(Role role) {
        this.roleMapper.updateRole(role);
    }

    public void deleteRole(String id) {
        this.roleMapper.deleteRole(id);
    }
}
