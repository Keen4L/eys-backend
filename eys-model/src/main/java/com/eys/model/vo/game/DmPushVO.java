package com.eys.model.vo.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * DM 视图 - 游戏状态（全量信息）
 */
@Data
@Schema(description = "DM 游戏状态视图")
public class DmPushVO {

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

    @Schema(description = "玩家状态列表")
    private List<CommonPlayerVO> players;

    @Schema(description = "本回合出生点")
    private List<SpawnPointVO> spawnPoints;

    @Schema(description = "动作日志")
    private List<ActionLogVO> actionLogs;

    // ==================== 内部类 ====================

    /**
     * 出生点信息
     */
    @Data
    @Schema(description = "出生点")
    public static class SpawnPointVO {
        @Schema(description = "出生点配置ID")
        private Long spawnPointId;

        @Schema(description = "对局玩家ID")
        private Long gamePlayerId;
    }

    /**
     * 动作日志
     */
    @Data
    @Schema(description = "动作日志")
    public static class ActionLogVO {
        @Schema(description = "回合号")
        private Integer roundNo;

        @Schema(description = "阶段: START/NIGHT/VOTE/DAY")
        private String stage;

        @Schema(description = "动作类型: SKILL/KILL/REVIVE/VOTE")
        private String actionType;

        @Schema(description = "发起者对局玩家ID")
        private Long initiatorId;

        @Schema(description = "目标对局玩家ID列表")
        private List<Long> targetIds;

        @Schema(description = "技能ID")
        private Long skillId;
    }
}
