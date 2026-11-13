package com.knowhub.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户设置自己的主组织标签封装类
 * @param primaryOrg 主组织
 */
public record SetPrimaryOrgRequest(
        @NotBlank(message = "主组织标签不能为空")
        @Size(max = 300, message = "主组织标签长度不能超过300")
        String primaryOrg
) {
}