package com.qzb.springboot.controller;

import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.Department;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {
    @Resource
    private DepartmentService departmentService;

    @GetMapping("/getDepartmentByTid")
    public Result getDepartmentByTid(@RequestParam String tid){
        List<Department> departmentList = this.departmentService.getDepartmentByTid(tid);
        return Result.success(departmentList);
    }

    @PostMapping("/addDepartment")
    public Result addDepartment(@RequestBody Department department){
        this.departmentService.addDepartment(department);
        return Result.success();
    }


    @PutMapping("/updateDepartment")
    public Result updateDepartment(@RequestBody Department department){
        this.departmentService.updateDepartment(department);
        return Result.success();
    }


    @DeleteMapping("/deleteDepartment")
    public Result deleteDepartment(@RequestBody Department department){
        this.departmentService.deleteDepartment(department);
        return Result.success();
    }
}
