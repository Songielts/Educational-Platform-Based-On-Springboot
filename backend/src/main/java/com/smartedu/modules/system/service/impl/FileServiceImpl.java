package com.smartedu.modules.system.service.impl;

import com.smartedu.common.exception.BusinessException;
import com.smartedu.common.result.ResultCode;
import com.smartedu.modules.system.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件Service实现类
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    // 使用 upload.path 配置（适配你的配置格式）
    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Value("${upload.image-path:uploads/images/}")
    private String imagePath;

    @Value("${upload.video-path:uploads/videos/}")
    private String videoPath;

    @Value("${upload.max-image-size:10485760}")
    private long maxImageSize;

    @Value("${upload.max-video-size:1073741824}")  // 1GB（支持500MB及以上视频上传）
    private long maxVideoSize;

    // 访问URL前缀
    private static final String URL_PREFIX = "/uploads";

    // 允许的图片类型
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 初始化上传目录
     */
    @PostConstruct
    public void init() {
        try {
            // 获取基础目录
            Path baseDir = getBasePath();
            log.info("上传基础目录: {}", baseDir);

            // 创建图片目录
            Path imageDir = baseDir.resolve("images");
            if (!Files.exists(imageDir)) {
                Files.createDirectories(imageDir);
                log.info("创建图片目录: {}", imageDir);
            }

            // 创建视频目录
            Path videoDir = baseDir.resolve("videos");
            if (!Files.exists(videoDir)) {
                Files.createDirectories(videoDir);
                log.info("创建视频目录: {}", videoDir);
            }

            // 创建文档目录
            Path docDir = baseDir.resolve("docs");
            if (!Files.exists(docDir)) {
                Files.createDirectories(docDir);
                log.info("创建文档目录: {}", docDir);
            }

        } catch (IOException e) {
            log.error("创建上传目录失败", e);
        }
    }

    @Override
    public String uploadImage(MultipartFile file) {
        // 1. 验证文件
        validateFile(file, ALLOWED_IMAGE_TYPES, maxImageSize, "图片");

        // 2. 生成文件名
        String newFilename = generateFilename(file.getOriginalFilename());

        // 3. 创建日期子目录
        String dateDir = java.time.LocalDate.now().toString().replace("-", "");

        try {
            // 4. 获取目标目录
            Path targetDir = getBasePath().resolve("images").resolve(dateDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 5. 保存文件
            Path targetPath = targetDir.resolve(newFilename);
            file.transferTo(targetPath.toFile());

            log.info("图片上传成功: {}", targetPath);

            // 6. 返回访问URL
            return URL_PREFIX + "/images/" + dateDir + "/" + newFilename;

        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "图片上传失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> uploadVideoChunk(MultipartFile file, String uploadId, Integer chunkNumber, Integer totalChunks) throws IOException {
        return Map.of();
    }

    @Override
    public String mergeVideoChunks(String uploadId, String fileName, Integer totalChunks) throws IOException {
        return "";
    }

    @Override
    public String uploadDocument(MultipartFile file) throws IOException {
        return "";
    }

    @Override
    public String uploadVideo(MultipartFile file) {
        // 1. 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "仅支持视频文件");
        }

        // 2. 验证文件大小（1GB上限，支持500MB+论文要求）
        validateFileSize(file, maxVideoSize, "视频");

        // 3. 生成文件名
        String newFilename = generateFilename(file.getOriginalFilename());

        // 4. 创建日期子目录
        String dateDir = java.time.LocalDate.now().toString().replace("-", "");

        try {
            // 5. 获取目标目录
            Path targetDir = getBasePath().resolve("videos").resolve(dateDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 6. 保存文件
            Path targetPath = targetDir.resolve(newFilename);
            file.transferTo(targetPath.toFile());

            log.info("视频上传成功: {}", targetPath);

            // 7. 返回访问URL
            return URL_PREFIX + "/videos/" + dateDir + "/" + newFilename;

        } catch (IOException e) {
            log.error("视频上传失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "视频上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String relativePath = fileUrl.replace(URL_PREFIX, "");
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            Path filePath = getBasePath().resolve(relativePath);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功: {}", filePath);

        } catch (IOException e) {
            log.error("文件删除失败: {}", fileUrl, e);
        }
    }

    /**
     * 获取基础路径
     */
    private Path getBasePath() {
        String path = uploadPath;
        // 清理路径
        if (path.startsWith("/") || path.startsWith("\\")) {
            path = path.substring(1);
        }
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        return Paths.get(path).toAbsolutePath().normalize();
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file, List<String> allowedTypes, long maxSize, String fileTypeName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要上传的" + fileTypeName);
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件格式不支持");
        }

        validateFileSize(file, maxSize, fileTypeName);
    }

    /**
     * 验证文件大小
     */
    private void validateFileSize(MultipartFile file, long maxSize, String fileTypeName) {
        if (file.getSize() > maxSize) {
            double maxSizeGB = maxSize / 1024.0 / 1024.0 / 1024.0;
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                    fileTypeName + "大小不能超过" + String.format("%.0fGB", maxSizeGB));
        }
    }

    /**
     * 生成文件名
     */
    private String generateFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}