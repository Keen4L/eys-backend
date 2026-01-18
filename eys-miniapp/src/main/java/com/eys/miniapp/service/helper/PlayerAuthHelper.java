package com.eys.miniapp.service.helper;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.GaGamePlayerMapper;
import com.eys.mapper.GaGameRecordMapper;
import com.eys.model.entity.GaGamePlayer;
import com.eys.model.entity.GaGameRecord;
import com.eys.model.enums.GameStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 玩家权限校验工具类
 * 在 Controller 层使用，确保当前登录用户是游戏中的玩家
 */
@Component
@RequiredArgsConstructor
public class PlayerAuthHelper {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;

    /**
     * 获取当前玩家的对局玩家ID
     * 校验当前登录用户是否为该游戏的玩家
     *
     * @param gameId 对局ID
     * @return 对局玩家ID（gamePlayerId）
     * @throws BusinessException 如果不是该游戏玩家则抛出异常
     */
    public Long getGamePlayerId(Long gameId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        GaGamePlayer player = gaGamePlayerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, gameId)
                        .eq(GaGamePlayer::getUserId, userId));
        if (player == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, "您不是该游戏的玩家");
        }
        return player.getId();
    }

    /**
     * 通过房间码获取当前玩家的对局玩家ID
     *
     * @param roomCode 房间邀请码
     * @return 对局玩家ID（gamePlayerId）
     * @throws BusinessException 如果房间不存在或不是该游戏玩家则抛出异常
     */
    public Long getGamePlayerIdByRoomCode(String roomCode) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 查找活跃的游戏
        GaGameRecord record = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .in(GaGameRecord::getStatus, GameStatus.WAITING, GameStatus.PLAYING));
        if (record == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        
        return getGamePlayerId(record.getId());
    }

    /**
     * 获取当前登录用户ID
     */
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
