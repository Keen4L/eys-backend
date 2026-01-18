package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 动作类型
 */
@Getter
@AllArgsConstructor
public enum ActionType {

    SKILL("SKILL", "技能使用"),
    KILL("KILL", "击杀玩家"),
    REVIVE("REVIVE", "复活玩家"),
    VOTE("VOTE", "投票"),
    SYSTEM("SYSTEM", "系统操作");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
