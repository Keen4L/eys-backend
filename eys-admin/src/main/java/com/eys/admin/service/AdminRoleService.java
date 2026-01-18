package com.eys.admin.service;

import com.eys.model.entity.CfgRole;
import com.eys.model.entity.CfgSkill;

import java.util.List;

/**
 * 角色管理服务接口
 */
public interface AdminRoleService {

    /**
     * 获取角色列表
     *
     * @param campType 阵营类型筛选（可为 null）
     * @return 角色列表
     */
    List<CfgRole> getRoleList(String campType);

    /**
     * 获取角色详情
     *
     * @param id 角色ID
     * @return 角色信息
     */
    CfgRole getRoleDetail(Long id);

    /**
     * 获取角色的技能列表
     *
     * @param roleId 角色ID
     * @return 技能列表
     */
    List<CfgSkill> getRoleSkills(Long roleId);

    /**
     * 新增角色
     *
     * @param role 角色信息
     * @return 角色ID
     */
    Long createRole(CfgRole role);

    /**
     * 更新角色
     *
     * @param id   角色ID
     * @param role 角色信息
     */
    void updateRole(Long id, CfgRole role);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);
}
