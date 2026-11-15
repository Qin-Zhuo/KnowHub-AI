package com.knowhub.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 初始化文件上传请求
 *
 * @param fileName 文件名
 * @param fileMd5 文件内容MD5
 * @param totalSize 文件总字节数
 * @param orgTagId （可选字段）文件所属组织标签 ID；未提供时使用当前用户主组织标签 ID
 * @param isPublic （可选字段，默认非公开）是否公开；未提供时按非公开处理
 */
public record UploadInitRequest(
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名长度不能超过255个字符")
        String fileName,

        @NotBlank(message = "文件MD5不能为空")
        @Pattern(regexp = "(?i)^[0-9a-f]{32}$", message = "文件MD5格式不正确")
        String fileMd5,

        @NotNull(message = "文件大小不能为空")
        @Positive(message = "文件大小必须大于0")
        Long totalSize,

        @Size(max = 300, message = "组织标签长度不能超过300个字符")
        @Pattern(regexp = "^[^,]+$", message = "组织标签不能包含逗号")
        String orgTagId,

        Boolean isPublic
) {
}
