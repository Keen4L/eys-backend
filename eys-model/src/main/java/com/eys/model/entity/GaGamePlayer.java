package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 对局玩家绑定
 */
@Data
@TableName("ga_game_player")
public class GaGamePlayer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对局ID
     */
    private Long gameId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 座位号
     */
    private Integer seatNo;

    /**
     * 分配的角色ID
     */
    private Long roleId;
}
