package com.qzb.springboot.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.service.AdminService;
import com.qzb.springboot.service.SaasService;
import com.qzb.springboot.service.UserDetailServiceImpl;
import com.qzb.springboot.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 基础前端接口
 */
@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    UserService userService;
    @Resource
    SaasService saasService;
    @Resource
    UserDetailServiceImpl userDetailService;
    @GetMapping("/")
    public Result hello() {
        return Result.success("访问成功");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        if (ObjectUtil.isEmpty(user.getId()) || ObjectUtil.isEmpty(user.getPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        //查询用户信息并设置token
        user = userService.login(user);
//        user = (User)userDetailService.loadUserByUsername(user.getId() + "-split-" + user.getPassword());
        return Result.success(user);
    }

    /**
     * 注册
     */
//    @PostMapping("/register")
//    public Result register(@RequestBody Account account) {
//        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
//                || ObjectUtil.isEmpty(account.getRole())) {
//            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
//        }
//        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
//            adminService.register(account);
//        }
//        if (RoleEnum.BUSINESS.name().equals(account.getRole())) {
//            businessService.register(account);
//        }
//        if (RoleEnum.USER.name().equals(account.getRole())) {
//            userService.register(account);
//        }
//        return Result.success();
//    }

    /**
     * 修改密码
     */
//    @PutMapping("/updatePassword")
//    public Result updatePassword(@RequestBody Account account) {
//        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
//                || ObjectUtil.isEmpty(account.getNewPassword())) {
//            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
//        }
//        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
//            adminService.updatePassword(account);
//        }
//        if (RoleEnum.BUSINESS.name().equals(account.getRole())) {
//            businessService.updatePassword(account);
//        }
//        if (RoleEnum.USER.name().equals(account.getRole())) {
//            userService.updatePassword(account);
//        }
//        return Result.success();
//    }

}
