package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 地图出生点位
 */
@Data
@TableName("cfg_map_spawn_point")
public class CfgMapSpawnPoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属地图ID
     */
    private Long mapId;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 地图上X坐标
     */
    private Integer posX;

    /**
     * 地图上Y坐标
     */
    private Integer posY;

    /**
     * 交互生效区域宽度
     */
    private Integer activeWidth;

    /**
     * 交互生效区域高度
     */
    private Integer activeHeight;
}
