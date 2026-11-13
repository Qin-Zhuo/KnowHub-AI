package com.knowhub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "organization_tags")
public class OrganizationTag {
    @Id
    @Size(max = 300)
    @Column(name = "tag_id", nullable = false, length = 300)
    private String tagId;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY) // 多个组织标签可以有同一个父标签；多个标签也可以由同一个用户创建。
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "parent_tag_id")
    private OrganizationTag parentTag; // 直接查询id对应的实体对象

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 惰性加载，当真正获取该数据时才加载
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;


}