package com.smartedu.modules.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课时实体类
 */
@Data
@TableName("edu_lesson")
public class Lesson implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 课时名称
     */
    private String lessonName;

    /**
     * 课时类型: 1-视频 2-文档 3-测验
     */
    private Integer lessonType;

    /**
     * 视频URL
     */
    private String videoUrl;

    /**
     * 视频时长(秒)
     */
    private Integer videoDuration;

    /**
     * 视频大小(字节)
     */
    private Long videoSize;

    /**
     * 文档URL
     */
    private String docUrl;

    /**
     * 内容文本
     */
    private String content;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否免费: 0-否 1-是
     */
    private Integer isFree;

    /**
     * 状态
     */
    private Integer status;

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
