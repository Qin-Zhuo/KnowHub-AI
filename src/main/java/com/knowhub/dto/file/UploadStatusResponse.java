package com.knowhub.dto.file;

import com.knowhub.model.FileUploadStatus;

import java.util.List;

/**
 * 当前用户可见的上传任务进度。
 *
 * @param fileId 上传任务 ID
 * @param status 当前文件处理状态
 * @param totalChunks 上传任务要求的分片总数
 * @param uploadedChunks 已在数据库登记的分片序号，按从小到大排列
 * @param progress 已登记分片数占总分片数的百分比，保留一位小数
 */
public record UploadStatusResponse(
        Long fileId,
        FileUploadStatus status,
        Integer totalChunks,
        List<Integer> uploadedChunks,
        double progress
) {
}
