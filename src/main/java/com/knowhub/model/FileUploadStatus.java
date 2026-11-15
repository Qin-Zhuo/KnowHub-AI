package com.knowhub.model;

/**
 * 文件从上传到可检索的处理状态。
 */
public enum FileUploadStatus {
    UPLOADING,   // 分片仍在上传
    UPLOADED,    // 分片已合并为完整文件
    PROCESSING,  // 后台正在解析和向量化
    READY,       // 已完成处理，可以检索
    FAILED       // 后台处理失败
}