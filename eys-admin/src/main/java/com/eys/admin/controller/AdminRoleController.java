package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.eys.admin.service.AdminRoleService;
import com.eys.common.result.Result;
import com.eys.model.entity.CfgRole;
import com.eys.model.entity.CfgSkill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 角色管理
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/admin/role")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    public Result<List<CfgRole>> list(
            @Parameter(description = "阵营类型") @RequestParam(required = false) String campType) {
        return Result.success(adminRoleService.getRoleList(campType));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<CfgRole> detail(@Parameter(description = "角色ID") @PathVariable Long id) {
        return Result.success(adminRoleService.getRoleDetail(id));
    }

    @Operation(summary = "获取角色技能列表")
    @GetMapping("/{id}/skills")
    public Result<List<CfgSkill>> skills(@Parameter(description = "角色ID") @PathVariable Long id) {
        return Result.success(adminRoleService.getRoleSkills(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public Result<Long> create(@RequestBody CfgRole role) {
        return Result.success(adminRoleService.createRole(role));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody CfgRole role) {
        adminRoleService.updateRole(id, role);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        adminRoleService.deleteRole(id);
        return Result.success();
    }
}
