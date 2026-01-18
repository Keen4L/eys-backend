package com.eys.model.json;

import com.eys.model.enums.TagEffect;
import com.eys.model.enums.TagExpiry;
import lombok.Data;

import java.util.List;

/**
 * 状态标签信息（存储于 ga_player_status.active_tags）
 */
@Data
public class TagInfo {

    /**
     * 来源技能ID
     */
    private Long skillId;

    /**
     * 来源技能名称
     */
    private String skillName;

    /**
     * 施加者玩家ID
     */
    private Long sourcePlayerId;

    /**
     * 被施加的回合数
     */
    private Integer appliedRound;

    /**
     * 过期时机
     */
    private TagExpiry expiry;

    /**
     * 效果列表
     */
    private List<TagEffect> effects;
}
