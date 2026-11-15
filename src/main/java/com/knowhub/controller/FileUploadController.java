package com.knowhub.controller;

import com.knowhub.common.ApiResponse;
import com.knowhub.common.ResultUtils;
import com.knowhub.dto.file.UploadInitRequest;
import com.knowhub.dto.file.UploadInitResponse;
import com.knowhub.dto.file.UploadStatusResponse;
import com.knowhub.service.FileUploadService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传 HTTP 入口：负责读取认证用户、校验请求格式并把业务交给 Service。
 * 文件分片的权限、状态和大小检查在 Service 中完成。
 */
@RestController
@RequestMapping("/api/v1/upload")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService){
        this.fileUploadService = fileUploadService;
    }

    /**
     * 第一步：初始化文件上传；若存在匹配的未完成任务，则返回原任务和已上传分片序号。
     * @param userDetails JWT 过滤器认证后放入安全上下文的当前用户
     * @param request 上传初始化参数
     * @return 文件 ID 和分片方案
     */
    @PostMapping("/init")
    public ApiResponse<UploadInitResponse> initializeUpload(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UploadInitRequest request){

        String username = userDetails.getUsername();

        UploadInitResponse uploadInitResponse = fileUploadService.initializeUpload(username, request);

        return ResultUtils.success(uploadInitResponse);
    }

    /**
     * 第二步：接收一个 multipart/form-data 分片。
     * 表单文件字段名必须是 chunk；当前登录用户从 JWT 认证信息取得，不从请求参数接收用户名。
     */
    @PutMapping(value = "/{fileId}/chunks/{chunkIndex}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> uploadChunk(@AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long fileId, @PathVariable Integer chunkIndex, @RequestPart("chunk") MultipartFile chunk
    ){
        // Controller 不处理对象存储和数据库细节，只转交当前用户、任务 ID、分片序号和分片内容。
        fileUploadService.uploadChunk(userDetails.getUsername(), fileId, chunkIndex, chunk);

        return ResultUtils.success(null);
    }

    /**
     * 查询上传状态和已登记分片；用户身份从 JWT 认证信息取得。
     */
    @GetMapping("/{fileId}/status")
    public ApiResponse<UploadStatusResponse> getUploadStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long fileId) {
        UploadStatusResponse response =
                fileUploadService.getUploadStatus(userDetails.getUsername(), fileId);

        return ResultUtils.success(response);
    }

    /**
     * 在所有分片上传完成后，请求服务端合并并校验完整文件。
     */
    @PostMapping("/{fileId}/merge")
    public ApiResponse<Void> mergeChunks(@AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long fileId
    ){
        fileUploadService.mergeChunks(userDetails.getUsername(), fileId);

        return ResultUtils.success(null);
    }

}
