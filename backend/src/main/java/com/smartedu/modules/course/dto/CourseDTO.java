package com.smartedu.modules.course.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 课程DTO
 */
@Data
public class CourseDTO {

    private Long id;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String courseCode;

    private Long subjectId;

    private String description;

    private String coverImage;

    private Integer educationStage;

    private Integer difficultyLevel;

    private Integer isPublic;

    private Integer lessonCount;
}
