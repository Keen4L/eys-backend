package com.eys.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.admin.service.AdminSkillService;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.CfgSkillMapper;
import com.eys.model.entity.CfgSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 技能管理服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminSkillServiceImpl implements AdminSkillService {

    private final CfgSkillMapper cfgSkillMapper;

    @Override
    public List<CfgSkill> getSkillList(Long roleId) {
        LambdaQueryWrapper<CfgSkill> wrapper = new LambdaQueryWrapper<>();
        if (roleId != null) {
            wrapper.eq(CfgSkill::getRoleId, roleId);
        }
        wrapper.orderByAsc(CfgSkill::getRoleId).orderByAsc(CfgSkill::getId);
        return cfgSkillMapper.selectList(wrapper);
    }

    @Override
    public CfgSkill getSkillDetail(Long id) {
        CfgSkill skill = cfgSkillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能不存在");
        }
        return skill;
    }

    @Override
    @Transactional
    public Long createSkill(CfgSkill skill) {
        cfgSkillMapper.insert(skill);
        return skill.getId();
    }

    @Override
    @Transactional
    public void updateSkill(Long id, CfgSkill skill) {
        CfgSkill existing = cfgSkillMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "技能不存在");
        }
        skill.setId(id);
        cfgSkillMapper.updateById(skill);
    }

    @Override
    @Transactional
    public void deleteSkill(Long id) {
        cfgSkillMapper.deleteById(id);
    }
}
