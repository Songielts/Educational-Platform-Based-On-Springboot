package com.smartedu.modules.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.exam.entity.ExamAnswer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生答题详情Mapper接口
 */
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {

    /**
     * 查询考试记录的答题详情
     */
    @Select("SELECT * FROM study_exam_answer WHERE record_id = #{recordId} AND deleted = 0")
    List<ExamAnswer> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 查询学生的错题
     */
    @Select("SELECT * FROM study_exam_answer WHERE student_id = #{studentId} AND is_correct = 0 AND deleted = 0")
    List<ExamAnswer> selectWrongAnswers(@Param("studentId") Long studentId);
}
