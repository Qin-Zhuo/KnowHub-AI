package com.knowhub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Getter
@Setter
@Entity
@Table(name = "file_upload")
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id; // 文件上传记录的主键；分片表通过它关联这次上传。

    // 初始化时由客户端提供；后续合并阶段必须由服务端对完整文件重新计算并比对，它不是权限凭证。
    @Column(name = "file_md5", nullable = false, length = 32, columnDefinition = "CHAR(32)")
    private String fileMd5;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName; // 用户上传时的文件名，主要用于展示。

    @Column(name = "total_size", nullable = false)
    private Long totalSize; // 文件总字节数，用于检查上传内容和计算分片数。

    // 记录本次上传采用的分片方案；即使以后修改默认配置，续传也仍按本次方案进行。
    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks; // 前端应上传的分片总数，合法序号为 0 到 totalChunks - 1。

    // 完整文件在 MinIO 中的位置；初始化时还没有合并文件，因此允许为空。
    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileUploadStatus status = FileUploadStatus.UPLOADING; // 数据库保存枚举名称，如 UPLOADING。

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 后台解析或向量化失败时保存原因，便于排查和展示。

    /*
     * Java 中保存关联实体；数据库的 user_id 外键实际保存 User.id。
     * 懒加载避免读取文件记录时自动加载完整用户信息。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /*
     * Java 中保存关联实体；数据库的 org_tag_id 外键实际保存 OrganizationTag.tagId。
     * 初始化 Service 会先确认当前用户可以使用该组织标签，再设置此关联。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_tag_id", nullable = false)
    private OrganizationTag organizationTag;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic; // 是否允许组织范围以外的用户访问。

    @Column(name = "created_at", nullable = false)
    private Instant createdAt; // 上传任务创建时间。

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // 记录最近一次业务状态或信息变更的时间。

    @Column(name = "merged_at")
    private Instant mergedAt; // 分片合并成功时间；合并前为空。
}
