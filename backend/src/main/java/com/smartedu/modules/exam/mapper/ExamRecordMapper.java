package com.smartedu.modules.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 考试记录Mapper接口
 */
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    /**
     * 查询学生的考试记录
     */
    @Select("SELECT * FROM study_exam_record WHERE student_id = #{studentId} AND exam_id = #{examId} AND deleted = 0 ORDER BY retry_count DESC LIMIT 1")
    ExamRecord selectLatestByStudentAndExam(@Param("studentId") Long studentId, @Param("examId") Long examId);

    /**
     * 查询学生的重试次数
     */
    @Select("SELECT COUNT(*) FROM study_exam_record WHERE student_id = #{studentId} AND exam_id = #{examId} AND deleted = 0")
    int countRetryByStudentAndExam(@Param("studentId") Long studentId, @Param("examId") Long examId);

    /**
     * 分页查询学生的考试记录
     */
    @Select("SELECT * FROM study_exam_record WHERE student_id = #{studentId} AND (deleted = 0 OR deleted IS NULL) ORDER BY create_time DESC")
    IPage<ExamRecord> selectStudentRecordPage(Page<ExamRecord> page,
                                              @Param("studentId") Long studentId);
    /**
     * 查询试卷的所有考试记录
     */
    List<ExamRecord> selectByExamId(@Param("examId") Long examId);

    @Select("SELECT COALESCE(MAX(retry_count), -1) + 1 FROM study_exam_record WHERE student_id = #{studentId} AND exam_id = #{examId}")
    int getNextRetryCount(@Param("studentId") Long studentId, @Param("examId") Long examId);


}
