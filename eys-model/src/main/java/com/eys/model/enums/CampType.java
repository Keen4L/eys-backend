package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 阵营类型
 */
@Getter
@AllArgsConstructor
public enum CampType {

    GOOSE("GOOSE", "鹅阵营"),
    DUCK("DUCK", "鸭阵营"),
    NEUTRAL("NEUTRAL", "中立阵营");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
