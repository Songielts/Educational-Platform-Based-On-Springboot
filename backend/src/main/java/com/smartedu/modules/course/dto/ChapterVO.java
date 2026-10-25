package com.smartedu.modules.course.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 章节视图对象
 */
@Data
public class ChapterVO {

    private Long id;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 章节名称
     */
    private String chapterName;

    /**
     * 父章节ID
     */
    private Long parentId;

    /**
     * 章节类型：1-章 2-节
     */
    private Integer chapterType;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 课时列表
     */
    private List<LessonVO> lessons;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}