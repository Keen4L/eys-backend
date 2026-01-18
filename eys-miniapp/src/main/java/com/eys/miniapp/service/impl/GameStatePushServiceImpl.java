package com.eys.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.mapper.*;
import com.eys.miniapp.service.GameStatePushService;
import com.eys.miniapp.websocket.GameWebSocket;
import com.eys.model.entity.*;
import com.eys.model.enums.WsMessageType;
import com.eys.model.vo.game.*;
import com.eys.model.vo.RoomInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏状态推送服务实现
 * 
 * 核心原则：
 * 1. 统一推送入口：所有状态变更后调用 pushGameState(gameId)
 * 2. 视图自动适配：DM 收全量，玩家收脱敏
 * 3. 无循环依赖：只注入 Mapper，不注入其他 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameStatePushServiceImpl implements GameStatePushService {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaPlayerSpawnMapper gaPlayerSpawnMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final GaSkillInstanceMapper gaSkillInstanceMapper;
    private final SysUserMapper sysUserMapper;

    // ==================== 推送接口 ====================

    @Override
    public void pushGameState(Long gameId) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null) {
            return;
        }

        Long dmUserId = record.getDmUserId();
        boolean isGameFinished = record.getStatus() == com.eys.model.enums.GameStatus.FINISHED;

        // 1. 推送给 DM（全量视图）
        DmPushVO dmState = buildDmGameState(gameId);
        if (dmUserId != null) {
            GameWebSocket.sendToUser(gameId, dmUserId, dmState);
        }

        // 2. 推送给所有玩家
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>().eq(GaGamePlayer::getGameId, gameId));

        for (GaGamePlayer player : players) {
            // DM 已推送，跳过
            if (dmUserId != null && dmUserId.equals(player.getUserId())) {
                continue;
            }
            
            // 游戏结束时，玩家也收到全量视图（含所有角色和动作日志）
            if (isGameFinished) {
                GameWebSocket.sendToUser(gameId, player.getUserId(), dmState);
            } else {
                PlayerPushVO playerState = buildPlayerGameState(player.getUserId(), gameId);
                GameWebSocket.sendToUser(gameId, player.getUserId(), playerState);
            }
        }
    }

    @Override
    public void pushSkillToPlayer(Long gameId, Long userId, Object skills) {
        GameWebSocket.pushSkillToUser(gameId, userId, skills);
    }

    @Override
    public void pushSkillResult(Long gameId, Long userId, PlayerSkillResultVO result) {
        GameWebSocket.pushSkillResultToUser(gameId, userId, result);
    }

    @Override
    public void pushSkillUsedToDm(Long gameId, Long initiatorGamePlayerId, Long skillId, java.util.List<Long> targetIds) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null || record.getDmUserId() == null) {
            return;
        }

        // 构建动作日志（复用 ActionLogVO）
        DmPushVO.ActionLogVO vo = new DmPushVO.ActionLogVO();
        vo.setActionType("SKILL");
        vo.setInitiatorId(initiatorGamePlayerId);
        vo.setSkillId(skillId);
        vo.setTargetIds(targetIds);
        vo.setRoundNo(record.getCurrentRound());
        vo.setStage(record.getCurrentStage() != null ? record.getCurrentStage().getCode() : null);

        GameWebSocket.pushSkillUsedToDm(gameId, record.getDmUserId(), vo);
    }

    // ==================== 视图构建 ====================


    /**
     * 构建 DM 视角游戏状态（全量信息）
     */
    public DmPushVO buildDmGameState(Long gameId) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null) {
            return null;
        }

        DmPushVO state = new DmPushVO();
        state.setGameId(gameId);
        state.setRoomCode(record.getRoomCode());
        state.setStatus(record.getStatus().getCode());
        state.setCurrentRound(record.getCurrentRound());
        state.setCurrentStage(record.getCurrentStage() != null ? record.getCurrentStage().getCode() : null);

        // 地图ID
        state.setMapId(record.getMapId());

        // 玩家状态列表（全量）
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, gameId)
                        .orderByAsc(GaGamePlayer::getSeatNo));

        List<CommonPlayerVO> playerStates = new ArrayList<>();
        for (GaGamePlayer player : players) {
            playerStates.add(buildPlayerState(player, false));
        }
        state.setPlayers(playerStates);

        // 出生点信息
        if (record.getCurrentRound() != null) {
            List<GaPlayerSpawn> spawns = gaPlayerSpawnMapper.selectList(
                    new LambdaQueryWrapper<GaPlayerSpawn>()
                            .eq(GaPlayerSpawn::getGameId, gameId)
                            .eq(GaPlayerSpawn::getRoundNo, record.getCurrentRound()));
            List<DmPushVO.SpawnPointVO> spawnVos = new ArrayList<>();
            for (GaPlayerSpawn spawn : spawns) {
                spawnVos.add(buildSpawnPoint(spawn));
            }
            state.setSpawnPoints(spawnVos);
        }

        // 动作日志
        List<GaActionLog> logs = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, gameId)
                        .orderByDesc(GaActionLog::getCreatedAt)
                        .last("LIMIT 50"));
        List<DmPushVO.ActionLogVO> logVos = new ArrayList<>();
        for (GaActionLog actionLog : logs) {
            logVos.add(buildActionLog(actionLog));
        }
        state.setActionLogs(logVos);

        return state;
    }

    /**
     * 构建玩家视角游戏状态（脱敏信息）
     */
    public PlayerPushVO buildPlayerGameState(Long userId, Long gameId) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null) {
            return null;
        }

        PlayerPushVO state = new PlayerPushVO();
        state.setGameId(gameId);
        state.setRoomCode(record.getRoomCode());
        state.setStatus(record.getStatus().getCode());
        state.setCurrentRound(record.getCurrentRound());
        state.setCurrentStage(record.getCurrentStage() != null ? record.getCurrentStage().getCode() : null);
        state.setMapId(record.getMapId());

        // 玩家列表（脱敏）
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, gameId)
                        .orderByAsc(GaGamePlayer::getSeatNo));

        List<CommonPlayerVO> playerStates = new ArrayList<>();
        GaGamePlayer currentPlayer = null;
        for (GaGamePlayer player : players) {
            CommonPlayerVO pState = buildPlayerState(player, true);
            pState.setIsMe(player.getUserId().equals(userId));
            playerStates.add(pState);

            if (player.getUserId().equals(userId)) {
                currentPlayer = player;
            }
        }
        state.setPlayers(playerStates);

        // 我的角色 ID
        if (currentPlayer != null && currentPlayer.getRoleId() != null) {
            state.setMyRoleId(currentPlayer.getRoleId());

            // 我的出生点 ID
            if (record.getCurrentRound() != null) {
                GaPlayerSpawn mySpawn = gaPlayerSpawnMapper.selectOne(
                        new LambdaQueryWrapper<GaPlayerSpawn>()
                                .eq(GaPlayerSpawn::getGameId, gameId)
                                .eq(GaPlayerSpawn::getGamePlayerId, currentPlayer.getId())
                                .eq(GaPlayerSpawn::getRoundNo, record.getCurrentRound()));
                if (mySpawn != null) {
                    state.setMySpawnPointId(mySpawn.getSpawnPointId());
                }
            }
        }

        return state;
    }

    // ==================== 视图构建辅助方法 ====================

    /**
     * 构建玩家状态（共享方法）
     * @param player 玩家实体
     * @param desensitize 是否脱敏（玩家视图脱敏，DM 视图不脱敏）
     */
    private CommonPlayerVO buildPlayerState(GaGamePlayer player, boolean desensitize) {
        CommonPlayerVO vo = new CommonPlayerVO();
        vo.setGamePlayerId(player.getId());
        vo.setUserId(player.getUserId());
        vo.setSeatNo(player.getSeatNo());

        GaPlayerStatus status = gaPlayerStatusMapper.selectById(player.getId());
        if (status != null) {
            vo.setIsAlive(status.getIsAlive());
        }

        // 敏感信息（DM 视图可见，玩家视图为 null）
        if (!desensitize) {
            vo.setRoleId(player.getRoleId());
            if (status != null) {
                vo.setActiveTags(status.getActiveTags());
            }

            // 构建技能实例列表
            List<GaSkillInstance> skillInstances = gaSkillInstanceMapper.selectList(
                    new LambdaQueryWrapper<GaSkillInstance>()
                            .eq(GaSkillInstance::getGamePlayerId, player.getId()));

            List<CommonPlayerVO.SkillInstanceVO> skillVos = new ArrayList<>();
            for (GaSkillInstance instance : skillInstances) {
                CommonPlayerVO.SkillInstanceVO skillVo =
                        new CommonPlayerVO.SkillInstanceVO();
                skillVo.setSkillInstanceId(instance.getId());
                skillVo.setSkillId(instance.getSkillId());
                skillVo.setIsActive(instance.getIsActive());
                skillVo.setUsedCount(instance.getUsedCount());
                skillVos.add(skillVo);
            }
            vo.setSkills(skillVos);
        }

        return vo;
    }

    private DmPushVO.SpawnPointVO buildSpawnPoint(GaPlayerSpawn spawn) {
        DmPushVO.SpawnPointVO vo = new DmPushVO.SpawnPointVO();
        vo.setSpawnPointId(spawn.getSpawnPointId());
        vo.setGamePlayerId(spawn.getGamePlayerId());
        return vo;
    }

    private DmPushVO.ActionLogVO buildActionLog(GaActionLog log) {
        DmPushVO.ActionLogVO vo = new DmPushVO.ActionLogVO();
        vo.setRoundNo(log.getRoundNo());
        vo.setStage(log.getStage() != null ? log.getStage().getCode() : null);
        vo.setActionType(log.getActionType() != null ? log.getActionType().getCode() : null);
        vo.setInitiatorId(log.getInitiatorId());
        vo.setTargetIds(log.getTargetIds());
        vo.setSkillId(log.getSkillId());
        return vo;
    }

    // ==================== 房间推送 ====================

    @Override
    public void pushRoomUpdate(Long gameId, RoomInfoVO roomInfo) {
        log.debug("推送房间更新: gameId={}", gameId);
        GameWebSocket.broadcastToGame(gameId, WsMessageType.ROOM_UPDATE, roomInfo);
    }

    @Override
    public void pushRoomDismissed(Long gameId) {
        log.debug("推送房间解散: gameId={}", gameId);
        GameWebSocket.broadcastToGame(gameId, WsMessageType.ROOM_DISMISSED, null);
    }

    @Override
    public RoomInfoVO buildRoomInfo(Long gameId) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null) {
            return null;
        }

        RoomInfoVO vo = new RoomInfoVO();
        vo.setGameId(record.getId());
        vo.setRoomCode(record.getRoomCode());
        vo.setDmUserId(record.getDmUserId());
        vo.setStatus(record.getStatus().getCode());
        vo.setCurrentRound(record.getCurrentRound());
        vo.setCurrentStage(record.getCurrentStage() != null ? record.getCurrentStage().getCode() : null);
        vo.setMapId(record.getMapId());

        // 玩家列表
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId())
                        .orderByAsc(GaGamePlayer::getSeatNo));

        // 批量获取用户信息
        Set<Long> userIds = players.stream().map(GaGamePlayer::getUserId).collect(Collectors.toSet());
        List<SysUser> users = userIds.isEmpty() ? List.of() : sysUserMapper.selectBatchIds(userIds);
        var userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<RoomInfoVO.PlayerInfo> playerInfos = new ArrayList<>();
        for (GaGamePlayer player : players) {
            RoomInfoVO.PlayerInfo info = new RoomInfoVO.PlayerInfo();
            info.setUserId(player.getUserId());
            info.setSeatNo(player.getSeatNo());

            // 填充用户信息
            SysUser user = userMap.get(player.getUserId());
            if (user != null) {
                info.setNickname(user.getNickname());
                info.setAvatarUrl(user.getAvatarUrl());
            }
            playerInfos.add(info);
        }
        vo.setPlayers(playerInfos);

        return vo;
    }
}
