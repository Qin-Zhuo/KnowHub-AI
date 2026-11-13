package com.knowhub.dto.user;

import java.util.List;

/** 当前用户的组织标签及主组织信息 */
public record UserOrganizationTagsResponse(
        List<String> orgTags,
        String primaryOrg,
        List<OrganizationTagDetailResponse> orgTagDetails
) {
}