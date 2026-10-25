package com.smartedu.modules.exam.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class AiSaveQuestionDTO {
    private Long courseId;

    @NotEmpty(message = "题目列表不能为空")
    @Valid
    private List<QuestionDTO> questions;
}