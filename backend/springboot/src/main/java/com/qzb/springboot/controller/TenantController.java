package com.qzb.springboot.controller;

import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.Tenant;
import com.qzb.springboot.service.TenantService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/tenant")
public class TenantController {
    @Resource
    private TenantService tenantService;

    @GetMapping("/getAlltenant")
    public Result getAlltenant(){

        return Result.success();
    }

    @PostMapping("/addTenant")
    public Result addTenant(@RequestBody Tenant tenant){

        return Result.success();
    }

    @PutMapping("/updateTenant")
    public Result updateTenant(@RequestBody Tenant tenant){

        return Result.success();
    }

}
