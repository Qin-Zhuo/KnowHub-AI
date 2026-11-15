package com.knowhub.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MinIO 连接配置；密钥通过环境变量提供。
 *
 * @param endpoint MinIO 的 S3 API 地址
 * @param accessKey 访问密钥
 * @param secretKey 私密密钥
 * @param bucket 保存上传对象的桶名
 */
@Validated
@ConfigurationProperties(prefix = "app.storage.minio")
public record MinioProperties(
        @NotBlank(message = "MinIO endpoint 不能为空")
        String endpoint,

        @NotBlank(message = "MinIO access key 不能为空")
        String accessKey,

        @NotBlank(message = "MinIO secret key 不能为空")
        String secretKey,

        @NotBlank(message = "MinIO bucket 不能为空")
        String bucket
) {
}