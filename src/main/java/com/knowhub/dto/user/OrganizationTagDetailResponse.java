package com.knowhub.dto.user;

/** 单个组织标签的详情 */
public record OrganizationTagDetailResponse(
        String tagId,
        String name,
        String description
) {
}