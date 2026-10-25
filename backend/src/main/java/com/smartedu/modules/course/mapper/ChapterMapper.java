package com.smartedu.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.course.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 章节Mapper接口
 */
@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {

    /**
     * 查询课程的章节列表
     */
    @Select("SELECT * FROM edu_chapter WHERE course_id = #{courseId} AND deleted = 0 ORDER BY sort_order ASC")
    List<Chapter> selectByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询章节的子章节
     */
    @Select("SELECT * FROM edu_chapter WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order ASC")
    List<Chapter> selectByParentId(@Param("parentId") Long parentId);


}
