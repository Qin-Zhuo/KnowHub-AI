package com.knowhub.dto.orgtag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssignOrganizationTagsRequest(
        @NotNull(message = "组织标签列表不能为空")
        @Size(max = 100, message = "一次最多分配100个组织标签")
        List<@NotBlank(message = "组织标签ID不能为空") @Size(max = 300, message = "组织标签ID长度不能超过300")
                String> orgTags
) {
}