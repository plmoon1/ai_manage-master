package com.qzb.springboot.controller;

import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.ScreenConfig;
import com.qzb.springboot.service.ScreenConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 大屏参数配置Controller
 */
@RestController
@RequestMapping("/screen/config")
public class ScreenConfigController {

    @Resource
    private ScreenConfigService screenConfigService;

    /**
     * 新增配置
     */
    @PostMapping("/add")
    public Result addConfig(@RequestBody ScreenConfig screenConfig) {
        boolean success = screenConfigService.addConfig(screenConfig);
        return success ? Result.success(true) : Result.error();
    }

    /**
     * 修改配置
     */
    @PostMapping("/update")
    public Result updateConfig(@RequestBody ScreenConfig screenConfig) {
        boolean success = screenConfigService.updateConfig(screenConfig);
        return success ? Result.success(true) : Result.error();
    }

    /**
     * 删除配置
     */
    @GetMapping("/delete/{id}")
    public Result deleteConfig(@PathVariable String id) {
        boolean success = screenConfigService.deleteConfig(id);
        return success ? Result.success(true) : Result.error();
    }

    /**
     * 按ID查询配置
     */
    @GetMapping("/get/{id}")
    public Result getConfigById(@PathVariable String id) {
        ScreenConfig config = screenConfigService.getConfigById(id);
        return Result.success(config);
    }

    /**
     * 按租户ID查询所有配置（管理端用）
     */
    @GetMapping("/list/{tid}")
    public Result listConfigByTid(@PathVariable String tid) {
        List<ScreenConfig> list = screenConfigService.listConfigByTid(tid);
        return Result.success(list);
    }

    /**
     * 按租户ID查询启用的配置（大屏端读取）
     */
    @GetMapping("/enabled/{tid}")
    public Result getEnabledConfigByTid(@PathVariable String tid) {
        ScreenConfig config = screenConfigService.getEnabledConfigByTid(tid);
        return Result.success(config);
    }

    @GetMapping("/getConfigByTidAndConfigName")
    public Result getConfigByTidAndConfigName(@RequestParam String tid,@RequestParam String configName) {
        ScreenConfig config = screenConfigService.getConfigByTidAndConfigName(tid,configName);
        return Result.success(config);
    }
}