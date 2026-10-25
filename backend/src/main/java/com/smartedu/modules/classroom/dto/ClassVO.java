package com.smartedu.modules.classroom.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级VO
 */
@Data
public class ClassVO {

    private Long id;

    private String classCode;

    private String className;

    private Long teacherId;

    private String teacherName;

    private String description;

    private String coverImage;

    private Integer maxStudents;

    private Integer currentStudents;

    private Integer status;

    private String joinCode;

    private LocalDateTime joinCodeExpire;

    private LocalDateTime createTime;
}
