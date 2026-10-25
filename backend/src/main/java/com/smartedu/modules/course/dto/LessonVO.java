package com.smartedu.modules.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课时视图对象
 */
@Data
public class LessonVO {

    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 课时名称
     */
    private String lessonName;

    /**
     * 视频URL
     */
    private String videoUrl;

    /**
     * 视频时长（秒）
     */
    private Integer videoDuration;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否已完成（学生学习进度）
     */
    private Boolean isCompleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}