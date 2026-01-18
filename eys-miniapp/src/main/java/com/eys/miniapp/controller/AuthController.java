package com.eys.miniapp.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.result.Result;
import com.eys.mapper.SysUserMapper;
import com.eys.model.entity.SysUser;
import com.eys.model.enums.RoleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序端 - 认证授权
 * 
 * 提供微信登录、获取token等认证相关接口。
 */
@Slf4j
@Tag(name = "认证授权")
@RestController
@RequestMapping("/api/mp/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;

    /**
     * 微信登录
     * 
     * 流程：
     * 1. 前端调用 wx.login() 获取 code
     * 2. 前端带着 code 和用户信息（可选）调用本接口
     * 3. 后端用 code 换取 openid（需调用微信API）
     * 4. 根据 openid 查找或创建用户
     * 5. 生成 SaToken 并返回
     * 
     * 注意：当前为简化版本，实际生产需要：
     * - 调用微信 code2Session 接口获取 openid
     * - 配置微信小程序 appid 和 secret
     */
    @Operation(summary = "微信登录", description = "使用微信临时登录凭证换取token")
    @PostMapping("/wx-login")
    public Result<LoginResultVO> wxLogin(@RequestBody WxLoginRequest request) {
        // TODO: 实际生产环境需要调用微信 code2Session 接口
        // String url = "https://api.weixin.qq.com/sns/jscode2session?appid={}&secret={}&js_code={}&grant_type=authorization_code";
        // 获取 openid 和 session_key
        
        // 当前简化版本：直接使用 code 作为 openid（仅用于开发测试）
        String openid = "wx_" + request.getCode();
        
        // 查找或创建用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getOpenid, openid));
        
        if (user == null) {
            // 新用户，自动注册
            user = new SysUser();
            user.setOpenid(openid);
            user.setNickname(request.getNickname() != null ? request.getNickname() : "玩家" + System.currentTimeMillis() % 10000);
            user.setAvatarUrl(request.getAvatarUrl());
            user.setRoleType(RoleType.PLAYER); // 默认为普通玩家
            sysUserMapper.insert(user);
            log.info("新用户注册: userId={}, openid={}", user.getId(), openid);
        } else {
            // 老用户，更新信息（如果有提供）
            boolean needUpdate = false;
            if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
                user.setNickname(request.getNickname());
                needUpdate = true;
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(request.getAvatarUrl());
                needUpdate = true;
            }
            if (needUpdate) {
                sysUserMapper.updateById(user);
            }
        }

        // 登录（生成 SaToken）
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 构建返回结果
        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setNickname(user.getNickname());
        result.setAvatarUrl(user.getAvatarUrl());
        result.setRoleType(user.getRoleType().name());

        return Result.success(result);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    // ==================== VO/DTO ====================

    @Data
    @Schema(description = "微信登录请求")
    public static class WxLoginRequest {
        @Schema(description = "微信临时登录凭证", required = true)
        private String code;

        @Schema(description = "用户昵称（可选，首次登录时使用）")
        private String nickname;

        @Schema(description = "用户头像URL（可选，首次登录时使用）")
        private String avatarUrl;
    }

    @Data
    @Schema(description = "登录结果")
    public static class LoginResultVO {
        @Schema(description = "登录token")
        private String token;

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "昵称")
        private String nickname;

        @Schema(description = "头像URL")
        private String avatarUrl;

        @Schema(description = "角色类型: PLAYER/DM/ADMIN")
        private String roleType;
    }
}
