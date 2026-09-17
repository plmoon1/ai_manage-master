package com.qzb.springboot.controller;

import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.exception.TaskException;
import com.qzb.springboot.security.PermissionConstants;
import com.qzb.springboot.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;


    @PutMapping("updateUserBySelf")
    public Result updateUserBySelf(@RequestBody User user){
        this.userService.updateUserBySelf(user);
        return Result.success();
    }

    /**
     * 添加新用户
     * */
    @PostMapping("/addUser")
    public Result addUser(@RequestBody User user){
        this.userService.addUser(user);
        return Result.success();
    }

    /**
     * 更新用户信息
     */

    @PutMapping("/updateUser")
    public Result updateUser(@RequestBody User user){
        this.userService.updateUser(user);
        return Result.success();
    }

    /** 删除用户*/
    @DeleteMapping("/deleteUser")
    public Result deleteUser(@RequestParam String uid, @RequestParam String tid){
        this.userService.deleteUser(uid,tid);
        return Result.success();
    }

    @GetMapping("/getAllUserByTid/{tid}")
    public Result getAllUser(@PathVariable String tid){
        List<User> users = this.userService.getAllUser(tid);
        return Result.success(users);
    }

    @GetMapping("/getUserByCondition")
    public Result getUserByCondition(User user)  {
        List<User> users = this.userService.getUserByCondition(user);
        return Result.success(users);
    }


    @GetMapping("/getUserByStatus")
    public Result getUserByStatus(@RequestParam String tid,
                                  @RequestParam String tatus){

        return Result.success();
    }

    @GetMapping("/searchUserByUid")
    public Result searchUserByUid(@RequestParam String uid,
                                  @RequestParam String tid){

        return Result.success();
    }

    @GetMapping("/searchUserByUname")
    public Result searchUserByUname(@RequestParam String tid,
                                    @RequestParam String uname){

        return Result.success();
    }

    @GetMapping("/getUserPage")
    public Result getUserPage(User user,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            PageInfo<User> page = userService.getUserPage(user, pageNum, pageSize);
            return Result.success(page);
        }catch (Exception e){
            e.printStackTrace();
            return Result.error();
        }
    }


    @PostMapping("/import")
    public Result importPlan(@RequestParam("file") MultipartFile file) {
        try {
            this.userService.importUserData(file);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCodeEnum.FILE_INSERT_ERROR.code, ResultCodeEnum.FILE_INSERT_ERROR.msg);
        }
    }

    // 只有拥有 user:list 权限的角色才能访问
//    @GetMapping("/list")
//    @PreAuthorize("hasPermission('" + PermissionConstants.USER_LIST + "')")
//    public Result list() {
//        return Result.success("用户列表");
//    }
//
//    // 只有拥有 user:add 权限的角色才能访问
//    @PostMapping("/add")
//    @PreAuthorize("hasPermission('" + PermissionConstants.USER_ADD + "')")
//    public Result add(@RequestBody User user) {
//        return Result.success("新增用户成功");
//    }
    @PutMapping("/resetUser")
    public Result resetUser(@RequestParam String uid){
        this.userService.resetUser(uid);
        return Result.success();
    }
}
