package com.smartedu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程实体类
 */
@Data
@TableName("edu_course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程编码
     */
    private String courseCode;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 学科ID
     */
    private Long subjectId;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 课程封面
     */
    private String coverImage;

    /**
     * 教育阶段: 1-小学 2-初中 3-高中 4-大学
     */
    private Integer educationStage;

    /**
     * 难度等级: 1-简单 2-中等 3-困难
     */
    private Integer difficultyLevel;

    /**
     * 课程总时长(小时)
     */
    private Integer durationHours;

    /**
     * 状态: 0-草稿 1-审核中 2-已发布 3-已下架
     */
    private Integer status;

    /**
     * 是否公开: 0-否 1-是
     */
    private Integer isPublic;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 删除标记
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

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
