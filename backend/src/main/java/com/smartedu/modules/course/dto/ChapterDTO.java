package com.smartedu.modules.course.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 章节DTO
 */
@Data
public class ChapterDTO {

    private Long id;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Long parentId;

    @NotBlank(message = "章节名称不能为空")
    private String chapterName;

    private Integer chapterType;

    private Integer sortOrder;
}
