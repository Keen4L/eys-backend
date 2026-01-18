package com.eys.model.vo.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 游戏结束结果
 */
@Data
@Schema(description = "游戏结束结果")
public class GameEndResultVO {

    @Schema(description = "刚结束的对局ID")
    private Long finishedGameId;

    @Schema(description = "新对局ID（等待中）")
    private Long newGameId;

    @Schema(description = "房间邀请码")
    private String roomCode;

    @Schema(description = "胜利阵营")
    private String victoryType;

    @Schema(description = "玩家游戏信息（包含所有玩家角色等结算信息）")
    private DmPushVO gameState;
}
