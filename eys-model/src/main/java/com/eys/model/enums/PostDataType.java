package com.eys.model.enums;

/**
 * 结果数据类型（技能使用后回传的结果）
 * 用于 SkillEffectConfig.DataRule.postDataType
 */
public enum PostDataType {

    /**
     * 无结果数据
     */
    NONE,

    /**
     * 目标阵营（鹅/鸭/中立）
     * 适用：先知鹅·查验身份、大白鹅·查验阵营
     */
    TARGET_CAMP,

    /**
     * 目标角色名
     * 预留
     */
    TARGET_ROLE,

    /**
     * 两目标是否同阵营
     * 适用：等式鹅
     */
    SAME_CAMP
}
