package com.knowhub.service.impl;

import com.knowhub.dto.user.*;
import com.knowhub.exception.BusinessException;
import com.knowhub.exception.ErrorCode;
import com.knowhub.model.OrganizationTag;
import com.knowhub.model.User;
import com.knowhub.repository.OrganizationTagRepository;
import com.knowhub.repository.UserRepository;
import com.knowhub.service.UserService;
import com.knowhub.utils.JwtUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceI implements UserService {

    private static final String PRIVATE_TAG_NAME_SUFFIX = "的私人空间";
    private static final int MAX_ORGANIZATION_TAG_NAME_LENGTH = 100;

    private final UserRepository userRepository;
    private final OrganizationTagRepository organizationTagRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserServiceI(
            UserRepository userRepository,
            OrganizationTagRepository organizationTagRepository,
            PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.organizationTagRepository = organizationTagRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public long register(String username, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        if (username.contains(",")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能包含逗号");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (!organizationTagRepository.existsById("default")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认组织尚未初始化");
        }

        Instant now = Instant.now();
        String privateTagId = "PRIVATE_" + username; // 构建 个人组织标签id

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.USER); // 设置为角色枚举类
        user.setOrgTagIds("default," + privateTagId); // 创建时有两个组织：1、默认组织；2、自己的私有组织
        user.setPrimaryOrgTagId(privateTagId); // 设置主要组织标签Id为：私有组织Id
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);

        OrganizationTag privateTag = new OrganizationTag(); // 创建私有组织标签表
        privateTag.setTagId(privateTagId);
        privateTag.setName(buildPrivateTagName(username));
        privateTag.setDescription("<" + username + ">的私人组织标签，仅用户本人可访问");
        privateTag.setCreatedBy(user); // 关联该用户为私人标签的创建者。
        privateTag.setCreatedAt(now);
        privateTag.setUpdatedAt(now);

        organizationTagRepository.save(privateTag);

        return user.getId(); // 返回创建成功的用户Id
    }

    @Override
    public String login(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "用户名或密码错误"
                ));

        // 停用账户不能登录；对外统一提示，避免泄露账户状态。
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 只有账户启用且密码正确时才签发 JWT。
        return jwtUtils.generateToken(user);
    }

    @Override
    public CurrentUserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "用户不存在"
                ));

        List<String> orgTagIds = user.getOrgTagIds() == null || user.getOrgTagIds().isBlank() ? List.of()
                    : Arrays.stream(user.getOrgTagIds().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();

        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                orgTagIds,
                user.getPrimaryOrgTagId()
        );
    }

    @Override
    public UserPageResponse getUserList(String keyword, String orgTagId, Integer status, int page, int size) {
        if(page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0，每页条数必须在1~100之间");
        }

        // 构造筛选条件
        Specification<User> specification = getSpecification(keyword, orgTagId, status);

        /*
           创建分页和排序规则：
            page - 1：Spring Data JPA 页码从0开始
            size：每页记录数
            排序规则："createdAt：按实体属性规则排序，不是数据库列名
         */
        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        // 执行数据库查询：specification->查询条件; pageable->分页和排序规则
        Page<User> userPage = userRepository.findAll(specification, pageable);

        // getContent()：取得这一页的List<User>
        List<UserListItemResponse> content = userPage.getContent().stream()
                .map(user -> new UserListItemResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.isEnabled() ? 1 : 0,
                        parseOrgTagIds(user.getOrgTagIds()), // "default,PRIVATE_qinzhuo" -> ["default", "PRIVATE_qinzhuo"]
                        user.getPrimaryOrgTagId(),
                        user.getCreatedAt()
                ))
                .toList();

        return new UserPageResponse(
                content, // 当前页用户
                userPage.getTotalElements(), // 符合筛选条件的总记录数。
                userPage.getTotalPages(), // 总页数
                userPage.getSize(), // 每页数量
                userPage.getNumber() + 1 // Spring 的页码从 0 开始，转换成接口使用的 1 起始页码。
        );
    }

    @Override
    @Transactional
    public void setPrimaryOrgTagId(String username, String primaryOrgTagId) {
        // 去除输入首尾空格，避免标签值带空格导致匹配失败。
        String normalizedPrimaryOrgTagId = primaryOrgTagId.trim();

        // 根据当前登录用户查询数据库中的用户实体。
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        // 解析该用户已加入的组织标签，并做完整匹配。
        List<String> orgTagIds = parseOrgTagIds(user.getOrgTagIds());
        if (!orgTagIds.contains(normalizedPrimaryOrgTagId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能将未加入的组织设为主组织");
        }

        // 用户标签列表里存在，但组织表里找不到，说明数据不一致。
        if (!organizationTagRepository.existsById(normalizedPrimaryOrgTagId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织标签不存在");
        }

        // 只修改主组织字段，不改变用户已加入的组织列表。
        user.setPrimaryOrgTagId(normalizedPrimaryOrgTagId);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    public UserOrganizationTagsResponse getUserOrganizationTags(String username) {
        // 先查出当前用户，组织列表以数据库里的 org_tag_ids 为准。
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "用户不存在"
                ));

        List<String> orgTagIds = parseOrgTagIds(user.getOrgTagIds());

        // 一次查询所有组织标签，避免逐个标签查询数据库。
        Map<String, OrganizationTag> tagsById = organizationTagRepository.findAllById(orgTagIds)
                        .stream()
                        .collect(Collectors.toMap(
                                OrganizationTag::getTagId,
                                Function.identity()
                        ));

        // 按用户 org_tag_ids 中的顺序组装详情，保证返回顺序稳定。
        List<OrganizationTagDetailResponse> details = orgTagIds.stream()
                .map(tagId -> {
                    OrganizationTag tag = tagsById.get(tagId);

                    // 用户记录引用了不存在的标签，属于数据不一致。
                    if (tag == null) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户组织标签数据不一致");
                    }

                    return new OrganizationTagDetailResponse(
                            tag.getTagId(),
                            tag.getName(),
                            tag.getDescription()
                    );
                })
                .toList();

        return new UserOrganizationTagsResponse(
                orgTagIds,
                user.getPrimaryOrgTagId(),
                details
        );
    }

    @NonNull
    private static Specification<User> getSpecification(String keyword, String orgTagId, Integer status) {
        if(status != null && status != 0 && status != 1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "status只能是0（禁用）或1（启用）");
        }

        String normalizedKeyword = keyword == null ? null : keyword.trim();
        String normalizedOrgTagId = orgTagId == null ? null : orgTagId.trim();

        /*
          specification：
            root：当前查询的实体，也就是User，用它来引用 username、orgTagIds 等属性
            cb：CriteriaBuilder，提供 like、equal、greaterThan 等条件构造方法
            predicates：本次查询启用的条件列表
            cb.and()：所有启用的查询条件都必须满足
         */
        Specification<User> specification = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if(normalizedKeyword != null && !normalizedKeyword.isBlank()){
                String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("username")), pattern));
            }

            if(normalizedOrgTagId != null && !normalizedOrgTagId.isBlank()){
                // org_tag_ids 是逗号分隔字符串，用 FIND_IN_SET 做完整标签匹配
                var tagPosition = cb.function(
                        "FIND_IN_SET", // MySQL 函数名
                        Integer.class,
                        cb.literal(normalizedOrgTagId),
                        root.<String>get("orgTagIds")
                );

                predicates.add(cb.greaterThan(tagPosition, 0));
            }

            if(status != null){
                predicates.add(cb.equal(root.get("enabled"), status == 1));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return specification;
    }

    private List<String> parseOrgTagIds(String orgTagIds) {
        if (orgTagIds == null || orgTagIds.isBlank()) {
            return List.of();
        }

        return Arrays.stream(orgTagIds.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    private static String buildPrivateTagName(String username) {
        int maxUsernameLength = MAX_ORGANIZATION_TAG_NAME_LENGTH - PRIVATE_TAG_NAME_SUFFIX.length();
        int endIndex = Math.min(username.length(), maxUsernameLength);

        // 避免截断代理对，导致生成的私人标签名称不合法。
        if (endIndex > 0 && Character.isHighSurrogate(username.charAt(endIndex - 1))) {
            endIndex--;
        }

        return username.substring(0, endIndex) + PRIVATE_TAG_NAME_SUFFIX;
    }
}
