package com.smartedu.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.course.entity.Lesson;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课时Mapper接口
 */
public interface LessonMapper extends BaseMapper<Lesson> {

    /**
     * 查询章节的课时列表
     */
    @Select("SELECT * FROM edu_lesson WHERE chapter_id = #{chapterId} AND deleted = 0 ORDER BY sort_order ASC")
    List<Lesson> selectByChapterId(@Param("chapterId") Long chapterId);

    /**
     * 查询课程的课时列表
     */
    @Select("SELECT * FROM edu_lesson WHERE course_id = #{courseId} AND deleted = 0 ORDER BY sort_order ASC")
    List<Lesson> selectByCourseId(@Param("courseId") Long courseId);

    /**
     * 统计课程的课时数量
     */
    @Select("SELECT COUNT(*) FROM edu_lesson WHERE course_id = #{courseId} AND deleted = 0")
    int countByCourseId(@Param("courseId") Long courseId);
}
