package com.qzb.springboot.service;

import cn.hutool.core.util.ObjectUtil;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.Account;
import com.qzb.springboot.entity.Admin;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.exception.TaskException;
import com.qzb.springboot.mapper.AdminMapper;
import com.qzb.springboot.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import javax.annotation.Resource;
import java.util.List;

@Service
public class AdminService {
    @Resource
    private AdminMapper adminMapper;
    public Admin getAdminById(String id){
        Admin admin = this.adminMapper.getAdminById(id);
        return admin;
    }




//    public Account login(Account account) {
//        Account dbAdmin = adminMapper.getAdminById(account.getId());
//        if (ObjectUtil.isNull(dbAdmin)) {
//            throw new TaskException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
//        }
//        if (!account.getPassword().equals(dbAdmin.getPassword())) {
//            throw new TaskException(ResultCodeEnum.USER_ACCOUNT_ERROR);
//        }
//        // 生成token
//        String tokenData = dbAdmin.getId() + "-" + RoleEnum.ADMIN.getRoleName();
//        String token = TokenUtils.createToken(tokenData, dbAdmin.getPassword());
//        dbAdmin.setToken(token);
//        return dbAdmin;
//    }


}
