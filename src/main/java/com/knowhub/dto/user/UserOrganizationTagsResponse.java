package com.knowhub.dto.user;

import java.util.List;

/** 当前用户的组织标签 ID、主组织标签 ID，以及对应的展示详情。 */
public record UserOrganizationTagsResponse(
        List<String> orgTagIds,
        String primaryOrgTagId,
        List<OrganizationTagDetailResponse> orgTagDetails
) {
}
