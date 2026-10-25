package com.smartedu.modules.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.exam.entity.ExamQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷题目关联Mapper接口
 */
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    @Select("SELECT * FROM edu_exam_question WHERE exam_id = #{examId} AND (deleted = 0 OR deleted IS NULL) ORDER BY question_order ASC, id ASC")
    List<ExamQuestion> selectByExamId(@Param("examId") Long examId);

    @Select("SELECT MAX(question_order) FROM edu_exam_question WHERE exam_id = #{examId} AND (deleted = 0 OR deleted IS NULL)")
    Integer selectMaxOrderByExamId(@Param("examId") Long examId);

    @Select("SELECT * FROM edu_exam_question WHERE exam_id = #{examId} AND question_id = #{questionId} AND (deleted = 0 OR deleted IS NULL) LIMIT 1")
    ExamQuestion selectByExamAndQuestion(@Param("examId") Long examId, @Param("questionId") Long questionId);

    // 修复：@Select → @Delete
    @Delete("DELETE FROM edu_exam_question WHERE exam_id = #{examId}")
    void deleteByExamId(@Param("examId") Long examId);

    // 逻辑删除（从试卷中移除单个题目）
    @Update("UPDATE edu_exam_question SET deleted = 1 WHERE exam_id = #{examId} AND question_id = #{questionId}")
    void deleteByExamAndQuestion(@Param("examId") Long examId, @Param("questionId") Long questionId);

    @Update("UPDATE edu_exam_question SET score = #{score} WHERE exam_id = #{examId} AND question_id = #{questionId} AND (deleted = 0 OR deleted IS NULL)")
    void updateScoreByExamAndQuestion(@Param("examId") Long examId, @Param("questionId") Long questionId, @Param("score") BigDecimal score);

    @Update("UPDATE edu_exam_question SET question_order = #{questionOrder}, score = #{score} WHERE exam_id = #{examId} AND question_id = #{questionId} AND (deleted = 0 OR deleted IS NULL)")
    void updateOrderAndScore(@Param("examId") Long examId, @Param("questionId") Long questionId,
                             @Param("questionOrder") Integer questionOrder, @Param("score") BigDecimal score);
}