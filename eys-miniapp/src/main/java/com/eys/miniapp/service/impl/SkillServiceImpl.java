package com.eys.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.common.utils.AssertUtils;
import com.eys.mapper.*;
import com.eys.miniapp.service.SkillService;
import com.eys.model.json.SkillEffectConfig;
import com.eys.miniapp.service.GameStatePushService;
import com.eys.model.entity.*;
import com.eys.model.enums.*;
import com.eys.model.vo.game.PlayerSkillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.eys.miniapp.processor.DataProcessor;
import com.eys.miniapp.processor.EffectProcessor;
import com.eys.miniapp.processor.TargetProcessor;
import com.eys.model.vo.game.PlayerSkillResultVO;

/**
 * 技能服务实现
 */
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final GaGameRecordMapper gaGameRecordMapper;
    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaSkillInstanceMapper gaSkillInstanceMapper;
    private final GaPlayerStatusMapper gaPlayerStatusMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final CfgSkillMapper cfgSkillMapper;
    private final EffectProcessor effectProcessor;
    private final TargetProcessor targetProcessor;
    private final DataProcessor dataProcessor;
    private final GameStatePushService pushService;

    @Override
    @Transactional
    public void useSkill(Long gamePlayerId, Long skillId, List<Long> targetIds) {
        GaSkillInstance instance = gaSkillInstanceMapper.selectOne(
                new LambdaQueryWrapper<GaSkillInstance>()
                        .eq(GaSkillInstance::getGamePlayerId, gamePlayerId)
                        .eq(GaSkillInstance::getSkillId, skillId));
        AssertUtils.notNull(instance, ResultCode.SKILL_NOT_FOUND);

        CfgSkill skill = cfgSkillMapper.selectById(skillId);
        AssertUtils.notNull(skill, ResultCode.SKILL_NOT_FOUND);

        GaGamePlayer player = gaGamePlayerMapper.selectById(gamePlayerId);
        GaGameRecord record = gaGameRecordMapper.selectById(player.getGameId());

        // 检查游戏状态
        if (record.getStatus() != GameStatus.PLAYING) {
            throw new BusinessException(ResultCode.GAME_NOT_PLAYING);
        }

        // 检查游戏阶段（技能通常在夜晚使用）
        if (record.getCurrentStage() != GameStage.NIGHT) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "当前阶段不可使用技能");
        }

        // 检查玩家是否存活
        GaPlayerStatus playerStatus = gaPlayerStatusMapper.selectById(gamePlayerId);
        if (playerStatus == null || !playerStatus.getIsAlive()) {
            throw new BusinessException(ResultCode.PLAYER_ALREADY_DEAD, "死亡玩家无法使用技能");
        }

        // 检查技能是否可用
        if (!instance.getIsActive()) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "技能已失效");
        }

        // 检查使用限制
        SkillEffectConfig config = skill.getSkillConfig();
        if (config != null && config.getLimitRule() != null) {
            SkillEffectConfig.LimitRule limit = config.getLimitRule();
            if (limit.getTotalMax() != null && limit.getTotalMax() > 0
                    && instance.getUsedCount() >= limit.getTotalMax()) {
                throw new BusinessException(ResultCode.BIZ_ERROR, "技能使用次数已达上限");
            }
        }

        // 更新使用次数
        instance.setUsedCount(instance.getUsedCount() + 1);
        gaSkillInstanceMapper.updateById(instance);

        // 记录动作日志（玩家主动使用技能，sourceType 应为 PLAYER_ACTION）
        GaActionLog log = new GaActionLog();
        log.setGameId(player.getGameId());
        log.setRoundNo(record.getCurrentRound());
        log.setStage(record.getCurrentStage());
        log.setSourceType(ActionSourceType.PLAYER_ACTION);
        log.setActionType(ActionType.SKILL);
        log.setInitiatorId(gamePlayerId);
        log.setTargetIds(targetIds);
        log.setSkillId(skill.getId());
        log.setCreatedAt(LocalDateTime.now());
        gaActionLogMapper.insert(log);

        // 应用技能效果（由 EffectProcessor 统一处理 TAG/INHERIT 等效果）
        effectProcessor.applyEffect(skill, player, targetIds, record);

        // 通知 DM：玩家使用了技能
        pushService.pushSkillUsedToDm(player.getGameId(), gamePlayerId, skillId, targetIds);

        // 检查是否需要回传结果（DataProcessor 处理 postData）
        PlayerSkillResultVO skillResult = dataProcessor.resolvePostData(skill, targetIds, record);
        if (skillResult != null) {
            pushService.pushSkillResult(player.getGameId(), player.getUserId(), skillResult);
        }

        // 推送游戏状态
        pushService.pushGameState(player.getGameId());
    }
}
