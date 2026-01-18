package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.eys.model.json.SkillEffectConfig;
import lombok.Data;

/**
 * 技能配置
 */
@Data
@TableName(value = "cfg_skill", autoResultMap = true)
public class CfgSkill {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属角色ID
     */
    private Long roleId;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 技能图标URL
     */
    private String imgUrl;

    /**
     * 技能效果配置JSON（五规则结构）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private SkillEffectConfig skillConfig;
}
