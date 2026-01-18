package com.eys.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.eys.admin.service.AdminSkillService;
import com.eys.common.result.Result;
import com.eys.model.entity.CfgSkill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 技能管理
 */
@Tag(name = "技能管理")
@RestController
@RequestMapping("/api/admin/skill")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminSkillController {

    private final AdminSkillService adminSkillService;

    @Operation(summary = "获取技能列表")
    @GetMapping("/list")
    public Result<List<CfgSkill>> list(
            @Parameter(description = "角色ID") @RequestParam(required = false) Long roleId) {
        return Result.success(adminSkillService.getSkillList(roleId));
    }

    @Operation(summary = "获取技能详情")
    @GetMapping("/{id}")
    public Result<CfgSkill> detail(@Parameter(description = "技能ID") @PathVariable Long id) {
        return Result.success(adminSkillService.getSkillDetail(id));
    }

    @Operation(summary = "新增技能")
    @PostMapping
    public Result<Long> create(@RequestBody CfgSkill skill) {
        return Result.success(adminSkillService.createSkill(skill));
    }

    @Operation(summary = "更新技能")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "技能ID") @PathVariable Long id,
            @RequestBody CfgSkill skill) {
        adminSkillService.updateSkill(id, skill);
        return Result.success();
    }

    @Operation(summary = "删除技能")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "技能ID") @PathVariable Long id) {
        adminSkillService.deleteSkill(id);
        return Result.success();
    }
}
