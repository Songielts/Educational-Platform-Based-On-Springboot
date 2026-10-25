package com.smartedu.modules.course.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.course.entity.Video;
import com.smartedu.modules.course.service.VideoService;
import com.smartedu.modules.system.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/course/video")  // 注意：这里不需要 /api 前缀，因为后端配置了 context-path: /api
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private FileService fileService;

    // 获取章节视频列表
    @GetMapping("/list/{chapterId}")
    public Result getVideoList(@PathVariable Long chapterId) {
        List<Video> list = videoService.getVideosByChapterId(chapterId);
        return Result.success(list);
    }

    // 上传视频
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result uploadVideo(@RequestParam("file") MultipartFile file,
                              @RequestParam Long chapterId,
                              @RequestParam Long courseId,
                              @RequestParam String videoName) {
        try {
            String videoUrl = fileService.uploadVideo(file);

            Video video = new Video();
            video.setChapterId(chapterId);
            video.setCourseId(courseId);
            video.setVideoName(videoName);
            video.setVideoUrl(videoUrl);
            video.setVideoSize(file.getSize());

            videoService.save(video);
            return Result.success(video);
        } catch (Exception e) {
            return Result.error("视频上传失败：" + e.getMessage());
        }
    }

    // 删除视频
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result deleteVideo(@PathVariable Long id) {
        videoService.removeById(id);
        return Result.success();
    }
}