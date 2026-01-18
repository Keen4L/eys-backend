-- ==========================================
-- 鹅鸭杀辅助工具数据初始化 (2_init_data.sql)
-- ==========================================
-- 版本: 3.0
-- 最后更新: 2026-01-14
-- 说明: 本文件包含系统初始数据，包括管理员账号、角色定义、技能配置等
-- ==========================================

USE `eys_game`;

-- 清空旧数据
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `sys_user`;
TRUNCATE TABLE `cfg_role`;
TRUNCATE TABLE `cfg_skill`;
SET FOREIGN_KEY_CHECKS = 1;


-- ==========================================
-- 1. 系统基础数据
-- ==========================================

-- 1.1 默认管理员账号
-- 用户名: admin, 密码: admin (BCrypt加密)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `role_type`) 
VALUES (1, 'admin', '$2a$10$8BRgX5fjnKy9EG8okatIzuz/ioq7.HXK7qs6tUunQ0BiYJdJg1OFG', '超级管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);


-- ==========================================
-- 2. 角色定义 (cfg_role)
-- ==========================================
-- 阵营 [CampType]: GOOSE-鹅(好人), DUCK-鸭(坏人), NEUTRAL-中立

INSERT INTO `cfg_role` (`id`, `name`, `camp_type`, `description`) VALUES
-- ============ 鹅阵营 (1-14) ============
(1, '网红鹅', 'GOOSE', '被刀后同归于尽，带走凶手。'),
(2, '正义鹅', 'GOOSE', '全局一刀；若未使用，死后可开一枪。'),
(3, '先知鹅', 'GOOSE', '可查验身份3次，消耗10金币/次。'),
(4, '警长鹅', 'GOOSE', '每晚一刀（刀好人自己死）；全局可禁闭一人。'),
(5, '保镖鹅', 'GOOSE', '每晚守护一人，不可连续守护同一人，守护成功后技能失效。'),
(6, '加拿大鹅', 'GOOSE', '被刀后，本回合所有鸭子技能封锁。'),
(7, '医生鹅', 'GOOSE', '白天可对一人注射（解药或毒药，DM裁决）。'),
(8, '殡仪鹅', 'GOOSE', '全局一次验尸：挖到鹅继承技能，挖到其他阵营获得一刀。'),
(9, '大白鹅', 'GOOSE', '每回合可查验座位左一或右一是否为鸭子。'),
(10, '菲律宾鹅', 'GOOSE', '刺鸭对方死，刺错自己死，只能发动一次。'),
(11, '恋爱脑鹅', 'GOOSE', '每回合暗恋一人，连续两回合暗恋同一人则目标被击杀。'),
(12, '魔术鹅', 'GOOSE', '可置换两人的生命状态。'),
(13, '决斗鹅', 'GOOSE', '被杀后可与凶手决斗，一局定胜负。'),
(14, '等式鹅', 'GOOSE', '第2回合起可查验两人是否同阵营（不可重复查验相同组合）。'),

-- ============ 中立阵营 (15-18) ============
(15, '猎鹰', 'NEUTRAL', '每晚一刀；场上只剩鹅时进入猎杀时刻。'),
(16, '呆呆鸟', 'NEUTRAL', '被投票出局则游戏结束，呆呆鸟独赢。'),
(17, '鹦鹉', 'NEUTRAL', '开局复制一人的全部技能。'),
(18, '鹈鹕', 'NEUTRAL', '每晚可吞噬1-2人（被吞者无法投票和使用技能），累计吞4人胜利。'),

-- ============ 鸭阵营 (19-26) ============
(19, '鸭王', 'DUCK', '每晚一刀；死亡时可带走一人（被刀/被毒不可发动）。'),
(20, '美女鸭', 'DUCK', '每晚一刀；开局魅惑一人，自己死对方也死。'),
(21, '忍者鸭', 'DUCK', '每回合标记一人（不可重复），被投出时标记者一起死。'),
(22, '禁言鸭', 'DUCK', '每晚一刀；可禁言一人（最多累计3次）。'),
(23, '刺客鸭', 'DUCK', '每晚一刀；可指定猜测目标身份，猜对击杀，猜错自己死。'),
(24, '炸弹鸭', 'DUCK', '无刀，可在夜间自爆（选择任意数量玩家）。'),
(25, '梦魇鸭', 'DUCK', '每晚一刀；每回合可梦魇一人（封锁技能，不可连续梦同一人）。'),
(26, '火种鸭', 'DUCK', '每晚一刀；场上只剩一只鸭时可出双刀。');


-- ==========================================
-- 3. 技能配置 (cfg_skill)
-- ==========================================
--
-- [skill_config JSON结构说明] - 五规则格式:
-- {
--   "triggerRule": {                           // 触发规则 → TriggerProcessor
--     "mode": "[TriggerMode]",                  // 触发方式: DM_PUSH/DM_EXECUTE
--     "isNormal": true|false                  // 可选：是否为普通技能（仅DM_EXECUTE有效）
--   },
--   "limitRule": {                             // 限制规则 → LimitProcessor
--     "totalMax": -1,                          // 全局最大次数，-1表示不限
--     "minRound": 1,                           // 最早可用回合
--     "invalidCondition": "..."                // 可选：技能失效条件描述
--   },
--   "targetRule": {                            // 目标规则 → TargetProcessor
--     "selectMode": "[TargetSelectMode]",      // 目标选择方式
--     "aliveState": "[AliveState]",            // 存活状态要求
--     "excludeSelf": true/false,               // 是否排除自己
--     "countRange": [min, max],                // 数量范围，-1表示不限
--     "restriction": "[TargetRestriction]"     // 额外限制条件
--   },
--   "effectRule": {                            // 效果规则 → EffectProcessor
--     "type": "[EffectType]",                  // 效果类型: NONE/TAG/INHERIT
--     "tagEffects": ["[TagEffect]", ...],      // 标签效果列表（type=TAG时有效）
--     "tagExpiry": "[TagExpiry]",              // 过期时机（type=TAG时有效）
--     "inheritMode": "[InheritMode]"           // 继承模式（type=INHERIT时有效）
--   },
--   "dataRule": {                              // 数据规则 → DataProcessor
--     "preDataType": "[PreDataType]",          // 可选：推送时携带的预数据类型
--     "postDataType": "[PostDataType]"         // 可选：使用后回传的结果类型
--   }
-- }
--

-- ============ 鹅阵营技能 ============

-- 1. 网红鹅 - 同归于尽
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(1, '同归于尽', '被刀后带走凶手', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "AUTO_ATTACKER"},
  "effectRule": {"type": "NONE"}
}');

-- 2. 正义鹅 - 刀/死后开枪
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(2, '刀', '全局一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(2, '死后开枪', '死后可开一枪', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}');

-- 3. 先知鹅 - 查验身份
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(3, '查验身份', '查验目标真实身份（消耗10金币）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 3, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"},
  "dataRule": {"postDataType": "TARGET_CAMP"}
}');

-- 4. 警长鹅 - 刀/禁闭
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(4, '刀', '每晚一刀（刀好人自己死）', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(4, '禁闭', '禁闭一人（封锁技能1回合）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "TAG", "tagEffects": ["BLOCK_SKILL"], "tagExpiry": "NEXT_ROUND"}
}');

-- 5. 保镖鹅 - 守护
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(5, '守护', '守护一人（不可连续守护同一人，守护成功后失效）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": -1, "minRound": 1, "invalidCondition": "守护成功"},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1], "restriction": "NO_CONSECUTIVE_SAME_TARGET"},
  "effectRule": {"type": "TAG", "tagEffects": [], "tagExpiry": "NEXT_ROUND"}
}');

-- 6. 加拿大鹅 - 死亡沉默鸭子
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(6, '死亡沉默鸭子', '被刀后本回合所有鸭子技能封锁', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "AUTO_ALIVE_DUCK"},
  "effectRule": {"type": "TAG", "tagEffects": ["BLOCK_SKILL"], "tagExpiry": "NEXT_ROUND"}
}');

-- 7. 医生鹅 - 注射
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(7, '注射', '对一人注射（解药或毒药，DM裁决）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"},
  "dataRule": {"preDataType": "TOP_VOTED_PLAYER"}
}');

-- 8. 殡仪鹅 - 挖尸 + 刀（隐藏技能，初始不可用）
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(8, '挖尸', '验尸继承技能（鹅继承全部，其他激活刀）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "DEAD", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "INHERIT", "inheritMode": "GOOSE_ALL_ELSE_KNIFE"}
}'),
(8, '刀', '隐藏刀（初始不可用，挖到非鹅时激活）', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": 0, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}');

-- 9. 大白鹅 - 查验阵营
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(9, '查验阵营', '查验座位左一或右一是否为鸭', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "AUTO_LEFT_RIGHT"},
  "effectRule": {"type": "NONE"}
}');

-- 10. 菲律宾鹅 - 刺杀
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(10, '刺杀', '刺鸭对方死，刺错自己死', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}');

-- 11. 恋爱脑鹅 - 暗恋
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(11, '暗恋', '暗恋一人（连续两回合暗恋同一人则击杀）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "TAG", "tagEffects": [], "tagExpiry": "AFTER_2_ROUND"}
}');

-- 12. 魔术鹅 - 置换
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(12, '置换', '置换两人的生命状态', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [2, 2], "restriction": "NO_REPEAT_TARGET"},
  "effectRule": {"type": "TAG", "tagEffects": [], "tagExpiry": "NEXT_ROUND"}
}');

-- 13. 决斗鹅 - 决斗
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(13, '决斗', '被杀后与凶手决斗', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "AUTO_ATTACKER"},
  "effectRule": {"type": "NONE"}
}');

-- 14. 等式鹅 - 查验同营
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(14, '查验同营', '查验两人是否同阵营', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": -1, "minRound": 2},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [2, 2], "restriction": "NO_REPEAT_TARGET"},
  "effectRule": {"type": "NONE"},
  "dataRule": {"postDataType": "SAME_CAMP"}
}');


-- ============ 中立阵营技能 ============

-- 15. 猎鹰 - 刀/猎杀
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(15, '刀', '每晚一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(15, '猎杀', '场上只剩鹅时猎杀全部', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "AUTO_ALIVE_GOOSE"},
  "effectRule": {"type": "NONE"}
}');

-- 16. 呆呆鸟 - 反噬
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(16, '反噬', '被投出局则游戏结束，呆呆鸟独赢', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "NONE"},
  "effectRule": {"type": "NONE"}
}');

-- 17. 鹦鹉 - 模仿
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(17, '模仿', '开局复制目标的全部技能', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "INHERIT", "inheritMode": "ALL"}
}');

-- 18. 鹈鹕 - 吞噬
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(18, '吞噬', '吞噬1-2人（被吞者无法投票和使用技能）', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 2]},
  "effectRule": {"type": "TAG", "tagEffects": ["BLOCK_VOTE", "BLOCK_SKILL"], "tagExpiry": "PERMANENT"}
}');


-- ============ 鸭阵营技能 ============

-- 19. 鸭王 - 刀/死后带人
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(19, '刀', '每晚一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(19, '死后带人', '死亡时带走一人（被刀/被毒不可发动）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}');

-- 20. 美女鸭 - 刀/魅惑
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(20, '刀', '每晚一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(20, '魅惑', '开局魅惑一人（自己死对方也死）', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": true, "countRange": [1, 1]},
  "effectRule": {"type": "TAG", "tagEffects": [], "tagExpiry": "PERMANENT"}
}');

-- 21. 忍者鸭 - 标记
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(21, '标记', '标记一人（被投出时一起死）', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1], "restriction": "NO_REPEAT_TARGET"},
  "effectRule": {"type": "TAG", "tagEffects": [], "tagExpiry": "PERMANENT"}
}');

-- 22. 禁言鸭 - 刀/禁言
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(22, '刀', '每晚一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(22, '禁言', '禁言一人（不可重复，最多3次）', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 3, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1], "restriction": "NO_REPEAT_TARGET"},
  "effectRule": {"type": "TAG", "tagEffects": [], "tagExpiry": "NEXT_ROUND"}
}');

-- 23. 刺客鸭 - 刀/刺验
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(23, '刀', '每晚一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(23, '刺验', '猜测目标身份刺杀（猜对击杀，猜错自己死）', '{
  "triggerRule": {"mode": "DM_EXECUTE"},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER_ROLE", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}');

-- 24. 炸弹鸭 - 自爆
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(24, '自爆', '夜间自爆（选择任意数量玩家）', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": 1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [0, -1]},
  "effectRule": {"type": "NONE"}
}');

-- 25. 梦魇鸭 - 刀/梦魇
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(25, '刀', '每晚一刀', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1]},
  "effectRule": {"type": "NONE"}
}'),
(25, '梦魇', '梦魇一人（封锁技能，不可连续两回合梦同一人）', '{
  "triggerRule": {"mode": "DM_PUSH"},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 1], "restriction": "NO_CONSECUTIVE_SAME_TARGET"},
  "effectRule": {"type": "TAG", "tagEffects": ["BLOCK_SKILL"], "tagExpiry": "NEXT_ROUND"}
}');

-- 26. 火种鸭 - 伪双刀
INSERT INTO `cfg_skill` (`role_id`, `name`, `description`, `skill_config`) VALUES
(26, '伪双刀', '每晚一刀（场上只剩一只鸭时可双刀）', '{
  "triggerRule": {"mode": "DM_EXECUTE", "isNormal": true},
  "limitRule": {"totalMax": -1, "minRound": 1},
  "targetRule": {"selectMode": "MANUAL_PLAYER", "aliveState": "ALIVE", "excludeSelf": false, "countRange": [1, 2]},
  "effectRule": {"type": "NONE"}
}');


-- ==========================================
-- 数据验证
-- ==========================================
SELECT 
  (SELECT COUNT(*) FROM cfg_role) AS '角色总数', 
  (SELECT COUNT(*) FROM cfg_skill) AS '技能总数',
  (SELECT COUNT(*) FROM cfg_role WHERE camp_type = 'GOOSE') AS '鹅阵营角色数',
  (SELECT COUNT(*) FROM cfg_role WHERE camp_type = 'DUCK') AS '鸭阵营角色数',
  (SELECT COUNT(*) FROM cfg_role WHERE camp_type = 'NEUTRAL') AS '中立阵营角色数';
