package com.smartedu.modules.course.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 课时DTO
 */
@Data
public class LessonDTO {

    private Long id;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotBlank(message = "课时名称不能为空")
    private String lessonName;

    private Integer lessonType;

    private String videoUrl;

    private Integer videoDuration;

    private Long videoSize;

    private String docUrl;

    private String content;

    private Integer sortOrder;

    private Integer isFree;
}
