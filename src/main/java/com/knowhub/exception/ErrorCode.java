package com.knowhub.exception;

import lombok.Getter;

/**
 * 应用统一使用的业务响应码以及默认提示信息
 */
@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或登录状态无效"),
    FORBIDDEN(40300, "无权执行此操作"),
    NOT_FOUND(40400, "请求的数据不存在"),
    CONFLICT(40900, "请求与当前数据状态冲突"),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持"),
    SYSTEM_ERROR(50000, "系统内部错误");

    private final int code; // 业务响应码
    private final String message; // 信息

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
