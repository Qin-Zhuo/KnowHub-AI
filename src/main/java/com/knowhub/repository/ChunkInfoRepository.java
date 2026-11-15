package com.knowhub.repository;

import com.knowhub.model.ChunkInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 访问文件分片记录表 chunk_info。
 * JpaRepository 已提供 save、findById、delete 等基础数据库操作。
 */
public interface ChunkInfoRepository extends JpaRepository<ChunkInfo, Long> {

    /**
     * 查询指定文件的所有分片记录，并按分片序号从小到大排列。<br>
     * 查询条件沿着 ChunkInfo.fileUpload 关联到 FileUpload.id，<br>
     * 等价于按 chunk_info.file_id 查询。<br>
     * 排序后的记录可以按正确顺序用于进度恢复或文件合并。
     *
     * @param fileId 文件记录 ID，即 FileUpload.id
     * @return 该文件的分片记录列表；没有分片时返回空列表
     */
    List<ChunkInfo> findAllByFileUpload_IdOrderByChunkIndexAsc(Long fileId);

    /**
     * 查询指定文件的指定序号分片。<br>
     * 常用于检查某个分片是否已经登记，以及读取它的 MD5 和 MinIO 对象键。
     *
     * @param fileId 文件记录 ID，即 FileUpload.id
     * @param chunkIndex 文件中的分片序号，从0开始
     * @return 找到时包含分片记录，找不到时为空
     */
    Optional<ChunkInfo> findByFileUpload_IdAndChunkIndex(Long fileId, Integer chunkIndex);

    /**
     * 检查指定文件的指定序号是否已有分片记录。<br>
     * 可以快速判断是否为重复上传；并发请求仍由数据库的
     * (file_id, chunk_index) 唯一约束兜底，不能只依赖此查询。
     *
     * @param fileId 文件记录 ID，即 FileUpload.id
     * @param chunkIndex 文件中的分片序号，从0开始
     * @return 已登记时为 true，否则为 false
     */
    boolean existsByFileUpload_IdAndChunkIndex(Long fileId, Integer chunkIndex);
}
