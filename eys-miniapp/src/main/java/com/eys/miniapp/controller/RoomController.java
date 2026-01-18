package com.eys.miniapp.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.eys.common.result.Result;
import com.eys.miniapp.service.RoomService;
import com.eys.model.vo.RoomInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序端 - 房间管理
 * 
 * 权限说明：
 * - 创建房间：需要 DM 权限（@SaCheckRole）
 * - 其他操作：任意登录用户
 */
@Tag(name = "房间管理")
@RestController
@RequestMapping("/api/mp/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "创建房间", description = "需要DM或管理员权限")
    @PostMapping("/create")
    @SaCheckRole("DM")
    public Result<RoomInfoVO> create() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(roomService.createRoom(userId));
    }

    @Operation(summary = "加入房间")
    @PostMapping("/join")
    public Result<RoomInfoVO> join(@Parameter(description = "房间邀请码") @RequestParam String roomCode) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(roomService.joinRoom(userId, roomCode));
    }

    @Operation(summary = "离开房间")
    @PostMapping("/leave")
    public Result<Void> leave(@Parameter(description = "房间邀请码") @RequestParam String roomCode) {
        Long userId = StpUtil.getLoginIdAsLong();
        roomService.leaveRoom(userId, roomCode);
        return Result.success();
    }

    @Operation(summary = "获取房间信息")
    @GetMapping("/info")
    public Result<RoomInfoVO> info(@Parameter(description = "房间邀请码") @RequestParam String roomCode) {
        return Result.success(roomService.getRoomInfo(roomCode));
    }

    @Operation(summary = "解散房间")
    @PostMapping("/dismiss")
    public Result<Void> dismiss(@Parameter(description = "对局ID") @RequestParam Long gameId) {
        Long userId = StpUtil.getLoginIdAsLong();
        roomService.dismissRoom(userId, gameId);
        return Result.success();
    }

    @Operation(summary = "交换座位")
    @PostMapping("/swap-seats")
    public Result<Void> swapSeats(
            @Parameter(description = "房间邀请码") @RequestParam String roomCode,
            @Parameter(description = "玩家1用户ID") @RequestParam Long playerId1,
            @Parameter(description = "玩家2用户ID") @RequestParam Long playerId2) {
        Long userId = StpUtil.getLoginIdAsLong();
        roomService.swapSeats(userId, roomCode, playerId1, playerId2);
        return Result.success();
    }

    @Operation(summary = "踢出玩家")
    @PostMapping("/kick")
    public Result<Void> kick(
            @Parameter(description = "房间邀请码") @RequestParam String roomCode,
            @Parameter(description = "被踢玩家用户ID") @RequestParam Long targetUserId) {
        Long userId = StpUtil.getLoginIdAsLong();
        roomService.kickPlayer(userId, roomCode, targetUserId);
        return Result.success();
    }

    @Operation(summary = "回到房间", description = "游戏结束后，玩家通过此接口加入新对局")
    @PostMapping("/rejoin")
    public Result<RoomInfoVO> rejoinRoom(
            @Parameter(description = "房间邀请码") @RequestParam String roomCode,
            @Parameter(description = "刚结束的对局ID") @RequestParam Long oldGameId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(roomService.rejoinRoom(userId, roomCode, oldGameId));
    }
}

