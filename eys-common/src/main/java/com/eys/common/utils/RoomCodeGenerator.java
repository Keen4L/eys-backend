package com.eys.common.utils;

import com.eys.common.constants.SystemConstants;

import java.security.SecureRandom;
import java.util.Set;

/**
 * 房间码生成工具
 * 使用安全随机数生成，排除易混淆字符（如O/0, I/1）
 */
public final class RoomCodeGenerator {

    private RoomCodeGenerator() {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 最大重试次数（防止极端情况死循环）
     */
    private static final int MAX_RETRY = 100;

    /**
     * 生成房间码
     */
    public static String generate() {
        return generateCode();
    }

    /**
     * 生成房间码（排除已存在的）
     *
     * @param excludeCodes 需要排除的房间码集合
     * @return 不重复的房间码
     */
    public static String generate(Set<String> excludeCodes) {
        if (excludeCodes == null || excludeCodes.isEmpty()) {
            return generateCode();
        }

        for (int i = 0; i < MAX_RETRY; i++) {
            String code = generateCode();
            if (!excludeCodes.contains(code)) {
                return code;
            }
        }

        // 极端情况：重试次数用尽
        return generateCode();
    }

    /**
     * 生成单个房间码
     * 后续如需改为时间戳等其他方式，修改此方法即可
     */
    private static String generateCode() {
        StringBuilder sb = new StringBuilder(SystemConstants.ROOM_CODE_LENGTH);
        for (int i = 0; i < SystemConstants.ROOM_CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(SystemConstants.ROOM_CODE_CHARS.length());
            sb.append(SystemConstants.ROOM_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }
}
