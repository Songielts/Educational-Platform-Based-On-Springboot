package com.smartedu.modules.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频学习进度实体类
 */
@Data
@TableName("study_video_progress")
public class VideoProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 课时ID (对应视频ID)
     */
    private Long lessonId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 当前播放位置(秒)
     */
    private Integer currentPosition;

    /**
     * 视频总时长(秒)
     */
    private Integer totalDuration;

    /**
     * 学习进度百分比
     */
    private BigDecimal progressPercent;

    /**
     * 是否完成: 0-否 1-是
     */
    private Integer isCompleted;

    /**
     * 学习次数
     */
    private Integer studyCount;

    /**
     * 累计学习时长(秒)
     */
    private Integer totalStudyTime;

    /**
     * 最后学习时间
     */
    private LocalDateTime lastStudyTime;

    /**
     * 删除标记
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

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

    // ============ 兼容方法 (用于Controller和Service) ============

    /**
     * 获取视频ID (别名 for lessonId)
     */
    public Long getVideoId() {
        return this.lessonId;
    }

    /**
     * 设置视频ID (别名 for lessonId)
     */
    public void setVideoId(Long videoId) {
        this.lessonId = videoId;
    }

    /**
     * 获取当前时间(秒) (别名 for currentPosition)
     */
    public Integer getCurrentTime() {
        return this.currentPosition;
    }

    /**
     * 设置当前时间(秒) (别名 for currentPosition)
     */
    public void setCurrentTime(Integer currentTime) {
        this.currentPosition = currentTime;
    }

    /**
     * 获取总时长(秒) (别名 for totalDuration)
     */
    public Integer getTotalTime() {
        return this.totalDuration;
    }

    /**
     * 设置总时长(秒) (别名 for totalDuration)
     */
    public void setTotalTime(Integer totalTime) {
        this.totalDuration = totalTime;
    }
}