package com.knowhub.controller;

import com.knowhub.common.ApiResponse;
import com.knowhub.common.ResultUtils;
import com.knowhub.dto.orgtag.AssignOrganizationTagsRequest;
import com.knowhub.dto.user.UserListQueryRequest;
import com.knowhub.dto.user.UserPageResponse;
import com.knowhub.service.OrganizationTagService;
import com.knowhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final OrganizationTagService organizationTagService;

    public AdminUserController(UserService userService, OrganizationTagService organizationTagService) {
        this.userService = userService;
        this.organizationTagService = organizationTagService;
    }

    @GetMapping("/list")
    public ApiResponse<UserPageResponse> getUserList(@Valid @ModelAttribute UserListQueryRequest request) {
        UserPageResponse response = userService.getUserList(
                request.getKeyword(),
                request.getOrgTagId(),
                request.getStatus(),
                request.getPage(),
                request.getSize()
        );

        return ResultUtils.success(response);
    }

    @PutMapping("/{userId}/org-tags")
    public ApiResponse<Void> assignOrganizationTags(@PathVariable Long userId,
            @Valid @RequestBody AssignOrganizationTagsRequest request) {

        organizationTagService.assignOrganizationTags(userId, request.orgTagIds());

        return ResultUtils.success(null);
    }
}
