package com.eys.common.constants;

/**
 * 系统常量
 */
public final class SystemConstants {

    private SystemConstants() {
    }

    // ========== 房间相关 ==========

    /**
     * 房间码长度
     */
    public static final int ROOM_CODE_LENGTH = 6;

    /**
     * 房间码字符集（避免混淆的字符）
     */
    public static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    // ========== 分页相关 ==========

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页条数
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页条数
     */
    public static final int MAX_PAGE_SIZE = 100;

    // ========== WebSocket 相关 ==========

    /**
     * WebSocket 心跳间隔（秒）
     */
    public static final int WS_HEARTBEAT_INTERVAL = 30;

    // ========== 动作日志相关 ==========

    /**
     * 最近动作日志数量（DM 视图）
     */
    public static final int RECENT_ACTION_LOG_LIMIT = 50;

    // ========== 技能相关 ==========

    /**
     * 普通刀技能名称（用于继承效果）
     * 如果目标不是鹅，则继承名称包含此关键词的技能
     */
    public static final String KNIFE_SKILL_NAME_KEYWORD = "刀";
}
