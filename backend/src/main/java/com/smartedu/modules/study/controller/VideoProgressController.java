package com.smartedu.modules.study.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.study.dto.VideoProgressDTO;
import com.smartedu.modules.study.entity.VideoProgress;
import com.smartedu.modules.study.service.VideoProgressService;
import com.smartedu.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频学习进度控制器
 */
@RestController
@RequestMapping("/api/study/video/progress")
public class VideoProgressController {

    @Autowired
    private VideoProgressService progressService;

    // 获取当前登录用户ID
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getId();
        }
        return null;
    }

    /**
     * 保存学习进度（增量模式）
     * POST /api/study/video/progress/save
     */
    @PostMapping("/save")
    @PreAuthorize("hasRole('STUDENT')")
    public Result saveProgress(@RequestBody VideoProgressDTO dto) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error("未登录");
        }

        // 使用增量模式保存
        progressService.saveOrUpdateProgressWithDelta(studentId, dto);
        return Result.success("进度保存成功");
    }

    /**
     * 获取视频学习进度
     * GET /api/study/video/progress/{videoId}
     */
    @GetMapping("/{videoId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result getProgress(@PathVariable Long videoId) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error("未登录");
        }

        VideoProgress progress = progressService.getProgress(studentId, videoId);

        // 统一返回格式
        Map<String, Object> result = new HashMap<>();
        if (progress == null) {
            result.put("currentTime", 0);
            result.put("totalStudyTime", 0);
            result.put("isCompleted", 0);
            result.put("studyCount", 0);
        } else {
            result.put("currentTime", progress.getCurrentPosition() != null ? progress.getCurrentPosition() : 0);
            result.put("totalStudyTime", progress.getTotalStudyTime() != null ? progress.getTotalStudyTime() : 0);
            result.put("isCompleted", progress.getIsCompleted() != null ? progress.getIsCompleted() : 0);
            result.put("studyCount", progress.getStudyCount() != null ? progress.getStudyCount() : 0);
        }

        return Result.success(result);
    }

    /**
     * 获取课程学习进度统计
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result getCourseProgress(@PathVariable Long courseId) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error("未登录");
        }

        Long totalVideos = progressService.lambdaQuery()
                .eq(VideoProgress::getStudentId, studentId)
                .eq(VideoProgress::getCourseId, courseId)
                .count();

        Long completedVideos = progressService.lambdaQuery()
                .eq(VideoProgress::getStudentId, studentId)
                .eq(VideoProgress::getCourseId, courseId)
                .eq(VideoProgress::getIsCompleted, 1)
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalVideos", totalVideos);
        result.put("completedVideos", completedVideos);
        result.put("progress", totalVideos > 0 ? (completedVideos * 100 / totalVideos) : 0);

        return Result.success(result);
    }
}