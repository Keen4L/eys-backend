package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eys.model.enums.CampType;
import lombok.Data;

/**
 * 角色定义
 */
@Data
@TableName("cfg_role")
public class CfgRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 所属阵营
     */
    private CampType campType;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色卡牌/头像图片URL
     */
    private String imgUrl;

    /**
     * 是否启用
     */
    private Boolean isEnabled;
}
