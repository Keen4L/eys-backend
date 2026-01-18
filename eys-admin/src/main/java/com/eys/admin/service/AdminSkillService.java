package com.eys.admin.service;

import com.eys.model.entity.CfgSkill;

import java.util.List;

/**
 * 技能管理服务接口
 */
public interface AdminSkillService {

    /**
     * 获取技能列表
     *
     * @param roleId 角色ID（可为 null 表示获取全部）
     * @return 技能列表
     */
    List<CfgSkill> getSkillList(Long roleId);

    /**
     * 获取技能详情
     *
     * @param id 技能ID
     * @return 技能信息
     */
    CfgSkill getSkillDetail(Long id);

    /**
     * 新增技能
     *
     * @param skill 技能信息
     * @return 技能ID
     */
    Long createSkill(CfgSkill skill);

    /**
     * 更新技能
     *
     * @param id    技能ID
     * @param skill 技能信息
     */
    void updateSkill(Long id, CfgSkill skill);

    /**
     * 删除技能
     *
     * @param id 技能ID
     */
    void deleteSkill(Long id);
}
