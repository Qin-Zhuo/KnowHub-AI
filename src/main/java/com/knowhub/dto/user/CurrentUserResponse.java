package com.knowhub.dto.user;

import java.util.List;

/**
 * 当前用户信息返回封装类
 * @param id
 * @param username
 * @param role
 * @param orgTags
 * @param primaryOrg
 */
public record CurrentUserResponse(
        Long id,
        String username,
        String role,
        List<String> orgTags,
        String primaryOrg
) {
}