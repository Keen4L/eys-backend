package com.eys.admin.service;

import com.eys.model.vo.LeaderboardVO;
import com.eys.model.vo.UserStatsVO;

import java.util.List;

/**
 * 管理端统计服务接口
 */
public interface AdminStatsService {

    /**
     * 获取总排行榜
     *
     * @param limit 返回条数
     * @return 排行榜数据
     */
    List<LeaderboardVO> getLeaderboard(int limit);

    /**
     * 获取阵营维度排行榜
     *
     * @param campType 阵营类型
     * @param limit    返回条数
     * @return 排行榜数据
     */
    List<LeaderboardVO> getLeaderboardByCamp(String campType, int limit);

    /**
     * 获取角色维度排行榜
     *
     * @param roleId 角色ID
     * @param limit  返回条数
     * @return 排行榜数据
     */
    List<LeaderboardVO> getLeaderboardByRole(Long roleId, int limit);

    /**
     * 获取用户详细战绩
     *
     * @param userId 用户ID
     * @return 用户战绩统计
     */
    UserStatsVO getUserStats(Long userId);
}
