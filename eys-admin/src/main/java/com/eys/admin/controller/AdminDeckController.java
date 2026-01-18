package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.eys.admin.service.AdminDeckService;
import com.eys.common.result.Result;
import com.eys.model.entity.CfgDeck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 预设牌组管理
 */
@Tag(name = "预设牌组管理")
@RestController
@RequestMapping("/api/admin/deck")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminDeckController {

    private final AdminDeckService adminDeckService;

    @Operation(summary = "获取预设牌组列表")
    @GetMapping("/list")
    public Result<List<CfgDeck>> list() {
        return Result.success(adminDeckService.getDeckList());
    }

    @Operation(summary = "获取预设牌组详情")
    @GetMapping("/{id}")
    public Result<CfgDeck> detail(@Parameter(description = "牌组ID") @PathVariable Long id) {
        return Result.success(adminDeckService.getDeckDetail(id));
    }

    @Operation(summary = "新增预设牌组")
    @PostMapping
    public Result<Long> create(@RequestBody CfgDeck deck) {
        return Result.success(adminDeckService.createDeck(deck));
    }

    @Operation(summary = "更新预设牌组")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "牌组ID") @PathVariable Long id,
            @RequestBody CfgDeck deck) {
        adminDeckService.updateDeck(id, deck);
        return Result.success();
    }

    @Operation(summary = "删除预设牌组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "牌组ID") @PathVariable Long id) {
        adminDeckService.deleteDeck(id);
        return Result.success();
    }
}
