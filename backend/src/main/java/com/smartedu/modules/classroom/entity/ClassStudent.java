package com.smartedu.modules.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级学生关联实体类
 */
@Data
@TableName("edu_class_student")
public class ClassStudent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 状态: 0-退出 1-在班
     */
    private Integer status;

    /**
     * 删除标记
     */
    private Integer deleted;
}
