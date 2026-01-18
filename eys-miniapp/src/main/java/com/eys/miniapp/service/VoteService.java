package com.eys.miniapp.service;

/**
 * 投票服务接口
 * 负责投票行为的验证和记录
 */
public interface VoteService {

    /**
     * 玩家投票
     *
     * @param gamePlayerId 投票者对局玩家ID
     * @param targetId     目标对局玩家ID（null 表示弃票）
     */
    void vote(Long gamePlayerId, Long targetId);
}
