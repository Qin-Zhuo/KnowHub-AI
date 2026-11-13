package com.knowhub.service.impl;

import com.knowhub.dto.orgtag.OrganizationTagTreeResponse;
import com.knowhub.exception.BusinessException;
import com.knowhub.exception.ErrorCode;
import com.knowhub.model.OrganizationTag;
import com.knowhub.model.User;
import com.knowhub.repository.OrganizationTagRepository;
import com.knowhub.repository.UserRepository;
import com.knowhub.service.OrganizationTagService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class OrganizationTagServiceI implements OrganizationTagService {

    private final OrganizationTagRepository organizationTagRepository;
    private final UserRepository userRepository;

    public OrganizationTagServiceI(OrganizationTagRepository organizationTagRepository, UserRepository userRepository) {
        this.organizationTagRepository = organizationTagRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public String createOrganizationTag(String tagId, String name, String description, String parentTagId, String creatorUsername) {
        String normalizedTagId = tagId.trim();
        String normalizedName = name.trim();

        if (normalizedTagId.startsWith("PRIVATE_")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "PRIVATE_前缀保留给用户私人组织");
        }

        // org_tags 使用逗号分隔；标签 ID 含逗号会破坏成员关系的精确匹配。
        if (normalizedTagId.contains(",")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能包含逗号");
        }

        if (organizationTagRepository.existsById(normalizedTagId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "组织标签ID已存在");
        }

        OrganizationTag parentTag = null;
        if (parentTagId != null && !parentTagId.isBlank()) {
            String normalizedParentId = parentTagId.trim();

            // 私人标签会从管理树中过滤，不能作为公共组织标签的父级。
            if (normalizedParentId.startsWith("PRIVATE_")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将组织标签挂到私人组织下");
            }

            parentTag = organizationTagRepository.findById(normalizedParentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "父组织标签不存在"));
        }

        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "创建者用户不存在"));

        Instant now = Instant.now();

        OrganizationTag organizationTag = new OrganizationTag();
        organizationTag.setTagId(normalizedTagId);
        organizationTag.setName(normalizedName);
        organizationTag.setDescription(description == null || description.isBlank() ? null : description.trim());
        organizationTag.setParentTag(parentTag);
        organizationTag.setCreatedBy(creator);
        organizationTag.setCreatedAt(now);
        organizationTag.setUpdatedAt(now);

        organizationTagRepository.save(organizationTag);

        return normalizedTagId;
    }

    @Override
    @Transactional
    public void assignOrganizationTags(Long userId, List<String> requestedTagIds) {
        if (requestedTagIds == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "组织标签列表不能为空");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        String privateTagId = "PRIVATE_" + user.getUsername();
        Set<String> finalTags = new LinkedHashSet<>();

        for (String requestedTagId : requestedTagIds) {
            if (requestedTagId == null || requestedTagId.isBlank()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "组织标签ID不能为空");
            }

            String tagId = requestedTagId.trim();

            if (tagId.startsWith("PRIVATE_") && !tagId.equals(privateTagId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "不能分配其他用户的私人空间");
            }

            if (!organizationTagRepository.existsById(tagId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "组织标签不存在：" + tagId
                );
            }

            finalTags.add(tagId);
        }

        boolean alreadyHadPrivateTag = user.getOrgTags() != null
                        && Arrays.stream(user.getOrgTags().split(","))
                        .map(String::trim)
                        .anyMatch(privateTagId::equals);

        if (alreadyHadPrivateTag) {
            finalTags.add(privateTagId);
        }

        String currentPrimaryTag = user.getPrimaryOrgTag();

        if (currentPrimaryTag == null || !finalTags.contains(currentPrimaryTag)) {
            if (finalTags.contains(privateTagId)) {
                currentPrimaryTag = privateTagId;
            } else {
                currentPrimaryTag = finalTags.stream()
                        .findFirst()
                        .orElse(null);
            }
        }

        user.setOrgTags(String.join(",", finalTags));
        user.setPrimaryOrgTag(currentPrimaryTag);
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationTagTreeResponse> getOrganizationTagTree() {
        // 读取全部标签并按 ID 排序，让返回的同级节点顺序稳定。
        List<OrganizationTag> tags = organizationTagRepository.findAll(
                Sort.by(Sort.Direction.ASC, "tagId")
        );

        // 私人标签不属于公共组织树。
        tags = tags.stream()
                .filter(tag -> !tag.getTagId().startsWith("PRIVATE_"))
                .toList();

        // 先为每个标签创建节点，并建立 ID 到节点的映射。
        Map<String, OrganizationTagTreeResponse> nodeMap = new LinkedHashMap<>();
        for (OrganizationTag tag : tags) {
            nodeMap.put(
                    tag.getTagId(),
                    new OrganizationTagTreeResponse(
                            tag.getTagId(),
                            tag.getName(),
                            tag.getDescription()
                    )
            );
        }

        List<OrganizationTagTreeResponse> roots = new ArrayList<>();

        // 再根据 parent_tag_id 把节点挂到父节点下；没有父节点的就是根节点。
        for (OrganizationTag tag : tags) {
            OrganizationTagTreeResponse node = nodeMap.get(tag.getTagId());

            if (tag.getParentTag() == null) {
                roots.add(node);
                continue;
            }

            String parentTagId = tag.getParentTag().getTagId();
            OrganizationTagTreeResponse parent = nodeMap.get(parentTagId);

            if (parent == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "组织层级数据不一致");
            }

            parent.addChild(node);
        }

        return roots;
    }

    @Override
    @Transactional
    public void updateOrganizationTag(String tagId, String name, String description, String parentTagId) {
        String normalizedTagId = tagId.trim();

        OrganizationTag tag = organizationTagRepository.findById(normalizedTagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "组织标签不存在"));

        // PRIVATE 标签属于用户个人空间，不通过此管理员接口修改。
        if (normalizedTagId.startsWith("PRIVATE_")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能通过此接口修改私人组织标签");
        }

        OrganizationTag newParent = null;

        if (parentTagId != null && !parentTagId.isBlank()) {
            String normalizedParentId = parentTagId.trim();

            // 公共组织标签不能挂到用户私人空间下。
            if (normalizedParentId.startsWith("PRIVATE_")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将组织标签挂到私人组织下");
            }

            newParent = organizationTagRepository.findById(normalizedParentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "父组织标签不存在"));

            // 从新父标签沿父级向上检查，不能把自己或自己的祖先当作新父标签。
            HashSet<String> visited = new HashSet<>();
            OrganizationTag current = newParent;

            while (current != null) {
                String currentId = current.getTagId();

                if (currentId.equals(normalizedTagId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将组织标签挂到自身或其子级下面");
                }

                // 同时发现数据库里已经存在的循环层级，避免沿环无限遍历。
                if (!visited.add(currentId)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "现有组织层级存在循环");
                }

                current = current.getParentTag();
            }
        }

        tag.setName(name.trim());
        tag.setDescription(
                description == null || description.isBlank()
                        ? null
                        : description.trim()
        );
        tag.setParentTag(newParent);
        tag.setUpdatedAt(Instant.now());

        organizationTagRepository.save(tag);
    }

    @Override
    @Transactional
    public void deleteOrganizationTag(String tagId) {
        String normalizedTagId = tagId.trim();

        OrganizationTag tag = organizationTagRepository.findById(normalizedTagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "组织标签不存在"));

        // 注册流程依赖默认组织，禁止删除。
        if ("default".equals(normalizedTagId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "默认组织不能删除");
        }

        // 私人标签只能随用户生命周期处理，不能由管理员接口删除。
        if (normalizedTagId.startsWith("PRIVATE_")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能通过此接口删除私人组织标签");
        }

        // 有子标签时拒绝删除，避免破坏组织层级。
        if (organizationTagRepository.existsByParentTag_TagId(normalizedTagId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该组织标签仍有子标签，不能删除");
        }

        // 有用户成员关系或主组织引用时拒绝删除。
        if (userRepository.countUsersUsingOrgTag(normalizedTagId) > 0 || userRepository.existsByPrimaryOrgTag(normalizedTagId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该组织标签仍被用户使用，不能删除");
        }

        organizationTagRepository.delete(tag);
    }
}
