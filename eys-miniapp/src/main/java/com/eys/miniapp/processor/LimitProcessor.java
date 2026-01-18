package com.eys.miniapp.processor;

import com.eys.model.json.SkillEffectConfig;
import com.eys.model.entity.CfgSkill;
import com.eys.model.entity.GaGameRecord;
import com.eys.model.entity.GaSkillInstance;
import com.eys.model.enums.InvalidCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 技能使用限制处理器
 * 负责所有与技能可用性相关的检查：次数限制、回合限制、失效条件
 */
@Component
@RequiredArgsConstructor
public class LimitProcessor {

    private final ActionLogHelper actionLogHelper;

    /**
     * 检查技能是否可用
     *
     * @param skill    技能配置
     * @param instance 技能实例
     * @param record   游戏记录
     * @return null 表示可用，否则返回不可用原因
     */
    public String checkLimit(CfgSkill skill, GaSkillInstance instance, GaGameRecord record) {
        // 1. 检查技能是否已手动标记失效
        if (!instance.getIsActive()) {
            return "技能已失效";
        }

        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getLimitRule() == null) {
            return null;
        }

        SkillEffectConfig.LimitRule limit = config.getLimitRule();

        // 2. 检查最大使用次数（支持负数 usedCount，用于殡仪鹅刀）
        if (limit.getTotalMax() != null && limit.getTotalMax() >= 0) {
            if (instance.getUsedCount() >= limit.getTotalMax()) {
                return "技能使用次数已达上限";
            }
        }

        // 3. 检查最早可用回合
        if (limit.getMinRound() != null && limit.getMinRound() > 0) {
            if (record.getCurrentRound() < limit.getMinRound()) {
                return "技能需在第 " + limit.getMinRound() + " 回合后才能使用";
            }
        }

        // 4. 检查失效条件（如守护成功后失效）
        if (limit.getInvalidCondition() != null) {
            if (checkInvalidCondition(limit.getInvalidCondition(), instance, skill.getId(), record)) {
                return "技能因特定条件已失效";
            }
        }

        return null;
    }

    /**
     * 检查失效条件是否触发
     */
    private boolean checkInvalidCondition(InvalidCondition condition, GaSkillInstance instance,
            Long skillId, GaGameRecord record) {
        if (condition == InvalidCondition.GUARD_SUCCESS) {
            return actionLogHelper.checkGuardSuccess(
                    instance.getGamePlayerId(), skillId, record.getId(), record.getCurrentRound());
        }
        return false;
    }

    /**
     * 获取本回合已使用次数
     */
    public int getUsedCountInRound(Long gamePlayerId, Long skillId, Long gameId, Integer roundNo) {
        return actionLogHelper.getSkillUsedCountInRound(gamePlayerId, skillId, gameId, roundNo);
    }
}

