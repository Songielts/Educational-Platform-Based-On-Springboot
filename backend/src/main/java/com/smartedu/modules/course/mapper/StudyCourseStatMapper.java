package com.smartedu.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartedu.modules.course.entity.StudyCourseStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学生学习课程统计Mapper接口
 */
@Mapper
public interface StudyCourseStatMapper extends BaseMapper<StudyCourseStat> {

    /**
     * 根据学生ID和课程ID查询学习记录
     */
    default StudyCourseStat selectByStudentAndCourse(Long studentId, Long courseId) {
        return selectOne(
                new LambdaQueryWrapper<StudyCourseStat>()
                        .eq(StudyCourseStat::getStudentId, studentId)
                        .eq(StudyCourseStat::getCourseId, courseId)
                        .eq(StudyCourseStat::getDeleted, 0)
        );
    }

    /**
     * 检查学生是否已选课程
     */
    @Select("SELECT COUNT(*) FROM study_course_stat WHERE student_id = #{studentId} AND course_id = #{courseId} AND deleted = 0")
    int checkStudentCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}