package com.knowhub.service;

import java.io.InputStream;
import java.util.List;


/**
 * MinIO 文件分片存储服务
 */
public interface MinioStorageService {


    /**
     * 将一个文件分片写入配置的 bucket。
     * objectKey 应由服务端生成；inputStream 由调用方负责关闭。
     */
    void uploadChunk(String objectKey, InputStream inputStream, long objectSize);

    /**
     * 按列表顺序合并同一 bucket 中的分片对象。
     */
    void composeChunks(List<String> orderedSourceObjectKeys, String targetObjectKey);

    /**
     * 读取对象流；调用方负责关闭返回的 InputStream。
     */
    InputStream getObject(String objectKey);

    // 根据MinIO中的对象键进行删除
    void deleteObject(String objectKey);


}