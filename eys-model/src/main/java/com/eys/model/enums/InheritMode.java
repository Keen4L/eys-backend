package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 继承模式
 */
@Getter
@AllArgsConstructor
public enum InheritMode {

    /**
     * 继承目标所有技能
     */
    ALL("ALL", "继承目标所有技能"),

    /**
     * 条件继承：目标是鹅则继承全部技能，否则继承一刀
     * 用于殡仪鹅的挖尸技能
     */
    GOOSE_ALL_ELSE_KNIFE("GOOSE_ALL_ELSE_KNIFE", "鹅继承全部，否则继承刀");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
