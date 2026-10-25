package com.smartedu.modules.course.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程视图对象
 */
@Data
public class CourseVO {

    private Long id;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程简介
     */
    private String description;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 课程分类
     */
    private Long subjectId;

    /**
     * 分类名称
     */
    private String subjectName;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师名称
     */
    private String teacherName;

    /**
     * 课程状态：0-草稿 1-待审核 2-已发布 3-已下架
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 教育阶段：1-小学 2-初中 3-高中 4-大学
     */
    private Integer educationStage;

    /**
     * 教育阶段名称
     */
    private String educationStageName;

    /**
     * 难度等级：1-简单 2-中等 3-困难
     */
    private Integer difficultyLevel;

    /**
     * 难度等级名称
     */
    private String difficultyLevelName;

    /**
     * 课时数量
     */
    private Integer lessonCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ==================== 课程详情专用字段 ====================

    /**
     * 章节列表（课程详情使用）
     */
    private List<ChapterVO> chapters;

    /**
     * 课时列表（课程详情使用）
     */
    private List<LessonVO> lessons;

    /**
     * 是否已完成（学生学习进度）
     */
    private Boolean isCompleted;

    /**
     * 章节名称（课时详情使用）
     */
    private String chapterName;

    /**
     * 课时名称（课时详情使用）
     */
    private String lessonName;
}