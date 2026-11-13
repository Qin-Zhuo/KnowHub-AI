package com.knowhub.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求封装类
 * @param username 用户名
 * @param password 密码
 * @param confirmPassword 确认密码
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 255, message = "用户名长度需要在4到255个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 18, message = "密码长度需要在6到18个字符之间")
        String password,

        @NotBlank(message = "确认密码不能为空")
        @Size(min = 6, max = 18, message = "确认密码长度需要在6到18个字符之间")
        String confirmPassword
) {
}
