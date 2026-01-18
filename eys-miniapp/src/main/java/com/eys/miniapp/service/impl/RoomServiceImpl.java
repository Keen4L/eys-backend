package com.eys.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.common.utils.AssertUtils;
import com.eys.common.utils.RoomCodeGenerator;
import com.eys.mapper.*;
import com.eys.miniapp.service.GameStatePushService;
import com.eys.miniapp.service.RoomService;
import com.eys.miniapp.websocket.GameWebSocket;
import com.eys.model.entity.*;
import com.eys.model.enums.GameStatus;
import com.eys.model.enums.WsMessageType;
import com.eys.model.vo.RoomInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 房间管理服务实现
 * 
 * 核心职责：
 * 1. 房间生命周期管理（创建、加入、离开、解散）
 * 2. 断线处理逻辑
 * 3. 房间状态变更后触发推送
 * 4. 座位号连续性保证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final SysUserMapper sysUserMapper;
    private final CfgMapMapper cfgMapMapper;
    private final CfgRoleMapper cfgRoleMapper;
    private final GameStatePushService pushService;

    @Override
    @Transactional
    public RoomInfoVO createRoom(Long dmUserId) {
        // 检查 DM 是否有未结束的房间
        GaGameRecord existing = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getDmUserId, dmUserId)
                        .in(GaGameRecord::getStatus, GameStatus.WAITING, GameStatus.PLAYING));
        if (existing != null) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "您已有未结束的房间");
        }

        // 获取已存在的活跃房间码，避免重复
        Set<String> existingCodes = gaGameRecordMapper.selectList(
                new LambdaQueryWrapper<GaGameRecord>()
                        .in(GaGameRecord::getStatus, GameStatus.WAITING, GameStatus.PLAYING)
                        .select(GaGameRecord::getRoomCode))
                .stream()
                .map(GaGameRecord::getRoomCode)
                .collect(Collectors.toSet());

        // 生成不重复的房间码
        String roomCode = RoomCodeGenerator.generate(existingCodes);

        // 创建游戏记录
        GaGameRecord record = new GaGameRecord();
        record.setRoomCode(roomCode);
        record.setDmUserId(dmUserId);
        record.setStatus(GameStatus.WAITING);
        record.setCurrentRound(0);
        gaGameRecordMapper.insert(record);

        return buildRoomInfo(record);
    }

    @Override
    @Transactional
    public RoomInfoVO joinRoom(Long userId, String roomCode) {
        // 使用 FOR UPDATE 获取房间锁，防止并发加入导致座位号冲突
        GaGameRecord record = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .eq(GaGameRecord::getStatus, GameStatus.WAITING)
                        .last("FOR UPDATE"));
        AssertUtils.notNull(record, ResultCode.ROOM_NOT_FOUND);

        // 检查是否已在当前房间
        GaGamePlayer existing = gaGamePlayerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId())
                        .eq(GaGamePlayer::getUserId, userId));
        if (existing != null) {
            // 已在房间中，直接返回房间信息
            return buildRoomInfo(record);
        }

        // 检查是否已在其他活跃房间中（一个用户同时只能在一个房间）
        GaGamePlayer otherPlayer = gaGamePlayerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getUserId, userId)
                        .ne(GaGamePlayer::getGameId, record.getId())
                        .last("LIMIT 1"));
        if (otherPlayer != null) {
            // 检查该房间是否活跃
            GaGameRecord otherRecord = gaGameRecordMapper.selectById(otherPlayer.getGameId());
            if (otherRecord != null && 
                (otherRecord.getStatus() == GameStatus.WAITING || 
                 otherRecord.getStatus() == GameStatus.PLAYING)) {
                throw new BusinessException(ResultCode.BIZ_ERROR, "您已在其他房间中，请先退出");
            }
        }

        // 获取当前玩家数
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId()));

        // 检查房间人数上限（= 启用的角色数）
        long maxPlayers = cfgRoleMapper.selectCount(
                new LambdaQueryWrapper<CfgRole>().eq(CfgRole::getIsEnabled, true));
        if (players.size() >= maxPlayers) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "房间已满（最多" + maxPlayers + "人）");
        }

        // 分配座位号（当前玩家数 + 1，因为座位号连续）
        int seatNo = players.size() + 1;

        // 创建玩家记录
        GaGamePlayer player = new GaGamePlayer();
        player.setGameId(record.getId());
        player.setUserId(userId);
        player.setSeatNo(seatNo);
        gaGamePlayerMapper.insert(player);

        // 构建并推送房间更新
        RoomInfoVO roomInfo = buildRoomInfo(record);
        pushService.pushRoomUpdate(record.getId(), roomInfo);

        return roomInfo;
    }

    @Override
    @Transactional
    public void leaveRoom(Long userId, String roomCode) {
        // 主动离开：关闭WS但不发送KICKED通知
        removePlayerFromRoom(userId, roomCode, true, false, null);
    }

    @Override
    public RoomInfoVO getRoomInfo(String roomCode) {
        // 只查询活跃状态的房间（WAITING 或 PLAYING）
        GaGameRecord record = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .in(GaGameRecord::getStatus, GameStatus.WAITING, GameStatus.PLAYING));
        AssertUtils.notNull(record, ResultCode.ROOM_NOT_FOUND);

        return buildRoomInfo(record);
    }

    @Override
    @Transactional
    public void dismissRoom(Long dmUserId, Long gameId) {
        dismissRoomInternal(gameId, dmUserId, true);
    }

    // ==================== 私有方法 ====================

    /**
     * 内部解散房间
     *
     * @param gameId          游戏ID
     * @param dmUserId        DM用户ID（checkPermission=true时需要）
     * @param checkPermission 是否检查权限
     */
    private void dismissRoomInternal(Long gameId, Long dmUserId, boolean checkPermission) {
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null) {
            return;
        }

        // 权限检查（外部调用需要）
        if (checkPermission) {
            if (!record.getDmUserId().equals(dmUserId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "非房主无法解散房间");
            }
            if (record.getStatus() == GameStatus.PLAYING) {
                throw new BusinessException(ResultCode.BIZ_ERROR, "游戏进行中无法解散房间");
            }
        }

        String roomCode = record.getRoomCode();

        // 删除玩家记录
        gaGamePlayerMapper.delete(
                new LambdaQueryWrapper<GaGamePlayer>().eq(GaGamePlayer::getGameId, gameId));

        // 更新状态为已关闭
        record.setStatus(GameStatus.CLOSED);
        gaGameRecordMapper.updateById(record);

        // 推送房间解散通知
        pushService.pushRoomDismissed(gameId);
    }

    /**
     * 统一的玩家移除方法（离开/断线/被踢共用）
     *
     * @param userId           玩家用户ID
     * @param roomCode         房间邀请码
     * @param closeWs          是否关闭 WebSocket 连接
     * @param sendKickedNotify 是否发送 KICKED 通知（仅踢人时为 true）
     * @param closeReason      关闭原因
     * @return 是否成功移除（玩家存在并被删除）
     */
    @Transactional
    protected boolean removePlayerFromRoom(Long userId, String roomCode, boolean closeWs, 
                                          boolean sendKickedNotify, String closeReason) {
        GaGameRecord record = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .eq(GaGameRecord::getStatus, GameStatus.WAITING)
                        .last("FOR UPDATE"));
        if (record == null) {
            return false;
        }

        // 删除玩家记录
        int deleted = gaGamePlayerMapper.delete(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId())
                        .eq(GaGamePlayer::getUserId, userId));

        if (deleted > 0) {
            // 重排座位号
            reorderSeatNumbers(record.getId());

            // 关闭 WebSocket 连接（如需要）
            if (closeWs) {
                // 只有被踢时才发送 KICKED 通知
                if (sendKickedNotify) {
                    GameWebSocket.sendToUser(record.getId(), userId, WsMessageType.KICKED, closeReason);
                }
                GameWebSocket.closeUserSession(record.getId(), userId, closeReason);
            }

            // 推送房间更新
            RoomInfoVO roomInfo = buildRoomInfo(record);
            pushService.pushRoomUpdate(record.getId(), roomInfo);
            return true;
        }
        return false;
    }

    /**
     * 重排座位号（确保连续）
     * 按当前座位号升序重新分配 1, 2, 3...
     *
     * @param gameId 游戏ID
     */
    private void reorderSeatNumbers(Long gameId) {
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, gameId)
                        .orderByAsc(GaGamePlayer::getSeatNo));

        int newSeat = 1;
        for (GaGamePlayer p : players) {
            if (!p.getSeatNo().equals(newSeat)) {
                p.setSeatNo(newSeat);
                gaGamePlayerMapper.updateById(p);
            }
            newSeat++;
        }
    }

    /**
     * 构建房间信息 VO
     */
    private RoomInfoVO buildRoomInfo(GaGameRecord record) {
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

    // ==================== DM 管理操作 ====================

    @Override
    @Transactional
    public void swapSeats(Long dmUserId, String roomCode, Long playerId1, Long playerId2) {
        // 获取房间并校验权限
        GaGameRecord record = getRecordAndCheckDm(roomCode, dmUserId);

        // 获取两个玩家
        GaGamePlayer player1 = gaGamePlayerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId())
                        .eq(GaGamePlayer::getUserId, playerId1));
        GaGamePlayer player2 = gaGamePlayerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, record.getId())
                        .eq(GaGamePlayer::getUserId, playerId2));

        AssertUtils.notNull(player1, ResultCode.BIZ_ERROR, "玩家1不在房间中");
        AssertUtils.notNull(player2, ResultCode.BIZ_ERROR, "玩家2不在房间中");

        // 交换座位号
        Integer tempSeat = player1.getSeatNo();
        player1.setSeatNo(player2.getSeatNo());
        player2.setSeatNo(tempSeat);

        gaGamePlayerMapper.updateById(player1);
        gaGamePlayerMapper.updateById(player2);

        // 推送房间更新
        RoomInfoVO roomInfo = buildRoomInfo(record);
        pushService.pushRoomUpdate(record.getId(), roomInfo);
    }

    @Override
    @Transactional
    public void kickPlayer(Long dmUserId, String roomCode, Long targetUserId) {
        // 获取房间并校验 DM 权限
        getRecordAndCheckDm(roomCode, dmUserId);

        // 不能踢自己
        if (dmUserId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "DM不能踢出自己");
        }

        // 移除玩家并发送 KICKED 通知
        boolean removed = removePlayerFromRoom(targetUserId, roomCode, true, true, "您已被踢出房间");
        if (!removed) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "该玩家不在房间中");
        }
    }

    /**
     * 获取房间记录并校验 DM 权限
     */
    private GaGameRecord getRecordAndCheckDm(String roomCode, Long dmUserId) {
        GaGameRecord record = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .eq(GaGameRecord::getStatus, GameStatus.WAITING)
                        .last("FOR UPDATE"));
        AssertUtils.notNull(record, ResultCode.ROOM_NOT_FOUND);

        if (!record.getDmUserId().equals(dmUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有DM可以执行此操作");
        }

        return record;
    }
    @Override
    @Transactional
    public RoomInfoVO rejoinRoom(Long userId, String roomCode, Long oldGameId) {
        // 1. 验证旧对局是否存在且已结束
        GaGameRecord oldRecord = gaGameRecordMapper.selectById(oldGameId);
        AssertUtils.notNull(oldRecord, ResultCode.GAME_NOT_FOUND);
        if (oldRecord.getStatus() != GameStatus.FINISHED) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "游戏尚未结束");
        }
        if (!oldRecord.getRoomCode().equals(roomCode)) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "房间码不匹配");
        }

        // 2. 查找新对局（endGame时创建的WAITING状态对局，同房间码）
        GaGameRecord newRecord = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .eq(GaGameRecord::getStatus, GameStatus.WAITING)
                        .last("FOR UPDATE"));
        if (newRecord == null) {
            // DM可能已断线导致房间被删除
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND, "房间不存在，DM可能已离开");
        }

        // 3. 检查是否已在新对局中
        GaGamePlayer existing = gaGamePlayerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, newRecord.getId())
                        .eq(GaGamePlayer::getUserId, userId));
        if (existing != null) {
            return buildRoomInfo(newRecord);
        }

        // 4. 获取当前新对局的玩家数，分配座位号
        long currentCount = gaGamePlayerMapper.selectCount(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, newRecord.getId()));
        int seatNo = (int) currentCount + 1;

        // 5. 检查房间人数上限
        long maxPlayers = cfgRoleMapper.selectCount(
                new LambdaQueryWrapper<CfgRole>().eq(CfgRole::getIsEnabled, true));
        if (currentCount >= maxPlayers) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "房间已满");
        }

        // 6. 创建玩家记录
        GaGamePlayer player = new GaGamePlayer();
        player.setGameId(newRecord.getId());
        player.setUserId(userId);
        player.setSeatNo(seatNo);
        gaGamePlayerMapper.insert(player);

        // 7. 推送房间更新
        RoomInfoVO roomInfo = buildRoomInfo(newRecord);
        pushService.pushRoomUpdate(newRecord.getId(), roomInfo);

        return roomInfo;
    }
}
