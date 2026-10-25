package com.smartedu.modules.study.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.exam.dto.QuestionVO;
import com.smartedu.modules.study.service.StudyPracticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课程练习控制器
 * 处理课时练习题目的配置、学生练习答题、错题本等
 */
@Slf4j
@RestController
@RequestMapping("/study/practice")
@RequiredArgsConstructor
public class StudyPracticeController {

    private final StudyPracticeService studyPracticeService;

    // ==================== 通用：从 SecurityContext 提取当前用户ID ====================
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        if (principal instanceof String) {
            try {
                return Long.valueOf((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (principal instanceof Integer) return ((Integer) principal).longValue();
        return null;
    }

    // ==================== 课时题目管理（教师）====================

    /**
     * 获取课时的题目列表（教师查看）
     */
    @GetMapping("/lesson/{lessonId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public Result<List<QuestionVO>> getLessonQuestions(@PathVariable Long lessonId) {
        List<QuestionVO> questions = studyPracticeService.getLessonQuestions(lessonId);
        return Result.success(questions);
    }

    /**
     * 给课时添加题目（教师）
     */
    @PostMapping("/lesson/{lessonId}/question/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> addQuestionToLesson(
            @PathVariable Long lessonId,
            @PathVariable Long questionId,
            @RequestParam(required = false) Integer questionOrder) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        studyPracticeService.addQuestionToLesson(teacherId, lessonId, questionId, questionOrder);
        return Result.success("添加成功");
    }

    /**
     * 从课时移除题目（教师）
     */
    @DeleteMapping("/lesson/{lessonId}/question/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> removeQuestionFromLesson(
            @PathVariable Long lessonId,
            @PathVariable Long questionId) {
        Long teacherId = getCurrentUserId();
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        studyPracticeService.removeQuestionFromLesson(teacherId, lessonId, questionId);
        return Result.success("移除成功");
    }

    // ==================== 学生练习 ====================

    /**
     * 获取课时的练习题目（学生端）
     */
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<List<QuestionVO>> getPracticeQuestions(@PathVariable Long lessonId) {
        Long studentId = getCurrentUserId();
        List<QuestionVO> questions = studyPracticeService.getPracticeQuestions(studentId, lessonId);
        return Result.success(questions);
    }

    /**
     * 提交练习答案
     */
    @PostMapping("/lesson/{lessonId}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<Void> submitPracticeAnswers(
            @PathVariable Long lessonId,
            @RequestBody Map<Long, String> answers) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        studyPracticeService.submitPracticeAnswers(studentId, lessonId, answers);
        return Result.success("提交成功");
    }

    // ==================== 错题本 ====================

    /**
     * 获取学生的所有错题（考试 + 练习）
     */
    @GetMapping("/wrong-questions")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<List<QuestionVO>> getWrongQuestions() {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        List<QuestionVO> questions = studyPracticeService.getAllWrongQuestions(studentId);
        return Result.success(questions);
    }

    /**
     * 从错题本移除错题
     */
    @DeleteMapping("/wrong-question/{questionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<Void> removeWrongQuestion(
            @PathVariable Long questionId) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        studyPracticeService.removeWrongQuestion(studentId, questionId);
        return Result.success("移除成功");
    }
}