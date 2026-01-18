package com.eys.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ========== 成功 ==========
    SUCCESS(200, "操作成功"),

    // ========== 客户端错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // ========== 业务错误 5xx ==========
    BIZ_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(501, "系统内部错误"),

    // ========== 自定义业务错误码 1xxx ==========
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "用户已禁用"),

    GAME_NOT_FOUND(1101, "游戏不存在"),
    GAME_NOT_WAITING(1102, "游戏不在等待状态"),
    GAME_NOT_PLAYING(1103, "游戏不在进行中"),
    GAME_ALREADY_STARTED(1104, "游戏已开始"),
    GAME_PLAYER_FULL(1105, "游戏人数已满"),
    GAME_PLAYER_NOT_ENOUGH(1106, "玩家人数不足"),

    ROOM_NOT_FOUND(1201, "房间不存在"),
    ROOM_CODE_INVALID(1202, "房间码无效"),
    ROOM_ALREADY_JOINED(1203, "已在房间中"),
    ROOM_NOT_JOINED(1204, "不在房间中"),
    ROOM_FULL(1205, "房间人数已满"),

    ROLE_NOT_FOUND(1301, "角色不存在"),
    ROLE_COUNT_MISMATCH(1302, "角色数量与玩家数量不匹配"),

    SKILL_NOT_FOUND(1401, "技能不存在"),
    SKILL_NOT_AVAILABLE(1402, "技能不可用"),
    SKILL_ALREADY_USED(1403, "技能本阶段已使用"),
    INVALID_TARGET(1404, "目标不合法"),
    SKILL_USAGE_EXHAUSTED(1405, "技能次数已用尽"),

    PLAYER_NOT_FOUND(1501, "玩家不存在"),
    PLAYER_ALREADY_DEAD(1502, "玩家已死亡"),
    PLAYER_ALREADY_ALIVE(1503, "玩家已存活"),
    PLAYER_BLOCKED(1504, "玩家被封锁"),
    PLAYER_COUNT_MISMATCH(1505, "玩家人数与角色数量不匹配"),

    VOTE_NOT_ALLOWED(1601, "当前阶段不可投票"),
    VOTE_ALREADY_SUBMITTED(1602, "已提交投票"),
    NOT_VOTE_STAGE(1603, "当前不是投票阶段"),
    VOTE_BLOCKED(1604, "投票权已被封锁");

    /**
     * 响应码
     */
    private final int code;

    /**
     * 响应消息
     */
    private final String message;
}
