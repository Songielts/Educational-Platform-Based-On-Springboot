package com.smartedu.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.study.entity.LessonQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课时题目关联 Mapper
 */
@Mapper
public interface LessonQuestionMapper extends BaseMapper<LessonQuestion> {

    /**
     * 查询课时下的所有题目关联记录
     */
    @Select("SELECT * FROM study_lesson_question WHERE lesson_id = #{lessonId} AND (deleted = 0 OR deleted IS NULL) ORDER BY question_order")
    List<LessonQuestion> selectByLessonId(@Param("lessonId") Long lessonId);

    /**
     * 查询课时下的题目ID列表
     */
    @Select("SELECT question_id FROM study_lesson_question WHERE lesson_id = #{lessonId} AND deleted = 0 ORDER BY question_order")
    List<Long> selectQuestionIdsByLessonId(@Param("lessonId") Long lessonId);

    /**
     * 根据课时ID和题目ID查询关联记录
     */
    @Select("SELECT * FROM study_lesson_question WHERE lesson_id = #{lessonId} AND question_id = #{questionId} AND (deleted = 0 OR deleted IS NULL) LIMIT 1")
    LessonQuestion selectByLessonAndQuestion(@Param("lessonId") Long lessonId, @Param("questionId") Long questionId);

    /**
     * 检查题目是否已关联到课时
     */
    @Select("SELECT COUNT(*) FROM study_lesson_question WHERE lesson_id = #{lessonId} AND question_id = #{questionId} AND deleted = 0")
    int countByLessonAndQuestion(@Param("lessonId") Long lessonId, @Param("questionId") Long questionId);
}