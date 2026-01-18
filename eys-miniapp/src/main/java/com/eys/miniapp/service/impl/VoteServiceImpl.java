package com.eys.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.common.utils.AssertUtils;
import com.eys.mapper.*;
import com.eys.miniapp.service.GameStatePushService;
import com.eys.miniapp.service.VoteService;
import com.eys.model.entity.*;
import com.eys.model.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 投票服务实现
 * 负责投票行为的验证和记录
 */
@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final GameStatePushService pushService;

    @Override
    @Transactional
    public void vote(Long gamePlayerId, Long targetId) {
        // 获取玩家和游戏信息
        GaGamePlayer player = gaGamePlayerMapper.selectById(gamePlayerId);
        AssertUtils.notNull(player, ResultCode.PLAYER_NOT_FOUND);

        GaGameRecord record = gaGameRecordMapper.selectById(player.getGameId());
        AssertUtils.notNull(record, ResultCode.GAME_NOT_FOUND);

        // 验证游戏状态
        if (record.getStatus() != GameStatus.PLAYING) {
            throw new BusinessException(ResultCode.GAME_NOT_PLAYING);
        }
        if (record.getCurrentStage() != GameStage.VOTE) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "当前不是投票阶段");
        }

        // 验证玩家是否存活
        GaPlayerStatus status = gaPlayerStatusMapper.selectById(gamePlayerId);
        if (status == null || !status.getIsAlive()) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "死亡玩家无法投票");
        }

        // 检查是否已投票
        GaActionLog existingVote = gaActionLogMapper.selectOne(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, record.getId())
                        .eq(GaActionLog::getRoundNo, record.getCurrentRound())
                        .eq(GaActionLog::getActionType, ActionType.VOTE)
                        .eq(GaActionLog::getInitiatorId, gamePlayerId));
        if (existingVote != null) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "本回合已投票");
        }

        // 记录投票
        GaActionLog log = new GaActionLog();
        log.setGameId(record.getId());
        log.setRoundNo(record.getCurrentRound());
        log.setStage(GameStage.VOTE);
        log.setSourceType(ActionSourceType.PLAYER_ACTION);
        log.setActionType(ActionType.VOTE);
        log.setInitiatorId(gamePlayerId);
        log.setTargetIds(targetId != null ? List.of(targetId) : new ArrayList<>());
        log.setCreatedAt(LocalDateTime.now());
        gaActionLogMapper.insert(log);

        // 推送游戏状态（DM 通过 actionLogs 实时看到投票情况）
        pushService.pushGameState(record.getId());
    }
}
