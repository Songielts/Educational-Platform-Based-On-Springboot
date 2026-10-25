package com.smartedu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("edu_student_course")
public class StudentCourse {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long courseId;
    private Integer status;
    private LocalDateTime selectTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 必须添加这个字段
    @TableLogic
    private Integer deleted;
}