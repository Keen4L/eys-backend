package com.eys.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.admin.service.AdminDeckService;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.CfgDeckMapper;
import com.eys.model.entity.CfgDeck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预设牌组管理服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminDeckServiceImpl implements AdminDeckService {

    private final CfgDeckMapper cfgDeckMapper;

    @Override
    public List<CfgDeck> getDeckList() {
        return cfgDeckMapper.selectList(
                new LambdaQueryWrapper<CfgDeck>().orderByAsc(CfgDeck::getId));
    }

    @Override
    public CfgDeck getDeckDetail(Long id) {
        CfgDeck deck = cfgDeckMapper.selectById(id);
        if (deck == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "牌组不存在");
        }
        return deck;
    }

    @Override
    @Transactional
    public Long createDeck(CfgDeck deck) {
        cfgDeckMapper.insert(deck);
        return deck.getId();
    }

    @Override
    @Transactional
    public void updateDeck(Long id, CfgDeck deck) {
        CfgDeck existing = cfgDeckMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "牌组不存在");
        }
        deck.setId(id);
        cfgDeckMapper.updateById(deck);
    }

    @Override
    @Transactional
    public void deleteDeck(Long id) {
        cfgDeckMapper.deleteById(id);
    }
}
