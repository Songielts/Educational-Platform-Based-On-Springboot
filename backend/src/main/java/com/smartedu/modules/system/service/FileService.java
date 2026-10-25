package com.smartedu.modules.system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 文件Service接口
 */
public interface FileService {

    /**
     * 上传图片
     */
    String uploadImage(MultipartFile file) throws IOException;

    /**
     * 上传视频（分片）
     */
    Map<String, Object> uploadVideoChunk(MultipartFile file, String uploadId, Integer chunkNumber, Integer totalChunks) throws IOException;

    /**
     * 合并视频分片
     */
    String mergeVideoChunks(String uploadId, String fileName, Integer totalChunks) throws IOException;

    /**
     * 上传文档
     */
    String uploadDocument(MultipartFile file) throws IOException;

    String uploadVideo(MultipartFile file);

    /**
     * 删除文件
     */
    void deleteFile(String fileUrl);
}