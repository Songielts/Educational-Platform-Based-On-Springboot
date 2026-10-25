package com.smartedu.modules.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("study_lesson_question")
public class LessonQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private Long questionId;
    private Integer questionOrder;
    private BigDecimal score;
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
