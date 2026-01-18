package com.eys.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户权限类型
 */
@Getter
@AllArgsConstructor
public enum RoleType {

    PLAYER("PLAYER", "普通玩家"),
    DM("DM", "主持人"),
    ADMIN("ADMIN", "管理员");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;
}
