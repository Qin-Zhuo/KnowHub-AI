package com.knowhub.service;

import com.knowhub.dto.orgtag.OrganizationTagTreeResponse;

import java.util.List;

public interface OrganizationTagService {

    /**
     * 创建组织标签接口
     * @param tagId 新组织标签 ID
     * @param name 组织标签展示名称
     * @param description 组织标签描述
     * @param parentTagId 父组织标签 ID；为空时创建为根标签
     * @param creatorUsername 创建者用户名
     * @return 新建组织标签的 ID
     */
    String createOrganizationTag(String tagId, String name, String description, String parentTagId, String creatorUsername);

    /**
     * 给用户分配组织标签
     * @param userId 用户 ID
     * @param requestedTagIds 要分配的组织标签 ID 列表
     */
    void assignOrganizationTags(Long userId, List<String> requestedTagIds);


    /**
     * 获取组织标签的树结构
     * @return 组织标签树的根节点列表
     */
    List<OrganizationTagTreeResponse> getOrganizationTagTree();

    /**
     * 管理员更新组织标签
     *
     * @param tagId 要更新的标签 ID
     * @param name 新名称
     * @param description 新描述
     * @param parentTagId 新父标签 ID；为空时设为根节点
     */
    void updateOrganizationTag(
            String tagId,
            String name,
            String description,
            String parentTagId
    );

    /**
     * 删除没有被用户或子标签引用的组织标签
     *
     * @param tagId 要删除的标签 ID
     */
    void deleteOrganizationTag(String tagId);
}
