package com.smartedu.modules.study.service;

import com.smartedu.modules.exam.dto.QuestionVO;

import java.util.List;
import java.util.Map;

/**
 * 课程练习服务接口
 */
public interface StudyPracticeService {

    // ==================== 课时题目管理（教师）====================

    /**
     * 给课时添加题目
     */
    void addQuestionToLesson(Long teacherId, Long lessonId, Long questionId, Integer questionOrder);

    /**
     * 从课时移除题目
     */
    void removeQuestionFromLesson(Long teacherId, Long lessonId, Long questionId);

    /**
     * 获取课时的题目列表（教师查看）
     */
    List<QuestionVO> getLessonQuestions(Long lessonId);

    // ==================== 学生练习 ====================

    /**
     * 获取课时的练习题目（学生端，带学生答题记录）
     */
    List<QuestionVO> getPracticeQuestions(Long studentId, Long lessonId);

    /**
     * 提交练习答案
     */
    void submitPracticeAnswers(Long studentId, Long lessonId, Map<Long, String> answers);

    // ==================== 错题本 ====================

    /**
     * 获取学生的所有错题（考试 + 练习）
     */
    List<QuestionVO> getAllWrongQuestions(Long studentId);

    /**
     * 从错题本移除错题
     */
    void removeWrongQuestion(Long studentId, Long questionId);
}