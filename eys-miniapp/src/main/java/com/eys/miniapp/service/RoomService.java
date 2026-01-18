package com.eys.miniapp.service;

import com.eys.model.vo.RoomInfoVO;

/**
 * 房间管理服务接口
 */
public interface RoomService {

    /**
     * DM 创建房间
     *
     * @param dmUserId DM 用户ID
     * @return 房间信息
     */
    RoomInfoVO createRoom(Long dmUserId);

    /**
     * 玩家加入房间
     *
     * @param userId   玩家用户ID
     * @param roomCode 房间邀请码
     * @return 房间信息
     */
    RoomInfoVO joinRoom(Long userId, String roomCode);

    /**
     * 玩家离开房间
     *
     * @param userId   玩家用户ID
     * @param roomCode 房间邀请码
     */
    void leaveRoom(Long userId, String roomCode);

    /**
     * 获取房间信息
     *
     * @param roomCode 房间邀请码
     * @return 房间信息
     */
    RoomInfoVO getRoomInfo(String roomCode);

    /**
     * DM 解散房间
     *
     * @param dmUserId DM 用户ID
     * @param gameId   对局ID
     */
    void dismissRoom(Long dmUserId, Long gameId);

    /**
     * DM 交换两名玩家的座位号
     *
     * @param dmUserId  DM 用户ID
     * @param roomCode  房间邀请码
     * @param playerId1 玩家1的用户ID
     * @param playerId2 玩家2的用户ID
     */
    void swapSeats(Long dmUserId, String roomCode, Long playerId1, Long playerId2);

    /**
     * DM 踢出玩家
     *
     * @param dmUserId     DM 用户ID
     * @param roomCode     房间邀请码
     * @param targetUserId 被踢玩家的用户ID
     */
    void kickPlayer(Long dmUserId, String roomCode, Long targetUserId);

    /**
     * 玩家回到房间（游戏结束后，玩家通过此接口加入新对局）
     * 复用上一局的座位号逻辑（按加入顺序分配连续座位号）
     *
     * @param userId      玩家用户ID
     * @param roomCode    房间邀请码
     * @param oldGameId   刚结束的对局ID（用于验证）
     * @return 新对局的房间信息
     */
    RoomInfoVO rejoinRoom(Long userId, String roomCode, Long oldGameId);
}
