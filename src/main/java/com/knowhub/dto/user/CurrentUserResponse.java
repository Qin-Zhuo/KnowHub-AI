package com.knowhub.dto.user;

import java.util.List;

/**
 * 当前登录用户信息；组织字段返回标签 ID。
 * @param id 用户 ID
 * @param username 用户名
 * @param role 用户角色
 * @param orgTagIds 用户所属组织标签 ID 列表
 * @param primaryOrgTagId 当前主组织标签 ID
 */
public record CurrentUserResponse(
        Long id,
        String username,
        String role,
        List<String> orgTagIds,
        String primaryOrgTagId
) {
}
