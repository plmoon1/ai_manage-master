package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Role;

import java.util.List;

public interface RoleMapper {

    List<Role> getAllRole();

    String getRoleIdByName(String name);


    List<Role> getAllRoleByTid(String tid);

    void addRole(Role role);

    void updateRole(Role role);

    void deleteRole(String id);
}
