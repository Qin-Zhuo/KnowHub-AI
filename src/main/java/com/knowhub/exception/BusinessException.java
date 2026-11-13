package com.knowhub.exception;

import lombok.Getter;

/**
 * 用于表示可预期的业务异常
 */
@Getter
public class BusinessException extends RuntimeException{

    private final ErrorCode errorCode;

    /**
     * 默认内部错误码及对应信息
     * @param errorCode 内部错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 自定义详细信息
     * @param errorCode 内部错误码
     * @param message 自定义信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
