package com.knowhub.dto.orgtag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员更新组织标签信息
 * @param name 组织标签名称
 * @param description 组织标签描述
 * @param parentTag 组织标签的父标签Id；为空时设为根节点
 */
public record UpdateOrganizationTagRequest(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 100, message = "标签名称长度不能超过100")
        String name,

        String description,

        @Size(max = 300, message = "父标签ID长度不能超过300")
        String parentTag
) {
}
