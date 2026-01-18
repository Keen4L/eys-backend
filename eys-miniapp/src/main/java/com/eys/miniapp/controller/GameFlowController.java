package com.eys.miniapp.controller;

import com.eys.common.result.Result;
import com.eys.miniapp.service.GameFlowService;
import com.eys.miniapp.service.helper.DmAuthHelper;
import com.eys.model.vo.game.DmPushVO;
import com.eys.model.vo.game.GameEndResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 小程序端 - 游戏流程控制（DM 专用）
 */
@Tag(name = "游戏流程控制")
@RestController
@RequestMapping("/api/mp/game")
@RequiredArgsConstructor
public class GameFlowController {

    private final GameFlowService gameFlowService;
    private final DmAuthHelper dmAuthHelper;

    @Operation(summary = "开始游戏（配置游戏并分配身份）", description = "DM 传入地图和角色列表，可以内定部分玩家角色，未内定的随机分配")
    @PostMapping("/{gameId}/start")
    public Result<DmPushVO> start(
            @Parameter(description = "对局ID") @PathVariable Long gameId,
            @Parameter(description = "地图ID") @RequestParam Long mapId,
            @Parameter(description = "角色ID列表") @RequestParam List<Long> roleIds,
            @Parameter(description = "内定角色映射（gamePlayerId -> roleId）") @RequestParam(required = false) Map<Long, Long> assignedRoles) {
        dmAuthHelper.checkDm(gameId);
        return Result.success(gameFlowService.startGame(gameId, mapId, roleIds, assignedRoles));
    }

    @Operation(summary = "推进到下一阶段（DAY → NIGHT 时自动进入下一回合）")
    @PostMapping("/{gameId}/next")
    public Result<DmPushVO> nextStage(
            @Parameter(description = "对局ID") @PathVariable Long gameId) {
        dmAuthHelper.checkDm(gameId);
        return Result.success(gameFlowService.nextStage(gameId));
    }

    @Operation(summary = "击杀玩家")
    @PostMapping("/{gameId}/kill")
    public Result<Void> kill(
            @Parameter(description = "对局ID") @PathVariable Long gameId,
            @Parameter(description = "目标玩家ID列表") @RequestParam List<Long> targetPlayerIds) {
        dmAuthHelper.checkDm(gameId);
        gameFlowService.killPlayers(gameId, targetPlayerIds);
        return Result.success();
    }

    @Operation(summary = "复活玩家")
    @PostMapping("/{gameId}/revive")
    public Result<Void> revive(
            @Parameter(description = "对局ID") @PathVariable Long gameId,
            @Parameter(description = "目标玩家ID列表") @RequestParam List<Long> targetPlayerIds) {
        dmAuthHelper.checkDm(gameId);
        gameFlowService.revivePlayers(gameId, targetPlayerIds);
        return Result.success();
    }

    @Operation(summary = "结束游戏", description = "结束游戏并自动创建新对局（同房间码），返回结算信息和新对局ID")
    @PostMapping("/{gameId}/end")
    public Result<GameEndResultVO> end(
            @Parameter(description = "对局ID") @PathVariable Long gameId,
            @Parameter(description = "胜利阵营") @RequestParam String victoryType) {
        dmAuthHelper.checkDm(gameId);
        return Result.success(gameFlowService.endGame(gameId, victoryType));
    }

    @Operation(summary = "移除玩家标签")
    @PostMapping("/{gameId}/remove-tag")
    public Result<Void> removeTag(
            @Parameter(description = "对局ID") @PathVariable Long gameId,
            @Parameter(description = "目标玩家ID") @RequestParam Long targetPlayerId,
            @Parameter(description = "标签索引") @RequestParam Integer tagIndex) {
        dmAuthHelper.checkDm(gameId);
        gameFlowService.removeTag(gameId, targetPlayerId, tagIndex);
        return Result.success();
    }

    @Operation(summary = "DM 手动推送技能给玩家")
    @PostMapping("/{gameId}/push-skill")
    public Result<Void> pushSkillToPlayer(
            @Parameter(description = "对局ID") @PathVariable Long gameId,
            @Parameter(description = "玩家ID") @RequestParam Long playerId,
            @Parameter(description = "技能ID") @RequestParam(required = false) Long skillId) {
        dmAuthHelper.checkDm(gameId);
        gameFlowService.pushSkillToPlayer(gameId, playerId, skillId);
        return Result.success();
    }
}
