package com.knowhub.dto.orgtag;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询组织标签树的返回响应类
 */
@Getter
public class OrganizationTagTreeResponse {
    private final String tagId;
    private final String name;
    private final String description;
    private final List<OrganizationTagTreeResponse> children = new ArrayList<>();

    public OrganizationTagTreeResponse(String tagId, String name, String description) {
        this.tagId = tagId;
        this.name = name;
        this.description = description;
    }

    public void addChild(OrganizationTagTreeResponse child) {
        children.add(child);
    }
}