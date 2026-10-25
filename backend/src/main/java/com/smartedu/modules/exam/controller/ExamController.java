package com.smartedu.modules.exam.controller;

import com.smartedu.common.result.PageResult;
import com.smartedu.common.result.Result;
import com.smartedu.modules.exam.dto.*;
import com.smartedu.modules.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping("/record/list")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<PageResult<ExamRecordVO>> getStudentExamRecords(
            @AuthenticationPrincipal Long studentId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        var page = examService.getStudentExamRecords(studentId, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @GetMapping("/question/list")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<PageResult<QuestionVO>> getQuestionList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long bankId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficultyLevel,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        var page = examService.getQuestionPage(userId, bankId, questionType, difficultyLevel, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @PostMapping("/question")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Long> addQuestion(@AuthenticationPrincipal Long teacherId,
                                    @RequestBody @Validated QuestionDTO questionDTO) {
        Long questionId = examService.addQuestion(teacherId, questionDTO);
        return Result.success(questionId);
    }

    @PutMapping("/question/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> updateQuestion(@AuthenticationPrincipal Long teacherId,
                                       @PathVariable Long questionId,
                                       @RequestBody @Validated QuestionDTO questionDTO) {
        questionDTO.setId(questionId);
        examService.updateQuestion(teacherId, questionDTO);
        return Result.success("更新成功");
    }

    @DeleteMapping("/question/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> deleteQuestion(@AuthenticationPrincipal Long teacherId,
                                       @PathVariable Long questionId) {
        examService.deleteQuestion(teacherId, questionId);
        return Result.success("删除成功");
    }

    @GetMapping("/question/{questionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<QuestionVO> getQuestionDetail(@PathVariable Long questionId) {
        QuestionVO question = examService.getQuestionDetail(questionId);
        return Result.success(question);
    }

    @GetMapping("/{examId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public Result<List<ExamVO.ExamQuestionVO>> getExamQuestions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long examId) {
        List<ExamVO.ExamQuestionVO> questions = examService.getExamQuestions(examId);
        return Result.success(questions);
    }

    @PostMapping("/question/add-to-exam")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> addQuestionToExam(@AuthenticationPrincipal Long teacherId,
                                          @RequestBody @Validated AddQuestionToExamDTO dto) {
        examService.addQuestionToExam(teacherId, dto);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{examId}/question/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> removeQuestionFromExam(@AuthenticationPrincipal Long teacherId,
                                               @PathVariable Long examId,
                                               @PathVariable Long questionId) {
        examService.removeQuestionFromExam(teacherId, examId, questionId);
        return Result.success("移除成功");
    }

    @PutMapping("/question/score")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> updateQuestionScore(@AuthenticationPrincipal Long teacherId,
                                            @RequestBody @Validated UpdateQuestionScoreDTO dto) {
        examService.updateQuestionScore(teacherId, dto);
        return Result.success("更新成功");
    }

    @PutMapping("/{examId}/question-order")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> reorderQuestions(@AuthenticationPrincipal Long teacherId,
                                         @PathVariable Long examId,
                                         @RequestBody List<QuestionOrderDTO> questionOrders) {
        examService.reorderQuestions(teacherId, examId, questionOrders);
        return Result.success("排序已保存");
    }

    @PostMapping("/question/ai-save")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> saveAiQuestions(@AuthenticationPrincipal Long teacherId,
                                        @RequestBody @Validated AiSaveQuestionDTO dto) {
        examService.saveAiQuestions(teacherId, dto);
        return Result.success("保存成功");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> createExam(@AuthenticationPrincipal Long teacherId,
                                   @RequestBody @Validated ExamDTO examDTO) {
        examService.createExam(teacherId, examDTO);
        return Result.success("创建成功");
    }

    @PutMapping("/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> updateExam(@AuthenticationPrincipal Long teacherId,
                                   @PathVariable Long examId,
                                   @RequestBody @Validated ExamDTO examDTO) {
        examDTO.setId(examId);
        examService.updateExam(teacherId, examDTO);
        return Result.success("更新成功");
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> deleteExam(@AuthenticationPrincipal Long teacherId,
                                   @PathVariable Long examId) {
        examService.deleteExam(teacherId, examId);
        return Result.success("删除成功");
    }

    @GetMapping("/{examId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<ExamVO> getExamDetail(@PathVariable Long examId) {
        ExamVO exam = examService.getExamDetail(examId);
        return Result.success(exam);
    }

    @GetMapping("/teacher/list")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<PageResult<ExamVO>> getTeacherExamList(@AuthenticationPrincipal Long teacherId,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") Long current,
                                                         @RequestParam(defaultValue = "10") Long size) {
        var page = examService.getTeacherExamPage(teacherId, keyword, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @PostMapping("/{examId}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> publishExam(@AuthenticationPrincipal Long teacherId,
                                    @PathVariable Long examId) {
        examService.publishExam(teacherId, examId);
        return Result.success("已提交审核，等待管理员审批");
    }

    @PostMapping("/{examId}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> unpublishExam(@AuthenticationPrincipal Long teacherId,
                                      @PathVariable Long examId) {
        examService.unpublishExam(teacherId, examId);
        return Result.success("已取消发布");
    }

    @PostMapping("/{examId}/end")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> endExam(@AuthenticationPrincipal Long teacherId,
                                @PathVariable Long examId) {
        examService.endExam(teacherId, examId);
        return Result.success("考试已结束");
    }

    @GetMapping("/student/list")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<PageResult<ExamVO>> getStudentAvailableExams(
            @AuthenticationPrincipal Long studentId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        var page = examService.getStudentAvailableExams(studentId, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    @PostMapping("/{examId}/start")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<ExamVO> startExam(@AuthenticationPrincipal Long studentId,
                                    @PathVariable Long examId) {
        ExamVO exam = examService.startExam(studentId, examId);
        return Result.success(exam);
    }

    @PostMapping("/{examId}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<ExamRecordVO> submitExam(@AuthenticationPrincipal Long studentId,
                                           @PathVariable Long examId,
                                           @RequestBody Map<Long, String> answers) {
        ExamRecordVO record = examService.submitExam(studentId, examId, answers);
        return Result.success(record);
    }

    @GetMapping("/record/{recordId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<ExamRecordVO> getExamRecord(@AuthenticationPrincipal Long studentId,
                                              @PathVariable Long recordId) {
        ExamRecordVO record = examService.getExamRecord(studentId, recordId);
        return Result.success(record);
    }

    @GetMapping("/wrong-questions")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<List<QuestionVO>> getWrongQuestions(@AuthenticationPrincipal Long studentId) {
        List<QuestionVO> questions = examService.getWrongQuestions(studentId);
        return Result.success(questions);
    }

    @DeleteMapping("/wrong-questions/{questionId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public Result<Void> removeWrongQuestion(@AuthenticationPrincipal Long studentId,
                                            @PathVariable Long questionId) {
        examService.removeWrongQuestion(studentId, questionId);
        return Result.success("移除成功");
    }

    // ==================== 管理员考试审核接口 ====================

    /**
     * 查询待审核考试列表（管理员）
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<ExamVO>> getPendingExams(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        var page = examService.getPendingAuditExams(keyword, current, size);
        return Result.success(PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords()));
    }

    /**
     * 审核考试（管理员通过/拒绝）
     */
    @PostMapping("/admin/audit/{examId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> auditExam(@PathVariable Long examId,
                                  @RequestBody Map<String, Object> params) {
        boolean approved = Boolean.TRUE.equals(params.get("approved"));
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;
        examService.auditExam(examId, approved, reason);
        return Result.success(approved ? "审核通过" : "已拒绝");
    }

    /**
     * 管理员删除考试（任意状态）
     */
    @DeleteMapping("/admin/{examId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> adminDeleteExam(@PathVariable Long examId) {
        examService.adminDeleteExam(examId);
        return Result.success("删除成功");
    }
}