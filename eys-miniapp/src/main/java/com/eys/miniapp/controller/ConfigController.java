package com.eys.miniapp.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.result.Result;
import com.eys.mapper.CfgDeckMapper;
import com.eys.mapper.CfgMapMapper;
import com.eys.mapper.CfgRoleMapper;
import com.eys.mapper.CfgSkillMapper;
import com.eys.model.entity.CfgDeck;
import com.eys.model.entity.CfgMap;
import com.eys.model.entity.CfgRole;
import com.eys.model.entity.CfgSkill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序端 - 配置数据（只读）
 * 
 * 提供地图、角色、牌组、技能等配置数据的查询接口，供 DM 开局时使用。
 */
@Tag(name = "配置数据")
@RestController
@RequestMapping("/api/mp/config")
@RequiredArgsConstructor
public class ConfigController {

    private final CfgMapMapper cfgMapMapper;
    private final CfgRoleMapper cfgRoleMapper;
    private final CfgDeckMapper cfgDeckMapper;
    private final CfgSkillMapper cfgSkillMapper;

    // ==================== 地图 ====================

    @Operation(summary = "获取地图列表")
    @GetMapping("/maps")
    public Result<List<CfgMap>> getMapList() {
        return Result.success(cfgMapMapper.selectList(null));
    }

    @Operation(summary = "获取地图详情")
    @GetMapping("/maps/{id}")
    public Result<CfgMap> getMapDetail(@Parameter(description = "地图ID") @PathVariable Long id) {
        return Result.success(cfgMapMapper.selectById(id));
    }

    // ==================== 角色 ====================

    @Operation(summary = "获取角色列表", description = "可按阵营筛选，仅返回启用的角色")
    @GetMapping("/roles")
    public Result<List<CfgRole>> getRoleList(
            @Parameter(description = "阵营类型: GOOSE/DUCK/NEUTRAL") @RequestParam(required = false) String campType) {
        LambdaQueryWrapper<CfgRole> wrapper = new LambdaQueryWrapper<CfgRole>()
                .eq(CfgRole::getIsEnabled, true);
        if (campType != null && !campType.isBlank()) {
            wrapper.eq(CfgRole::getCampType, campType);
        }
        return Result.success(cfgRoleMapper.selectList(wrapper));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/roles/{id}")
    public Result<CfgRole> getRoleDetail(@Parameter(description = "角色ID") @PathVariable Long id) {
        return Result.success(cfgRoleMapper.selectById(id));
    }

    // ==================== 预设牌组 ====================

    @Operation(summary = "获取预设牌组列表")
    @GetMapping("/decks")
    public Result<List<CfgDeck>> getDeckList() {
        return Result.success(cfgDeckMapper.selectList(null));
    }

    @Operation(summary = "获取预设牌组详情")
    @GetMapping("/decks/{id}")
    public Result<CfgDeck> getDeckDetail(@Parameter(description = "牌组ID") @PathVariable Long id) {
        return Result.success(cfgDeckMapper.selectById(id));
    }

    // ==================== 技能 ====================

    @Operation(summary = "获取技能列表", description = "可按角色ID筛选")
    @GetMapping("/skills")
    public Result<List<CfgSkill>> getSkillList(
            @Parameter(description = "角色ID") @RequestParam(required = false) Long roleId) {
        LambdaQueryWrapper<CfgSkill> wrapper = new LambdaQueryWrapper<>();
        if (roleId != null) {
            wrapper.eq(CfgSkill::getRoleId, roleId);
        }
        return Result.success(cfgSkillMapper.selectList(wrapper));
    }

    @Operation(summary = "获取技能详情")
    @GetMapping("/skills/{id}")
    public Result<CfgSkill> getSkillDetail(@Parameter(description = "技能ID") @PathVariable Long id) {
        return Result.success(cfgSkillMapper.selectById(id));
    }
}

