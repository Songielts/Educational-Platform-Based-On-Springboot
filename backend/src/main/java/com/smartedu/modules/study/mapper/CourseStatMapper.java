package com.smartedu.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.study.entity.CourseStat;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程学习统计 Mapper
 * 新增：selectByStudent 方法（学习统计看板用）
 */
@Repository
public interface CourseStatMapper extends BaseMapper<CourseStat> {

    /**
     * 根据学生ID和课程ID查询学习统计
     */
    CourseStat selectByStudentAndCourse(@Param("studentId") Long studentId,
                                        @Param("courseId") Long courseId);

    /**
     * 查询学生的所有课程学习统计
     * 新增：@Select 注解，避免 XML 配置缺失导致 404
     */
    @Select("SELECT * FROM study_course_stat " +
            "WHERE student_id = #{studentId} " +
            "ORDER BY completion_rate DESC")
    List<CourseStat> selectByStudent(@Param("studentId") Long studentId);

    /**
     * 根据课程ID查询统计
     */
    List<CourseStat> selectByCourse(@Param("courseId") Long courseId);

    /**
     * 累计学习时长
     */
    @Select("SELECT COALESCE(SUM(total_study_time), 0) " +
            "FROM study_course_stat " +
            "WHERE student_id = #{studentId}")
    Integer sumStudyTimeByStudent(@Param("studentId") Long studentId);

    /**
     * 已完成课程数
     */
    @Select("SELECT COUNT(*) " +
            "FROM study_course_stat " +
            "WHERE student_id = #{studentId} " +
            "  AND completion_rate >= 100")
    Integer countCompletedCourses(@Param("studentId") Long studentId);
}