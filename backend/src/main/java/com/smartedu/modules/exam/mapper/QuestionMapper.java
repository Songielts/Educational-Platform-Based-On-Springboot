package com.smartedu.modules.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.exam.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目 Mapper
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 根据创建者查询题目列表
     */
    @Select("SELECT * FROM exam_question WHERE creator_id = #{creatorId} AND deleted = 0 ORDER BY create_time DESC")
    List<Question> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 查询题目的选项（JSON字符串）
     */
    @Select("SELECT question_options FROM exam_question WHERE id = #{id} AND (deleted = 0 OR deleted IS NULL)")
    String selectQuestionOptionsById(@Param("id") Long id);
}