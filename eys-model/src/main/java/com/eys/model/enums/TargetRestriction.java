package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 目标额外限制条件
 */
@Getter
@AllArgsConstructor
public enum TargetRestriction {

    NO_REPEAT_TARGET("NO_REPEAT_TARGET", "不可重复选择同一目标"),
    NO_CONSECUTIVE_SAME_TARGET("NO_CONSECUTIVE_SAME_TARGET", "不可连续两回合选择同一目标");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
