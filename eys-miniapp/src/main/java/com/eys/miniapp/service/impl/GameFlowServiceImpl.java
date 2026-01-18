package com.eys.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.common.utils.AssertUtils;
import com.eys.mapper.*;
import com.eys.miniapp.processor.*;
import com.eys.miniapp.service.GameFlowService;
import com.eys.miniapp.service.GameStatePushService;
import com.eys.model.entity.*;
import com.eys.model.enums.*;
import com.eys.model.json.SkillEffectConfig;
import com.eys.model.vo.game.DmPushVO;
import com.eys.model.vo.game.GameEndResultVO;
import com.eys.model.vo.game.PlayerSkillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏流程控制服务实现
 * 负责游戏核心流程：开始游戏、阶段推进、状态获取、击杀/复活、结束游戏
 */
@Service
@RequiredArgsConstructor
public class GameFlowServiceImpl implements GameFlowService {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaSkillInstanceMapper gaSkillInstanceMapper;
    private final GaPlayerSpawnMapper gaPlayerSpawnMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final CfgSkillMapper cfgSkillMapper;
    private final CfgMapSpawnPointMapper cfgMapSpawnPointMapper;
    private final SysUserMapper sysUserMapper;
    private final LimitProcessor limitProcessor;
    private final TargetProcessor targetProcessor;
    private final DataProcessor dataProcessor;
    private final GameStatePushService pushService;

    // ==================== 开始游戏 ====================

    @Override
    @Transactional
    public DmPushVO startGame(Long gameId, Long mapId, List<Long> roleIds, Map<Long, Long> assignedRoles) {
        // 1. 验证游戏状态
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);
        if (record.getStatus() != GameStatus.WAITING) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "游戏已开始或已结束");
        }

        // 2. 获取玩家列表
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, gameId)
                        .orderByAsc(GaGamePlayer::getSeatNo));
        if (players.isEmpty()) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "房间内没有玩家");
        }
        if (roleIds == null || roleIds.size() != players.size()) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "角色数量与玩家人数不匹配");
        }

        // 3. 分配角色（先处理内定，再随机分配剩余）
        Map<Long, Long> finalAssignment = new HashMap<>();
        List<Long> remainingRoles = new ArrayList<>(roleIds);
        List<Long> remainingPlayerIds = new ArrayList<>();

        for (GaGamePlayer player : players) {
            if (assignedRoles != null && assignedRoles.containsKey(player.getId())) {
                Long roleId = assignedRoles.get(player.getId());
                finalAssignment.put(player.getId(), roleId);
                remainingRoles.remove(roleId);
            } else {
                remainingPlayerIds.add(player.getId());
            }
        }

        // 随机分配剩余角色
        Collections.shuffle(remainingRoles);
        for (int i = 0; i < remainingPlayerIds.size(); i++) {
            finalAssignment.put(remainingPlayerIds.get(i), remainingRoles.get(i));
        }

        // 4. 创建玩家状态和技能实例
        for (GaGamePlayer player : players) {
            Long roleId = finalAssignment.get(player.getId());
            player.setRoleId(roleId);
            gaGamePlayerMapper.updateById(player);

            // 创建玩家状态
            GaPlayerStatus status = new GaPlayerStatus();
            status.setGamePlayerId(player.getId());
            status.setIsAlive(true);
            status.setActiveTags(new ArrayList<>());
            status.setUpdatedAt(LocalDateTime.now());
            gaPlayerStatusMapper.insert(status);

            // 创建技能实例
            List<CfgSkill> skills = cfgSkillMapper.selectList(
                    new LambdaQueryWrapper<CfgSkill>().eq(CfgSkill::getRoleId, roleId));
            for (CfgSkill skill : skills) {
                GaSkillInstance instance = new GaSkillInstance();
                instance.setGamePlayerId(player.getId());
                instance.setSkillId(skill.getId());
                instance.setIsActive(true);
                instance.setUsedCount(0);
                gaSkillInstanceMapper.insert(instance);
            }
        }

        // 5. 分配出生点
        assignSpawnPoints(gameId, mapId, players, 1);

        // 6. 更新游戏状态（DM 点击开始直接进入 START 阶段）
        record.setMapId(mapId);
        record.setRoleIds(roleIds);
        record.setStatus(GameStatus.PLAYING);
        record.setCurrentRound(1);
        record.setCurrentStage(GameStage.START); // 直接进入开始阶段
        record.setStartedAt(LocalDateTime.now());
        gaGameRecordMapper.updateById(record);

        // 7. 构建并推送游戏状态
        pushService.pushGameState(gameId);
        return pushService.buildDmGameState(gameId);
    }

    /**
     * 分配出生点
     */
    private void assignSpawnPoints(Long gameId, Long mapId, List<GaGamePlayer> players, int roundNo) {
        List<CfgMapSpawnPoint> spawnPoints = cfgMapSpawnPointMapper.selectList(
                new LambdaQueryWrapper<CfgMapSpawnPoint>().eq(CfgMapSpawnPoint::getMapId, mapId));

        if (spawnPoints.isEmpty()) {
            return;
        }

        Collections.shuffle(spawnPoints);
        for (int i = 0; i < players.size() && i < spawnPoints.size(); i++) {
            GaPlayerSpawn spawn = new GaPlayerSpawn();
            spawn.setGameId(gameId);
            spawn.setGamePlayerId(players.get(i).getId());
            spawn.setSpawnPointId(spawnPoints.get(i).getId());
            spawn.setRoundNo(roundNo);
            gaPlayerSpawnMapper.insert(spawn);
        }
    }

    // ==================== 阶段推进 ====================

    @Override
    @Transactional
    public DmPushVO nextStage(Long gameId) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);

        if (record.getStatus() != GameStatus.PLAYING) {
            throw new BusinessException(ResultCode.GAME_NOT_PLAYING);
        }

        // 推进阶段：START -> NIGHT -> VOTE -> DAY -> NIGHT(回合+1) 循环
        GameStage currentStage = record.getCurrentStage();
        GameStage nextStage;
        int nextRound = record.getCurrentRound();

        switch (currentStage) {
            case START:
                nextStage = GameStage.NIGHT;
                // 分配出生点
                List<GaGamePlayer> startPlayers = getAlivePlayers(gameId);
                assignSpawnPoints(gameId, record.getMapId(), startPlayers, nextRound);
                break;
            case NIGHT:
                nextStage = GameStage.VOTE;
                break;
            case VOTE:
                nextStage = GameStage.DAY;
                break;
            case DAY:
                nextStage = GameStage.NIGHT;
                nextRound++;
                // 清理过期标签
                cleanExpiredTags(gameId, nextRound);
                // 重新分配出生点
                List<GaGamePlayer> alivePlayers = getAlivePlayers(gameId);
                assignSpawnPoints(gameId, record.getMapId(), alivePlayers, nextRound);
                break;
            default:
                throw new BusinessException(ResultCode.BIZ_ERROR, "无效的游戏阶段");
        }

        record.setCurrentStage(nextStage);
        record.setCurrentRound(nextRound);
        gaGameRecordMapper.updateById(record);

        // 推送状态更新
        pushService.pushGameState(gameId);
        return pushService.buildDmGameState(gameId);
    }

    private List<GaGamePlayer> getAlivePlayers(Long gameId) {
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>().eq(GaGamePlayer::getGameId, gameId));

        if (players.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询状态
        List<Long> playerIds = players.stream().map(GaGamePlayer::getId).toList();
        Map<Long, GaPlayerStatus> statusMap = gaPlayerStatusMapper.selectBatchIds(playerIds).stream()
                .collect(Collectors.toMap(GaPlayerStatus::getGamePlayerId, s -> s));

        return players.stream()
                .filter(p -> {
                    GaPlayerStatus status = statusMap.get(p.getId());
                    return status != null && status.getIsAlive();
                })
                .collect(Collectors.toList());
    }

    /**
     * 清理过期标签（简化版：按回合数清理）
     */
    private void cleanExpiredTags(Long gameId, int currentRound) {
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>().eq(GaGamePlayer::getGameId, gameId));

        if (players.isEmpty()) {
            return;
        }

        // 批量查询状态
        List<Long> playerIds = players.stream().map(GaGamePlayer::getId).toList();
        List<GaPlayerStatus> statuses = gaPlayerStatusMapper.selectBatchIds(playerIds);

        for (GaPlayerStatus status : statuses) {
            if (status != null && status.getActiveTags() != null) {
                // 清理带有过期时机的标签（NEXT_ROUND 和 AFTER_2_ROUND 需要根据回合数判断）
                boolean modified = status.getActiveTags().removeIf(tag -> {
                    if (tag.getExpiry() == TagExpiry.NEXT_ROUND) {
                        return tag.getAppliedRound() != null && tag.getAppliedRound() < currentRound - 1;
                    }
                    if (tag.getExpiry() == TagExpiry.AFTER_2_ROUND) {
                        return tag.getAppliedRound() != null && tag.getAppliedRound() < currentRound - 2;
                    }
                    return false;
                });
                if (modified) {
                    status.setUpdatedAt(LocalDateTime.now());
                    gaPlayerStatusMapper.updateById(status);
                }
            }
        }
    }

    // ==================== 击杀/复活玩家 ====================

    @Override
    @Transactional
    public void killPlayers(Long gameId, List<Long> targetPlayerIds) {
        if (targetPlayerIds == null || targetPlayerIds.isEmpty()) {
            return;
        }

        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);

        for (Long targetPlayerId : targetPlayerIds) {
            GaPlayerStatus status = gaPlayerStatusMapper.selectById(targetPlayerId);
            if (status == null) {
                continue;
            }

            status.setIsAlive(false);
            status.setDeathRound(record.getCurrentRound());
            status.setDeathStage(record.getCurrentStage());
            status.setActiveTags(new ArrayList<>()); // 死亡时清除所有标签
            status.setUpdatedAt(LocalDateTime.now());
            gaPlayerStatusMapper.updateById(status);
        }

        // 记录日志（一条日志记录所有目标）
        GaActionLog log = new GaActionLog();
        log.setGameId(gameId);
        log.setRoundNo(record.getCurrentRound());
        log.setStage(record.getCurrentStage());
        log.setSourceType(ActionSourceType.DM_EXECUTE);
        log.setActionType(ActionType.KILL);
        log.setTargetIds(targetPlayerIds);
        log.setCreatedAt(LocalDateTime.now());
        gaActionLogMapper.insert(log);

        // 推送状态更新
        pushService.pushGameState(gameId);
    }

    @Override
    @Transactional
    public void revivePlayers(Long gameId, List<Long> targetPlayerIds) {
        if (targetPlayerIds == null || targetPlayerIds.isEmpty()) {
            return;
        }

        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);

        for (Long targetPlayerId : targetPlayerIds) {
            GaPlayerStatus status = gaPlayerStatusMapper.selectById(targetPlayerId);
            if (status == null) {
                continue;
            }

            status.setIsAlive(true);
            status.setDeathRound(null);
            status.setDeathStage(null);
            status.setUpdatedAt(LocalDateTime.now());
            gaPlayerStatusMapper.updateById(status);
        }

        // 记录日志（一条日志记录所有目标）
        GaActionLog log = new GaActionLog();
        log.setGameId(gameId);
        log.setRoundNo(record.getCurrentRound());
        log.setStage(record.getCurrentStage());
        log.setSourceType(ActionSourceType.DM_EXECUTE);
        log.setActionType(ActionType.REVIVE);
        log.setTargetIds(targetPlayerIds);
        log.setCreatedAt(LocalDateTime.now());
        gaActionLogMapper.insert(log);

        // 推送状态更新
        pushService.pushGameState(gameId);
    }

    // ==================== 结束游戏 ====================

    @Override
    @Transactional
    public GameEndResultVO endGame(Long gameId, String victoryType) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);

        // 1. 结束当前游戏
        record.setStatus(GameStatus.FINISHED);
        record.setVictoryType(CampType.valueOf(victoryType));
        record.setFinishedAt(LocalDateTime.now());
        gaGameRecordMapper.updateById(record);

        // 2. 自动创建新对局（同房间码、同DM、无玩家）
        GaGameRecord newRecord = new GaGameRecord();
        newRecord.setRoomCode(record.getRoomCode());
        newRecord.setDmUserId(record.getDmUserId());
        newRecord.setStatus(GameStatus.WAITING);
        newRecord.setCurrentRound(0);
        gaGameRecordMapper.insert(newRecord);

        // 3. 构建游戏最终状态（含所有玩家角色等结算信息）
        DmPushVO gameState = pushService.buildDmGameState(gameId);

        // 4. 构建返回结果
        GameEndResultVO result = new GameEndResultVO();
        result.setFinishedGameId(gameId);
        result.setNewGameId(newRecord.getId());
        result.setRoomCode(record.getRoomCode());
        result.setVictoryType(victoryType);
        result.setGameState(gameState);

        // 5. 推送游戏结束状态给所有人（含结算信息）
        pushService.pushGameState(gameId);

        return result;
    }

    // ==================== 移除标签 ====================

    @Override
    @Transactional
    public void removeTag(Long gameId, Long targetPlayerId, int tagIndex) {
        GaPlayerStatus status = gaPlayerStatusMapper.selectById(targetPlayerId);
        AssertUtils.notNull(status, ResultCode.PLAYER_NOT_FOUND);

        if (status.getActiveTags() != null && tagIndex >= 0 && tagIndex < status.getActiveTags().size()) {
            status.getActiveTags().remove(tagIndex);
            status.setUpdatedAt(LocalDateTime.now());
            gaPlayerStatusMapper.updateById(status);
        }

        pushService.pushGameState(gameId);
    }

    // ==================== DM 推送技能 ====================

    @Override
    public void pushSkillToPlayer(Long gameId, Long playerId, Long skillId) {
        // 1. 验证游戏和玩家
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);

        GaGamePlayer player = gaGamePlayerMapper.selectById(playerId);
        AssertUtils.notNull(player, ResultCode.PLAYER_NOT_FOUND);

        // 2. 获取玩家技能实例
        LambdaQueryWrapper<GaSkillInstance> wrapper = new LambdaQueryWrapper<GaSkillInstance>()
                .eq(GaSkillInstance::getGamePlayerId, playerId)
                .eq(GaSkillInstance::getIsActive, true);

        if (skillId != null) {
            wrapper.eq(GaSkillInstance::getSkillId, skillId);
        }

        List<GaSkillInstance> instances = gaSkillInstanceMapper.selectList(wrapper);
        if (instances.isEmpty()) {
            return; // 没有可推送的技能
        }

        // 3. 构建技能信息列表（只推送可用的非普通技能）
        List<PlayerSkillVO> skillsToSend = new ArrayList<>();

        for (GaSkillInstance instance : instances) {
            // 跳过未激活的技能实例
            if (!instance.getIsActive()) {
                continue;
            }

            CfgSkill skill = cfgSkillMapper.selectById(instance.getSkillId());
            if (skill == null) {
                continue;
            }

            // 跳过普通技能（isNormal = true）
            SkillEffectConfig config = skill.getSkillConfig();
            if (config != null && config.getTriggerRule() != null 
                    && Boolean.TRUE.equals(config.getTriggerRule().getIsNormal())) {
                continue;
            }

            // 检查使用限制
            String limitReason = limitProcessor.checkLimit(skill, instance, record);
            if (limitReason != null) {
                continue; // 跳过受限的技能
            }

            // 构建技能信息
            PlayerSkillVO vo = dataProcessor.buildSkillInfoBase(skill, instance, player, record, targetProcessor);
            if (vo != null) {
                skillsToSend.add(vo);
            }
        }

        // 4. 推送给玩家
        if (!skillsToSend.isEmpty()) {
            pushService.pushSkillToPlayer(gameId, player.getUserId(), skillsToSend);
        }
    }
}
