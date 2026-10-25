package com.smartedu.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.study.entity.PracticeAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 练习答题记录 Mapper
 */
@Mapper
public interface PracticeAnswerMapper extends BaseMapper<PracticeAnswer> {

    /**
     * 查询学生的练习答题记录（按课时）
     */
    @Select("SELECT * FROM study_practice_answer WHERE student_id = #{studentId} AND lesson_id = #{lessonId} AND (deleted = 0 OR deleted IS NULL)")
    List<PracticeAnswer> selectByStudentAndLesson(@Param("studentId") Long studentId, @Param("lessonId") Long lessonId);

    /**
     * 查询学生某道题的答题记录
     */
    @Select("SELECT * FROM study_practice_answer WHERE student_id = #{studentId} AND lesson_id = #{lessonId} AND question_id = #{questionId} AND (deleted = 0 OR deleted IS NULL) LIMIT 1")
    PracticeAnswer selectByStudentLessonQuestion(@Param("studentId") Long studentId, @Param("lessonId") Long lessonId, @Param("questionId") Long questionId);

    /**
     * 查询学生的所有错题（练习部分）
     */
    @Select("SELECT * FROM study_practice_answer WHERE student_id = #{studentId} AND is_correct = 0 AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC")
    List<PracticeAnswer> selectWrongAnswers(@Param("studentId") Long studentId);

    /**
     * 检查学生是否已经答过某题
     */
    @Select("SELECT COUNT(*) FROM study_practice_answer WHERE student_id = #{studentId} AND question_id = #{questionId} AND lesson_id = #{lessonId} AND deleted = 0")
    int countByStudentAndQuestion(@Param("studentId") Long studentId, @Param("lessonId") Long lessonId, @Param("questionId") Long questionId);
}