package com.smartedu.modules.classroom.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级学生VO
 */
@Data
public class ClassStudentVO {

    private Long studentId;

    private String username;

    private String realName;

    private String avatar;

    private LocalDateTime joinTime;
}