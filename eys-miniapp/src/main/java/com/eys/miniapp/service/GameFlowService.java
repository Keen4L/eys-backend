package com.eys.miniapp.service;

import com.eys.model.vo.game.DmPushVO;
import com.eys.model.vo.game.GameEndResultVO;

import java.util.List;
import java.util.Map;

/**
 * 游戏流程控制服务接口（DM 专用）
 * 负责游戏的开始、阶段推进、状态获取、玩家击杀/复活、游戏结束等核心逻辑
 */
public interface GameFlowService {

    /**
     * 开始游戏 - 配置游戏并分配身份
     * DM 传入地图 ID 和角色列表，可以内定部分玩家的角色，未内定的随机分配
     *
     * @param gameId        对局ID
     * @param mapId         地图ID
     * @param roleIds       角色ID列表（数量需与玩家人数一致）
     * @param assignedRoles 内定角色映射（gamePlayerId -> roleId），可为 null 表示全随机
     * @return DM 游戏状态
     */
    DmPushVO startGame(Long gameId, Long mapId, List<Long> roleIds, Map<Long, Long> assignedRoles);

    /**
     * 推进到下一阶段（DAY → NIGHT 时自动进入下一回合）
     *
     * @param gameId 对局ID
     * @return DM 游戏状态
     */
    DmPushVO nextStage(Long gameId);

    /**
     * 击杀玩家（批量）
     *
     * @param gameId          对局ID
     * @param targetPlayerIds 目标对局玩家ID列表
     */
    void killPlayers(Long gameId, List<Long> targetPlayerIds);

    /**
     * 复活玩家（批量）
     *
     * @param gameId          对局ID
     * @param targetPlayerIds 目标对局玩家ID列表
     */
    void revivePlayers(Long gameId, List<Long> targetPlayerIds);

    /**
     * 结束游戏
     * 会自动创建新对局（同房间码、同DM、无玩家）
     *
     * @param gameId      对局ID
     * @param victoryType 胜利阵营
     * @return 游戏结束结果（含新对局ID）
     */
    GameEndResultVO endGame(Long gameId, String victoryType);

    /**
     * 移除玩家标签（DM 专用）
     *
     * @param gameId         对局ID
     * @param targetPlayerId 目标对局玩家ID
     * @param tagIndex       标签索引
     */
    void removeTag(Long gameId, Long targetPlayerId, int tagIndex);

    /**
     * DM 手动推送技能给玩家
     *
     * @param gameId   对局ID
     * @param playerId 对局玩家ID
     * @param skillId  技能ID（可选，null 表示推送该玩家所有可用技能）
     */
    void pushSkillToPlayer(Long gameId, Long playerId, Long skillId);
}
