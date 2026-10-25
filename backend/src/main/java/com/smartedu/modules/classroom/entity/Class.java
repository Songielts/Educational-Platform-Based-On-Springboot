package com.smartedu.modules.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级实体类
 */
@Data
@TableName("edu_class")
public class Class implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级编码
     */
    private String classCode;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 班级描述
     */
    private String description;

    /**
     * 班级封面
     */
    private String coverImage;

    /**
     * 最大学生数
     */
    private Integer maxStudents;

    /**
     * 当前学生数
     */
    private Integer currentStudents;

    /**
     * 状态: 0-关闭 1-开放
     */
    private Integer status;

    /**
     * 班级加入码
     */
    private String joinCode;

    /**
     * 加入码过期时间
     */
    private LocalDateTime joinCodeExpire;

    /**
     * 删除标记
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
