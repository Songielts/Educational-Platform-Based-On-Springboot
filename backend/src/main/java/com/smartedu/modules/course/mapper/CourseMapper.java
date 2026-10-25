package com.smartedu.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.course.dto.CourseVO;
import com.smartedu.modules.course.dto.StudentCourseVO;
import com.smartedu.modules.course.entity.Course;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {

    @Select("SELECT * FROM edu_course WHERE course_code = #{courseCode} AND (deleted = 0 OR deleted IS NULL)")
    Course selectByCourseCode(@Param("courseCode") String courseCode);

    IPage<Course> selectPublicCoursePage(Page<Course> page,
                                         @Param("keyword") String keyword,
                                         @Param("subjectId") Long subjectId,
                                         @Param("educationStage") Integer educationStage);

    IPage<Course> selectTeacherCoursePage(Page<Course> page,
                                          @Param("teacherId") Long teacherId,
                                          @Param("keyword") String keyword);

    @Select("SELECT c.* FROM edu_course c " +
            "INNER JOIN edu_class_course cc ON c.id = cc.course_id " +
            "WHERE cc.class_id = #{classId} AND (c.deleted = 0 OR c.deleted IS NULL)")
    List<Course> selectClassCourses(@Param("classId") Long classId);

    @Select("UPDATE edu_course SET view_count = view_count + 1 WHERE id = #{courseId}")
    int increaseViewCount(@Param("courseId") Long courseId);

    @Select("UPDATE edu_course SET like_count = like_count + 1 WHERE id = #{courseId}")
    int increaseLikeCount(@Param("courseId") Long courseId);

    @Select("UPDATE edu_course SET favorite_count = favorite_count + 1 WHERE id = #{courseId}")
    int increaseFavoriteCount(@Param("courseId") Long courseId);

    List<CourseVO> getAllPublishedCourses();

    /**
     * 查询学生的课程列表（关联 study_course_stat 表获取真实进度）
     * 修复：取消 totalLessons/completedLessons/completionRate 的硬编码0
     */
    @Select("SELECT " +
            "  c.id AS courseId, " +
            "  c.course_name AS courseName, " +
            "  c.cover_image AS coverImage, " +
            "  u.real_name AS teacherName, " +
            "  IFNULL(scs.total_lessons, " +
            "    (SELECT COUNT(*) FROM edu_lesson l " +
            "     JOIN edu_chapter ch ON l.chapter_id = ch.id " +
            "     WHERE ch.course_id = c.id AND (l.deleted = 0 OR l.deleted IS NULL))" +
            "  ) AS totalLessons, " +
            "  IFNULL(scs.completed_lessons, 0) AS completedLessons, " +
            "  IFNULL(scs.completion_rate, 0) AS completionRate, " +
            "  esc.create_time AS lastStudyTime " +
            "FROM edu_course c " +
            "INNER JOIN edu_student_course esc ON c.id = esc.course_id " +
            "LEFT JOIN sys_user u ON c.teacher_id = u.id " +
            "LEFT JOIN study_course_stat scs ON scs.student_id = esc.student_id AND scs.course_id = c.id " +
            "WHERE esc.student_id = #{studentId} " +
            "ORDER BY esc.create_time DESC")
    List<StudentCourseVO> selectStudentCourses(@Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM edu_student_course WHERE student_id = #{studentId} AND course_id = #{courseId}")
    int countStudentCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Insert("INSERT INTO edu_student_course (student_id, course_id, create_time) VALUES (#{studentId}, #{courseId}, NOW())")
    int insertStudentCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}