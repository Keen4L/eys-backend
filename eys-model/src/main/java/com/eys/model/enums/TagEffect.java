package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标签效果（影响系统推送）
 * 只有两种效果，用于控制系统是否推送技能/投票给玩家
 */
@Getter
@AllArgsConstructor
public enum TagEffect {

    BLOCK_SKILL("BLOCK_SKILL", "封锁技能"),
    BLOCK_VOTE("BLOCK_VOTE", "封锁投票");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
