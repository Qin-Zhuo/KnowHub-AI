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

    @ManyToOne(fetch = FetchType.LAZY) // 多个子标签可以指向同一个父标签；空值表示根标签。
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "parent_tag_id")
    private OrganizationTag parentTag; // JPA 关联对象；数据库 parent_tag_id 存储父标签的 tag_id

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // 关联创建者；数据库 created_by 保存 users.id。
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;


}
