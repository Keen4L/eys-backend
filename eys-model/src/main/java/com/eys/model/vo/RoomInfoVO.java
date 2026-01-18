package com.eys.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 房间信息（返回值对象）
 */
@Data
@Schema(description = "房间信息")
public class RoomInfoVO {

    @Schema(description = "对局ID")
    private Long gameId;

    @Schema(description = "房间邀请码")
    private String roomCode;

    @Schema(description = "主持人用户ID")
    private Long dmUserId;

    @Schema(description = "地图ID")
    private Long mapId;

    @Schema(description = "游戏状态")
    private String status;

    @Schema(description = "当前回合")
    private Integer currentRound;

    @Schema(description = "当前阶段")
    private String currentStage;

    @Schema(description = "已加入的玩家列表")
    private List<PlayerInfo> players;

    @Data
    @Schema(description = "玩家信息")
    public static class PlayerInfo {
        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "座位号")
        private Integer seatNo;

        @Schema(description = "昵称")
        private String nickname;

        @Schema(description = "头像URL")
        private String avatarUrl;
    }
}
