package com.knowhub.repository;

import com.knowhub.model.OrganizationTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationTagRepository extends JpaRepository<OrganizationTag, String> {

    /**
     * 查询父标签是否存在子标签
     * @param parentTagId 父标签Id
     * @return 是否存在
     */
    boolean existsByParentTag_TagId(String parentTagId);
}