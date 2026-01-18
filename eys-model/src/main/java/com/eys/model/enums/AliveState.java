package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 目标存活状态要求
 */
@Getter
@AllArgsConstructor
public enum AliveState {

    ALIVE("ALIVE", "仅限存活玩家"),
    DEAD("DEAD", "仅限死亡玩家"),
    ANY("ANY", "不限");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
