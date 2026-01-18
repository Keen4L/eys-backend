package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 技能触发方式
 */
@Getter
@AllArgsConstructor
public enum TriggerMode {

    DM_PUSH("DM_PUSH", "DM手动推送"),
    DM_EXECUTE("DM_EXECUTE", "DM代为执行");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
