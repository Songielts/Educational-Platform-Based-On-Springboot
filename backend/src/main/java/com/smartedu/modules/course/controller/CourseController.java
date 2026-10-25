package com.smartedu.modules.course.controller;

import com.smartedu.common.result.PageResult;
import com.smartedu.common.result.Result;
import com.smartedu.modules.course.dto.*;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程控制器
 */
@Slf4j
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // ==================== 通用：从 SecurityContext 提取当前用户ID ====================
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("[getCurrentUserId] authentication is null or not authenticated");
            return null;
        }
        Object principal = authentication.getPrincipal();
        log.info("[getCurrentUserId] principal class={}, value={}",
                principal != null ? principal.getClass().getName() : "null", principal);

        if (principal == null) return null;

        // JwtAuthenticationFilter 中 principal 是 Long 类型的 userId
        if (principal instanceof Long) return (Long) principal;

        // 兼容 Integer（jwtUtils.getUserId 可能返回 int/Integer）
        if (principal instanceof Integer) return ((Integer) principal).longValue();

        // 兼容 String
        if (principal instanceof String) {
            try {
                return Long.valueOf((String) principal);
            } catch (NumberFormatException e) {
                log.warn("[getCurrentUserId] 无法将 String 转为 Long: {}", principal);
                return null;
            }
        }

        log.warn("[getCurrentUserId] 不支持的 principal 类型: {}", principal.getClass().getName());
        return null;
    }

    /**
     * 创建课程（教师/管理员）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> createCourse(@RequestBody @Validated CourseDTO courseDTO) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录或无法获取用户ID");
        }
        courseService.createCourse(teacherId, courseDTO);
        return Result.success("创建成功，等待管理员审核");
    }

    /**
     * 更新课程（教师/管理员）
     */
    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> updateCourse(@PathVariable Long courseId,
                                     @RequestBody @Validated CourseDTO courseDTO) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录或无法获取用户ID");
        }
        courseDTO.setId(courseId);
        courseService.updateCourse(teacherId, courseDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除课程（教师/管理员）
     */
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> deleteCourse(@PathVariable Long courseId) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录或无法获取用户ID");
        }
        courseService.deleteCourse(teacherId, courseId);
        return Result.success("删除成功");
    }

    /**
     * 获取课程详情
     */
    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<CourseVO> getCourseDetail(@PathVariable Long courseId) {
        CourseVO courseVO = courseService.getCourseDetail(courseId);
        return Result.success(courseVO);
    }

    /**
     * 分页查询公开课程
     */
    @GetMapping("/public/list")
    public Result<PageResult<CourseVO>> getPublicCourseList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer educationStage,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        var page = courseService.getPublicCoursePage(keyword, subjectId, educationStage, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    /**
     * 分页查询教师的课程
     */
    @GetMapping("/teacher/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<PageResult<CourseVO>> getTeacherCourseList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        var page = courseService.getTeacherCoursePage(teacherId, keyword, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    /**
     * 发布课程（教师提交审核）
     */
    @PostMapping("/{courseId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> publishCourse(@PathVariable Long courseId) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        courseService.publishCourse(teacherId, courseId);
        return Result.success("已提交审核，请等待管理员审批");
    }

    /**
     * 下架课程（教师）
     */
    @PostMapping("/{courseId}/offline")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> offlineCourse(@PathVariable Long courseId) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        courseService.offlineCourse(teacherId, courseId);
        return Result.success("下架成功");
    }

    // ==================== 管理员审核接口 ====================

    /**
     * 查询待审核课程列表（管理员）
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<CourseVO>> getPendingCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        var page = courseService.getPendingAuditCourses(keyword, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    /**
     * 审核课程（管理员通过/拒绝）
     */
    @PostMapping("/admin/audit/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> auditCourse(@PathVariable Long courseId,
                                    @RequestBody Map<String, Object> params) {
        boolean approved = Boolean.TRUE.equals(params.get("approved"));
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;
        courseService.auditCourse(courseId, approved, reason);
        return Result.success(approved ? "审核通过" : "已拒绝");
    }

    // ==================== 章节管理 ====================

    /**
     * 添加章节（教师/管理员）
     */
    @PostMapping("/chapter")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> addChapter(@RequestBody @Validated ChapterDTO chapterDTO) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        courseService.addChapter(teacherId, chapterDTO);
        return Result.success("添加成功");
    }

    /**
     * 更新章节（教师/管理员）
     */
    @PutMapping("/chapter/{chapterId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> updateChapter(@PathVariable Long chapterId,
                                      @RequestBody @Validated ChapterDTO chapterDTO) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        chapterDTO.setId(chapterId);
        courseService.updateChapter(teacherId, chapterDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除章节（教师/管理员）
     */
    @DeleteMapping("/chapter/{chapterId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> deleteChapter(@PathVariable Long chapterId) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        courseService.deleteChapter(teacherId, chapterId);
        return Result.success("删除成功");
    }

    /**
     * 获取课程的章节树
     */
    @GetMapping("/{courseId}/chapters")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<List<ChapterVO>> getCourseChapters(@PathVariable Long courseId) {
        List<ChapterVO> chapters = courseService.getCourseChapters(courseId);
        return Result.success(chapters);
    }

    // ==================== 课时管理 ====================

    /**
     * 添加课时（教师/管理员）
     */
    @PostMapping("/lesson")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> addLesson(@RequestBody @Validated LessonDTO lessonDTO) {
        Long teacherId = getCurrentUserId();
        log.info("[addLesson] 提取的teacherId={}, courseId={}, chapterId={}",
                teacherId, lessonDTO.getCourseId(), lessonDTO.getChapterId());
        if (teacherId == null) {
            return Result.error(401, "未登录或无法获取用户ID");
        }
        courseService.addLesson(teacherId, lessonDTO);
        return Result.success("添加成功");
    }

    /**
     * 更新课时（教师/管理员）
     */
    @PutMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> updateLesson(@PathVariable Long lessonId,
                                     @RequestBody @Validated LessonDTO lessonDTO) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        lessonDTO.setId(lessonId);
        courseService.updateLesson(teacherId, lessonDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除课时（教师/管理员）
     */
    @DeleteMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> deleteLesson(@PathVariable Long lessonId) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        courseService.deleteLesson(teacherId, lessonId);
        return Result.success("删除成功");
    }

    /**
     * 获取课时详情
     */
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<LessonVO> getLessonDetail(@PathVariable Long lessonId) {
        LessonVO lessonVO = courseService.getLessonDetail(lessonId);
        return Result.success(lessonVO);
    }

    /**
     * 查询学生学习的课程列表
     */
    @GetMapping("/student/list")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<StudentCourseVO>> getStudentCourses() {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        List<StudentCourseVO> courses = courseService.getStudentCourses(studentId);
        return Result.success(courses);
    }

    /**
     * 学生选课
     */
    @PostMapping("/student/select/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> studentSelectCourse(@PathVariable Long courseId) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        Course course = courseService.getById(courseId);
        if (course == null || (course.getDeleted() != null && course.getDeleted() != 0)) {
            return Result.error("课程不存在");
        }
        if (course.getStatus() != 2) {
            return Result.error("该课程暂未开放选课");
        }
        boolean alreadySelected = courseService.checkStudentCourse(studentId, courseId);
        if (alreadySelected) {
            return Result.error("您已经选过这门课程了");
        }
        courseService.selectCourse(studentId, courseId);
        return Result.success("选课成功");
    }

    /**
     * 查询所有已发布课程
     */
    @GetMapping("/all/published")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<CourseVO>> getAllPublishedCourses() {
        List<CourseVO> courses = courseService.getAllPublishedCourses();
        return Result.success(courses);
    }
}