package com.qzb.springboot.controller;

import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.Role;
import com.qzb.springboot.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {
    @Resource
    private RoleService roleService;
    @GetMapping("/getAllRole")
    public Result getAllRole(){
        List<Role> roles = this.roleService.getAllRole();
        return Result.success(roles);
    }

    @PostMapping("/addRole")
    public Result addRole(@RequestBody Role role){
        this.roleService.addRole(role);
        return Result.success();
    }

    @PutMapping("/updateRole")
    public Result updateRole(@RequestBody Role role){
        this.roleService.updateRole(role);
        return Result.success();
    }
    @DeleteMapping("/deleteRole")
    public Result deleteRole(@RequestParam String id){
        this.roleService.deleteRole(id);
        return Result.success();
    }

    // 角色列表
    @GetMapping("/getAllRoleByTid")
//    @PreAuthorize("hasPermission('role:list')")
    public Result getAllRoleByTid(@RequestParam String tid) {
        List<Role> list = this.roleService.getAllRoleByTid(tid);
        return Result.success(list);
    }

    // 给角色分配权限
    @PostMapping("/assignPermission")
//    @PreAuthorize("hasPermission('role:assign')")
    public Result assignPermission(String rid, List<String> pids) {
//        roleService.assignPermission(rid, pids);
        return Result.success();
    }
}
