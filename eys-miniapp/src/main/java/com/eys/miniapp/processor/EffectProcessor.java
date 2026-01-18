package com.eys.miniapp.processor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.mapper.*;
import com.eys.model.json.SkillEffectConfig;
import com.eys.model.json.TagInfo;
import com.eys.model.entity.*;
import com.eys.model.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 技能效果处理器
 * 根据技能配置执行效果：TAG（添加标签）、INHERIT（继承技能）、NONE（纯记录）
 */
@Component
@RequiredArgsConstructor
public class EffectProcessor {

    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaSkillInstanceMapper gaSkillInstanceMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final CfgSkillMapper cfgSkillMapper;
    private final CfgRoleMapper cfgRoleMapper;
    private final ActionLogHelper actionLogHelper;

    /**
     * 执行技能效果
     *
     * @param skill     技能配置
     * @param player    使用者
     * @param targetIds 目标ID列表
     * @param record    游戏记录
     */
    public void applyEffect(CfgSkill skill, GaGamePlayer player, List<Long> targetIds, GaGameRecord record) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getEffectRule() == null) {
            return;
        }

        SkillEffectConfig.EffectRule effectRule = config.getEffectRule();
        EffectType type = effectRule.getType();

        if (type == null || type == EffectType.NONE) {
            // NONE：纯记录，不执行任何系统效果
            return;
        }

        if (type == EffectType.TAG) {
            applyTagEffect(skill, player, targetIds, record, effectRule);
        } else if (type == EffectType.INHERIT) {
            applyInheritEffect(player, targetIds, effectRule);
        }
    }

    /**
     * 应用标签效果
     */
    private void applyTagEffect(CfgSkill skill, GaGamePlayer player, List<Long> targetIds,
            GaGameRecord record, SkillEffectConfig.EffectRule effectRule) {
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }

        List<TagEffect> tagEffects = effectRule.getTagEffects();
        TagExpiry expiry = effectRule.getTagExpiry();

        if (tagEffects == null || tagEffects.isEmpty()) {
            return;
        }

        // 为每个目标添加标签
        for (Long targetId : targetIds) {
            GaPlayerStatus status = gaPlayerStatusMapper.selectById(targetId);
            if (status == null) {
                continue;
            }

            // 创建标签
            TagInfo tag = new TagInfo();
            tag.setSkillId(skill.getId());
            tag.setSkillName(skill.getName());
            tag.setSourcePlayerId(player.getId());
            tag.setAppliedRound(record.getCurrentRound());
            tag.setExpiry(expiry != null ? expiry : TagExpiry.NEXT_ROUND);
            tag.setEffects(tagEffects);

            // 添加到玩家状态
            List<TagInfo> activeTags = status.getActiveTags();
            if (activeTags == null) {
                activeTags = new ArrayList<>();
            }
            activeTags.add(tag);
            status.setActiveTags(activeTags);
            status.setUpdatedAt(LocalDateTime.now());
            gaPlayerStatusMapper.updateById(status);
        }
    }

    /**
     * 应用继承效果（如殡仪鹅挖尸、鹦鹉模仿）
     * 
     * ALL 模式：复制目标的技能给使用者
     * GOOSE_ALL_ELSE_KNIFE 模式：
     * - 目标是鹅：继承目标全部技能
     * - 目标非鹅：给使用者自己的刀 usedCount -1（使刀可用）
     */
    private void applyInheritEffect(GaGamePlayer player, List<Long> targetIds,
            SkillEffectConfig.EffectRule effectRule) {
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }

        InheritMode mode = effectRule.getInheritMode();
        if (mode == null) {
            mode = InheritMode.ALL;
        }

        for (Long targetId : targetIds) {
            GaGamePlayer target = gaGamePlayerMapper.selectById(targetId);
            if (target == null || target.getRoleId() == null) {
                continue;
            }

            if (mode == InheritMode.ALL) {
                // ALL 模式：直接复制目标所有技能
                inheritAllSkills(player, target);
            } else if (mode == InheritMode.GOOSE_ALL_ELSE_KNIFE) {
                // 检查目标阵营
                CfgRole targetRole = cfgRoleMapper.selectById(target.getRoleId());
                if (targetRole != null && targetRole.getCampType() == CampType.GOOSE) {
                    // 目标是鹅：继承目标全部技能
                    inheritAllSkills(player, target);
                } else {
                    // 目标非鹅：给使用者自己的刀增加一次使用机会
                    grantKnifeUsage(player);
                }
            }
        }
    }

    /**
     * 继承目标的所有技能
     */
    private void inheritAllSkills(GaGamePlayer player, GaGamePlayer target) {
        List<CfgSkill> targetSkills = cfgSkillMapper.selectList(
                new LambdaQueryWrapper<CfgSkill>().eq(CfgSkill::getRoleId, target.getRoleId()));

        for (CfgSkill skill : targetSkills) {
            // 检查是否已有该技能
            GaSkillInstance existing = gaSkillInstanceMapper.selectOne(
                    new LambdaQueryWrapper<GaSkillInstance>()
                            .eq(GaSkillInstance::getGamePlayerId, player.getId())
                            .eq(GaSkillInstance::getSkillId, skill.getId()));
            if (existing != null) {
                continue;
            }

            GaSkillInstance instance = new GaSkillInstance();
            instance.setGamePlayerId(player.getId());
            instance.setSkillId(skill.getId());
            instance.setIsActive(true);
            instance.setUsedCount(0);
            gaSkillInstanceMapper.insert(instance);
        }
    }

    /**
     * 给使用者自己的刀增加一次使用机会
     * 实现方式：找到使用者的刀技能（EffectType=NONE 且 totalMax=0 的技能），将 usedCount -1
     */
    private void grantKnifeUsage(GaGamePlayer player) {
        // 获取使用者的所有技能实例
        List<GaSkillInstance> instances = gaSkillInstanceMapper.selectList(
                new LambdaQueryWrapper<GaSkillInstance>()
                        .eq(GaSkillInstance::getGamePlayerId, player.getId()));

        for (GaSkillInstance instance : instances) {
            CfgSkill skill = cfgSkillMapper.selectById(instance.getSkillId());
            if (skill == null)
                continue;

            SkillEffectConfig config = skill.getSkillConfig();
            if (config == null)
                continue;

            // 判断是否为刀技能：EffectType=NONE 且 totalMax=0
            SkillEffectConfig.EffectRule effectRule = config.getEffectRule();
            SkillEffectConfig.LimitRule limitRule = config.getLimitRule();

            boolean isKnife = effectRule != null
                    && effectRule.getType() == EffectType.NONE
                    && limitRule != null
                    && limitRule.getTotalMax() != null
                    && limitRule.getTotalMax() == 0;

            if (isKnife) {
                // 找到刀技能，usedCount -1 使其可用
                instance.setUsedCount(instance.getUsedCount() - 1);
                gaSkillInstanceMapper.updateById(instance);
                break; // 只处理一个刀
            }
        }
    }

    // ==================== 标签效果检查 ====================

    /**
     * 检查玩家是否有指定效果的标签
     */
    public boolean hasTagEffect(Long gamePlayerId, TagEffect effect) {
        GaPlayerStatus status = gaPlayerStatusMapper.selectById(gamePlayerId);
        if (status == null || status.getActiveTags() == null) {
            return false;
        }

        return status.getActiveTags().stream()
                .anyMatch(tag -> tag.getEffects() != null && tag.getEffects().contains(effect));
    }

    /**
     * 检查是否应该推送技能给玩家（无 BLOCK_SKILL 标签）
     */
    public boolean shouldPushSkill(Long gamePlayerId) {
        return !hasTagEffect(gamePlayerId, TagEffect.BLOCK_SKILL);
    }

    /**
     * 检查是否应该推送投票给玩家（无 BLOCK_VOTE 标签）
     */
    public boolean shouldPushVote(Long gamePlayerId) {
        return !hasTagEffect(gamePlayerId, TagEffect.BLOCK_VOTE);
    }

    // ==================== 标签生命周期 ====================

    /**
     * 移除指定索引的标签
     */
    public void removeTag(Long gamePlayerId, int tagIndex) {
        GaPlayerStatus status = gaPlayerStatusMapper.selectById(gamePlayerId);
        if (status == null || status.getActiveTags() == null) {
            return;
        }

        if (tagIndex >= 0 && tagIndex < status.getActiveTags().size()) {
            status.getActiveTags().remove(tagIndex);
            status.setUpdatedAt(LocalDateTime.now());
            gaPlayerStatusMapper.updateById(status);
        }
    }

    /**
     * 玩家死亡时清除所有标签
     */
    public void clearAllTags(Long gamePlayerId) {
        GaPlayerStatus status = gaPlayerStatusMapper.selectById(gamePlayerId);
        if (status == null) {
            return;
        }

        status.setActiveTags(new ArrayList<>());
        status.setUpdatedAt(LocalDateTime.now());
        gaPlayerStatusMapper.updateById(status);
    }

    // ==================== 技能失效条件检查 ====================

    /**
     * 检查技能是否因特定条件失效
     * 根据 limitRule.invalidCondition 判断
     *
     * @param instance     技能实例
     * @param skill        技能配置
     * @param gameId       对局ID
     * @param currentRound 当前回合
     * @return true 表示已失效
     */
    public boolean checkInvalidCondition(GaSkillInstance instance, CfgSkill skill, Long gameId, Integer currentRound) {
        if (!instance.getIsActive()) {
            return true; // 已手动标记失效
        }

        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getLimitRule() == null) {
            return false;
        }

        InvalidCondition condition = config.getLimitRule().getInvalidCondition();
        if (condition == null) {
            return false; // 无失效条件
        }

        return evaluateInvalidCondition(condition, instance, skill.getId(), gameId, currentRound);
    }

    /**
     * 评估失效条件
     * GUARD_SUCCESS（守护成功）：上回合使用了此技能，且目标被刀了
     */
    private boolean evaluateInvalidCondition(InvalidCondition condition, GaSkillInstance instance,
            Long skillId, Long gameId, Integer currentRound) {
        if (condition == InvalidCondition.GUARD_SUCCESS) {
            return actionLogHelper.checkGuardSuccess(instance.getGamePlayerId(), skillId, gameId, currentRound);
        }
        return false;
    }

    /**
     * 标记技能为失效
     */
    public void invalidateSkill(Long skillInstanceId) {
        GaSkillInstance instance = gaSkillInstanceMapper.selectById(skillInstanceId);
        if (instance != null) {
            instance.setIsActive(false);
            gaSkillInstanceMapper.updateById(instance);
        }
    }
}
