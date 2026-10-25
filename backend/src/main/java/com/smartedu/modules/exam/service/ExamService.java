package com.smartedu.modules.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.exam.dto.*;
import com.smartedu.modules.exam.dto.ExamVO;
import com.smartedu.modules.exam.dto.QuestionVO;
import com.smartedu.modules.exam.dto.ExamRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 考试Service接口
 */
public interface ExamService {

    // ==================== 题目管理 ====================

    /**
     * 添加题目，返回题目ID
     */
    Long addQuestion(Long teacherId, QuestionDTO questionDTO);

    /**
     * 更新题目
     */
    void updateQuestion(Long teacherId, QuestionDTO questionDTO);

    /**
     * 删除题目
     */
    void deleteQuestion(Long teacherId, Long questionId);

    /**
     * 获取题目详情
     */
    QuestionVO getQuestionDetail(Long questionId);

    /**
     * 分页查询题目
     */
    Page<QuestionVO> getQuestionPage(Long teacherId, Long bankId, Integer questionType, Integer difficultyLevel, Long current, Long size);

    // ==================== 试卷题目管理（新增）====================

    /**
     * 获取试卷的题目列表
     */
    List<ExamVO.ExamQuestionVO> getExamQuestions(Long examId);

    /**
     * 添加题目到试卷
     */
    void addQuestionToExam(Long teacherId, AddQuestionToExamDTO dto);

    /**
     * 从试卷中移除题目
     */
    void removeQuestionFromExam(Long teacherId, Long examId, Long questionId);

    /**
     * 更新试卷中题目的分值
     */
    void updateQuestionScore(Long teacherId, UpdateQuestionScoreDTO dto);

    /**
     * 重新排序试卷题目
     */
    void reorderQuestions(Long teacherId, Long examId, List<QuestionOrderDTO> questionOrders);

    /**
     * 保存AI生成的题目
     */
    void saveAiQuestions(Long teacherId, AiSaveQuestionDTO dto);
    /**
     * AI生成题目
     */
    List<Map<String, Object>> generateAIQuestions(Long teacherId, AIGenerateQuestionDTO dto);

    // ==================== 试卷管理 ====================

    /**
     * 创建试卷
     */
    void createExam(Long teacherId, ExamDTO examDTO);

    /**
     * 更新试卷
     */
    void updateExam(Long teacherId, ExamDTO examDTO);

    /**
     * 删除试卷
     */
    void deleteExam(Long teacherId, Long examId);

    /**
     * 获取试卷详情
     */
    ExamVO getExamDetail(Long examId);

    /**
     * 分页查询教师的试卷
     */
    Page<ExamVO> getTeacherExamPage(Long teacherId, String keyword, Long current, Long size);

    /**
     * 发布试卷
     */
    void publishExam(Long teacherId, Long examId);

    /**
     * 取消发布试卷（新增）
     */
    void unpublishExam(Long teacherId, Long examId);

    /**
     * 结束考试
     */
    void endExam(Long teacherId, Long examId);

    // ==================== 管理员审核接口（新增）====================

    Page<ExamVO> getPendingAuditExams(String keyword, Long current, Long size);

    /**
     * 审核考试（管理员通过/拒绝）
     */
    void auditExam(Long examId, boolean approved, String reason);

    /**
     * 管理员删除考试
     */
    void adminDeleteExam(Long examId);

    // ==================== 学生获取可参加考试 ====================

    /**
     * 分页查询学生可参加的考试列表
     */
    Page<ExamVO> getStudentAvailableExams(Long studentId, Long current, Long size);

    // ==================== 考试参与 ====================

    /**
     * 开始考试
     */
    ExamVO startExam(Long studentId, Long examId);

    /**
     * 提交答案
     */
    ExamRecordVO submitExam(Long studentId, Long examId, Map<Long, String> answers);

    /**
     * 获取考试记录
     */
    ExamRecordVO getExamRecord(Long studentId, Long recordId);

    /**
     * 分页查询学生的考试记录
     */
    Page<ExamRecordVO> getStudentExamRecords(Long studentId, Long current, Long size);

    // ==================== 错题本 ====================

    /**
     * 获取学生的错题本
     */
    List<QuestionVO> getWrongQuestions(Long studentId);

    /**
     * 移除错题
     */
    void removeWrongQuestion(Long studentId, Long questionId);
}