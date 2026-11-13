package com.knowhub.dto.user;

import java.time.Instant;
import java.util.List;

/**
 * 每一页的用户列表封装类
 * @param id
 * @param username
 * @param role
 * @param status
 * @param orgTags
 * @param primaryOrg
 * @param createdAt
 */
public record UserListItemResponse(
        Long id,
        String username,
        String role,
        int status, // 1：启用，0：禁用 和enabled状态对应
        List<String> orgTags,
        String primaryOrg,
        Instant createdAt
) {
}