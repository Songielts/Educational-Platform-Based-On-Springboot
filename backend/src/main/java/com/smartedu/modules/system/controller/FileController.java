package com.smartedu.modules.system.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 文件上传控制器（修复版）
 *
 * 关键修复：
 * 1. Range 请求使用自定义 InputStreamResource，只返回请求的字节范围
 * 2. ClientAbortException 返回空 204 响应，不触发全局异常处理器
 * 3. 路径解析更健壮
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Value("${upload.path:D:/Online study/smart-edu-platform/uploads}")
    private String uploadPath;

    /**
     * 上传图片
     */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileService.uploadImage(file);
        return Result.success("上传成功", url);
    }

    /**
     * 上传视频（直接上传）
     */
    @PostMapping("/video")
    public Result<String> uploadVideo(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileService.uploadVideo(file);
        return Result.success("上传成功", url);
    }

    /**
     * 通用文件流获取
     */
    @GetMapping("/file/stream")
    public ResponseEntity<Resource> streamFile(
            @RequestParam("path") String path,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        return doStreamFile(path, rangeHeader, null);
    }

    /**
     * 获取视频文件流（强制返回 video/mp4）
     */
    @GetMapping("/video/stream")
    public ResponseEntity<Resource> streamVideo(
            @RequestParam("path") String path,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        return doStreamFile(path, rangeHeader, "video/mp4");
    }

    /**
     * 核心修复：统一的文件流处理
     */
    private ResponseEntity<Resource> doStreamFile(String path, String rangeHeader, String forceContentType) {
        log.info("[doStreamFile] path={}, range={}, type={}", path, rangeHeader, forceContentType);

        File file = null;
        try {
            // 安全检查
            if (path == null || path.isEmpty() || path.contains("..")) {
                log.error("[doStreamFile] 非法路径: {}", path);
                return ResponseEntity.badRequest().build();
            }

            // 构建完整路径（path 格式: /uploads/videos/20260516/xxx.mp4）
            String relativePath = path;
            if (relativePath.startsWith("/uploads/")) {
                relativePath = relativePath.substring("/uploads/".length());
            } else if (relativePath.startsWith("uploads/")) {
                relativePath = relativePath.substring("uploads/".length());
            }
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }

            Path filePath = Paths.get(uploadPath, relativePath);
            file = filePath.toFile();

            log.info("[doStreamFile] 完整路径: {}", filePath);

            if (!file.exists() || !file.isFile()) {
                log.error("[doStreamFile] 文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            long fileLength = file.length();
            String contentType = forceContentType != null ? forceContentType : resolveContentType(path);

            // 处理 Range 请求
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequest(file, rangeHeader, fileLength, contentType);
            }

            // 返回完整文件（200 OK）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentLength(fileLength);

            log.info("[doStreamFile] 返回完整文件: {} bytes", fileLength);
            return ResponseEntity.ok().headers(headers).body(new FileSystemResource(file));

        } catch (Exception e) {
            // 修复：浏览器断开时返回空响应，不触发全局异常处理器
            String exName = e.getClass().getName();
            String causeName = e.getCause() != null ? e.getCause().getClass().getName() : "";
            if (exName.contains("ClientAbort") || causeName.contains("ClientAbort")) {
                log.debug("[doStreamFile] 客户端断开: {}", path);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            log.error("[doStreamFile] 异常: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 核心修复：正确处理 Range 请求，只返回请求的字节范围
     */
    private ResponseEntity<Resource> handleRangeRequest(File file, String rangeHeader,
                                                        long fileLength, String contentType) {
        try {
            // 解析 Range: bytes=start-end
            String rangeValue = rangeHeader.substring(6); // 去掉 "bytes="
            int dashIndex = rangeValue.indexOf('-');
            if (dashIndex < 0) {
                return rangeNotSatisfiable(fileLength);
            }

            long start = Long.parseLong(rangeValue.substring(0, dashIndex).trim());
            long end = (dashIndex + 1 < rangeValue.length())
                    ? Long.parseLong(rangeValue.substring(dashIndex + 1).trim())
                    : fileLength - 1;

            // 边界校验
            if (start < 0) start = 0;
            if (end >= fileLength) end = fileLength - 1;
            if (start > end) {
                return rangeNotSatisfiable(fileLength);
            }

            long contentLength = end - start + 1;

            // 核心修复：创建只返回指定范围的 InputStreamResource
            Resource rangeResource = createRangeResource(file, start, contentLength);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
            headers.setContentLength(contentLength);

            log.info("[handleRange] bytes {}-{}/{}, length={}", start, end, fileLength, contentLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(rangeResource);

        } catch (NumberFormatException e) {
            log.error("[handleRange] 解析失败: {}", rangeHeader);
            return rangeNotSatisfiable(fileLength);
        }
    }

    private ResponseEntity<Resource> rangeNotSatisfiable(long fileLength) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                .build();
    }

    /**
     * 核心修复：创建只读取指定字节范围的 Resource
     * 之前返回完整的 FileSystemResource，浏览器拿到的是整个文件，但只有部分内容头，导致播放失败
     */
    private Resource createRangeResource(File file, long start, long length) {
        return new Resource() {
            @Override
            public String getDescription() {
                return "bytes " + start + "-" + (start + length - 1) + " of " + file.getAbsolutePath();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(start);
                return new InputStream() {
                    private long remaining = length;

                    @Override
                    public int read() throws IOException {
                        if (remaining <= 0) return -1;
                        remaining--;
                        return raf.read();
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        if (remaining <= 0) return -1;
                        int toRead = (int) Math.min(len, remaining);
                        int read = raf.read(b, off, toRead);
                        if (read > 0) remaining -= read;
                        return read;
                    }

                    @Override
                    public void close() throws IOException {
                        raf.close();
                    }
                };
            }

            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public URL getURL() throws IOException {
                return null;
            }

            @Override
            public URI getURI() throws IOException {
                return null;
            }

            @Override
            public File getFile() throws IOException {
                return null;
            }

            @Override
            public long contentLength() {
                return length;
            }

            @Override
            public long lastModified() throws IOException {
                return 0;
            }

            @Override
            public Resource createRelative(String relativePath) throws IOException {
                return null;
            }

            @Override
            public String getFilename() {
                return "";
            }
        };
    }

    /**
     * 根据文件路径解析 Content-Type
     */
    private String resolveContentType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) return "image/jpeg";
        if (lowerPath.endsWith(".png")) return "image/png";
        if (lowerPath.endsWith(".gif")) return "image/gif";
        if (lowerPath.endsWith(".webp")) return "image/webp";
        if (lowerPath.endsWith(".mp4")) return "video/mp4";
        if (lowerPath.endsWith(".webm")) return "video/webm";
        if (lowerPath.endsWith(".pdf")) return "application/pdf";
        if (lowerPath.endsWith(".mp3")) return "audio/mpeg";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    @PostMapping("/video/chunk")
    public Result<Map<String, Object>> uploadVideoChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkNumber") Integer chunkNumber,
            @RequestParam("totalChunks") Integer totalChunks) throws Exception {
        Map<String, Object> result = fileService.uploadVideoChunk(file, uploadId, chunkNumber, totalChunks);
        return Result.success(result);
    }

    @PostMapping("/video/merge")
    public Result<String> mergeVideoChunks(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks) throws Exception {
        String url = fileService.mergeVideoChunks(uploadId, fileName, totalChunks);
        return Result.success("合并成功", url);
    }

    @PostMapping("/doc")
    public Result<String> uploadDocument(@RequestParam("file") MultipartFile file) throws Exception {
        String url = fileService.uploadDocument(file);
        return Result.success("上传成功", url);
    }
}