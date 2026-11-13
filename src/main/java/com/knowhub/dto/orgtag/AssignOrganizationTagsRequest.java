package com.knowhub.dto.orgtag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 管理员为用户分配的组织标签 ID 列表。 */
public record AssignOrganizationTagsRequest(
        @NotNull(message = "组织标签ID列表不能为空")
        @Size(max = 100, message = "一次最多分配100个组织标签")
        List<@NotBlank(message = "组织标签ID不能为空") @Size(max = 300, message = "组织标签ID长度不能超过300")
                String> orgTagIds
) {
}
