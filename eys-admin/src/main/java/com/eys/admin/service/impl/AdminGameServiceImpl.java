package com.eys.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.admin.service.AdminGameService;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.*;
import com.eys.model.entity.*;
import com.eys.model.enums.GameStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对局记录管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGameServiceImpl implements AdminGameService {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaPlayerSpawnMapper gaPlayerSpawnMapper;
    private final GaSkillInstanceMapper gaSkillInstanceMapper;
    private final GaActionLogMapper gaActionLogMapper;

    @Override
    public List<GaGameRecord> getGameList(String status) {
        LambdaQueryWrapper<GaGameRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GaGameRecord::getStatus, GameStatus.valueOf(status));
        }
        wrapper.orderByDesc(GaGameRecord::getId);
        return gaGameRecordMapper.selectList(wrapper);
    }

    @Override
    public GaGameRecord getGameDetail(Long id) {
        GaGameRecord game = gaGameRecordMapper.selectById(id);
        if (game == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对局不存在");
        }
        return game;
    }

    @Override
    @Transactional
    public void deleteGame(Long id) {
        GaGameRecord game = gaGameRecordMapper.selectById(id);
        if (game == null) {
            return;
        }

        log.info("开始删除对局 {} 及其关联数据", id);

        // 1. 获取该对局的所有玩家ID
        List<GaGamePlayer> players = gaGamePlayerMapper.selectList(
                new LambdaQueryWrapper<GaGamePlayer>().eq(GaGamePlayer::getGameId, id));
        List<Long> playerIds = players.stream().map(GaGamePlayer::getId).toList();

        // 2. 删除玩家状态
        if (!playerIds.isEmpty()) {
            gaPlayerStatusMapper.deleteBatchIds(playerIds);
            log.debug("删除玩家状态: {} 条", playerIds.size());
        }

        // 3. 删除技能实例
        if (!playerIds.isEmpty()) {
            gaSkillInstanceMapper.delete(
                    new LambdaQueryWrapper<GaSkillInstance>()
                            .in(GaSkillInstance::getGamePlayerId, playerIds));
        }

        // 4. 删除出生点分配
        gaPlayerSpawnMapper.delete(
                new LambdaQueryWrapper<GaPlayerSpawn>().eq(GaPlayerSpawn::getGameId, id));

        // 5. 删除动作日志
        gaActionLogMapper.delete(
                new LambdaQueryWrapper<GaActionLog>().eq(GaActionLog::getGameId, id));

        // 6. 删除玩家记录
        gaGamePlayerMapper.delete(
                new LambdaQueryWrapper<GaGamePlayer>().eq(GaGamePlayer::getGameId, id));

        // 7. 最后删除游戏记录
        gaGameRecordMapper.deleteById(id);

        log.info("对局 {} 及关联数据删除完成", id);
    }
}

