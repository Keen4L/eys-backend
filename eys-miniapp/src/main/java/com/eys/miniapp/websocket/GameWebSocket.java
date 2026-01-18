package com.eys.miniapp.websocket;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eys.mapper.GaGamePlayerMapper;
import com.eys.mapper.GaGameRecordMapper;
import com.eys.miniapp.service.GameStatePushService;
import com.eys.model.entity.GaGamePlayer;
import com.eys.model.entity.GaGameRecord;
import com.eys.model.enums.GameStatus;
import com.eys.model.enums.WsMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 游戏 WebSocket 端点
 * 用于实时推送游戏状态给 DM 和玩家
 * 
 * 核心职责：
 * 1. 管理 WebSocket 连接（按 gameId 分组）
 * 2. 处理心跳检测和超时
 * 3. 连接时验证用户是否为该对局参与者
 * 4. 提供消息推送能力
 */
@Slf4j
@Component
@ServerEndpoint("/ws/game/{gameId}")
public class GameWebSocket implements ApplicationContextAware {

    // 对局连接管理：gameId -> (userId -> session)
    private static final Map<Long, Map<Long, Session>> gameSessions = new ConcurrentHashMap<>();

    // 心跳超时管理：session -> 最后心跳时间
    private static final Map<Session, Long> lastHeartbeat = new ConcurrentHashMap<>();

    // 心跳超时时间（毫秒）
    private static final long HEARTBEAT_TIMEOUT_MS = 30_000;

    // 心跳检测间隔（毫秒）
    private static final long HEARTBEAT_CHECK_INTERVAL_MS = 10_000;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Spring ApplicationContext
    private static ApplicationContext applicationContext;

    // 心跳检测定时器
    private static final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    static {
        // 启动心跳检测任务
        heartbeatScheduler.scheduleAtFixedRate(
                GameWebSocket::checkHeartbeatTimeout,
                HEARTBEAT_CHECK_INTERVAL_MS,
                HEARTBEAT_CHECK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 连接建立
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("gameId") Long gameId) {
        try {
            // 从 URL 参数获取 token
            var tokenParams = session.getRequestParameterMap().get("token");
            if (tokenParams == null || tokenParams.isEmpty()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "缺少token参数"));
                return;
            }
            String token = tokenParams.get(0);
            if (token == null || token.isBlank()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "token为空"));
                return;
            }

            // 验证 token 并获取用户ID
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "未登录"));
                return;
            }
            Long userId = Long.parseLong(loginId.toString());

            // 验证用户是否是该对局的参与者（DM 或玩家）
            if (!validateParticipant(gameId, userId)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "非该对局参与者"));
                return;
            }

            // 加入对局
            gameSessions.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                    .put(userId, session);

            // 存储用户信息到 session
            session.getUserProperties().put("userId", userId);
            session.getUserProperties().put("gameId", gameId);

            // 初始化心跳时间
            lastHeartbeat.put(session, System.currentTimeMillis());

            log.info("用户 {} 连接到对局 {}", userId, gameId);

            // 发送欢迎消息
            sendMessage(session, WsMessageType.CONNECTED, "连接成功");

            // 自动推送当前游戏状态
            pushCurrentStateToUser(gameId, userId);

        } catch (Exception e) {
            log.error("WebSocket 连接失败", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "连接失败"));
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 验证用户是否为该对局的参与者
     */
    private boolean validateParticipant(Long gameId, Long userId) {
        if (applicationContext == null) {
            return false;
        }

        GaGameRecordMapper recordMapper = applicationContext.getBean(GaGameRecordMapper.class);
        GaGameRecord record = recordMapper.selectById(gameId);
        if (record == null) {
            return false;
        }

        // 游戏已结束或已关闭不允许连接
        if (record.getStatus() == GameStatus.FINISHED || record.getStatus() == GameStatus.CLOSED) {
            return false;
        }

        // DM 可以连接
        if (userId.equals(record.getDmUserId())) {
            return true;
        }

        // 检查是否为该对局的玩家
        GaGamePlayerMapper playerMapper = applicationContext.getBean(GaGamePlayerMapper.class);
        GaGamePlayer player = playerMapper.selectOne(
                new LambdaQueryWrapper<GaGamePlayer>()
                        .eq(GaGamePlayer::getGameId, gameId)
                        .eq(GaGamePlayer::getUserId, userId));
        return player != null;
    }

    /**
     * 推送当前游戏状态给刚连接的用户
     */
    private void pushCurrentStateToUser(Long gameId, Long userId) {
        try {
            if (applicationContext == null) {
                return;
            }

            GaGameRecordMapper recordMapper = applicationContext.getBean(GaGameRecordMapper.class);
            GaGameRecord record = recordMapper.selectById(gameId);
            if (record == null) {
                return;
            }

            GameStatePushService pushService = applicationContext.getBean(GameStatePushService.class);

            if (record.getStatus() == GameStatus.WAITING) {
                // 等待中：推送房间信息
                var roomInfo = pushService.buildRoomInfo(gameId);
                if (roomInfo != null) {
                    sendToUser(gameId, userId, WsMessageType.ROOM_UPDATE, roomInfo);
                }
            } else if (record.getStatus() == GameStatus.PLAYING) {
                // 游戏进行中：先推送房间信息（用于头像昵称等数据显示）
                var roomInfo = pushService.buildRoomInfo(gameId);
                if (roomInfo != null) {
                    sendToUser(gameId, userId, WsMessageType.ROOM_UPDATE, roomInfo);
                }

                // 然后推送游戏状态
                if (userId.equals(record.getDmUserId())) {
                    // DM 视图
                    var dmState = pushService.buildDmGameState(gameId);
                    if (dmState != null) {
                        sendToUser(gameId, userId, WsMessageType.GAME_STATE, dmState);
                    }
                } else {
                    // 玩家视图
                    var playerState = pushService.buildPlayerGameState(userId, gameId);
                    if (playerState != null) {
                        sendToUser(gameId, userId, WsMessageType.GAME_STATE, playerState);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("推送初始状态失败: userId={}, gameId={}", userId, gameId, e);
        }
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        Long gameId = (Long) session.getUserProperties().get("gameId");

        // 清理心跳记录
        lastHeartbeat.remove(session);

        if (userId != null && gameId != null) {
            // 清理连接记录
            Map<Long, Session> sessions = gameSessions.get(gameId);
            if (sessions != null) {
                sessions.remove(userId);
                if (sessions.isEmpty()) {
                    gameSessions.remove(gameId);
                }
            }

            log.info("用户 {} 断开对局 {} 连接", userId, gameId);
        }
    }

    /**
     * 收到消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        // 更新心跳时间
        lastHeartbeat.put(session, System.currentTimeMillis());

        // 心跳处理
        if ("ping".equals(message)) {
            sendMessage(session, WsMessageType.PONG, null);
        }
    }

    /**
     * 发生错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误", error);
    }

    // ==================== 心跳检测 ====================

    /**
     * 检查心跳超时
     */
    private static void checkHeartbeatTimeout() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Session, Long> entry : lastHeartbeat.entrySet()) {
            Session session = entry.getKey();
            long lastTime = entry.getValue();
            if (now - lastTime > HEARTBEAT_TIMEOUT_MS) {
                log.warn("Session 心跳超时，强制关闭连接");
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "心跳超时"));
                } catch (IOException e) {
                    log.error("关闭超时连接失败", e);
                }
            }
        }
    }

    // ==================== 静态推送方法 ====================

    /**
     * 向对局内所有用户推送消息（带消息类型）
     */
    public static void broadcastToGame(Long gameId, WsMessageType type, Object data) {
        Map<Long, Session> sessions = gameSessions.get(gameId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sessions.values().forEach(session -> sendMessage(session, type, data));
    }

    /**
     * 向指定用户推送消息（带消息类型）
     */
    public static void sendToUser(Long gameId, Long userId, WsMessageType type, Object data) {
        Map<Long, Session> sessions = gameSessions.get(gameId);
        if (sessions == null) {
            return;
        }
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, type, data);
        }
    }

    /**
     * 向对局内所有用户广播游戏状态
     */
    public static void broadcastToGame(Long gameId, Object data) {
        broadcastToGame(gameId, WsMessageType.GAME_STATE, data);
    }

    /**
     * 向指定用户推送游戏状态
     */
    public static void sendToUser(Long gameId, Long userId, Object data) {
        sendToUser(gameId, userId, WsMessageType.GAME_STATE, data);
    }

    /**
     * 向指定用户推送技能
     */
    public static void pushSkillToUser(Long gameId, Long userId, Object skillData) {
        sendToUser(gameId, userId, WsMessageType.SKILL_PUSH, skillData);
    }

    /**
     * 向指定用户推送技能结果
     */
    public static void pushSkillResultToUser(Long gameId, Long userId, Object resultData) {
        sendToUser(gameId, userId, WsMessageType.SKILL_RESULT, resultData);
    }

    /**
     * 向 DM 推送技能使用通知
     */
    public static void pushSkillUsedToDm(Long gameId, Long dmUserId, Object skillUsedData) {
        sendToUser(gameId, dmUserId, WsMessageType.SKILL_USED, skillUsedData);
    }

    /**
     * 关闭指定用户的 WebSocket 连接（用于踢人）
     */
    public static void closeUserSession(Long gameId, Long userId, String reason) {
        Map<Long, Session> sessions = gameSessions.get(gameId);
        if (sessions == null) {
            return;
        }
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
            } catch (IOException e) {
                log.warn("关闭 WebSocket 连接失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 检查用户在指定对局是否在线
     */
    public static boolean isUserOnline(Long gameId, Long userId) {
        Map<Long, Session> sessions = gameSessions.get(gameId);
        if (sessions == null) {
            return false;
        }
        Session session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取对局内所有在线用户ID
     */
    public static Set<Long> getOnlineUserIds(Long gameId) {
        Map<Long, Session> sessions = gameSessions.get(gameId);
        if (sessions == null) {
            return Set.of();
        }
        return sessions.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * 发送消息给单个 session
     */
    private static void sendMessage(Session session, WsMessageType type, Object data) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            WsMessage message = new WsMessage(type.name(), data, System.currentTimeMillis());
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(message));
        } catch (IOException e) {
            log.error("发送 WebSocket 消息失败", e);
        }
    }

    /**
     * WebSocket 消息结构
     */
    public record WsMessage(String type, Object data, Long timestamp) {
    }
}
