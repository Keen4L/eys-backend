package com.eys.model.vo.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 技能结果（使用后回传给玩家）
 */
@Data
@Schema(description = "技能结果")
public class PlayerSkillResultVO {

    @Schema(description = "技能ID")
    private Long skillId;

    @Schema(description = "结果类型")
    private String resultType;

    @Schema(description = "结果数据")
    private Map<String, Object> result;
}
