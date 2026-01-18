package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.eys.admin.service.AdminMapService;
import com.eys.common.result.Result;
import com.eys.model.entity.CfgMap;
import com.eys.model.entity.CfgMapSpawnPoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 地图管理
 */
@Tag(name = "地图管理")
@RestController
@RequestMapping("/api/admin/map")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminMapController {

    private final AdminMapService adminMapService;

    @Operation(summary = "获取地图列表")
    @GetMapping("/list")
    public Result<List<CfgMap>> list() {
        return Result.success(adminMapService.getMapList());
    }

    @Operation(summary = "获取地图详情")
    @GetMapping("/{id}")
    public Result<CfgMap> detail(@Parameter(description = "地图ID") @PathVariable Long id) {
        return Result.success(adminMapService.getMapDetail(id));
    }

    @Operation(summary = "获取地图出生点")
    @GetMapping("/{id}/spawn-points")
    public Result<List<CfgMapSpawnPoint>> spawnPoints(@Parameter(description = "地图ID") @PathVariable Long id) {
        return Result.success(adminMapService.getSpawnPoints(id));
    }

    @Operation(summary = "新增地图")
    @PostMapping
    public Result<Long> create(@RequestBody CfgMap map) {
        return Result.success(adminMapService.createMap(map));
    }

    @Operation(summary = "更新地图")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "地图ID") @PathVariable Long id,
            @RequestBody CfgMap map) {
        adminMapService.updateMap(id, map);
        return Result.success();
    }

    @Operation(summary = "删除地图")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "地图ID") @PathVariable Long id) {
        adminMapService.deleteMap(id);
        return Result.success();
    }

    @Operation(summary = "新增出生点")
    @PostMapping("/{mapId}/spawn-point")
    public Result<Long> createSpawnPoint(
            @Parameter(description = "地图ID") @PathVariable Long mapId,
            @RequestBody CfgMapSpawnPoint spawnPoint) {
        return Result.success(adminMapService.createSpawnPoint(mapId, spawnPoint));
    }

    @Operation(summary = "删除出生点")
    @DeleteMapping("/spawn-point/{id}")
    public Result<Void> deleteSpawnPoint(@Parameter(description = "出生点ID") @PathVariable Long id) {
        adminMapService.deleteSpawnPoint(id);
        return Result.success();
    }
}
