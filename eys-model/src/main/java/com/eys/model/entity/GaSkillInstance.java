package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 技能运行时实例
 */
@Data
@TableName("ga_skill_instance")
public class GaSkillInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对局玩家ID
     */
    private Long gamePlayerId;

    /**
     * 技能配置ID
     */
    private Long skillId;

    /**
     * 技能是否可用
     */
    private Boolean isActive;

    /**
     * 已使用次数
     */
    private Integer usedCount;
}
