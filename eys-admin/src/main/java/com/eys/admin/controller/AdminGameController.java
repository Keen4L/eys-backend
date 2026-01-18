package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.eys.admin.service.AdminGameService;
import com.eys.common.result.Result;
import com.eys.model.entity.GaGameRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 对局记录管理
 */
@Tag(name = "对局记录管理")
@RestController
@RequestMapping("/api/admin/game")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminGameController {

    private final AdminGameService adminGameService;

    @Operation(summary = "获取对局列表")
    @GetMapping("/list")
    public Result<List<GaGameRecord>> list(
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(adminGameService.getGameList(status));
    }

    @Operation(summary = "获取对局详情")
    @GetMapping("/{id}")
    public Result<GaGameRecord> detail(@Parameter(description = "对局ID") @PathVariable Long id) {
        return Result.success(adminGameService.getGameDetail(id));
    }

    @Operation(summary = "删除对局记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "对局ID") @PathVariable Long id) {
        adminGameService.deleteGame(id);
        return Result.success();
    }
}
