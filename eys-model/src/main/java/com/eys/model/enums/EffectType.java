package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 技能效果类型
 */
@Getter
@AllArgsConstructor
public enum EffectType {

    TAG("TAG", "添加状态标签"),
    INHERIT("INHERIT", "继承技能"),
    NONE("NONE", "无系统效果（纯记录）");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
