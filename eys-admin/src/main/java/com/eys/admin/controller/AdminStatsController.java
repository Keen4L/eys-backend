package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.eys.admin.service.AdminStatsService;
import com.eys.common.result.Result;
import com.eys.model.vo.LeaderboardVO;
import com.eys.model.vo.UserStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 统计与排行榜
 */
@Tag(name = "统计与排行榜")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "获取总排行榜")
    @GetMapping("/leaderboard")
    public Result<List<LeaderboardVO>> leaderboard(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(adminStatsService.getLeaderboard(limit));
    }

    @Operation(summary = "获取阵营维度排行榜")
    @GetMapping("/leaderboard/camp")
    public Result<List<LeaderboardVO>> leaderboardByCamp(
            @Parameter(description = "阵营类型") @RequestParam String campType,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(adminStatsService.getLeaderboardByCamp(campType, limit));
    }

    @Operation(summary = "获取角色维度排行榜")
    @GetMapping("/leaderboard/role")
    public Result<List<LeaderboardVO>> leaderboardByRole(
            @Parameter(description = "角色ID") @RequestParam Long roleId,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(adminStatsService.getLeaderboardByRole(roleId, limit));
    }

    @Operation(summary = "获取用户战绩")
    @GetMapping("/user/{userId}")
    public Result<UserStatsVO> userStats(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return Result.success(adminStatsService.getUserStats(userId));
    }
}
