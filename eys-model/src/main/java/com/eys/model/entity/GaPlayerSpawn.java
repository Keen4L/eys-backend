package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 玩家出生点记录
 */
@Data
@TableName("ga_player_spawn")
public class GaPlayerSpawn {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对局ID
     */
    private Long gameId;

    /**
     * 回合数
     */
    private Integer roundNo;

    /**
     * 对局玩家ID
     */
    private Long gamePlayerId;

    /**
     * 出生点ID
     */
    private Long spawnPointId;
}
