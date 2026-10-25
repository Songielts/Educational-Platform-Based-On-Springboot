package com.smartedu.modules.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("study_practice_answer")
public class PracticeAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long lessonId;
    private Long questionId;
    private String studentAnswer;
    private Integer isCorrect;
    private BigDecimal obtainScore;
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}