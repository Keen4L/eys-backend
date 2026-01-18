package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 目标选择方式
 */
@Getter
@AllArgsConstructor
public enum TargetSelectMode {

    MANUAL_PLAYER("MANUAL_PLAYER", "手动选人"),
    MANUAL_PLAYER_ROLE("MANUAL_PLAYER_ROLE", "手动选人+选角色"),
    AUTO_ATTACKER("AUTO_ATTACKER", "自动-出刀人"),
    AUTO_ALIVE_DUCK("AUTO_ALIVE_DUCK", "自动-存活的鸭子"),
    AUTO_ALIVE_GOOSE("AUTO_ALIVE_GOOSE", "自动-存活的鹅"),
    AUTO_LEFT_RIGHT("AUTO_LEFT_RIGHT", "自动-左一或右一"),
    NONE("NONE", "无目标");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    /**
     * 是否需要手动选择目标
     */
    public boolean isManual() {
        return this == MANUAL_PLAYER || this == MANUAL_PLAYER_ROLE;
    }

    /**
     * 是否自动选择目标
     */
    public boolean isAuto() {
        return this.code.startsWith("AUTO_");
    }
}
