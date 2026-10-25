package com.smartedu.modules.study.controller;

import com.smartedu.common.result.PageResult;
import com.smartedu.common.result.Result;
import com.smartedu.modules.course.mapper.LessonMapper;
import com.smartedu.modules.study.dto.*;
import com.smartedu.modules.study.entity.CourseStat;
import com.smartedu.modules.study.entity.VideoProgress;
import com.smartedu.modules.study.mapper.CourseStatMapper;
import com.smartedu.modules.study.mapper.VideoProgressMapper;
import com.smartedu.modules.study.service.StudyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学习控制器
 * 新增：POST /study/progress/{lessonId} 接口（匹配前端30秒上报）
 */
@Slf4j
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private CourseStatMapper courseStatMapper;

    @Autowired
    private LessonMapper lessonMapper;

    // ==================== 新增：课时学习进度（匹配前端路径） ====================

    /**
     * 更新课时学习进度
     * 前端路径：POST /study/progress/{lessonId}
     * 功能：保存播放位置到 study_video_progress，完成时更新 study_course_stat
     */
    @PostMapping("/progress/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> updateLessonProgress(@AuthenticationPrincipal Long studentId,
                                             @PathVariable Long lessonId,
                                             @RequestBody Map<String, Object> data) {
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        Long courseId = data.get("courseId") != null ? Long.valueOf(data.get("courseId").toString()) : null;
        Integer lastPosition = data.get("lastPosition") != null ? Integer.valueOf(data.get("lastPosition").toString()) : 0;
        Integer totalDuration = data.get("duration") != null ? Integer.valueOf(data.get("duration").toString()) : 0;
        // 如果前端没传有效duration（视频编码问题导致浏览器读不到），从 edu_lesson 表获取
        if (totalDuration == null || totalDuration <= 0) {
            com.smartedu.modules.course.entity.Lesson lesson = lessonMapper.selectById(lessonId);
            if (lesson != null && lesson.getVideoDuration() != null) {
                totalDuration = lesson.getVideoDuration().intValue();
                log.info("[updateLessonProgress] 前端duration=0，从edu_lesson获取: {}s", totalDuration);
            }
        }
        Integer progress = data.get("progress") != null ? Integer.valueOf(data.get("progress").toString()) : 0;
        Boolean completed = Boolean.TRUE.equals(data.get("completed"));

        // 1. 保存/更新视频播放进度（使用 VideoProgressMapper 的自定义方法）
        VideoProgress vp = videoProgressMapper.selectByStudentAndLesson(studentId, lessonId);
        if (vp == null) {
            vp = new VideoProgress();
            vp.setStudentId(studentId);
            vp.setLessonId(lessonId);
            vp.setCourseId(courseId);
            vp.setCurrentPosition(lastPosition);
            vp.setTotalDuration(totalDuration);
            vp.setProgressPercent(new BigDecimal(progress));
            vp.setIsCompleted(completed ? 1 : 0);
            vp.setStudyCount(1);
            vp.setTotalStudyTime(30);
            vp.setDeleted(0);  // <-- 关键：显式设置deleted=0，避免逻辑删除条件不匹配
            vp.setLastStudyTime(LocalDateTime.now());
            vp.setCreateTime(LocalDateTime.now());
            vp.setUpdateTime(LocalDateTime.now());
            videoProgressMapper.insert(vp);
        } else {
            int newIsCompleted = completed ? 1 : (vp.getIsCompleted() != null ? vp.getIsCompleted() : 0);
            BigDecimal newProgress = new BigDecimal(progress);

            // 使用原子UPDATE（数据库层面study_count+1、total_study_time+30，避免并发覆盖）
            videoProgressMapper.updateProgressAtomic(
                    vp.getId(),
                    lastPosition,
                    totalDuration > 0 ? totalDuration : vp.getTotalDuration(),
                    newProgress,
                    newIsCompleted,
                    30,  // 每次增加30分钟
                    LocalDateTime.now()
            );
        }

        log.info("[updateLessonProgress] studentId={}, lessonId={}, progress={}, completed={}",
                studentId, lessonId, progress, completed);

        // 2. 如果标记完成，更新 study_course_stat（使用 CourseStatMapper 的自定义方法）
        if (completed && courseId != null) {
            updateCourseStat(studentId, courseId);
        }

        return Result.success();
    }

    /**
     * 内部方法：更新课程学习统计（完成课时 + 完成率）
     * 使用 CourseStatMapper.selectByStudentAndCourse 和 LessonMapper.countByCourseId
     */
    private void updateCourseStat(Long studentId, Long courseId) {
        // 查询课程总课时数（使用 LessonMapper 的自定义方法）
        int totalLessons = lessonMapper.countByCourseId(courseId);
        if (totalLessons == 0) {
            log.warn("[updateCourseStat] 课程 {} 无有效课时", courseId);
            return;
        }

        // 查询或创建统计记录（使用 CourseStatMapper 的自定义方法）
        CourseStat stat = courseStatMapper.selectByStudentAndCourse(studentId, courseId);

        if (stat == null) {
            stat = new CourseStat();
            stat.setStudentId(studentId);
            stat.setCourseId(courseId);
            stat.setTotalLessons(totalLessons);
            stat.setCompletedLessons(1);
            stat.setCompletionRate(
                    new BigDecimal(100).divide(new BigDecimal(totalLessons), 2, RoundingMode.HALF_UP)
            );
            stat.setTotalStudyTime(0);
            stat.setLastStudyTime(LocalDateTime.now());
            stat.setCreateTime(LocalDateTime.now());
            stat.setUpdateTime(LocalDateTime.now());
            courseStatMapper.insert(stat);
            log.info("[updateCourseStat] 新建统计记录 studentId={}, courseId={}, completed=1/{}",
                    studentId, courseId, totalLessons);
        } else {
            // 防重：如果 completionRate 已经是100%就不再增加
            if (stat.getCompletionRate() != null && stat.getCompletionRate().intValue() >= 100) {
                log.info("[updateCourseStat] 课程 {} 已完成，跳过", courseId);
                return;
            }
            int completed = stat.getCompletedLessons() != null ? stat.getCompletedLessons() : 0;
            int newCompleted = Math.min(completed + 1, totalLessons);
            stat.setCompletedLessons(newCompleted);
            stat.setCompletionRate(
                    new BigDecimal(newCompleted * 100).divide(new BigDecimal(totalLessons), 2, RoundingMode.HALF_UP)
            );
            stat.setLastStudyTime(LocalDateTime.now());
            stat.setUpdateTime(LocalDateTime.now());
            courseStatMapper.updateById(stat);
            log.info("[updateCourseStat] 更新统计记录 studentId={}, courseId={}, completed={}/{}",
                    studentId, courseId, newCompleted, totalLessons);
        }
    }

    // ==================== 原有方法（保持不变） ====================

    @PostMapping("/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> updateVideoProgress(@AuthenticationPrincipal Long studentId,
                                            @RequestBody @Validated VideoProgressDTO progressDTO) {
        studyService.updateVideoProgress(studentId, progressDTO);
        return Result.success();
    }

    @GetMapping("/progress/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<VideoProgressVO> getVideoProgress(@AuthenticationPrincipal Long studentId,
                                                    @PathVariable Long lessonId) {
        VideoProgressVO progress = studyService.getVideoProgress(studentId, lessonId);
        return Result.success(progress);
    }

    @GetMapping("/progress/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<VideoProgressVO>> getCourseProgress(@AuthenticationPrincipal Long studentId,
                                                           @PathVariable Long courseId) {
        List<VideoProgressVO> progressList = studyService.getCourseProgress(studentId, courseId);
        return Result.success(progressList);
    }

    @PostMapping("/note")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> addNote(@AuthenticationPrincipal Long studentId,
                                @RequestBody @Validated StudyNoteDTO noteDTO) {
        studyService.addNote(studentId, noteDTO);
        return Result.success("添加成功");
    }

    @PutMapping("/note/{noteId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> updateNote(@AuthenticationPrincipal Long studentId,
                                   @PathVariable Long noteId,
                                   @RequestBody @Validated StudyNoteDTO noteDTO) {
        noteDTO.setId(noteId);
        studyService.updateNote(studentId, noteDTO);
        return Result.success("更新成功");
    }

    @DeleteMapping("/note/{noteId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> deleteNote(@AuthenticationPrincipal Long studentId,
                                   @PathVariable Long noteId) {
        studyService.deleteNote(studentId, noteId);
        return Result.success("删除成功");
    }

    @GetMapping("/note/{noteId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<StudyNoteVO> getNoteDetail(@PathVariable Long noteId) {
        StudyNoteVO note = studyService.getNoteDetail(noteId);
        return Result.success(note);
    }

    @GetMapping("/note/list")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<PageResult<StudyNoteVO>> getNoteList(
            @AuthenticationPrincipal Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {

        var page = studyService.getNotePage(studentId, courseId, lessonId, keyword, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @PostMapping("/note/{noteId}/favorite")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> toggleNoteFavorite(@AuthenticationPrincipal Long studentId,
                                           @PathVariable Long noteId) {
        studyService.toggleNoteFavorite(studentId, noteId);
        return Result.success();
    }

    @GetMapping("/stat")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<StudyStatVO> getStudentStudyStat(@AuthenticationPrincipal Long studentId) {
        StudyStatVO stat = studyService.getStudentStudyStat(studentId);
        return Result.success(stat);
    }

    @GetMapping("/stat/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<CourseStatVO> getCourseStat(@AuthenticationPrincipal Long studentId,
                                              @PathVariable Long courseId) {
        CourseStatVO stat = studyService.getCourseStat(studentId, courseId);
        return Result.success(stat);
    }

    @GetMapping("/stat/trend")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Map<String, Object>>> getStudyTrend(@AuthenticationPrincipal Long studentId,
                                                           @RequestParam(defaultValue = "7") Integer days) {
        List<Map<String, Object>> trend = studyService.getStudyTrend(studentId, days);
        return Result.success(trend);
    }

    @GetMapping("/stat/knowledge")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Map<String, Object>>> getKnowledgeMastery(@AuthenticationPrincipal Long studentId) {
        List<Map<String, Object>> mastery = studyService.getKnowledgeMastery(studentId);
        return Result.success(mastery);
    }
}