package com.knowhub.repository;

import com.knowhub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 通过username查询用户
     * @param username 用户名
     * @return 查询到的用户: Optional包裹：表示结果可能存在也可能不存在
     */
    Optional<User> findByUsername(String username);

    /**
     * 查询用户名是否已经被注册
     * @param username 用户名
     * @return 是否注册
     */
    boolean existsByUsername(String username);


    /** 统计仍包含指定组织标签 ID 的用户数量。 */
    @Query(
            value = "SELECT COUNT(*) FROM users WHERE FIND_IN_SET(:tagId, org_tag_ids) > 0",
            nativeQuery = true
    )
    long countUsersUsingOrgTagId(@Param("tagId") String tagId);

    /** 判断是否有用户将指定组织标签 ID 设为主组织。 */
    boolean existsByPrimaryOrgTagId(String primaryOrgTagId);
}
