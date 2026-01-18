package com.eys.miniapp.controller;

import com.eys.common.result.Result;
import com.eys.miniapp.service.SkillService;
import com.eys.miniapp.service.helper.PlayerAuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序端 - 技能
 */
@Tag(name = "技能")
@RestController
@RequestMapping("/api/mp/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final PlayerAuthHelper playerAuthHelper;

    @Operation(summary = "使用技能")
    @PostMapping("/use")
    public Result<Void> use(
            @Parameter(description = "对局ID") @RequestParam Long gameId,
            @Parameter(description = "技能配置ID") @RequestParam Long skillId,
            @Parameter(description = "目标ID列表") @RequestParam(required = false) List<Long> targetIds) {
        Long gamePlayerId = playerAuthHelper.getGamePlayerId(gameId);
        skillService.useSkill(gamePlayerId, skillId, targetIds);
        return Result.success();
    }
}
