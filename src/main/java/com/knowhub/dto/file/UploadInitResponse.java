package com.knowhub.dto.file;

import java.util.List;

/**
 * 上传初始化结果，前端据此按服务端方案切分文件，并跳过已上传的分片。
 *
 * @param fileId 文件上传记录 ID
 * @param chunkSize 服务端分配的分片大小，单位为字节
 * @param totalChunks 分片总数
 * @param uploadedChunks 已上传分片序号；复用未完成任务时前端据此跳过这些分片
 */
public record UploadInitResponse(
        Long fileId,
        Long chunkSize,
        Integer totalChunks,
        List<Integer> uploadedChunks
) {
}
