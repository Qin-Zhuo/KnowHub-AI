package com.knowhub.service.impl;

import com.knowhub.config.UploadProperties;
import com.knowhub.dto.file.UploadInitRequest;
import com.knowhub.dto.file.UploadInitResponse;
import com.knowhub.dto.file.UploadStatusResponse;
import com.knowhub.exception.BusinessException;
import com.knowhub.exception.ErrorCode;
import com.knowhub.model.*;
import com.knowhub.repository.ChunkInfoRepository;
import com.knowhub.repository.FileUploadRepository;
import com.knowhub.repository.OrganizationTagRepository;
import com.knowhub.repository.UserRepository;
import com.knowhub.service.FileUploadService;
import com.knowhub.service.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * 当前实现上传初始化、复用匹配的未完成任务、逐片存入 MinIO、登记分片信息并查询上传进度，
 * 并在分片齐全后合并完整文件和校验文件摘要。
 */
@Service
public class FileUploadServiceI implements FileUploadService {

    private final UserRepository userRepository;
    private final OrganizationTagRepository organizationTagRepository;
    private final FileUploadRepository fileUploadRepository;

    private final UploadProperties uploadProperties;
    private final ChunkInfoRepository chunkInfoRepository;
    private final MinioStorageService minioStorageService;


    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceI.class);

    public FileUploadServiceI(
            UserRepository userRepository,
            OrganizationTagRepository organizationTagRepository,
            FileUploadRepository fileUploadRepository,
            UploadProperties uploadProperties,
            ChunkInfoRepository chunkInfoRepository, MinioStorageService minioStorageService) {
        this.userRepository = userRepository;
        this.organizationTagRepository = organizationTagRepository;
        this.fileUploadRepository = fileUploadRepository;
        this.uploadProperties = uploadProperties;
        this.chunkInfoRepository = chunkInfoRepository;
        this.minioStorageService = minioStorageService;
    }

    @Override
    @Transactional
    public UploadInitResponse initializeUpload(String username, UploadInitRequest request) {

        // 调用顺序：Controller 已先用 @Valid 校验请求格式；这里再根据 JWT 认证出的用户名确定上传者。
        // 不从请求体接收 userId，避免客户端冒用其他用户身份创建上传任务。
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        // 未指定组织时归到用户当前主组织；显式指定时先去掉首尾空格，再检查用户的组织成员关系。
        String orgTagId = request.orgTagId();
        if(orgTagId == null || orgTagId.isBlank()){
            orgTagId = user.getPrimaryOrgTagId();
        } else {
            orgTagId = orgTagId.trim();
        }

        if (orgTagId == null || orgTagId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户没有可用的主组织标签");
        }

        // 组织标签存在并不代表当前用户有权使用；必须同时出现在用户自己的组织标签列表中。
        List<String> userOrgTagIds = user.getOrgTagIds() == null ? List.of() : Arrays.stream(user.getOrgTagIds().split(","))
                .map(String::trim)
                .filter(tagId -> !tagId.isEmpty())
                .toList();

        if(!userOrgTagIds.contains(orgTagId)){
            throw new BusinessException(ErrorCode.FORBIDDEN, "你无权向该组织上传文件");
        }

        OrganizationTag organizationTag = organizationTagRepository.findById(orgTagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "组织标签不存在"));

        // MD5 和文件名来自客户端，MD5 此时只是任务元数据；后续合并完整文件时仍需由服务端重新计算校验。
        String fileMd5 = request.fileMd5().trim().toLowerCase(Locale.ROOT);
        String fileName = request.fileName().trim();

        boolean requestedPublic = Boolean.TRUE.equals(request.isPublic());

        // 查询所有相同fileMd5的相同状态的文件上传任务
        List<FileUpload> candidates = fileUploadRepository.findAllByUser_IdAndFileMd5AndStatusOrderByCreatedAtDesc(
                user.getId(),
                fileMd5,
                FileUploadStatus.UPLOADING
        );


        for(FileUpload candidate : candidates){
            boolean sameUploadOptions = Objects.equals(candidate.getTotalSize(), request.totalSize())
                    && Objects.equals(candidate.getOrganizationTag().getTagId(), orgTagId)
                    && candidate.isPublic() == requestedPublic;

            if(!sameUploadOptions){
                continue;
            }

            // 找到文件大小、组织标签和公开设置都相同的未完成任务后，返回旧任务及其已登记的分片序号。
            List<Integer> uploadedChunks = chunkInfoRepository
                    .findAllByFileUpload_IdOrderByChunkIndexAsc(candidate.getId())
                    .stream()
                    .map(ChunkInfo::getChunkIndex)
                    .toList();

            return new UploadInitResponse(
                    candidate.getId(),
                    candidate.getChunkSize(),
                    candidate.getTotalChunks(),
                    uploadedChunks
            );
        }

        // 创建新的文件上传记录
        // 只有新建任务时才按当前配置计算分片方案；恢复任务直接使用数据库里保存的原方案。
        // 除法向上取整，保证文件大小不是 chunkSize 整数倍时仍会分配最后一片。
        long chunkSize = uploadProperties.chunkSizeBytes();
        int totalChunks = Math.toIntExact((request.totalSize() - 1) / chunkSize + 1);

        Instant now = Instant.now();

        FileUpload fileUpload = new FileUpload();
        fileUpload.setFileMd5(fileMd5);
        fileUpload.setFileName(fileName);
        fileUpload.setTotalSize(request.totalSize());
        fileUpload.setChunkSize(chunkSize);
        fileUpload.setTotalChunks(totalChunks);
        fileUpload.setStatus(FileUploadStatus.UPLOADING);
        fileUpload.setUser(user);
        fileUpload.setOrganizationTag(organizationTag);
        fileUpload.setPublic(requestedPublic);
        fileUpload.setCreatedAt(now);
        fileUpload.setUpdatedAt(now);

        // 没有可复用的未完成任务时才创建记录，因此新任务的已上传分片列表为空。
        FileUpload saved = fileUploadRepository.save(fileUpload);

        // IDENTITY 主键由数据库生成，save 返回后从实体读取；客户端后续用它标识本次上传任务。
        return new UploadInitResponse(
                saved.getId(),
                chunkSize,
                totalChunks,
                List.of()
        );
    }

    @Override
    public void uploadChunk(String username, Long fileId, Integer chunkIndex, MultipartFile chunk) {

        // 先用认证用户定位用户记录，再按 fileId + userId 查询任务；只知道自增 fileId 不能操作别人的任务。
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        // 查询文件上传任务的创建记录
        FileUpload fileUpload = fileUploadRepository.findByIdAndUser_Id(fileId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "上传任务不存在"));

        // 只接受尚在分片上传阶段的任务，已合并或进入处理阶段的任务不可继续写入分片。
        if(fileUpload.getStatus() != FileUploadStatus.UPLOADING) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前文件不处于上传状态");
        }

        if(chunkIndex == null || chunkIndex < 0 || chunkIndex >= fileUpload.getTotalChunks()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分片序号超出范围");
        }

        // 分片序号从 0 开始；根据序号计算文件偏移，再推导该片应有字节数。
        // 最后一片取剩余字节数，所以允许比配置的目标分片大小更小。
        long offset = (long) chunkIndex * fileUpload.getChunkSize();
        long expectedSize = Math.min(fileUpload.getChunkSize(), fileUpload.getTotalSize() - offset);

        if(chunk == null || chunk.isEmpty() || chunk.getSize() != expectedSize){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分片大小不符合上传方案");
        }

        // 当前分片大小受服务端 chunkSize 配置约束；读入内存用于计算摘要并上传到 MinIO。
        byte[] content;
        try{
            content = chunk.getBytes();
        } catch (IOException exception){
            log.error(
                    "读取上传分片失败，fileId={}, chunkIndex={}",
                    fileId,
                    chunkIndex,
                    exception
            );
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取分片失败");
        }

        // 对实际收到的分片字节计算摘要；这不同于初始化请求里的完整文件 MD5。
        String chunkMd5 = DigestUtils.md5DigestAsHex(content);

        // 同一任务的同一序号若已登记：内容相同视为网络重试成功，内容不同则拒绝覆盖。
        Optional<ChunkInfo> existing = chunkInfoRepository.findByFileUpload_IdAndChunkIndex(fileId, chunkIndex);
        if(existing.isPresent()){
            if(existing.get().getChunkMd5().equals(chunkMd5)){
                return ; // 同内容重试按成功处理，支持网络重试
            }
            throw new BusinessException(ErrorCode.CONFLICT, "该分片已上传，但内容不一致");
        }

        // 对象键由服务端生成并带随机后缀，避免用户文件名影响路径，也避免并发请求互相覆盖对象。
        String objectKey = "chunks/" + fileId + "/" + chunkIndex + "/" + UUID.randomUUID();

        // 对象存储和关系数据库没有共同事务：先上传对象，再登记对象键和分片摘要。
        try(InputStream inputStream = new ByteArrayInputStream(content)) {
            minioStorageService.uploadChunk(objectKey, inputStream, content.length);
        } catch (IOException exception){
            log.error(
                    "处理分片输入流失败，fileId={}, chunkIndex={}",
                    fileId,
                    chunkIndex,
                    exception
            );
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取分片流失败");
        }

        ChunkInfo chunkInfo = new ChunkInfo();
        chunkInfo.setFileUpload(fileUpload);
        chunkInfo.setChunkIndex(chunkIndex);
        chunkInfo.setChunkMd5(chunkMd5);
        chunkInfo.setObjectKey(objectKey);
        chunkInfo.setCreatedAt(Instant.now());

        try {
            // saveAndFlush 立即执行数据库写入，让唯一键冲突等登记失败在当前代码块中暴露。
            chunkInfoRepository.saveAndFlush(chunkInfo);
        } catch (RuntimeException exception) {
            // 数据库登记失败时补偿删除刚上传的 MinIO 对象，避免留下没有数据库记录的孤立分片。
            try {
                minioStorageService.deleteObject(objectKey);
            } catch (BusinessException cleanupException) {
                // 清理也失败时保留日志，便于后续定位孤立对象。
                log.error(
                        "数据库登记失败后，清理 MinIO 分片也失败，fileId={}, chunkIndex={}, objectKey={}",
                        fileId,
                        chunkIndex,
                        objectKey,
                        cleanupException
                );
            }
            throw exception;
        }
    }

    /**
     * 依据数据库中的分片登记记录计算进度；只有属于当前用户的任务才可查询。
     */
    @Override
    @Transactional(readOnly = true)
    public UploadStatusResponse getUploadStatus(String username, Long fileId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        FileUpload fileUpload = fileUploadRepository.findByIdAndUser_Id(fileId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "上传任务不存在"));

        // 只把已成功登记到 MySQL 的分片计入进度；按序返回也便于前端恢复上传。
        List<Integer> uploadedChunks = chunkInfoRepository
                .findAllByFileUpload_IdOrderByChunkIndexAsc(fileId)
                .stream()
                .map(ChunkInfo::getChunkIndex)
                .toList();

        int totalChunks = fileUpload.getTotalChunks();
        double progress = totalChunks <= 0
                ? 0.0
                : Math.round(uploadedChunks.size() * 1000.0 / totalChunks) / 10.0;

        return new UploadStatusResponse(
                fileUpload.getId(),
                fileUpload.getStatus(),
                totalChunks,
                uploadedChunks,
                progress
        );
    }

    @Override
    public void mergeChunks(String username, Long fileId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));

        FileUpload fileUpload = fileUploadRepository.findByIdAndUser_Id(fileId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "上传任务不存在"));

        // 重复请求时，如果前一次已经完成合并，就按成功处理。
        if (fileUpload.getStatus() == FileUploadStatus.UPLOADED
                && fileUpload.getObjectKey() != null) {
            return;
        }

        if (fileUpload.getStatus() != FileUploadStatus.UPLOADING) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前文件状态不允许合并");
        }

        List<ChunkInfo> chunks =
                chunkInfoRepository.findAllByFileUpload_IdOrderByChunkIndexAsc(fileId);

        // 数量相等还不够；后面还要检查序号连续，防止缺片。
        if (chunks.size() != fileUpload.getTotalChunks()) {
            throw new BusinessException(ErrorCode.CONFLICT, "文件分片尚未上传完整");
        }

        List<String> sourceObjectKeys = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkInfo chunk = chunks.get(i);

            // 必须是 0 到 totalChunks - 1 连续序号。
            if (!Objects.equals(chunk.getChunkIndex(), i)) {
                throw new BusinessException(ErrorCode.CONFLICT, "文件分片序号不完整");
            }

            sourceObjectKeys.add(chunk.getObjectKey());
        }

        // 对象键由服务端生成，不把用户提供的文件名拼进存储路径。
        String mergedObjectKey = "files/" + fileId + "/original";

        FileChecksum checksum;
        try {
            // MinIO 按 sourceObjectKeys 的顺序组合对象。
            minioStorageService.composeChunks(sourceObjectKeys, mergedObjectKey);

            // 流式读取合并结果，计算真实字节数和完整文件 MD5，不把整份文件放入内存。
            try (InputStream inputStream = minioStorageService.getObject(mergedObjectKey)) {
                checksum = calculateChecksum(inputStream);
            }
        } catch (BusinessException | IOException exception) {
            // 合并或读取校验数据失败时，尽量清理可能生成的目标对象。
            deleteMergedObjectSafely(fileId, mergedObjectKey);

            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }

            log.error("读取合并文件进行校验失败，fileId={}, objectKey={}",
                    fileId, mergedObjectKey, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件合并校验失败");
        }

        boolean sizeMatches = checksum.size() == fileUpload.getTotalSize();
        boolean md5Matches = checksum.md5().equalsIgnoreCase(fileUpload.getFileMd5());

        if (!sizeMatches || !md5Matches) {
            deleteMergedObjectSafely(fileId, mergedObjectKey);

            // 内容和初始化时声明的信息不一致，不能进入后续处理。
            fileUpload.setStatus(FileUploadStatus.FAILED);
            fileUpload.setUpdatedAt(Instant.now());
            fileUploadRepository.saveAndFlush(fileUpload);

            throw new BusinessException(ErrorCode.CONFLICT, "合并文件校验失败");
        }

        fileUpload.setObjectKey(mergedObjectKey);
        fileUpload.setMergedAt(Instant.now());
        fileUpload.setUpdatedAt(fileUpload.getMergedAt());
        fileUpload.setStatus(FileUploadStatus.UPLOADED);

        try {
            // MinIO 和 MySQL 不共享事务；数据库更新失败时补偿删除刚生成的完整文件对象。
            fileUploadRepository.saveAndFlush(fileUpload);
        } catch (RuntimeException exception) {
            deleteMergedObjectSafely(fileId, mergedObjectKey);
            throw exception;
        }
    }

    private FileChecksum calculateChecksum(InputStream inputStream) throws IOException {
        final MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exception) {
            // Java 环境应当提供 MD5；缺失属于运行环境异常。
            throw new IllegalStateException("当前 Java 环境不支持 MD5", exception);
        }

        byte[] buffer = new byte[8192];
        long size = 0;
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
            size += bytesRead;
        }

        return new FileChecksum(size, HexFormat.of().formatHex(digest.digest()));
    }

    private void deleteMergedObjectSafely(Long fileId, String objectKey) {
        try {
            minioStorageService.deleteObject(objectKey);
        } catch (BusinessException cleanupException) {
            log.error("清理合并文件失败，fileId={}, objectKey={}",
                    fileId, objectKey, cleanupException);
        }
    }

    private record FileChecksum(long size, String md5) {
    }
}
