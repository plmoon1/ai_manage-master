package com.qzb.springboot.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.qzb.springboot.common.Constants;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.Account;
import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.mapper.UserMapper;
import com.qzb.springboot.service.AdminService;

import com.qzb.springboot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Token工具类
 */
@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    private static AdminService staticAdminService;

    private static UserService staticUserService;

    private static UserMapper staticUserMapper;
    @Resource
    AdminService adminService;

    @Resource
    UserService userService;

    @Resource
    UserMapper userMapper;
    @PostConstruct
    public void setUserService() {
        staticAdminService = adminService;
        staticUserService = userService;
        staticUserMapper = userMapper;
    }

    /**
     * 生成token
     */
    public static String createToken(String data, String sign) {
        return JWT.create().withAudience(data) // 将 userId-role 保存到 token 里面,作为载荷playload
                .withExpiresAt(DateUtil.offsetHour(new Date(), 168)) // 2小时后token过期
//                .withExpiresAt(DateUtil.offsetSecond(new Date(), 30)) // 30s后token过期,测试用
                .sign(Algorithm.HMAC256(sign)); // 以 password 作为 token 的密钥
    }

    /**
     * 获取当前登录的用户信息
     */
    public static User getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader(Constants.TOKEN);
            if (ObjectUtil.isNotEmpty(token)) {
                String userRole = JWT.decode(token).getAudience().get(0);
                String userId = userRole.split("-")[0];  // 获取用户id
                String role = userRole.split("-")[1];    // 获取角色
                User dbUser = staticUserMapper.getUserInfo(userId);
                // 添加权限
                List<Permission> permissions = staticUserMapper.getPermissionsByRoleIdAndTid(dbUser.getRid(), dbUser.getTid());
                dbUser.setPermissions(permissions);
                return dbUser;
                }

        } catch (Exception e) {
            log.error("获取当前用户信息出错", e);
        }
        return new User();  // 返回空的账号对象
    }

    public static List<String> getPermissions(){
        User user = getCurrentUser();
        List<String> permissions = user.getPermissions().stream().map(p -> p.getPermission()).collect(Collectors.toList());
        return permissions;
    }
}

