package com.eys.miniapp.processor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.mapper.*;
import com.eys.model.json.SkillEffectConfig;
import com.eys.model.entity.*;
import com.eys.model.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 技能目标处理器
 * 负责所有目标相关逻辑：获取可选目标、验证目标、自动目标解析、目标限制校验、构建目标选项 VO
 */
@Component
@RequiredArgsConstructor
public class TargetProcessor {

    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final CfgRoleMapper cfgRoleMapper;

    // ==================== 目标选择模式 ====================

    /**
     * 获取技能的目标选择模式
     */
    public TargetSelectMode getSelectMode(CfgSkill skill) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getTargetRule() == null) {
            return TargetSelectMode.NONE;
        }
        TargetSelectMode mode = config.getTargetRule().getSelectMode();
        return mode != null ? mode : TargetSelectMode.NONE;
    }

    // ==================== 获取可选目标 ====================

    /**
     * 获取可选目标列表（用于前端展示）
     */
    public List<GaGamePlayer> getAvailableTargets(CfgSkill skill, GaGamePlayer player, GaGameRecord record) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getTargetRule() == null) {
            return new ArrayList<>();
        }

        SkillEffectConfig.TargetRule rule = config.getTargetRule();
        TargetSelectMode mode = rule.getSelectMode();

        // 无目标技能
        if (mode == null || mode == TargetSelectMode.NONE) {
            return new ArrayList<>();
        }

        // 自动目标模式
        if (mode.isAuto()) {
            return getAutoTargets(mode, player, record);
        }

        // 手动选择模式：应用过滤条件
        List<GaGamePlayer> allPlayers = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId())
                        .orderByAsc(GaGamePlayer::getSeatNo));

        return allPlayers.stream()
                .filter(p -> filterByBasicRules(p, player.getId(), rule))
                .filter(p -> filterByRestriction(p, player.getId(), skill.getId(), record, rule))
                .collect(Collectors.toList());
    }

    /**
     * 构建可选目标 ID 列表（精简版本 - 仅返回 gamePlayerId）
     */
    public List<Long> buildTargetPlayerIds(CfgSkill skill, GaGamePlayer player, GaGameRecord record) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getTargetRule() == null) {
            return new ArrayList<>();
        }

        SkillEffectConfig.TargetRule rule = config.getTargetRule();
        // 非手动选择模式不需要构建选项
        if (rule.getSelectMode() == null || !rule.getSelectMode().isManual()) {
            return new ArrayList<>();
        }

        // 获取可选目标
        List<GaGamePlayer> availableTargets = getAvailableTargets(skill, player, record);

        // 仅返回 ID 列表
        return availableTargets.stream()
                .map(GaGamePlayer::getId)
                .collect(Collectors.toList());
    }

    // ==================== 自动目标解析 ====================

    /**
     * 获取自动目标
     */
    private List<GaGamePlayer> getAutoTargets(TargetSelectMode mode, GaGamePlayer player, GaGameRecord record) {
        List<GaGamePlayer> allPlayers = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId()));

        switch (mode) {
            case AUTO_ATTACKER:
                // 获取本回合攻击该玩家的人（从 ActionLog 查询）
                return getAttackers(player.getId(), record.getId(), record.getCurrentRound());

            case AUTO_ALIVE_DUCK:
                // 存活的鸭子
                return allPlayers.stream()
                        .filter(p -> isAlive(p.getId()))
                        .filter(p -> getCampType(p.getRoleId()) == CampType.DUCK)
                        .collect(Collectors.toList());

            case AUTO_ALIVE_GOOSE:
                // 存活的鹅
                return allPlayers.stream()
                        .filter(p -> isAlive(p.getId()))
                        .filter(p -> getCampType(p.getRoleId()) == CampType.GOOSE)
                        .collect(Collectors.toList());

            case AUTO_LEFT_RIGHT:
                // 左一或右一（相邻座位的存活玩家）
                return getAdjacentPlayers(player, allPlayers);

            default:
                return new ArrayList<>();
        }
    }

    /**
     * 获取攻击者列表（本回合刀了该玩家的人）
     */
    private List<GaGamePlayer> getAttackers(Long targetId, Long gameId, Integer roundNo) {
        List<GaActionLog> attacks = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .eq(GaActionLog::getRoundNo, roundNo)
                        .eq(GaActionLog::getActionType, ActionType.SKILL));

        List<Long> attackerIds = new ArrayList<>();
        for (GaActionLog attack : attacks) {
            if (attack.getTargetIds() != null && attack.getTargetIds().contains(targetId)) {
                attackerIds.add(attack.getInitiatorId());
            }
        }

        if (attackerIds.isEmpty()) {
            return new ArrayList<>();
        }

        return gaGamePlayerMapper.selectBatchIds(attackerIds);
    }

    /**
     * 获取相邻座位的存活玩家（左一或右一）
     */
    private List<GaGamePlayer> getAdjacentPlayers(GaGamePlayer player, List<GaGamePlayer> allPlayers) {
        // 按座位号排序的存活玩家
        List<GaGamePlayer> alivePlayers = allPlayers.stream()
                .filter(p -> isAlive(p.getId()))
                .sorted((a, b) -> a.getSeatNo() - b.getSeatNo())
                .collect(Collectors.toList());

        if (alivePlayers.size() <= 1) {
            return new ArrayList<>();
        }

        int playerIndex = -1;
        for (int i = 0; i < alivePlayers.size(); i++) {
            if (alivePlayers.get(i).getId().equals(player.getId())) {
                playerIndex = i;
                break;
            }
        }

        if (playerIndex == -1) {
            return new ArrayList<>();
        }

        List<GaGamePlayer> result = new ArrayList<>();
        // 左一
        int leftIndex = (playerIndex - 1 + alivePlayers.size()) % alivePlayers.size();
        result.add(alivePlayers.get(leftIndex));
        // 右一
        int rightIndex = (playerIndex + 1) % alivePlayers.size();
        if (rightIndex != leftIndex) {
            result.add(alivePlayers.get(rightIndex));
        }

        return result;
    }

    // ==================== 目标验证 ====================

    /**
     * 验证目标是否有效
     * 
     * @return null 表示验证通过，否则返回错误信息
     */
    public String validateTargets(CfgSkill skill, GaGamePlayer player, List<Long> targetIds, GaGameRecord record) {
        SkillEffectConfig config = skill.getSkillConfig();
        TargetSelectMode mode = getSelectMode(skill);

        // 无目标技能
        if (mode == TargetSelectMode.NONE) {
            return null;
        }

        // 检查目标是否为空
        if (targetIds == null || targetIds.isEmpty()) {
            SkillEffectConfig.TargetRule rule = config.getTargetRule();
            if (rule.getCountRange() != null && rule.getCountRange().size() >= 1) {
                int minCount = rule.getCountRange().get(0);
                if (minCount > 0) {
                    return "至少需要选择 " + minCount + " 个目标";
                }
            }
            return null; // 允许 0 个目标
        }

        SkillEffectConfig.TargetRule rule = config.getTargetRule();

        // 检查目标数量
        if (rule.getCountRange() != null && rule.getCountRange().size() >= 2) {
            int minCount = rule.getCountRange().get(0);
            int maxCount = rule.getCountRange().get(1);
            if (targetIds.size() < minCount) {
                return "至少需要选择 " + minCount + " 个目标";
            }
            if (maxCount > 0 && targetIds.size() > maxCount) {
                return "最多只能选择 " + maxCount + " 个目标";
            }
        }

        // 检查目标是否在可选列表中
        List<GaGamePlayer> available = getAvailableTargets(skill, player, record);
        List<Long> availableIds = available.stream().map(GaGamePlayer::getId).collect(Collectors.toList());

        for (Long targetId : targetIds) {
            if (!availableIds.contains(targetId)) {
                return "目标不在可选范围内";
            }
        }

        // 检查 TargetRestriction
        String restrictionError = checkRestriction(rule.getRestriction(), player.getId(), skill.getId(),
                targetIds, record);
        if (restrictionError != null) {
            return restrictionError;
        }

        return null;
    }

    // ==================== 目标限制校验 ====================

    /**
     * 检查目标限制
     */
    private String checkRestriction(TargetRestriction restriction, Long playerId, Long skillId,
            List<Long> targetIds, GaGameRecord record) {
        if (restriction == null) {
            return null;
        }

        switch (restriction) {
            case NO_REPEAT_TARGET:
                // 不能重复选择之前选过的目标
                return checkNoRepeatTarget(playerId, skillId, targetIds, record.getId());

            case NO_CONSECUTIVE_SAME_TARGET:
                // 不能连续两回合选择同一目标
                return checkNoConsecutiveSameTarget(playerId, skillId, targetIds, record);

            default:
                return null;
        }
    }

    /**
     * 检查不重复目标限制
     * 全局不能重复选择之前选过的目标
     */
    private String checkNoRepeatTarget(Long playerId, Long skillId, List<Long> targetIds, Long gameId) {
        // 获取该技能之前所有使用记录的目标
        List<GaActionLog> history = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .eq(GaActionLog::getActionType, ActionType.SKILL)
                        .eq(GaActionLog::getInitiatorId, playerId)
                        .eq(GaActionLog::getSkillId, skillId));

        for (GaActionLog log : history) {
            if (log.getTargetIds() != null) {
                for (Long targetId : targetIds) {
                    if (log.getTargetIds().contains(targetId)) {
                        return "不能重复选择之前已选过的目标";
                    }
                }
            }
        }

        return null;
    }

    /**
     * 检查不连续同目标限制
     * 不能连续两回合选择同一目标
     */
    private String checkNoConsecutiveSameTarget(Long playerId, Long skillId, List<Long> targetIds,
            GaGameRecord record) {
        if (record.getCurrentRound() == null || record.getCurrentRound() < 2) {
            return null; // 第 1 回合无限制
        }

        // 获取上回合该技能的使用记录
        List<GaActionLog> lastRoundLogs = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, record.getId())
                        .eq(GaActionLog::getRoundNo, record.getCurrentRound() - 1)
                        .eq(GaActionLog::getActionType, ActionType.SKILL)
                        .eq(GaActionLog::getInitiatorId, playerId)
                        .eq(GaActionLog::getSkillId, skillId));

        for (GaActionLog log : lastRoundLogs) {
            if (log.getTargetIds() != null) {
                for (Long targetId : targetIds) {
                    if (log.getTargetIds().contains(targetId)) {
                        return "不能连续两回合选择同一目标";
                    }
                }
            }
        }

        return null;
    }

    // ==================== 辅助方法 ====================

    /**
     * 基础规则过滤（排除自己、存活状态）
     */
    private boolean filterByBasicRules(GaGamePlayer target, Long selfId, SkillEffectConfig.TargetRule rule) {
        // 排除自己
        if (Boolean.TRUE.equals(rule.getExcludeSelf()) && target.getId().equals(selfId)) {
            return false;
        }

        // 存活状态过滤
        if (rule.getAliveState() != null) {
            boolean isAlive = isAlive(target.getId());
            AliveState required = rule.getAliveState();
            if (required == AliveState.ALIVE && !isAlive)
                return false;
            if (required == AliveState.DEAD && isAlive)
                return false;
        }

        return true;
    }

    /**
     * 限制条件过滤（提前过滤不可选的目标）
     */
    private boolean filterByRestriction(GaGamePlayer target, Long selfId, Long skillId,
            GaGameRecord record, SkillEffectConfig.TargetRule rule) {
        TargetRestriction restriction = rule.getRestriction();
        if (restriction == null) {
            return true;
        }

        switch (restriction) {
            case NO_REPEAT_TARGET:
                // 检查该目标是否之前选过
                return !hasTargetedBefore(selfId, skillId, target.getId(), record.getId());

            case NO_CONSECUTIVE_SAME_TARGET:
                // 检查上回合是否选过该目标
                return !hasTargetedLastRound(selfId, skillId, target.getId(), record);

            default:
                return true;
        }
    }

    private boolean hasTargetedBefore(Long playerId, Long skillId, Long targetId, Long gameId) {
        List<GaActionLog> history = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .eq(GaActionLog::getActionType, ActionType.SKILL)
                        .eq(GaActionLog::getInitiatorId, playerId)
                        .eq(GaActionLog::getSkillId, skillId));

        for (GaActionLog log : history) {
            if (log.getTargetIds() != null && log.getTargetIds().contains(targetId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTargetedLastRound(Long playerId, Long skillId, Long targetId, GaGameRecord record) {
        if (record.getCurrentRound() == null || record.getCurrentRound() < 2) {
            return false;
        }

        List<GaActionLog> lastRoundLogs = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, record.getId())
                        .eq(GaActionLog::getRoundNo, record.getCurrentRound() - 1)
                        .eq(GaActionLog::getActionType, ActionType.SKILL)
                        .eq(GaActionLog::getInitiatorId, playerId)
                        .eq(GaActionLog::getSkillId, skillId));

        for (GaActionLog log : lastRoundLogs) {
            if (log.getTargetIds() != null && log.getTargetIds().contains(targetId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlive(Long gamePlayerId) {
        GaPlayerStatus status = gaPlayerStatusMapper.selectById(gamePlayerId);
        return status != null && status.getIsAlive();
    }

    private CampType getCampType(Long roleId) {
        if (roleId == null)
            return null;
        CfgRole role = cfgRoleMapper.selectById(roleId);
        return role != null ? role.getCampType() : null;
    }
}
