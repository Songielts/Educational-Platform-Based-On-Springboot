package com.smartedu.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.study.entity.StudyNote;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学习笔记Mapper接口
 */
public interface StudyNoteMapper extends BaseMapper<StudyNote> {

    /**
     * 分页查询学生的笔记
     */
    IPage<StudyNote> selectStudentNotePage(Page<StudyNote> page,
                                            @Param("studentId") Long studentId,
                                            @Param("courseId") Long courseId,
                                            @Param("lessonId") Long lessonId,
                                            @Param("keyword") String keyword);

    /**
     * 查询课时的笔记
     */
    @Select("SELECT * FROM study_note WHERE lesson_id = #{lessonId} AND student_id = #{studentId} AND deleted = 0 ORDER BY create_time DESC")
    List<StudyNote> selectByLesson(@Param("lessonId") Long lessonId, @Param("studentId") Long studentId);

    /**
     * 查询收藏的笔记
     */
    @Select("SELECT * FROM study_note WHERE student_id = #{studentId} AND is_favorite = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<StudyNote> selectFavoriteNotes(@Param("studentId") Long studentId);
}
