package com.eys.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.admin.service.AdminRoleService;
import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;
import com.eys.mapper.CfgRoleMapper;
import com.eys.mapper.CfgSkillMapper;
import com.eys.model.entity.CfgRole;
import com.eys.model.entity.CfgSkill;
import com.eys.model.enums.CampType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final CfgRoleMapper cfgRoleMapper;
    private final CfgSkillMapper cfgSkillMapper;

    @Override
    public List<CfgRole> getRoleList(String campType) {
        LambdaQueryWrapper<CfgRole> wrapper = new LambdaQueryWrapper<>();
        if (campType != null && !campType.isEmpty()) {
            wrapper.eq(CfgRole::getCampType, CampType.valueOf(campType));
        }
        wrapper.orderByAsc(CfgRole::getId);
        return cfgRoleMapper.selectList(wrapper);
    }

    @Override
    public CfgRole getRoleDetail(Long id) {
        CfgRole role = cfgRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    @Override
    public List<CfgSkill> getRoleSkills(Long roleId) {
        return cfgSkillMapper.selectList(
                new LambdaQueryWrapper<CfgSkill>().eq(CfgSkill::getRoleId, roleId));
    }

    @Override
    @Transactional
    public Long createRole(CfgRole role) {
        cfgRoleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(Long id, CfgRole role) {
        CfgRole existing = cfgRoleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        role.setId(id);
        cfgRoleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        cfgSkillMapper.delete(new LambdaQueryWrapper<CfgSkill>().eq(CfgSkill::getRoleId, id));
        cfgRoleMapper.deleteById(id);
    }
}
