package com.eys.miniapp.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.result.Result;
import com.eys.common.result.ResultCode;
import com.eys.common.utils.AssertUtils;
import com.eys.mapper.SysUserMapper;
import com.eys.mapper.SysUserMatchMapper;
import com.eys.model.entity.SysUser;
import com.eys.model.entity.SysUserMatch;
import com.eys.model.vo.UserStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序端 - 用户信息
 * 
 * 提供当前登录用户的个人信息和战绩查询。
 */
@Tag(name = "用户信息")
@RestController
@RequestMapping("/api/mp/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper sysUserMapper;
    private final SysUserMatchMapper sysUserMatchMapper;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        AssertUtils.notNull(user, ResultCode.USER_NOT_FOUND);

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setRoleType(user.getRoleType().name());
        return Result.success(vo);
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/me")
    public Result<Void> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        AssertUtils.notNull(user, ResultCode.USER_NOT_FOUND);

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        sysUserMapper.updateById(user);
        return Result.success();
    }

    @Operation(summary = "获取当前用户战绩统计")
    @GetMapping("/stats")
    public Result<UserStatsVO> getMyStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 查询用户所有战绩记录
        List<SysUserMatch> matches = sysUserMatchMapper.selectList(
                new LambdaQueryWrapper<SysUserMatch>().eq(SysUserMatch::getUserId, userId));
        
        // 计算统计数据
        UserStatsVO vo = new UserStatsVO();
        vo.setUserId(userId);
        vo.setTotalGames(matches.size());
        int wins = (int) matches.stream().filter(m -> Boolean.TRUE.equals(m.getIsWinner())).count();
        vo.setWinGames(wins);
        vo.setWinRate(vo.getTotalGames() > 0 
                ? (double) wins / vo.getTotalGames() * 100 
                : 0.0);
        
        return Result.success(vo);
    }

    // ==================== VO/DTO ====================

    @Data
    @Schema(description = "用户信息")
    public static class UserInfoVO {
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "昵称")
        private String nickname;
        @Schema(description = "头像URL")
        private String avatarUrl;
        @Schema(description = "角色类型: PLAYER/DM/ADMIN")
        private String roleType;
    }

    @Data
    @Schema(description = "更新用户请求")
    public static class UpdateUserRequest {
        @Schema(description = "昵称")
        private String nickname;
        @Schema(description = "头像URL")
        private String avatarUrl;
    }
}

