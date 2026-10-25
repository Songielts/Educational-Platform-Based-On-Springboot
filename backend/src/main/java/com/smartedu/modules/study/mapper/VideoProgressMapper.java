package com.smartedu.modules.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartedu.modules.study.entity.VideoProgress;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频学习进度 Mapper
 * 新增：selectByStudentAndTimeRange、sumStudyTimeByStudent 方法（学习统计用）
 */
@Repository
public interface VideoProgressMapper extends BaseMapper<VideoProgress> {

    /**
     * 根据学生ID和课时ID查询学习进度
     */
    @Select("SELECT * FROM study_video_progress " +
            "WHERE student_id = #{studentId} AND lesson_id = #{lessonId} " +
            "LIMIT 1")
    VideoProgress selectByStudentAndLesson(@Param("studentId") Long studentId,
                                           @Param("lessonId") Long lessonId);

    /**
     * 查询学生的所有视频学习记录
     */
    List<VideoProgress> selectByStudentAndCourse(@Param("studentId") Long studentId,
                                                 @Param("courseId") Long courseId);

    /**
     * 更新学习进度
     */
    int updateProgress(@Param("studentId") Long studentId,
                       @Param("lessonId") Long lessonId,
                       @Param("position") Integer position,
                       @Param("progress") Integer progress);

    /**
     * 标记课时已完成
     */
    int markCompleted(@Param("studentId") Long studentId,
                      @Param("lessonId") Long lessonId);

    // ==================== 新增：学习统计专用方法 ====================

    /**
     * 直接UPDATE学习进度（绕过MyBatis-Plus逻辑删除拦截器）
     * 解决 deleted=null 导致 updateById 的 AND deleted=0 条件不匹配问题
     */
    @Update("UPDATE study_video_progress " +
            "SET current_position = #{currentPosition}, " +
            "    total_duration = #{totalDuration}, " +
            "    progress_percent = #{progressPercent}, " +
            "    is_completed = #{isCompleted}, " +
            "    study_count = #{studyCount}, " +
            "    total_study_time = #{totalStudyTime}, " +
            "    last_study_time = #{lastStudyTime}, " +
            "    update_time = NOW() " +
            "WHERE id = #{id}")
    int updateProgressDirect(@Param("id") Long id,
                             @Param("currentPosition") Integer currentPosition,
                             @Param("totalDuration") Integer totalDuration,
                             @Param("progressPercent") java.math.BigDecimal progressPercent,
                             @Param("isCompleted") Integer isCompleted,
                             @Param("studyCount") Integer studyCount,
                             @Param("totalStudyTime") Integer totalStudyTime,
                             @Param("lastStudyTime") LocalDateTime lastStudyTime);

    /**
     * 原子UPDATE学习进度（数据库层面原子+1/+30，避免并发覆盖）
     */
    @Update("UPDATE study_video_progress " +
            "SET current_position = #{currentPosition}, " +
            "    total_duration = #{totalDuration}, " +
            "    progress_percent = #{progressPercent}, " +
            "    is_completed = #{isCompleted}, " +
            "    study_count = study_count + 1, " +           // <-- 原子+1
            "    total_study_time = total_study_time + #{addStudyTime}, " +  // <-- 原子+addStudyTime
            "    last_study_time = #{lastStudyTime}, " +
            "    update_time = NOW() " +
            "WHERE id = #{id}")
    int updateProgressAtomic(@Param("id") Long id,
                             @Param("currentPosition") Integer currentPosition,
                             @Param("totalDuration") Integer totalDuration,
                             @Param("progressPercent") java.math.BigDecimal progressPercent,
                             @Param("isCompleted") Integer isCompleted,
                             @Param("addStudyTime") Integer addStudyTime,
                             @Param("lastStudyTime") LocalDateTime lastStudyTime);

    /**
     * 查询学生累计学习时长（分钟）
     */
    @Select("SELECT COALESCE(SUM(total_study_time), 0) " +
            "FROM study_video_progress " +
            "WHERE student_id = #{studentId}")
    Integer sumStudyTimeByStudent(@Param("studentId") Long studentId);

    /**
     * 查询学生在指定时间范围内的学习记录
     */
    @Select("SELECT * FROM study_video_progress " +
            "WHERE student_id = #{studentId} " +
            "  AND last_study_time >= #{startTime} " +
            "ORDER BY last_study_time DESC")
    List<VideoProgress> selectByStudentAndTimeRange(@Param("studentId") Long studentId,
                                                    @Param("startTime") LocalDateTime startTime);
}