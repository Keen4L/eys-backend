package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 技能失效条件
 */
@Getter
@AllArgsConstructor
public enum InvalidCondition {

    /**
     * 守护成功后失效
     * 判断逻辑：上回合使用了该技能，且目标在本回合被刀（ActionLog 中有技能名含"刀"的记录）
     */
    GUARD_SUCCESS("GUARD_SUCCESS", "守护成功");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
