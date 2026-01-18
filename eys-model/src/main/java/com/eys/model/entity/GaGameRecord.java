package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.eys.model.enums.CampType;
import com.eys.model.enums.GameStage;
import com.eys.model.enums.GameStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对局主记录
 */
@Data
@TableName(value = "ga_game_record", autoResultMap = true)
public class GaGameRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间邀请码
     */
    private String roomCode;

    /**
     * 本场DM的用户ID
     */
    private Long dmUserId;

    /**
     * 使用的地图ID
     */
    private Long mapId;

    /**
     * 本局使用的角色ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> roleIds;

    /**
     * 游戏状态
     */
    private GameStatus status;

    /**
     * 当前回合数
     */
    private Integer currentRound;

    /**
     * 当前阶段
     */
    private GameStage currentStage;

    /**
     * 胜利阵营
     */
    private CampType victoryType;

    /**
     * 中立获胜者的用户ID
     */
    private Long winnerUserId;

    /**
     * 游戏正式开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 游戏结束时间
     */
    private LocalDateTime finishedAt;
}
