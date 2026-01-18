package com.eys.model.vo.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 玩家视图 - 游戏状态（脱敏）
 */
@Data
@Schema(description = "玩家游戏状态视图")
public class PlayerPushVO {

    @Schema(description = "对局ID")
    private Long gameId;

    @Schema(description = "房间邀请码")
    private String roomCode;

    @Schema(description = "游戏状态")
    private String status;

    @Schema(description = "当前回合")
    private Integer currentRound;

    @Schema(description = "当前阶段")
    private String currentStage;

    @Schema(description = "地图ID")
    private Long mapId;

    @Schema(description = "玩家状态列表（脱敏，敏感字段为 null）")
    private List<CommonPlayerVO> players;

    @Schema(description = "我的出生点ID")
    private Long mySpawnPointId;

    @Schema(description = "我的角色ID")
    private Long myRoleId;
}
