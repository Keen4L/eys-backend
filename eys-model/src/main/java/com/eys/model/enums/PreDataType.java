package com.eys.model.enums;

/**
 * 预数据类型（技能推送时携带的上下文数据）
 * 用于 SkillEffectConfig.DataRule.preDataType
 */
public enum PreDataType {

    /**
     * 无预数据
     */
    NONE,

    /**
     * 上回合票数最高玩家
     * 适用：医生·注射
     */
    TOP_VOTED_PLAYER
}
