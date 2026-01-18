-- ==========================================
-- 鹅鸭杀面杀辅助工具数据库结构初始化 (1_init_schema.sql)
-- ==========================================
-- 版本: 2.1
-- 最后更新: 2026-01-14
-- 说明: 本文件定义了线下鹅鸭杀面杀辅助工具的完整数据库结构
-- ==========================================
--
-- 变更记录:
-- [2026-01-14] v2.1 - 更新项目名称为"鹅鸭杀面杀辅助工具"，完善注释
-- [2026-01-13] v2.0 - 初始版本，定义完整数据库结构
-- ==========================================

-- ==========================================
-- 枚举类型定义 (供代码参考, SQL使用varchar存储)
-- ==========================================
-- 
-- [CampType] 阵营类型:
--   GOOSE    - 鹅阵营（好人）
--   DUCK     - 鸭阵营（坏人）
--   NEUTRAL  - 中立阵营
--
-- [RoleType] 用户权限类型:
--   PLAYER   - 普通玩家
--   DM       - 主持人（Dungeon Master）
--   ADMIN    - 后台管理员
--
-- [GameStatus] 游戏状态:
--   WAITING  - 等待中（房间已创建，等待玩家加入）
--   PLAYING  - 游戏进行中
--   FINISHED - 游戏正常结束
--   CLOSED   - 游戏被强制关闭
--
-- [GameStage] 游戏阶段:
--   START - 游戏开始（第一回合开始前）
--   NIGHT - 夜晚阶段（技能发动）
--   VOTE  - 投票阶段
--   DAY   - 白天阶段
--
-- [VictoryType] 胜利类型:
--   GOOSE   - 鹅阵营胜利
--   DUCK    - 鸭阵营胜利
--   NEUTRAL - 中立阵营胜利（特定中立角色单独获胜）
--
-- [ActionSourceType] 动作来源类型（记录动作是如何触发的）:
--   DM_PUSH      - DM手动推送给玩家执行
--   DM_EXECUTE   - DM直接代为执行（玩家不参与）
--
-- [ActionType] 动作类型:
--   SKILL   - 技能使用
--   KILL    - 击杀玩家
--   REVIVE  - 复活玩家
--   VOTE    - 投票
--   SYSTEM  - 系统操作
--
-- [TriggerMode] 技能触发方式:
--   DM_PUSH      - DM推送给玩家（玩家主动使用）
--   DM_EXECUTE   - DM代为执行（被动技能/DM裁决）
--
-- [EffectType] 技能效果类型:
--   TAG     - 对目标添加状态标签
--   INHERIT - 继承/复制技能（为玩家实例化新技能）
--   NONE    - 无系统效果（纯记录，如普通刀）
-- 注意：所有动作都会记录到 ActionLog，无需单独的 LOG 类型
--
-- [TargetSelectMode] 目标选择方式:
--   MANUAL_PLAYER      - 手动选人（玩家列表）
--   MANUAL_PLAYER_ROLE - 手动选人+选角色（如刺客鸭刺验）
--   AUTO_ATTACKER      - 自动-出刀人（攻击自己的人）
--   AUTO_ALIVE_DUCK    - 自动-存活的鸭子
--   AUTO_ALIVE_GOOSE   - 自动-存活的鹅
--   AUTO_LEFT_RIGHT    - 自动-左一或右一
--   NONE               - 无目标
--
-- [AliveState] 目标存活状态要求:
--   ALIVE  - 仅限存活玩家
--   DEAD   - 仅限死亡玩家
--   ANY    - 不限
--
-- [TargetRestriction] 目标额外限制条件:
--   NO_REPEAT_TARGET          - 不可重复选择同一目标（全局）
--   NO_CONSECUTIVE_SAME_TARGET - 不可连续两回合选择同一目标
--
-- [TagEffect] 状态标签效果（影响系统给玩家推送的东西）:
--   BLOCK_SKILL   - 封锁技能（不推送技能）
--   BLOCK_VOTE    - 封锁投票（不推送投票）
--
-- [TagExpiry] 标签过期时机:
--   NEXT_ROUND    - 过1回合后过期
--   AFTER_2_ROUND - 过2回合后过期
--   PERMANENT     - 永久生效（直到游戏结束）
--
--
-- [InheritMode] 继承模式:
--   ALL                  - 继承目标所有技能
--   GOOSE_ALL_ELSE_KNIFE - 鹅继承全部技能，否则继承刀
--
-- [PreDataType] 预数据类型（技能推送时携带）:
--   NONE             - 无
--   TOP_VOTED_PLAYER - 上回合票数最高者
--
-- [PostDataType] 结果数据类型（技能使用后回传）:
--   NONE        - 无
--   TARGET_CAMP - 目标阵营
--   TARGET_ROLE - 目标角色
--   SAME_CAMP   - 两人是否同阵营
--
-- ==========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `eys_game` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `eys_game`;


-- ==========================================
-- 1. 系统用户与战绩模块 (System & Statistics)
-- ==========================================

-- 1.1 系统用户表
-- 说明: 存储所有用户信息，包括微信小程序用户和后台管理员
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `openid` varchar(64) UNIQUE DEFAULT NULL COMMENT '微信小程序OpenID（小程序用户必填）',
  `username` varchar(50) UNIQUE DEFAULT NULL COMMENT '后台管理员登录账号（管理员必填）',
  `password` varchar(100) DEFAULT NULL COMMENT 'BCrypt加密后的密码（管理员必填）',
  `nickname` varchar(50) NOT NULL COMMENT '用户昵称（显示名）',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `role_type` varchar(20) DEFAULT 'PLAYER' COMMENT '用户权限 [RoleType]: PLAYER-玩家, DM-主持人, ADMIN-管理员',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户基础表';


-- 1.2 用户对局战绩明细表
-- 说明: 每局游戏结束后，为每位参与玩家生成一条战绩记录
--       统计数据（总场次、胜率、角色胜率等）通过聚合查询动态计算
CREATE TABLE IF NOT EXISTS `sys_user_match` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `game_id` bigint NOT NULL COMMENT '对局ID，关联 ga_game_record.id',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联 sys_user.id',
  `role_id` bigint NOT NULL COMMENT '本局扮演的角色ID，关联 cfg_role.id',
  `camp_type` varchar(20) NOT NULL COMMENT '本局所属阵营 [CampType]: GOOSE-鹅, DUCK-鸭, NEUTRAL-中立',
  `is_winner` tinyint NOT NULL DEFAULT 0 COMMENT '是否获胜: 1-胜利, 0-失败',
  `played_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '游戏结束时间（战绩记录时间）',
  UNIQUE KEY `uk_game_user` (`game_id`, `user_id`) COMMENT '同一对局同一用户只有一条记录',
  INDEX `idx_user` (`user_id`) COMMENT '按用户查询战绩',
  INDEX `idx_user_role` (`user_id`, `role_id`) COMMENT '按用户+角色查询战绩'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户对局战绩明细表';


-- ==========================================
-- 2. 核心配置模块 (Configuration)
-- 说明: 存储游戏的静态配置数据，如地图、角色、技能等
-- ==========================================

-- 2.1 地图基础配置表
-- 说明: 定义游戏可用的地图
CREATE TABLE IF NOT EXISTS `cfg_map` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '地图名称（如: 太空站、丛林等）',
  `background_url` varchar(255) NOT NULL COMMENT '地图底图资源URL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图基础配置表';


-- 2.2 地图出生点位表
-- 说明: 定义地图上的出生点/区域位置，用于随机分配玩家出生位置
CREATE TABLE IF NOT EXISTS `cfg_map_spawn_point` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `map_id` bigint NOT NULL COMMENT '所属地图ID，关联 cfg_map.id',
  `area_name` varchar(50) NOT NULL COMMENT '区域名称（如: 实验室、餐厅、引擎室等）',
  `pos_x` int NOT NULL COMMENT '地图上X坐标（像素）',
  `pos_y` int NOT NULL COMMENT '地图上Y坐标（像素）',
  `active_width` int DEFAULT 60 COMMENT '交互生效区域宽度（像素）',
  `active_height` int DEFAULT 60 COMMENT '交互生效区域高度（像素）',
  INDEX `idx_map` (`map_id`) COMMENT '按地图查询出生点'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图出生点位配置表';


-- 2.3 角色定义表
-- 说明: 定义游戏中所有可用角色及其阵营归属
CREATE TABLE IF NOT EXISTS `cfg_role` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(20) NOT NULL COMMENT '角色名称（如: 警长鹅、鸭王等）',
  `camp_type` varchar(20) NOT NULL COMMENT '所属阵营 [CampType]: GOOSE-鹅(好人), DUCK-鸭(坏人), NEUTRAL-中立',
  `description` text COMMENT '角色描述/技能说明',
  `img_url` varchar(255) COMMENT '角色卡牌/头像图片URL',
  `is_enabled` tinyint DEFAULT 1 COMMENT '是否启用: 1-启用（可被选入牌组）, 0-禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色定义表';


-- 2.4 技能配置表
-- 说明: 定义角色拥有的技能，使用JSON配置技能规则
--       一个角色可以有多个技能（如鸭王有"刺杀"和"临终带走"）
--
-- [skill_config JSON结构 - 五规则]:
-- {
--   "triggerRule": {                            // → TriggerProcessor
--     "mode": "DM_PUSH|DM_EXECUTE",             // [TriggerMode] 触发方式
--     "isNormal": true|false                  // 可选：是否为普通技能（仅DM_EXECUTE有效）
--   },
--   "limitRule": {                              // → LimitProcessor
--     "totalMax": -1,                           // 全局最大使用次数，-1表示无限制
--     "minRound": 1,                            // 最小可用回合
--     "invalidCondition": "GUARD_SUCCESS"       // 可选：失效条件
--   },
--   "targetRule": {                             // → TargetProcessor
--     "selectMode": "MANUAL_PLAYER|AUTO_*",     // [TargetSelectMode] 选择模式
--     "aliveState": "ALIVE|DEAD|ANY",           // [AliveState] 目标存活状态
--     "excludeSelf": true|false,                // 是否排除自己
--     "countRange": [1, 1],                     // 选择数量范围 [min, max]
--     "restriction": "NO_REPEAT_TARGET"         // 可选：[TargetRestriction] 额外限制
--   },
--   "effectRule": {                             // → EffectProcessor
--     "type": "TAG|INHERIT|NONE",               // [EffectType] 效果类型
--     "tagEffects": ["BLOCK_SKILL", ...],       // type=TAG时：添加的标签效果
--     "tagExpiry": "NEXT_ROUND",                // type=TAG时：标签过期时机
--     "inheritMode": "ALL|GOOSE_ALL_ELSE_KNIFE" // type=INHERIT时：继承模式
--   },
--   "dataRule": {                               // → DataProcessor
--     "preDataType": "TOP_VOTED_PLAYER",        // 可选：推送时携带的预数据类型
--     "postDataType": "TARGET_CAMP"             // 可选：使用后回传的结果类型
--   }
-- }
CREATE TABLE IF NOT EXISTS `cfg_skill` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '所属角色ID，关联 cfg_role.id',
  `name` varchar(50) NOT NULL COMMENT '技能名称（如: 处决、禁闭等）',
  `description` text COMMENT '技能描述',
  `img_url` varchar(255) COMMENT '技能图标URL',
  `skill_config` json COMMENT '技能配置JSON（五规则结构：triggerRule/limitRule/targetRule/effectRule/dataRule）',
  INDEX `idx_role` (`role_id`) COMMENT '按角色查询技能'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能配置表';


-- 2.5 预设牌组表
-- 说明: DM可使用预设牌组快速开局，也可自定义角色组合
-- 
-- [role_ids JSON结构]: 角色ID数组
-- 示例: [1, 2, 3, 4, 5, 19, 20, 21]  // 8个角色的牌组
CREATE TABLE IF NOT EXISTS `cfg_deck` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '牌组名称（如: 8人经典局、12人进阶局等）',
  `player_count` int NOT NULL COMMENT '适用玩家人数',
  `role_ids` json NOT NULL COMMENT '角色ID数组 [role_id, role_id, ...]',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预设牌组配置表';


-- ==========================================
-- 3. 对局运行时模块 (Game Session Runtime)
-- 说明: 存储进行中对局的实时状态数据
-- ==========================================

-- 3.1 对局主记录表
-- 说明: 每开一局游戏创建一条记录，记录游戏的整体状态
--
-- [role_ids JSON结构]: 本局使用的角色ID数组
-- 示例: [1, 4, 5, 19, 22, 24, 15, 16]
CREATE TABLE IF NOT EXISTS `ga_game_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `room_code` varchar(10) UNIQUE NOT NULL COMMENT '房间邀请码（6位数字/字母）',
  `dm_user_id` bigint NOT NULL COMMENT '本场DM的用户ID，关联 sys_user.id',
  `map_id` bigint DEFAULT NULL COMMENT '使用的地图ID，关联 cfg_map.id',
  `role_ids` json DEFAULT NULL COMMENT '本局使用的角色ID列表 [role_id, ...]',
  `status` varchar(20) DEFAULT 'WAITING' COMMENT '游戏状态 [GameStatus]: WAITING-等待, PLAYING-进行中, FINISHED-结束, CLOSED-关闭',
  `current_round` int DEFAULT 1 COMMENT '当前回合数（从1开始）',
  `current_stage` varchar(20) DEFAULT NULL COMMENT '当前阶段 [GameStage]: null(未开始), START, NIGHT, VOTE, DAY',
  `victory_type` varchar(20) DEFAULT NULL COMMENT '胜利类型 [VictoryType]: GOOSE-鹅胜, DUCK-鸭胜, NEUTRAL-中立胜',
  `winner_user_id` bigint DEFAULT NULL COMMENT '中立获胜者的用户ID（仅 victory_type=NEUTRAL时有效）',
  `started_at` datetime DEFAULT NULL COMMENT '游戏正式开始时间（身份分配完成后）',
  `finished_at` datetime DEFAULT NULL COMMENT '游戏结束时间',
  INDEX `idx_dm` (`dm_user_id`) COMMENT '按DM查询对局',
  INDEX `idx_status` (`status`) COMMENT '按状态查询对局'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对局主记录表';


-- 3.2 对局玩家绑定表
-- 说明: 记录参与本局游戏的玩家及其座位号和角色分配
CREATE TABLE IF NOT EXISTS `ga_game_player` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID（对局玩家ID，后续表引用此ID）',
  `game_id` bigint NOT NULL COMMENT '对局ID，关联 ga_game_record.id',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联 sys_user.id',
  `seat_no` int NOT NULL COMMENT '座位号（从1开始，按入座顺序分配）',
  `role_id` bigint DEFAULT NULL COMMENT '分配的角色ID，关联 cfg_role.id（游戏开始后分配）',
  UNIQUE KEY `uk_game_user` (`game_id`, `user_id`) COMMENT '同一对局同一用户只能有一个座位',
  UNIQUE KEY `uk_game_seat` (`game_id`, `seat_no`) COMMENT '同一对局同一座位只能有一个玩家',
  INDEX `idx_game` (`game_id`) COMMENT '按对局查询玩家'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对局玩家绑定表';


-- 3.3 玩家实时状态表
-- 说明: 记录玩家在对局中的实时状态（存活、死亡、标签效果等）
--
-- [active_tags JSON结构]: 当前生效的状态标签数组
-- [{
--   "skillId": 5,                    // 来源技能ID
--   "skillName": "禁闭",              // 来源技能名称
--   "appliedRound": 2,               // 被施加的回合数
--   "expiry": "NEXT_ROUND",          // [TagExpiry] 过期时机
--   "effects": ["BLOCK_SKILL"],      // [TagEffect] 效果列表
--   "expired": false                 // 是否已过期
-- }, ...]
CREATE TABLE IF NOT EXISTS `ga_player_status` (
  `game_player_id` bigint PRIMARY KEY COMMENT '对局玩家ID，关联 ga_game_player.id（一对一）',
  `is_alive` tinyint DEFAULT 1 COMMENT '存活状态: 1-存活, 0-死亡',
  `death_round` int DEFAULT NULL COMMENT '死亡发生的回合数（存活时为NULL）',
  `death_stage` varchar(20) DEFAULT NULL COMMENT '死亡发生的阶段 [GameStage]（存活时为NULL）',
  `active_tags` json DEFAULT NULL COMMENT '当前生效的状态标签JSON数组，详见上方结构说明',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家实时状态表';


-- 3.4 技能运行时实例表
-- 说明: 记录玩家拥有的技能实例及使用次数等运行时状态
CREATE TABLE IF NOT EXISTS `ga_skill_instance` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `game_player_id` bigint NOT NULL COMMENT '对局玩家ID，关联 ga_game_player.id',
  `skill_id` bigint NOT NULL COMMENT '技能配置ID，关联 cfg_skill.id',
  `is_active` tinyint DEFAULT 1 COMMENT '技能是否可用: 1-可用, 0-已失效（次数用尽或被封锁）',
  `used_count` int DEFAULT 0 COMMENT '已使用次数',
  INDEX `idx_player` (`game_player_id`) COMMENT '按玩家查询技能实例'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='技能运行时实例表';


-- ==========================================
-- 4. 动作流水与审计模块 (Action Logs)
-- 说明: 记录游戏中的所有动作，用于回溯、审计和战绩统计
-- ==========================================

-- 4.1 全量动作流水表
-- 说明: 记录所有游戏动作（技能、击杀、复活等），是游戏核心流水日志
--
-- [target_ids JSON结构]: 目标玩家ID数组
-- 示例: [3, 5]  // 两个目标的game_player_id
CREATE TABLE IF NOT EXISTS `ga_action_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `game_id` bigint NOT NULL COMMENT '对局ID，关联 ga_game_record.id',
  `round_no` int NOT NULL COMMENT '发生的回合数',
  `stage` varchar(20) NOT NULL COMMENT '发生的阶段 [GameStage]',
  `source_type` varchar(20) NOT NULL COMMENT '动作来源 [ActionSourceType]: DM_PUSH-DM推送, DM_EXECUTE-DM执行',
  `action_type` varchar(20) NOT NULL COMMENT '动作类型 [ActionType]: SKILL-技能, KILL-击杀, REVIVE-复活, VOTE-投票, SYSTEM-系统',
  `initiator_id` bigint NOT NULL COMMENT '发起者ID，关联 ga_game_player.id',
  `target_ids` json DEFAULT NULL COMMENT '目标ID数组 [game_player_id, ...]',
  `skill_id` bigint DEFAULT NULL COMMENT '关联技能ID（action_type=SKILL时有效），关联 cfg_skill.id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '动作发生时间',
  INDEX `idx_game_round` (`game_id`, `round_no`) COMMENT '按对局+回合查询动作',
  INDEX `idx_initiator` (`initiator_id`) COMMENT '按发起者查询动作'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全量动作流水审计表';


-- 4.2 玩家出生点记录表
-- 说明: 记录每回合玩家被分配的出生位置
CREATE TABLE IF NOT EXISTS `ga_player_spawn` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `game_id` bigint NOT NULL COMMENT '对局ID，关联 ga_game_record.id',
  `round_no` int NOT NULL COMMENT '回合数',
  `game_player_id` bigint NOT NULL COMMENT '对局玩家ID，关联 ga_game_player.id',
  `spawn_point_id` bigint NOT NULL COMMENT '出生点ID，关联 cfg_map_spawn_point.id',
  UNIQUE KEY `uk_round_spawn` (`game_id`, `round_no`, `game_player_id`) COMMENT '同一回合同一玩家只有一个出生点'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家出生点记录表';
