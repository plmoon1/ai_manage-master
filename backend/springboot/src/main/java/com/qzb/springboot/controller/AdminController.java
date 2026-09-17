package com.qzb.springboot.controller;

import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.Admin;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.service.AdminService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Resource
    private AdminService adminService;
    /**
     * 获得单个管理员信息
     * */
    @GetMapping("/getAdminById/{id}")
    public Result getAdminByTid(@PathVariable String id){
        Admin admin = this.adminService.getAdminById(id);
        return Result.success(admin);
    }







}
