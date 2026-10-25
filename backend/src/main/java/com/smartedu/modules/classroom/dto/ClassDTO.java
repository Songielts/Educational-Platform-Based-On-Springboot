package com.smartedu.modules.classroom.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 班级DTO
 */
@Data
public class ClassDTO {

    private Long id;

    @NotBlank(message = "班级编码不能为空")
    private String classCode;

    @NotBlank(message = "班级名称不能为空")
    private String className;

    private String description;

    private String coverImage;

    @NotNull(message = "最大学生数不能为空")
    private Integer maxStudents;
}
