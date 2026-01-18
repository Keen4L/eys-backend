package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游戏状态
 */
@Getter
@AllArgsConstructor
public enum GameStatus {

    WAITING("WAITING", "等待中"),
    PLAYING("PLAYING", "进行中"),
    FINISHED("FINISHED", "已结束"),
    CLOSED("CLOSED", "已关闭");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
