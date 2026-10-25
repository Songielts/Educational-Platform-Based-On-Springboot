package com.smartedu.modules.study.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.modules.study.dto.*;

import java.util.List;
import java.util.Map;

/**
 * 学习Service接口
 */
public interface StudyService {

    // ==================== 视频学习进度 ====================

    /**
     * 更新视频学习进度
     */
    void updateVideoProgress(Long studentId, VideoProgressDTO progressDTO);

    /**
     * 获取视频学习进度
     */
    VideoProgressVO getVideoProgress(Long studentId, Long lessonId);

    /**
     * 获取课程的学习进度列表
     */
    List<VideoProgressVO> getCourseProgress(Long studentId, Long courseId);

    // ==================== 学习笔记 ====================

    /**
     * 添加笔记
     */
    void addNote(Long studentId, StudyNoteDTO noteDTO);

    /**
     * 更新笔记
     */
    void updateNote(Long studentId, StudyNoteDTO noteDTO);

    /**
     * 删除笔记
     */
    void deleteNote(Long studentId, Long noteId);

    /**
     * 获取笔记详情
     */
    StudyNoteVO getNoteDetail(Long noteId);

    /**
     * 分页查询笔记
     */
    Page<StudyNoteVO> getNotePage(Long studentId, Long courseId, Long lessonId, String keyword, Long current, Long size);

    /**
     * 收藏/取消收藏笔记
     */
    void toggleNoteFavorite(Long studentId, Long noteId);

    // ==================== 学习统计 ====================

    /**
     * 获取学生的学习统计
     */
    StudyStatVO getStudentStudyStat(Long studentId);

    /**
     * 获取课程学习统计
     */
    CourseStatVO getCourseStat(Long studentId, Long courseId);

    /**
     * 获取学习趋势数据
     */
    List<Map<String, Object>> getStudyTrend(Long studentId, Integer days);

    /**
     * 获取知识点掌握情况
     */
    List<Map<String, Object>> getKnowledgeMastery(Long studentId);
}
