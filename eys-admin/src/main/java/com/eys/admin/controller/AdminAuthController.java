package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.Result;
import com.eys.common.result.ResultCode;
import com.eys.mapper.SysUserMapper;
import com.eys.model.entity.SysUser;
import com.eys.model.enums.RoleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 认证
 */
@Tag(name = "管理端认证")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysUserMapper sysUserMapper;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        // 1. 查找用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 2. 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 3. 验证是否有管理员权限
        if (user.getRoleType() != RoleType.ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }

        // 4. 登录
        StpUtil.login(user.getId());

        // 5. 返回结果
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        return Result.success(response);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    @SaCheckRole("ADMIN")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long adminId = StpUtil.getLoginIdAsLong();
        SysUser admin = sysUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 验证旧密码
        if (!BCrypt.checkpw(request.getOldPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "旧密码错误");
        }

        // 更新密码
        SysUser update = new SysUser();
        update.setId(adminId);
        update.setPassword(BCrypt.hashpw(request.getNewPassword()));
        sysUserMapper.updateById(update);

        return Result.success();
    }

    @Data
    public static class LoginRequest {
        @Parameter(description = "用户名")
        private String username;
        @Parameter(description = "密码")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private Long userId;
        private String nickname;
    }

    @Data
    public static class ChangePasswordRequest {
        @Parameter(description = "旧密码")
        private String oldPassword;
        @Parameter(description = "新密码")
        private String newPassword;
    }
}


