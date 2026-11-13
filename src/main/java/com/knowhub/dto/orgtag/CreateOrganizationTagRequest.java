package com.knowhub.dto.orgtag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建组织标签请求；tagId 和 parentTagId 都是标签标识，不是展示名称。
 * @param tagId 新标签 ID
 * @param name 展示名称
 * @param description 描述
 * @param parentTagId 父标签 ID；为空时创建为根标签
 */
public record CreateOrganizationTagRequest(
        @NotBlank(message = "标签ID不能为空")
        @Size(max = 300, message = "标签ID长度不能超过300")
        @Pattern(regexp = "^[^,]+$", message = "标签ID不能包含逗号")
        String tagId,

        @NotBlank(message = "标签名称不能为空")
        @Size(max = 100, message = "标签名称长度不能超过100")
        String name,

        String description,

        @Size(max = 300, message = "父标签ID长度不能超过300")
        String parentTagId
) {
}
