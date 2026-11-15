package com.knowhub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "chunk_info",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chunk_file_index",
                columnNames = {"file_id", "chunk_index"}
        )
)
public class ChunkInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id; // 分片记录主键，不是分片序号。

    /*
     * 关联所属文件；数据库 file_id 保存 FileUpload.id。
     * 懒加载可以避免查询分片记录时自动加载整份文件信息。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileUpload fileUpload;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex; // 文件内分片序号，从0开始。

    // 服务端根据收到的分片内容计算，用于检查重传的分片是否一致。
    @Column(name = "chunk_md5", nullable = false, length = 32, columnDefinition = "CHAR(32)")
    private String chunkMd5;

    // 分片在 MinIO 中的对象键，不是本机磁盘路径。
    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt; // 分片保存并登记的时间。
}
