package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.eys.model.json.TagInfo;
import com.eys.model.enums.GameStage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 玩家实时状态
 */
@Data
@TableName(value = "ga_player_status", autoResultMap = true)
public class GaPlayerStatus {

    /**
     * 对局玩家ID（与 ga_game_player.id 一对一）
     */
    @TableId
    private Long gamePlayerId;

    /**
     * 存活状态
     */
    private Boolean isAlive;

    /**
     * 死亡发生的回合数
     */
    private Integer deathRound;

    /**
     * 死亡发生的阶段
     */
    private GameStage deathStage;

    /**
     * 当前生效的状态标签
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TagInfo> activeTags;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}
