package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.eys.model.enums.ActionSourceType;
import com.eys.model.enums.ActionType;
import com.eys.model.enums.GameStage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 全量动作流水
 */
@Data
@TableName(value = "ga_action_log", autoResultMap = true)
public class GaActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对局ID
     */
    private Long gameId;

    /**
     * 发生的回合数
     */
    private Integer roundNo;

    /**
     * 发生的阶段
     */
    private GameStage stage;

    /**
     * 动作来源
     */
    private ActionSourceType sourceType;

    /**
     * 动作类型
     */
    private ActionType actionType;

    /**
     * 发起者ID（对局玩家ID）
     */
    private Long initiatorId;

    /**
     * 目标ID数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> targetIds;

    /**
     * 关联技能ID
     */
    private Long skillId;

    /**
     * 动作发生时间
     */
    private LocalDateTime createdAt;
}
