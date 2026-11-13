package com.knowhub.common;

import com.knowhub.exception.ErrorCode;

/**
 * 创建统一接口响应的工具类
 */
public final class ResultUtils {

    // 定义为private明知提供静态方法，不需要创建对象
    private ResultUtils(){}

    /**
     * 成功返回数据
     * @param data 数据
     * @return 指定数据类型的 ApiResponse<T>
     * @param <T> 泛型
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data
        );
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                message,
                data
        );
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                errorCode.getCode(),
                message,
                null
        );
    }
}
