package com.eys.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 排行榜数据（返回值对象）
 */
@Data
@Schema(description = "排行榜数据")
public class LeaderboardVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "总场次")
    private Integer totalGames;

    @Schema(description = "胜利场次")
    private Integer winGames;

    @Schema(description = "胜率（百分比）")
    private Double winRate;

    @Schema(description = "排名")
    private Integer rank;
}
