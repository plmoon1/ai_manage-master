package com.qzb.springboot.exception;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
@Slf4j
@ControllerAdvice(basePackages="com.qzb.springboot.controller")
public class GlobalExceptionHandler {

//    private static final Log log = LogFactory.get();


    //统一异常处理@ExceptionHandler,主要用于Exception
    @ExceptionHandler(Exception.class)
    @ResponseBody//返回json串
    public Result error(HttpServletRequest request, Exception e){
        log.error("异常信息：",e);
        e.printStackTrace();
        return Result.error();
    }

    @ExceptionHandler(TaskException.class)
    @ResponseBody//返回json串
    public Result taskError(HttpServletRequest request, TaskException e){
        log.error("异常信息：",e);
        e.printStackTrace();
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(AuthorityException.class)
    @ResponseBody//返回json串
    public Result authorityError(AuthorityException e){
        log.error("异常信息：",e);
        e.printStackTrace();
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(MeetingException.class)
    @ResponseBody//返回json串
    public Result meetingError(MeetingException e){
        log.error("异常信息：",e);
        e.printStackTrace();
        return Result.error(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(PermissionException.class)
    @ResponseBody//返回json串
    public Result permissionError(PermissionException e){
        log.error("异常信息：",e);
        e.printStackTrace();
        return Result.error(e.getCode(), e.getMsg());
    }

    // ===================== 核心：捕获 @PreAuthorize 权限不足异常 =====================
    @ExceptionHandler(AccessDeniedException.class)
    public Result handleAccessDeniedException(AccessDeniedException e) {
        // 返回和你Security配置一致的无权限响应
        return Result.error(ResultCodeEnum.Authority_ERROR);
    }

    // 捕获未登录异常（方法级）
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public Result handleAuthException(InsufficientAuthenticationException e) {
        return Result.error(ResultCodeEnum.TOKEN_CHECK_ERROR);
    }
}
