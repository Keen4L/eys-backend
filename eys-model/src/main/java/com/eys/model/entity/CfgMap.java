package com.eys.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地图配置
 */
@Data
@TableName("cfg_map")
public class CfgMap {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 地图名称
     */
    private String name;

    /**
     * 地图底图资源URL
     */
    private String backgroundUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
