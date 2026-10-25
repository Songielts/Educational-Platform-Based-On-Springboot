package com.smartedu.modules.study.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习笔记VO
 */
@Data
public class StudyNoteVO {

    private Long id;

    private Long studentId;

    private Long courseId;

    private String courseName;

    private Long lessonId;

    private String lessonName;

    private String noteTitle;

    private String noteContent;

    private Integer videoPosition;

    private String videoPositionStr;

    private Integer isFavorite;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
