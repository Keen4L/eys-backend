package com.eys.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户战绩统计（返回值对象）
 */
@Data
@Schema(description = "用户战绩统计")
public class UserStatsVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "总场次")
    private Integer totalGames;

    @Schema(description = "胜利场次")
    private Integer winGames;

    @Schema(description = "总胜率（百分比）")
    private Double winRate;

    @Schema(description = "阵营维度战绩")
    private List<CampStats> campStats;

    @Schema(description = "角色维度战绩")
    private List<RoleStats> roleStats;

    @Data
    @Schema(description = "阵营战绩")
    public static class CampStats {
        @Schema(description = "阵营类型")
        private String campType;

        @Schema(description = "场次")
        private Integer games;

        @Schema(description = "胜利场次")
        private Integer wins;

        @Schema(description = "胜率（百分比）")
        private Double winRate;
    }

    @Data
    @Schema(description = "角色战绩")
    public static class RoleStats {
        @Schema(description = "角色ID")
        private Long roleId;

        @Schema(description = "角色名称")
        private String roleName;

        @Schema(description = "场次")
        private Integer games;

        @Schema(description = "胜利场次")
        private Integer wins;

        @Schema(description = "胜率（百分比）")
        private Double winRate;
    }
}
