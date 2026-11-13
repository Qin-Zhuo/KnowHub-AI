package com.knowhub.common;

/**
 * 返回前端的基础封装类
 * @param code 业务响应码
 * @param message 具体信息
 * @param data 数据
 * @param <T> 数据类型（泛型）
 */
public record ApiResponse<T>(int code, String message, T data) {
}
