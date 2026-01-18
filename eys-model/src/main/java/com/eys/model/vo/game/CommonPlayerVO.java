package com.eys.model.vo.game;

import com.eys.model.json.TagInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 玩家状态 VO（共享，DM 和玩家视图通用）
 * 玩家视图中敏感字段（roleId、activeTags、skills）为 null
 */
@Data
@Schema(description = "玩家状态")
public class CommonPlayerVO {

    @Schema(description = "对局玩家ID")
    private Long gamePlayerId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "座位号")
    private Integer seatNo;

    @Schema(description = "是否存活")
    private Boolean isAlive;

    @Schema(description = "是否是当前玩家自己（仅玩家视图）")
    private Boolean isMe;

    // ==================== DM 视图专属（玩家视图为 null）====================

    @Schema(description = "角色ID（DM 视图可见，玩家视图为 null）")
    private Long roleId;

    @Schema(description = "当前生效的标签列表（DM 视图可见，玩家视图为 null）")
    private List<TagInfo> activeTags;

    @Schema(description = "技能实例列表（DM 视图可见，玩家视图为 null）")
    private List<SkillInstanceVO> skills;

    // ==================== 内部类 ====================

    /**
     * 技能实例
     */
    @Data
    @Schema(description = "技能实例")
    public static class SkillInstanceVO {
        @Schema(description = "技能实例ID")
        private Long skillInstanceId;

        @Schema(description = "技能配置ID")
        private Long skillId;

        @Schema(description = "是否激活可用")
        private Boolean isActive;

        @Schema(description = "已使用次数")
        private Integer usedCount;
    }
}
