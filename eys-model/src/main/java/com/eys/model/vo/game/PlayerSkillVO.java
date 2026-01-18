package com.eys.model.vo.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 玩家技能推送 VO（DM 推送给玩家）
 * DM 只能推送可用的非普通技能
 */
@Data
@Schema(description = "玩家技能推送")
public class PlayerSkillVO {

    @Schema(description = "技能实例ID")
    private Long skillInstanceId;

    @Schema(description = "技能配置ID")
    private Long skillId;

    @Schema(description = "可选目标列表（gamePlayerId 列表）")
    private List<Long> targetPlayerIds;

    @Schema(description = "预加载数据（推送时携带）")
    private PreData preData;

    @Data
    @Schema(description = "预加载数据")
    public static class PreData {
        @Schema(description = "数据类型")
        private String type;

        @Schema(description = "数据内容")
        private Object data;
    }
}
