package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预设牌组
 */
@Data
@TableName(value = "cfg_deck", autoResultMap = true)
public class CfgDeck {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 牌组名称
     */
    private String name;

    /**
     * 适用玩家人数
     */
    private Integer playerCount;

    /**
     * 角色ID数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> roleIds;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
