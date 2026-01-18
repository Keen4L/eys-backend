package com.eys.web.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.eys.common.result.Result;
import com.eys.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常处理器
 * 优先级高于全局异常处理器
 */
@Slf4j
@RestControllerAdvice
@Order(-1)
public class SaTokenExceptionHandler {

    /**
     * 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("未登录: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
    }

    /**
     * 无角色权限异常
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.warn("无角色权限: {} - 需要角色: {}", request.getRequestURI(), e.getRole());
        return Result.fail(ResultCode.FORBIDDEN, "无权限访问");
    }

    /**
     * 无操作权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("无操作权限: {} - 需要权限: {}", request.getRequestURI(), e.getPermission());
        return Result.fail(ResultCode.FORBIDDEN, "无权限访问");
    }
}
