package com.knowhub.dto.user;

import java.util.List;

/**
 *
 * @param content
 * @param totalElements
 * @param totalPages
 * @param size
 * @param number
 */
public record UserPageResponse(
        List<UserListItemResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {
}