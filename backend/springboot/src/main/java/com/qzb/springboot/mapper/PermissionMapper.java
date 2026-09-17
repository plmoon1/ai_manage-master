package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.RolePermission;


import java.util.List;

public interface PermissionMapper {
    List<Permission> getAllPermissionByTid(String tid,String parentId);

    List<Permission> getAllPermissionByRid(String rid,String tid);

    void addPermissionToRole(RolePermission rolePermission);

    void addPermission(Permission permission);

    void deleteByRoleId(String rid);

    List<Permission> getPermissionMenu(String tid, String type);

    void updatePermission(Permission permission);

    void deletePermission(String id);

    Integer getPermissionNum(String rid);
}
