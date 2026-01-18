package com.eys.miniapp.controller;

import com.eys.common.result.Result;
import com.eys.miniapp.service.VoteService;
import com.eys.miniapp.service.helper.PlayerAuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序端 - 投票
 */
@Tag(name = "投票")
@RestController
@RequestMapping("/api/mp/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final PlayerAuthHelper playerAuthHelper;

    @Operation(summary = "投票")
    @PostMapping
    public Result<Void> vote(
            @Parameter(description = "对局ID") @RequestParam Long gameId,
            @Parameter(description = "目标玩家ID（null 表示弃票）") @RequestParam(required = false) Long targetId) {
        Long gamePlayerId = playerAuthHelper.getGamePlayerId(gameId);
        voteService.vote(gamePlayerId, targetId);
        return Result.success();
    }
}
