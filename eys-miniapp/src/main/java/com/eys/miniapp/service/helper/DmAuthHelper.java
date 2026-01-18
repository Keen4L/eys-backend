package com.eys.miniapp.service.helper;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.GaGameRecordMapper;
import com.eys.model.entity.GaGameRecord;
import com.eys.model.enums.GameStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DM 权限校验工具类
 * 在 Controller 层使用，确保当前登录用户是游戏的主持人
 */
@Component
@RequiredArgsConstructor
public class DmAuthHelper {

    private final GaGameRecordMapper gaGameRecordMapper;

    /**
     * 校验当前用户是否为指定游戏的 DM
     *
     * @param gameId 对局ID
     * @throws BusinessException 如果不是 DM 则抛出 FORBIDDEN 异常
     */
    public void checkDm(Long gameId) {
        Long userId = StpUtil.getLoginIdAsLong();
        GaGameRecord record = gaGameRecordMapper.selectById(gameId);
        if (record == null) {
            throw new BusinessException(ResultCode.GAME_NOT_FOUND);
        }
        if (!record.getDmUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有主持人可以执行此操作");
        }
    }

    /**
     * 校验当前用户是否为指定房间的 DM
     *
     * @param roomCode 房间邀请码
     * @throws BusinessException 如果不是 DM 则抛出 FORBIDDEN 异常
     */
    public void checkDmByRoomCode(String roomCode) {
        Long userId = StpUtil.getLoginIdAsLong();
        GaGameRecord record = gaGameRecordMapper.selectOne(
                new LambdaQueryWrapper<GaGameRecord>()
                        .eq(GaGameRecord::getRoomCode, roomCode)
                        .ne(GaGameRecord::getStatus, GameStatus.CLOSED));
        if (record == null) {
            throw new BusinessException(ResultCode.ROOM_NOT_FOUND);
        }
        if (!record.getDmUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有主持人可以执行此操作");
        }
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
