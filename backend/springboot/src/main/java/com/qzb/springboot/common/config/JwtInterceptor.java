package com.qzb.springboot.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.qzb.springboot.common.Constants;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.Account;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.exception.TaskException;
import com.qzb.springboot.mapper.UserMapper;
import com.qzb.springboot.service.UserDetailServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserDetailServiceImpl  userDetailService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从http请求的header中获取token
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isEmpty(token)) {
            // 如果没拿到，从参数里再拿一次
            token = request.getParameter(Constants.TOKEN);
        }
        // 2. 开始执行认证
        if (ObjectUtil.isEmpty(token)) {
            throw new TaskException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        User user = null;
        try {
            // 解析token获取存储的数据
            String userRole = JWT.decode(token).getAudience().get(0);
            String userId = userRole.split("-")[0];
            String role = userRole.split("-")[1];
            // 根据userId查询数据库
            user = userMapper.getUserInfo(userId);

        } catch (Exception e) {
            throw new TaskException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        if (ObjectUtil.isNull(user)) {
            throw new TaskException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        try {
            // 用户密码加签验证 token，用password做signature
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
            jwtVerifier.verify(token); // 验证token
            // 添加权限上下文
            UserDetails userDetails = userDetailService.loadUserByUsername(user.getId());
            // 核心：将用户信息放入上下文，权限校验才能生效
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);


        } catch (JWTVerificationException e) {
            throw new TaskException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }
}