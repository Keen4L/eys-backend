package com.eys.miniapp.processor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.mapper.CfgSkillMapper;
import com.eys.mapper.GaActionLogMapper;
import com.eys.model.entity.CfgSkill;
import com.eys.model.entity.GaActionLog;
import com.eys.model.enums.ActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 动作日志查询工具
 * 提供对 GaActionLog 的常用查询操作，供 Processor 层复用
 * 
 * 职责：
 * 1. 查询技能使用记录
 * 2. 判断"被刀"等常见场景
 * 3. 检查守护等条件性逻辑
 */
@Component
@RequiredArgsConstructor
public class ActionLogHelper {

    private final GaActionLogMapper gaActionLogMapper;
    private final CfgSkillMapper cfgSkillMapper;

    /**
     * 检查玩家在指定回合是否被"刀"了
     * 判断依据：ActionLog 中有技能名包含"刀"的记录，且目标包含该玩家
     *
     * @param targetPlayerId 目标玩家ID
     * @param gameId         对局ID
     * @param roundNo        回合数
     * @return true 表示被刀了
     */
    public boolean wasKnifedInRound(Long targetPlayerId, Long gameId, Integer roundNo) {
        List<GaActionLog> skillActions = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .eq(GaActionLog::getRoundNo, roundNo)
                        .eq(GaActionLog::getActionType, ActionType.SKILL));

        for (GaActionLog action : skillActions) {
            if (action.getTargetIds() == null || !action.getTargetIds().contains(targetPlayerId)) {
                continue;
            }

            if (action.getSkillId() != null) {
                CfgSkill skill = cfgSkillMapper.selectById(action.getSkillId());
                if (skill != null && skill.getName() != null && skill.getName().contains("刀")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查守护是否成功（用于技能失效条件判断）
     * 逻辑：上回合使用了指定技能守护某目标 AND 该目标在本回合被刀了
     *
     * @param gamePlayerId 使用守护技能的玩家ID
     * @param skillId      守护技能的配置ID
     * @param gameId       对局ID
     * @param currentRound 当前回合数
     * @return true 表示守护成功（技能应失效）
     */
    public boolean checkGuardSuccess(Long gamePlayerId, Long skillId, Long gameId, Integer currentRound) {
        if (currentRound == null || currentRound < 2) {
            return false; // 第 1 回合没有"上回合"
        }

        // 1. 查找上回合使用该技能的记录
        List<GaActionLog> lastRoundActions = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .eq(GaActionLog::getRoundNo, currentRound - 1)
                        .eq(GaActionLog::getActionType, ActionType.SKILL)
                        .eq(GaActionLog::getInitiatorId, gamePlayerId)
                        .eq(GaActionLog::getSkillId, skillId));

        if (lastRoundActions.isEmpty()) {
            return false; // 上回合没有使用该技能
        }

        // 2. 检查被守护的目标在本回合是否被刀
        for (GaActionLog guardAction : lastRoundActions) {
            List<Long> guardedTargets = guardAction.getTargetIds();
            if (guardedTargets == null || guardedTargets.isEmpty()) {
                continue;
            }

            for (Long targetId : guardedTargets) {
                if (wasKnifedInRound(targetId, gameId, currentRound)) {
                    return true; // 守护成功，技能失效
                }
            }
        }

        return false;
    }

    /**
     * 获取指定回合内技能的使用次数
     *
     * @param gamePlayerId 玩家ID
     * @param skillId      技能ID
     * @param gameId       对局ID
     * @param roundNo      回合数
     * @return 使用次数
     */
    public int getSkillUsedCountInRound(Long gamePlayerId, Long skillId, Long gameId, Integer roundNo) {
        Long count = gaActionLogMapper.selectCount(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .eq(GaActionLog::getRoundNo, roundNo)
                        .eq(GaActionLog::getActionType, ActionType.SKILL)
                        .eq(GaActionLog::getInitiatorId, gamePlayerId)
                        .eq(GaActionLog::getSkillId, skillId));
        return count != null ? count.intValue() : 0;
    }
}
