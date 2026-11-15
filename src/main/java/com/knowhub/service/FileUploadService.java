package com.knowhub.service;

import com.knowhub.dto.file.UploadInitRequest;
import com.knowhub.dto.file.UploadInitResponse;
import com.knowhub.dto.file.UploadStatusResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传业务接口。
 */
public interface FileUploadService {

    /**
     * 初始化一份文件的上传任务并返回服务端确定的分片方案；匹配到同用户、同文件及相同上传选项的未完成任务时会复用它。
     *
     * @param username 当前登录用户名，由认证信息取得
     * @param request 上传初始化参数
     * @return 服务端分配的文件 ID 和分片方案
     */
    UploadInitResponse initializeUpload(String username, UploadInitRequest request);


    /**
     * 上传指定文件任务中的一个分片；实际对象写入 MinIO，分片索引和摘要登记在 MySQL。
     * 同一个分片如果重试，后续会按内容 MD5 处理：内容相同就当作重复成功；内容不同则返回冲突。
     *
     * @param username 当前登录用户，由认证信息取得
     * @param fileId 上传初始化时生成的文件记录 ID
     * @param chunkIndex 分片序号，从 0 开始
     * @param chunk 本次上传的分片内容
     */
    void uploadChunk(String username, Long fileId, Integer chunkIndex, MultipartFile chunk);

    /**
     * 查询当前用户上传任务的状态及已登记分片，供前端刷新进度或恢复上传时使用。
     *
     * @param username 当前登录用户，由认证信息取得
     * @param fileId 上传任务 ID
     * @return 上传状态、已上传分片序号和完成百分比
     */
    UploadStatusResponse getUploadStatus(String username, Long fileId);

    /**
     * 将 MinIO 中指定上传任务的所有分片按序合并为完整文件。
     *
     * @param username 当前登录用户，由认证信息取得
     * @param fileId 上传初始化时生成的文件记录 ID
     */
    void mergeChunks(String username, Long fileId);
}
