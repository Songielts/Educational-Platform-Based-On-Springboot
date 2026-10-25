package com.smartedu.modules.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.exam.entity.Exam;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷Mapper接口
 */
public interface ExamMapper extends BaseMapper<Exam> {

    /**
     * 分页查询教师的试卷
     */
    IPage<Exam> selectTeacherExamPage(Page<Exam> page,
                                      @Param("teacherId") Long teacherId,
                                      @Param("keyword") String keyword);

    /**
     * 查询学生可参加的考试
     */
    IPage<Exam> selectAvailableExamsForStudent(Page<Exam> page,
                                               @Param("studentId") Long studentId);
    /**
     * 分页查询学生可参加的考试
     */
    IPage<Exam> selectStudentExamPage(Page<Exam> page, @Param("keyword") String keyword);

    /**
     * 查询学生的试卷列表
     */
    List<Exam> selectStudentExams(@Param("studentId") Long studentId,
                                  @Param("classId") Long classId);

    /**
     * 查询课程的试卷
     */
    @Select("SELECT * FROM edu_exam WHERE course_id = #{courseId} AND status = 1 AND deleted = 0")
    List<Exam> selectByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据ID查询试卷（绕过逻辑删除过滤，兼容 deleted IS NULL）
     */
    @Select("SELECT * FROM edu_exam WHERE id = #{examId}")
    Exam selectByIdIgnoreDeleted(@Param("examId") Long examId);

    /**
     * 更新试卷的题目数量和总分（绕过逻辑删除过滤）
     */
    @Update("UPDATE edu_exam SET question_count = #{questionCount}, total_score = #{totalScore} WHERE id = #{examId}")
    int updateStatsIgnoreDeleted(@Param("examId") Long examId,
                                 @Param("questionCount") Integer questionCount,
                                 @Param("totalScore") BigDecimal totalScore);

    /**
     * 更新试卷状态（绕过逻辑删除过滤）
     */
    @Update("UPDATE edu_exam SET status = #{status} WHERE id = #{examId}")
    int updateStatusIgnoreDeleted(@Param("examId") Long examId, @Param("status") Integer status);

    /**
     * 修复 deleted IS NULL 的数据为 0
     */
    @Update("UPDATE edu_exam SET deleted = 0 WHERE id = #{examId} AND deleted IS NULL")
    int fixNullDeleted(@Param("examId") Long examId);
}