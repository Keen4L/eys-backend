package com.eys.common.utils;

import com.eys.common.exception.BusinessException;
import com.eys.common.result.ResultCode;

/**
 * 断言工具类，用于参数校验
 */
public final class AssertUtils {

    private AssertUtils() {
    }

    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, ResultCode resultCode) {
        if (object == null) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言对象不为空（自定义消息）
     */
    public static void notNull(Object object, ResultCode resultCode, String message) {
        if (object == null) {
            throw new BusinessException(resultCode, message);
        }
    }

    /**
     * 断言字符串不为空白
     */
    public static void notBlank(String str, ResultCode resultCode) {
        if (str == null || str.isBlank()) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, ResultCode resultCode) {
        if (!condition) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言条件为真（自定义消息）
     */
    public static void isTrue(boolean condition, ResultCode resultCode, String message) {
        if (!condition) {
            throw new BusinessException(resultCode, message);
        }
    }

    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BusinessException(resultCode);
        }
    }

    /**
     * 断言条件为假（自定义消息）
     */
    public static void isFalse(boolean condition, ResultCode resultCode, String message) {
        if (condition) {
            throw new BusinessException(resultCode, message);
        }
    }
}
