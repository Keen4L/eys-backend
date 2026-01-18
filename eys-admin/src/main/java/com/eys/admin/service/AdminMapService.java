package com.eys.admin.service;

import com.eys.model.entity.CfgMap;
import com.eys.model.entity.CfgMapSpawnPoint;

import java.util.List;

/**
 * 地图管理服务接口
 */
public interface AdminMapService {

    /**
     * 获取地图列表
     *
     * @return 地图列表
     */
    List<CfgMap> getMapList();

    /**
     * 获取地图详情
     *
     * @param id 地图ID
     * @return 地图信息
     */
    CfgMap getMapDetail(Long id);

    /**
     * 获取地图出生点列表
     *
     * @param mapId 地图ID
     * @return 出生点列表
     */
    List<CfgMapSpawnPoint> getSpawnPoints(Long mapId);

    /**
     * 新增地图
     *
     * @param map 地图信息
     * @return 地图ID
     */
    Long createMap(CfgMap map);

    /**
     * 更新地图
     *
     * @param id  地图ID
     * @param map 地图信息
     */
    void updateMap(Long id, CfgMap map);

    /**
     * 删除地图
     *
     * @param id 地图ID
     */
    void deleteMap(Long id);

    /**
     * 新增出生点
     *
     * @param mapId      地图ID
     * @param spawnPoint 出生点信息
     * @return 出生点ID
     */
    Long createSpawnPoint(Long mapId, CfgMapSpawnPoint spawnPoint);

    /**
     * 删除出生点
     *
     * @param id 出生点ID
     */
    void deleteSpawnPoint(Long id);
}
