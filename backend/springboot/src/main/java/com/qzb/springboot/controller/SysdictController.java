package com.qzb.springboot.controller;

import com.qzb.springboot.common.Result;

import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.entity.SysDict;


import com.qzb.springboot.service.SysdictService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/sysdict")
public class SysdictController {

    @Resource
    SysdictService sysdictService;
//    @GetMapping("/getAllSysdicByTid")
//    public Result getAllSysdicByTid(@RequestParam String tid){
//        List<SysDict> sysDicts = this.sysdictService.getAllSysdicByTid(tid);
//        return Result.success(sysDicts);
//    }

//    @PostMapping("/addSysdict")
//    public Result addSysdict(@RequestBody SysDict sysDict){
//        this.sysdictService.addSysdict(sysDict);
//        return Result.success();
//    }
//
//    @PutMapping("/updateSysdict")
//    public Result updateSysdict(@RequestBody SysDict sysDict){
//        this.sysdictService.updateSysdict(sysDict);
//        return Result.success();
//    }
//
//    @DeleteMapping("deleteSysdict")
//    public Result deleteSysdict(@RequestParam String tid,@RequestParam String sid){
//        this.sysdictService.deleteSysdict(tid,sid);
//        return Result.success();
//    }


    /** ********************************************
     * 2026-01-19
     * 新增字典接口，以上接口暂时不用
     * ********************************************/

    /**
     * 根据租户ID获取所有字典数据（树形结构）
     */
    @GetMapping("/getAllSysdicByTid")
    public Result getAllSysdicByTid(@RequestParam String tid) {
        List<SysDict> sysDicts = sysdictService.getAllSysdicByTid(tid);
        return Result.success(sysDicts);
    }

    /**
     * 根据ID获取字典详情
     */
    @GetMapping("/getById")
    public Result getById(@RequestParam String id) {
        SysDict sysDict = sysdictService.getSysDictById(id);
        if (sysDict == null) {
            return Result.error(ResultCodeEnum.NOT_FOUND);
        }
        return Result.success(sysDict);
    }

    /**
     * 获取字典类型列表
     */
    @GetMapping("/getFields")
    public Result getFields(@RequestParam String tid) {
        List<SysDict> fields = sysdictService.getDistinctFields(tid);
        return Result.success(fields);
    }

    /**
     * 新增字典
     */
    @PostMapping("/insert")
    public Result insert(@RequestBody SysDict sysDict) {
        try {
            sysdictService.insertSysDict(sysDict);
            return Result.success();
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.DUPLICATE_ERROR.code, e.getMessage());
        }
    }

    /**
     * 更新字典
     */
    @PostMapping("/update")
    public Result update(@RequestBody SysDict sysDict) {
        try {
            sysdictService.updateSysDict(sysDict);
            return Result.success();
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.DUPLICATE_ERROR.code, e.getMessage());
        }
    }

    /**
     * 删除字典（包括所有下级数据）
     */
    @PostMapping("/delete")
    public Result delete(@RequestParam String id) {
        try {
            sysdictService.deleteSysDict(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.SYSTEM_ERROR.code, e.getMessage());
        }
    }

    /**
     * 获取需要删除的所有ID（用于删除前确认）
     */
    @GetMapping("/getDeleteIds")
    public Result getDeleteIds(@RequestParam String id) {
        List<String> ids = sysdictService.getDeleteIds(id);
        return Result.success(ids);
    }

    /**
     * 查询字典树形结构
     * @param id 字典节点ID
     * @return 树形结构数据
     */
    @GetMapping("/getTree/{id}")
    public Result getDictTree(@PathVariable String id) {
        try {
            SysDict dictTree = sysdictService.getDictTreeById(id);
            return Result.success(dictTree);
        } catch (Exception e) {
            return Result.error(ResultCodeEnum.NOT_FOUND);
        }
    }


    @GetMapping("/getMeetingStatus")
    public Result getMeetingStatus(@RequestParam String tid){
        List<SysDict> list = this.sysdictService.getMeetingStatus(tid);
        return Result.success(list);
    }

}
