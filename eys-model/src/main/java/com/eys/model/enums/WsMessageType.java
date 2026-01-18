package com.eys.model.enums;

/**
 * WebSocket 消息类型
 * 统一客户端消息格式
 */
public enum WsMessageType {

    /**
     * 游戏状态更新
     */
    GAME_STATE,

    /**
     * 技能推送（含预数据）
     */
    SKILL_PUSH,

    /**
     * 技能结果回传（仅发给使用者，如查验结果）
     */
    SKILL_RESULT,

    /**
     * 技能已使用通知（发给 DM，告知玩家使用了什么技能）
     */
    SKILL_USED,

    /**
     * 连接成功
     */
    CONNECTED,

    /**
     * 心跳响应
     */
    PONG,

    /**
     * 房间信息更新（有人加入/离开）
     */
    ROOM_UPDATE,

    /**
     * 房间被解散
     */
    ROOM_DISMISSED,

    /**
     * 被踢出房间（通知被踢玩家）
     */
    KICKED
}
