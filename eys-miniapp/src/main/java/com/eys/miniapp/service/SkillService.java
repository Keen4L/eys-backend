package com.eys.miniapp.service;

import java.util.List;

/**
 * 技能服务接口
 */
public interface SkillService {

    /**
     * 使用技能
     *
     * @param gamePlayerId 发起者对局玩家ID
     * @param skillId      技能配置ID
     * @param targetIds    目标对局玩家ID列表
     */
    void useSkill(Long gamePlayerId, Long skillId, List<Long> targetIds);
}
