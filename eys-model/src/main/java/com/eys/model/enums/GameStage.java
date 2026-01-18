package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游戏阶段
 */
@Getter
@AllArgsConstructor
public enum GameStage {

    START("START", "开始阶段"),
    NIGHT("NIGHT", "夜晚阶段"),
    VOTE("VOTE", "投票阶段"),
    DAY("DAY", "白天阶段");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
