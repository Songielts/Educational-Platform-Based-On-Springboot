package com.smartedu.modules.exam.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.exam.dto.AIGenerateQuestionDTO;
import com.smartedu.modules.exam.dto.AiSaveQuestionDTO;
import com.smartedu.modules.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI出题控制器
 *
 * 修复说明：
 * - 新增 /question/generate 和 /question/save 路径，与前端 ai.ts 对齐
 * - 保留原 /generate-questions 兼容旧调用
 * - save 接口使用项目中已有的 AiSaveQuestionDTO
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIQuestionController {

    private final ExamService examService;

    /**
     * AI生成题目（新路径，与前端 ai.ts 对齐）
     * 前端调用：POST /api/ai/question/generate
     */
    @PostMapping("/question/generate")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<Map<String, Object>>> generateQuestions(
            @AuthenticationPrincipal Long teacherId,
            @RequestBody @Validated AIGenerateQuestionDTO dto) {
        List<Map<String, Object>> questions = examService.generateAIQuestions(teacherId, dto);
        return Result.success(questions);
    }

    /**
     * AI生成题目（旧路径，兼容旧前端代码）
     * 前端调用：POST /api/ai/generate-questions
     */
    @PostMapping("/generate-questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<List<Map<String, Object>>> generateQuestionsLegacy(
            @AuthenticationPrincipal Long teacherId,
            @RequestBody @Validated AIGenerateQuestionDTO dto) {
        List<Map<String, Object>> questions = examService.generateAIQuestions(teacherId, dto);
        return Result.success(questions);
    }

    /**
     * 保存AI生成的题目到题库
     * 前端调用：POST /api/ai/question/save
     * 使用项目中已有的 AiSaveQuestionDTO
     */
    @PostMapping("/question/save")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result<Void> saveAIQuestions(
            @AuthenticationPrincipal Long teacherId,
            @RequestBody @Validated AiSaveQuestionDTO dto) {
        examService.saveAiQuestions(teacherId, dto);
        return Result.success("保存成功");
    }
}