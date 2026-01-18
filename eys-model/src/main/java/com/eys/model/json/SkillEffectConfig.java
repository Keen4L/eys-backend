package com.eys.model.json;

import com.eys.model.enums.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * 技能效果配置（对应 cfg_skill.skill_config JSON）
 */
@Data
@Slf4j
public class SkillEffectConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 从 JSON 字符串解析配置
     */
    public static SkillEffectConfig parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, SkillEffectConfig.class);
        } catch (Exception e) {
            log.warn("解析 SkillEffectConfig JSON 失败: {}, 原因: {}", json, e.getMessage());
            return null;
        }
    }

    /**
     * 触发规则 → TriggerProcessor
     */
    private TriggerRule triggerRule;

    /**
     * 限制规则 → LimitProcessor
     */
    private LimitRule limitRule;

    /**
     * 目标规则 → TargetProcessor
     */
    private TargetRule targetRule;

    /**
     * 效果规则 → EffectProcessor
     */
    private EffectRule effectRule;

    /**
     * 数据规则 → DataProcessor
     */
    private DataRule dataRule;

    // ==================== 内部类 ====================

    /**
     * 触发规则配置
     */
    @Data
    public static class TriggerRule implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 触发方式：DM_PUSH DM手动推送 / DM_EXECUTE DM代为执行
         */
        private TriggerMode mode;

        /**
         * 是否为普通技能（仅DM_EXECUTE模式有效，给前端的信息）
         * true - 普通技能，DM 代为执行
         * false/null - 特殊技能
         */
        private Boolean isNormal;
    }

    /**
     * 使用限制规则配置
     */
    @Data
    public static class LimitRule implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 全局最大次数（-1 表示无限）
         */
        private Integer totalMax;

        /**
         * 最早可用回合
         */
        private Integer minRound;

        /**
         * 技能失效条件
         */
        private InvalidCondition invalidCondition;
    }

    /**
     * 目标规则配置
     */
    @Data
    public static class TargetRule implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 目标选择方式
         */
        private TargetSelectMode selectMode;

        /**
         * 目标存活状态要求
         */
        private AliveState aliveState;

        /**
         * 是否排除自己
         */
        private Boolean excludeSelf;

        /**
         * 目标数量范围 [min, max]，-1 表示不限
         */
        private List<Integer> countRange;

        /**
         * 额外限制条件
         */
        private TargetRestriction restriction;
    }

    /**
     * 效果规则配置
     */
    @Data
    public static class EffectRule implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 效果类型：TAG-添加标签 / INHERIT-继承技能 / NONE-无系统效果（纯记录）
         */
        private EffectType type;

        /**
         * 标签效果列表（type=TAG 时有效）
         */
        private List<TagEffect> tagEffects;

        /**
         * 标签过期时机（type=TAG 时有效）
         */
        private TagExpiry tagExpiry;

        /**
         * 继承模式（type=INHERIT 时有效）
         * ALL - 继承目标所有技能
         * GOOSE_ALL_ELSE_KNIFE - 鹅继承全部，否则继承刀
         */
        private InheritMode inheritMode;
    }

    /**
     * 数据规则配置 → DataProcessor
     */
    @Data
    public static class DataRule implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 推送时预加载的数据类型
         * 如：TOP_VOTED_PLAYER（医生注射时携带上回合票最高者）
         */
        private PreDataType preDataType;

        /**
         * 使用后回传的结果类型
         * 如：TARGET_CAMP（先知查验回传目标阵营）
         */
        private PostDataType postDataType;
    }
}
