package com.eys.admin.service;

import com.eys.model.entity.GaGameRecord;

import java.util.List;

/**
 * 对局管理服务接口
 */
public interface AdminGameService {

    /**
     * 获取对局列表
     *
     * @param status 游戏状态筛选（可为 null）
     * @return 对局列表
     */
    List<GaGameRecord> getGameList(String status);

    /**
     * 获取对局详情
     *
     * @param id 对局ID
     * @return 对局信息
     */
    GaGameRecord getGameDetail(Long id);

    /**
     * 删除对局记录
     *
     * @param id 对局ID
     */
    void deleteGame(Long id);
}
