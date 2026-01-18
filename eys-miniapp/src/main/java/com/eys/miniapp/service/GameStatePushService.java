package com.eys.miniapp.service;

import com.eys.model.vo.game.DmPushVO;
import com.eys.model.vo.game.PlayerPushVO;
import com.eys.model.vo.RoomInfoVO;
import com.eys.model.vo.game.PlayerSkillResultVO;

/**
 * 游戏状态推送服务
 * 
 * 核心职责：
 * 1. 构建游戏状态视图（DM/玩家）
 * 2. 推送给对应用户
 * 
 * 使用方式：任何状态变更后，调用 pushGameState(gameId) 即可
 */
public interface GameStatePushService {

    /**
     * 推送游戏状态给对局内所有人
     * - DM 收到：DmPushVO（全量信息）
     * - 玩家收到：PlayerPushVO（脱敏信息）
     * 
     * @param gameId 对局ID
     */
    void pushGameState(Long gameId);

    /**
     * 推送技能给指定玩家
     */
    void pushSkillToPlayer(Long gameId, Long userId, Object skills);

    /**
     * 推送技能结果给指定玩家
     */
    void pushSkillResult(Long gameId, Long userId, PlayerSkillResultVO result);

    /**
     * 推送技能使用通知给 DM
     */
    void pushSkillUsedToDm(Long gameId, Long initiatorGamePlayerId, Long skillId, java.util.List<Long> targetIds);

    /**
     * 构建 DM 视角游戏状态（全量信息）
     */
    DmPushVO buildDmGameState(Long gameId);

    /**
     * 构建玩家视角游戏状态（脱敏信息）
     */
    PlayerPushVO buildPlayerGameState(Long userId, Long gameId);

    /**
     * 推送房间更新给对局内所有人
     *
     * @param gameId   对局ID
     * @param roomInfo 房间信息
     */
    void pushRoomUpdate(Long gameId, RoomInfoVO roomInfo);

    /**
     * 推送房间解散通知给对局内所有人
     *
     * @param gameId 对局ID
     */
    void pushRoomDismissed(Long gameId);

    /**
     * 构建房间信息（用于 WebSocket 重连时推送）
     *
     * @param gameId 对局ID
     * @return 房间信息
     */
    RoomInfoVO buildRoomInfo(Long gameId);
}
