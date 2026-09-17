package com.qzb.springboot.controller;

import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.RolePermission;
import com.qzb.springboot.service.PermissionService;
import org.springframework.security.core.parameters.P;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/permission")
public class PermissionController {
    @Resource
    private PermissionService permissionService;

    @GetMapping("/getAllPermissionByTidPage")
    public Result getAllPermissionByTid(@RequestParam String tid,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String parentId){
        PageInfo<Permission> page = this.permissionService.getAllPermissionByTid(tid,pageNum,pageSize,parentId);
        return Result.success(page);
    }

    @GetMapping("/getAllPermissionByRidAndTid")
    public Result getAllPermissionByRid(@RequestParam String rid,@RequestParam String tid){
        List<Permission> list = this.permissionService.getAllPermissionByRid(rid,tid);
        List<String> list2 = list.stream().map(p -> p.getId()).collect(Collectors.toList());
        return Result.success(list2);
    }

    @GetMapping("/getPermissionTreeByTid")
    public Result getPermissionTreeByTid(@RequestParam String tid){
        List<Permission> list = this.permissionService.getPermissionTree(tid);
        return Result.success(list);
    }

    @PostMapping("/addPermissionToRole")
    @Transactional(rollbackFor = Exception.class)
    public Result addPermissionToRole(@RequestBody RolePermission rolePermission){
        this.permissionService.addPermisionToRole(rolePermission);
        return Result.success();
    }

    @PostMapping("/addPermission")
    public Result addPermission(@RequestBody Permission permission){
        this.permissionService.addPermission(permission);
        return Result.success();
    }
    @GetMapping("/getPermissionMenu")
    public Result getPermissionMenu(@RequestParam String tid,@RequestParam String type){
        List<Permission> list = this.permissionService.getPermissionMenu(tid,type);
        return Result.success(list);
    }

    @PutMapping("/updatePermission")
    public Result updatePermission(@RequestBody Permission permission){
        this.permissionService.updatePermission(permission);
        return Result.success();
    }
    @DeleteMapping("/deletePermission")
    public Result deletePermission(@RequestParam String id){
        this.permissionService.deletePermission(id);
        return Result.success();
    }
}
