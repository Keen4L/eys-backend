package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标签过期时机
 */
@Getter
@AllArgsConstructor
public enum TagExpiry {

    NEXT_ROUND("NEXT_ROUND", "过1回合后过期"),
    AFTER_2_ROUND("AFTER_2_ROUND", "过2回合后过期"),
    PERMANENT("PERMANENT", "永久生效");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
