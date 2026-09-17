package com.qzb.springboot.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.enums.ResultCodeEnum;

import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.entity.UserExcel;
import com.qzb.springboot.exception.TaskException;
import com.qzb.springboot.mapper.UserMapper;
import com.qzb.springboot.utils.TimeUtils;
import com.qzb.springboot.utils.UserExcelListener;
import org.springframework.stereotype.Service;
import com.qzb.springboot.utils.TokenUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserExcelListener userExcelListener;

    public User login(User user) {
        User dbUser = userMapper.getUserInfo(user.getId());
        if (ObjectUtil.isNull(dbUser)) {
            throw new TaskException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new TaskException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        // 生成token
        String tokenData = dbUser.getId() + "-" + dbUser.getRole();
        String token = TokenUtils.createToken(tokenData, dbUser.getPassword());
        dbUser.setToken(token);
        // 添加权限
        List<Permission> permissions = userMapper.getPermissionsByRoleIdAndTid(dbUser.getRid(), dbUser.getTid());
        dbUser.setPermissions(permissions);
        return dbUser;
    }


    public void addUser(User user){
        user.setPassword(user.getId());
        String currentTime = TimeUtils.getCurrentTime();
        user.setCreateTime(currentTime);
        user.setUpdateTime(currentTime);
        this.userMapper.addUser(user);
    }

    public void updateUser(User user){
        user.setUpdateTime(TimeUtils.getCurrentTime());
        this.userMapper.updateUser(user);
    }

    public void updateUserBySelf(User user){
        this.userMapper.updateUserBySelf(user);
    }

    public List<User> getAllUser(String tid){
        return this.userMapper.getAllUser(tid);
    }

    public void deleteUser(String uid,String tid){
        this.userMapper.deleteUser(uid,tid);
    }
    public List<User> getUserByCondition(User user){
        return this.userMapper.getUserByCondition(user);
    }

    public PageInfo<User> getUserPage(User user, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> list = userMapper.getUserByCondition(user);
        return PageInfo.of(list);
    }

    public void importUserData(MultipartFile file) {
        // 校验文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传的Excel文件不能为空");
        }
        // 校验文件格式
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new RuntimeException("仅支持.xlsx/.xls格式的Excel文件");
        }

        // 读取Excel文件
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, UserExcel.class, userExcelListener)
                    .sheet() // 读取第一个sheet
                    .headRowNumber(1) // 表头行号（第1行）
                    .doRead();
        } catch (IOException e) {
//            userExcelListener.exceptionClear();
            e.printStackTrace();
            throw new RuntimeException("读取Excel文件失败：" + e.getMessage());
        }
    }

    public void resetUser(String uid) {
        this.userMapper.resetUser(uid);
    }
}
