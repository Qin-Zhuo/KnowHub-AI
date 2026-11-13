package com.knowhub.dto.user;

/**
 * 登录成功响应
 * @param token 访问令牌
 */
public record LoginResponse(String token){
}
