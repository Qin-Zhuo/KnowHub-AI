package com.knowhub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Size(max = 255)
    @NotNull
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "enabled", nullable = false)
    // 账号开关；禁用后不能登录，已有 JWT 请求也会被过滤器拒绝。
    private boolean enabled = true;

    @NotNull
    @Enumerated(EnumType.STRING) // 将 Role.USER 存成字符串 USER，对应数据库的 ENUM('USER','ADMIN')
    @Column(name = "role", nullable = false)
    private Role role;

    @Size(max = 300)
    @Column(name = "org_tag_ids", length = 300)
    private String orgTagIds; // 逗号分隔的组织标签 ID；沿用教程当前表结构。

    @Size(max = 300)
    @Column(name = "primary_org_tag_id", length = 300)
    private String primaryOrgTagId; // 用户当前选择的主组织标签 ID。

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Role {
        USER,
        ADMIN
    }
}
