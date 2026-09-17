package com.qzb.springboot.service;

import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.RolePermission;
import com.qzb.springboot.mapper.PermissionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    @Resource
    private PermissionMapper permissionMapper;
    public PageInfo<Permission> getAllPermissionByTid(String tid,Integer pageNum,Integer pageSize,String parentId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Permission> list = this.permissionMapper.getAllPermissionByTid(tid,parentId);
        return PageInfo.of(list);
    }

    public List<Permission> getAllPermissionByRid(String rid,String tid) {
        return this.permissionMapper.getAllPermissionByRid(rid,tid);
    }

    // 获取树形权限结构
    public List<Permission> getPermissionTree(String tid) {
        // 1. 查询该租户的所有权限，按排序号升序
        List<Permission> allPermissions = this.permissionMapper.getAllPermissionByTid(tid,null);
        // 2. 平级转树形
        return buildTree(allPermissions);
    }

    // 递归构建树形结构
    private List<Permission> buildTree(List<Permission> allPermissions) {
        // 找到所有顶级权限（parentId为null/空）
        List<Permission> rootList = allPermissions.stream()
                .filter(p -> p.getParentId().equals(p.getId()))
                .collect(Collectors.toList());

        // 递归给每个顶级权限设置子节点
        rootList.forEach(root -> setChildren(root, allPermissions));
        return rootList;
    }

    // 递归设置子节点
    private void setChildren(Permission parent, List<Permission> allPermissions) {
        List<Permission> children = allPermissions.stream()
                .filter(p -> parent.getId().equals(p.getParentId()) && !parent.getId().equals(p.getId()))
                .collect(Collectors.toList());
        parent.setChildren(children);
        // 递归给子节点设置子节点
        children.forEach(child -> setChildren(child, allPermissions));
    }

    public void addPermisionToRole(RolePermission rolePermission) {
        permissionMapper.deleteByRoleId(rolePermission.getRid());
       if (!rolePermission.getPids().isEmpty()) {
           this.permissionMapper.addPermissionToRole(rolePermission);
       }
    }

    public void addPermission(Permission permission) {
        this.permissionMapper.addPermission(permission);
    }

    public List<Permission> getPermissionMenu(String tid, String type) {
        return this.permissionMapper.getPermissionMenu(tid,type);
    }

    public void updatePermission(Permission permission) {
        this.permissionMapper.updatePermission(permission);
    }

    public void deletePermission(String id) {
        this.permissionMapper.deletePermission(id);
    }
}
