package com.knowhub.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 文件上传相关配置。
 *
 * @param chunkSizeBytes 单个上传分片的目标大小，单位为字节
 */
@Validated
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        @Positive(message = "上传分片大小必须大于0")
        long chunkSizeBytes
) {
}