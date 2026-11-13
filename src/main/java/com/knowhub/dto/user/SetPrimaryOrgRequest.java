package com.knowhub.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户设置自己的主组织标签封装类
 * @param primaryOrgTagId 主组织标签 ID
 */
public record SetPrimaryOrgRequest(
        @NotBlank(message = "主组织标签ID不能为空")
        @Size(max = 300, message = "主组织标签ID长度不能超过300")
        String primaryOrgTagId
) {
}
