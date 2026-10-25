package com.smartedu.modules.study.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 学习笔记DTO
 */
@Data
public class StudyNoteDTO {

    private Long id;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Long lessonId;

    private String noteTitle;

    @NotBlank(message = "笔记内容不能为空")
    private String noteContent;

    /**
     * 视频时间点(秒)
     */
    private Integer videoPosition;
}
