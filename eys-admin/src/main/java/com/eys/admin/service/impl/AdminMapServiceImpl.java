package com.eys.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.admin.service.AdminMapService;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.CfgMapMapper;
import com.eys.mapper.CfgMapSpawnPointMapper;
import com.eys.model.entity.CfgMap;
import com.eys.model.entity.CfgMapSpawnPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地图管理服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminMapServiceImpl implements AdminMapService {

    private final CfgMapMapper cfgMapMapper;
    private final CfgMapSpawnPointMapper cfgMapSpawnPointMapper;

    @Override
    public List<CfgMap> getMapList() {
        return cfgMapMapper.selectList(new LambdaQueryWrapper<CfgMap>().orderByAsc(CfgMap::getId));
    }

    @Override
    public CfgMap getMapDetail(Long id) {
        CfgMap map = cfgMapMapper.selectById(id);
        if (map == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地图不存在");
        }
        return map;
    }

    @Override
    public List<CfgMapSpawnPoint> getSpawnPoints(Long mapId) {
        return cfgMapSpawnPointMapper.selectList(
                new LambdaQueryWrapper<CfgMapSpawnPoint>().eq(CfgMapSpawnPoint::getMapId, mapId));
    }

    @Override
    @Transactional
    public Long createMap(CfgMap map) {
        cfgMapMapper.insert(map);
        return map.getId();
    }

    @Override
    @Transactional
    public void updateMap(Long id, CfgMap map) {
        CfgMap existing = cfgMapMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地图不存在");
        }
        map.setId(id);
        cfgMapMapper.updateById(map);
    }

    @Override
    @Transactional
    public void deleteMap(Long id) {
        cfgMapSpawnPointMapper.delete(
                new LambdaQueryWrapper<CfgMapSpawnPoint>().eq(CfgMapSpawnPoint::getMapId, id));
        cfgMapMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Long createSpawnPoint(Long mapId, CfgMapSpawnPoint spawnPoint) {
        spawnPoint.setMapId(mapId);
        cfgMapSpawnPointMapper.insert(spawnPoint);
        return spawnPoint.getId();
    }

    @Override
    @Transactional
    public void deleteSpawnPoint(Long id) {
        cfgMapSpawnPointMapper.deleteById(id);
    }
}
