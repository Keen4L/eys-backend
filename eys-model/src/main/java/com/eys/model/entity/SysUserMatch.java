package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eys.model.enums.CampType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户对局战绩明细
 */
@Data
@TableName("sys_user_match")
public class SysUserMatch {

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
     * 本局扮演的角色ID
     */
    private Long roleId;

    /**
     * 本局所属阵营
     */
    private CampType campType;

    /**
     * 是否获胜
     */
    private Boolean isWinner;

    /**
     * 游戏结束时间
     */
    private LocalDateTime playedAt;
}
