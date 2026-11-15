package com.knowhub.service.impl;

import com.knowhub.config.MinioProperties;
import com.knowhub.exception.BusinessException;
import com.knowhub.exception.ErrorCode;
import com.knowhub.service.MinioStorageService;
import io.minio.ComposeObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SourceObject;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 封装 MinIO 对象写入和删除操作，避免文件业务直接依赖 SDK 参数类。
 */
@Service
public class MinioStorageServiceI implements MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageServiceI.class);

    // 客户端由 MinioConfig 创建，Spring 注入后供本类调用。
    private final MinioClient minioClient;

    // 从配置中取得 bucket 名称等 MinIO 参数。
    private final MinioProperties minioProperties;

    public MinioStorageServiceI(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public void uploadChunk(String objectKey, InputStream inputStream, long objectSize) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            // 已知对象总大小，由 SDK 自动决定实际上传分片大小。
                            .stream(inputStream, objectSize, -1L)
                            .contentType("application/octet-stream")
                            .build()
            );
        } catch (MinioException exception) {
            log.error("上传文件分片到 MinIO 失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件分片存储失败");
        }
    }

    @Override
    public void composeChunks(List<String> orderedSourceObjectKeys, String targetObjectKey) {
        try {
            List<SourceObject> sources = orderedSourceObjectKeys.stream()
                    .map(objectKey -> SourceObject.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build())
                    .toList();

            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(targetObjectKey)
                            .sources(sources)
                            .build()
            );
        } catch (MinioException exception) {
            log.error("合并 MinIO 文件分片失败，targetObjectKey={}", targetObjectKey, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件合并失败");
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException exception) {
            log.error("读取 MinIO 文件失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取合并文件失败");
        }
    }

    /**
     * 删除指定对象，供上传失败后的清理流程使用。
     */
    @Override
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException exception) {
            log.error("删除 MinIO 对象失败，objectKey={}", objectKey, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件分片清理失败");
        }
    }
}
