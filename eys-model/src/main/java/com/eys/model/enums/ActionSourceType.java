package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 动作来源类型
 */
@Getter
@AllArgsConstructor
public enum ActionSourceType {

    DM_PUSH("DM_PUSH", "DM手动推送"),
    DM_EXECUTE("DM_EXECUTE", "DM代为执行"),
    PLAYER_ACTION("PLAYER_ACTION", "玩家自主操作");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
