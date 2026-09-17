package com.qzb.springboot.service;

import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.mapper.UserMapper;
import com.qzb.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.List;

@Service
public class UserDetailServiceImpl implements UserDetailsService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        // 1. 前端需传递租户ID（从请求头获取）
//        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        String tid = attributes.getRequest().getHeader("tid");
//
//        // 2. 查询用户
//        User user = userMapper.getUserByUsernameAndTid(username, tid);
//        if (user == null) throw new UsernameNotFoundException("用户不存在");
        User user = TokenUtils.getCurrentUser();
        // 3. 查询用户权限
        List<Permission> permissions = userMapper.getPermissionsByRoleIdAndTid(user.getRid(), user.getTid());
        user.setPermissions(permissions);
        return user;
    }
}
