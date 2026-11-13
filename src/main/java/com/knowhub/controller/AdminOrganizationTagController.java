package com.knowhub.controller;

import com.knowhub.common.ApiResponse;
import com.knowhub.common.ResultUtils;
import com.knowhub.dto.orgtag.CreateOrganizationTagRequest;
import com.knowhub.dto.orgtag.OrganizationTagTreeResponse;
import com.knowhub.dto.orgtag.UpdateOrganizationTagRequest;
import com.knowhub.service.OrganizationTagService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/org-tags")
public class AdminOrganizationTagController {

    private final OrganizationTagService organizationTagService;

    public AdminOrganizationTagController(
            OrganizationTagService organizationTagService
    ) {
        this.organizationTagService = organizationTagService;
    }

    @PostMapping
    public ApiResponse<String> createOrganizationTag(
            @Valid @RequestBody CreateOrganizationTagRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String tagId = organizationTagService.createOrganizationTag(
                request.tagId(),
                request.name(),
                request.description(),
                request.parentTag(),
                userDetails.getUsername()
        );

        return ResultUtils.success(tagId);
    }


    @GetMapping("/tree")
    public ApiResponse<List<OrganizationTagTreeResponse>> getOrganizationTagTree() {
        return ResultUtils.success(
                organizationTagService.getOrganizationTagTree()
        );
    }

    @PutMapping("/{tagId}")
    public ApiResponse<Void> updateOrganizationTag(
            @PathVariable String tagId,
            @Valid @RequestBody UpdateOrganizationTagRequest request) {

        organizationTagService.updateOrganizationTag(
                tagId,
                request.name(),
                request.description(),
                request.parentTag()
        );

        return ResultUtils.success("组织标签更新成功", null);
    }

    @DeleteMapping("/{tagId}")
    public ApiResponse<Void> deleteOrganizationTag(@PathVariable String tagId) {

        organizationTagService.deleteOrganizationTag(tagId);

        return ResultUtils.success("组织标签删除成功", null);
    }
}
