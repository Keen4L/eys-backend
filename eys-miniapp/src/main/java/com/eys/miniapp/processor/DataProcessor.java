package com.eys.miniapp.processor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.mapper.*;
import com.eys.model.json.SkillEffectConfig;
import com.eys.model.entity.*;
import com.eys.model.enums.*;
import com.eys.model.vo.game.PlayerSkillVO;
import com.eys.model.vo.game.PlayerSkillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据处理器
 * 负责 preData（推送时）和 postData（使用后）的数据解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataProcessor {

    private final GaGamePlayerMapper gaGamePlayerMapper;
    private final GaActionLogMapper gaActionLogMapper;
    private final CfgRoleMapper cfgRoleMapper;

    // ==================== PlayerSkillVO 构建（公共方法） ====================

    /**
     * 构建 PlayerSkillVO（精简版本）
     * 只包含：skillInstanceId, skillId, targetPlayerIds, preData
     */
    public PlayerSkillVO buildSkillInfoBase(CfgSkill skill, GaSkillInstance instance,
                                            GaGamePlayer player, GaGameRecord record, TargetProcessor targetProcessor) {
        PlayerSkillVO vo = new PlayerSkillVO();
        vo.setSkillInstanceId(instance.getId());
        vo.setSkillId(skill.getId());

        // 构建可选目标（仅返回 ID 列表）
        if (player != null && record != null && targetProcessor != null) {
            List<Long> targetIds = targetProcessor.buildTargetPlayerIds(skill, player, record);
            vo.setTargetPlayerIds(targetIds);
        }

        // 构建预数据
        vo.setPreData(resolvePreData(skill, record));

        return vo;
    }

    // ==================== PreData 处理（推送时） ====================

    /**
     * 解析技能推送时的预数据
     */
    public PlayerSkillVO.PreData resolvePreData(CfgSkill skill, GaGameRecord record) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getDataRule() == null) {
            return null;
        }

        PreDataType type = config.getDataRule().getPreDataType();
        if (type == null || type == PreDataType.NONE) {
            return null;
        }

        return switch (type) {
            case TOP_VOTED_PLAYER -> buildTopVotedPlayer(record);
            default -> null;
        };
    }

    /**
     * 构建上回合票数最高玩家数据
     */
    private PlayerSkillVO.PreData buildTopVotedPlayer(GaGameRecord record) {
        Integer lastRound = record.getCurrentRound() != null ? record.getCurrentRound() - 1 : null;
        if (lastRound == null || lastRound < 1) {
            return null;
        }

        List<GaActionLog> voteLogs = gaActionLogMapper.selectList(
                new LambdaQueryWrapper<GaActionLog>()
                        .eq(GaActionLog::getGameId, record.getId())
                        .eq(GaActionLog::getRoundNo, lastRound)
                        .eq(GaActionLog::getActionType, ActionType.VOTE));

        if (voteLogs.isEmpty()) {
            return null;
        }

        // 统计得票
        Map<Long, Integer> voteCount = new HashMap<>();
        for (GaActionLog log : voteLogs) {
            if (log.getTargetIds() != null && !log.getTargetIds().isEmpty()) {
                Long targetId = log.getTargetIds().get(0);
                voteCount.merge(targetId, 1, Integer::sum);
            }
        }

        if (voteCount.isEmpty()) {
            return null;
        }

        // 找出票数最高者
        int maxVotes = Collections.max(voteCount.values());
        Long topPlayerId = voteCount.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (topPlayerId == null) {
            return null;
        }

        // 构建返回数据（精简：只返回 playerId 和 voteCount）
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", topPlayerId);
        data.put("voteCount", maxVotes);

        PlayerSkillVO.PreData preData = new PlayerSkillVO.PreData();
        preData.setType(PreDataType.TOP_VOTED_PLAYER.name());
        preData.setData(data);

        return preData;
    }

    // ==================== PostData 处理（使用后） ====================

    /**
     * 解析技能使用后的结果数据
     */
    public PlayerSkillResultVO resolvePostData(CfgSkill skill, List<Long> targetIds, GaGameRecord record) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getDataRule() == null) {
            return null;
        }

        PostDataType type = config.getDataRule().getPostDataType();
        if (type == null || type == PostDataType.NONE) {
            return null;
        }

        return switch (type) {
            case TARGET_CAMP -> buildTargetCamp(skill, targetIds);
            case TARGET_ROLE -> buildTargetRole(skill, targetIds);
            case SAME_CAMP -> buildSameCamp(skill, targetIds);
            default -> null;
        };
    }

    /**
     * 构建目标阵营结果
     */
    private PlayerSkillResultVO buildTargetCamp(CfgSkill skill, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return null;
        }

        Long targetId = targetIds.get(0);
        GaGamePlayer target = gaGamePlayerMapper.selectById(targetId);
        if (target == null || target.getRoleId() == null) {
            return null;
        }

        CfgRole role = cfgRoleMapper.selectById(target.getRoleId());
        if (role == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetId", targetId);
        result.put("campType", role.getCampType().name());

        PlayerSkillResultVO vo = new PlayerSkillResultVO();
        vo.setSkillId(skill.getId());
        vo.setResultType(PostDataType.TARGET_CAMP.name());
        vo.setResult(result);

        return vo;
    }

    /**
     * 构建目标角色结果
     */
    private PlayerSkillResultVO buildTargetRole(CfgSkill skill, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return null;
        }

        Long targetId = targetIds.get(0);
        GaGamePlayer target = gaGamePlayerMapper.selectById(targetId);
        if (target == null || target.getRoleId() == null) {
            return null;
        }

        CfgRole role = cfgRoleMapper.selectById(target.getRoleId());
        if (role == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetId", targetId);
        result.put("roleId", role.getId());

        PlayerSkillResultVO vo = new PlayerSkillResultVO();
        vo.setSkillId(skill.getId());
        vo.setResultType(PostDataType.TARGET_ROLE.name());
        vo.setResult(result);

        return vo;
    }

    /**
     * 构建两人是否同阵营结果
     */
    private PlayerSkillResultVO buildSameCamp(CfgSkill skill, List<Long> targetIds) {
        if (targetIds == null || targetIds.size() < 2) {
            return null;
        }

        GaGamePlayer target1 = gaGamePlayerMapper.selectById(targetIds.get(0));
        GaGamePlayer target2 = gaGamePlayerMapper.selectById(targetIds.get(1));
        if (target1 == null || target2 == null) {
            return null;
        }

        CfgRole role1 = target1.getRoleId() != null ? cfgRoleMapper.selectById(target1.getRoleId()) : null;
        CfgRole role2 = target2.getRoleId() != null ? cfgRoleMapper.selectById(target2.getRoleId()) : null;
        if (role1 == null || role2 == null) {
            return null;
        }

        boolean isSameCamp = role1.getCampType() == role2.getCampType();

        Map<String, Object> result = new HashMap<>();
        result.put("target1Id", targetIds.get(0));
        result.put("target2Id", targetIds.get(1));
        result.put("isSameCamp", isSameCamp);

        PlayerSkillResultVO vo = new PlayerSkillResultVO();
        vo.setSkillId(skill.getId());
        vo.setResultType(PostDataType.SAME_CAMP.name());
        vo.setResult(result);

        return vo;
    }
}
