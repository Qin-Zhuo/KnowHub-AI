package com.knowhub.dto.user;

import java.time.Instant;
import java.util.List;

/**
 * 用户分页列表中的单条记录；组织字段返回标签 ID。
 * @param id 用户 ID
 * @param username 用户名
 * @param role 用户角色
 * @param status 账号状态，1 为启用、0 为禁用
 * @param orgTagIds 用户所属组织标签 ID 列表
 * @param primaryOrgTagId 当前主组织标签 ID
 * @param createdAt 用户创建时间
 */
public record UserListItemResponse(
        Long id,
        String username,
        String role,
        int status,
        List<String> orgTagIds,
        String primaryOrgTagId,
        Instant createdAt
) {
}
