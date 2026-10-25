package com.smartedu.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.course.entity.StudentCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentCourseMapper extends BaseMapper<StudentCourse> {

    @Select("SELECT COUNT(*) FROM edu_student_course WHERE student_id = #{studentId} AND course_id = #{courseId} AND deleted = 0")
    int countByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}