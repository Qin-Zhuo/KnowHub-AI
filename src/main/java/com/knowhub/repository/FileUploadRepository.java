package com.knowhub.repository;

import com.knowhub.model.FileUpload;
import com.knowhub.model.FileUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 文件上传记录的数据访问接口。
 */
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    /**
     * 查找当前用户相同文件摘要的未完成任务。
     * Service 还会比较文件大小、组织和公开设置，避免恢复到上传选项不同的任务。
     */
    List<FileUpload> findAllByUser_IdAndFileMd5AndStatusOrderByCreatedAtDesc(
            Long userId,
            String fileMd5,
            FileUploadStatus status
    );

    /**
     * 只按文件ID和上传者ID查记录，供后续分片、状态查询等操作校验归属。
     */
    Optional<FileUpload> findByIdAndUser_Id(Long fileId, Long userId);
}
