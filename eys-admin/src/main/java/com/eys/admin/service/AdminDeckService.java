package com.eys.admin.service;

import com.eys.model.entity.CfgDeck;

import java.util.List;

/**
 * 预设牌组管理服务接口
 */
public interface AdminDeckService {

    /**
     * 获取预设牌组列表
     *
     * @return 牌组列表
     */
    List<CfgDeck> getDeckList();

    /**
     * 获取牌组详情
     *
     * @param id 牌组ID
     * @return 牌组信息
     */
    CfgDeck getDeckDetail(Long id);

    /**
     * 新增牌组
     *
     * @param deck 牌组信息
     * @return 牌组ID
     */
    Long createDeck(CfgDeck deck);

    /**
     * 更新牌组
     *
     * @param id   牌组ID
     * @param deck 牌组信息
     */
    void updateDeck(Long id, CfgDeck deck);

    /**
     * 删除牌组
     *
     * @param id 牌组ID
     */
    void deleteDeck(Long id);
}
