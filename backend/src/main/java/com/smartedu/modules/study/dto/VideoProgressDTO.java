package com.smartedu.modules.study.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 视频学习进度DTO
 */
@Data
public class VideoProgressDTO {

    @NotNull(message = "课时ID不能为空")
    private Long lessonId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /**
     * 当前播放位置(秒)
     */
    @NotNull(message = "当前播放位置不能为空")
    private Integer currentPosition;

    /**
     * 视频总时长(秒)
     */
    private Integer totalDuration;

    /**
     * 学习进度百分比
     */
    private Integer progressPercent;

    /**
     * 本次学习时长(秒) - 旧字段名，保留兼容
     */
    private Integer studyTime;

    /**
     * 本次实际观看增量秒数（前端计算：currentTime - lastSavedTime）
     * 后端以此累加 total_study_time，避免重复计算
     */
    private Integer studySeconds;

    /**
     * 是否完成: 0-否 1-是
     */
    private Integer isCompleted;

    // ============ 兼容方法 ============

    public Long getVideoId() {
        return this.lessonId;
    }

    public void setVideoId(Long videoId) {
        this.lessonId = videoId;
    }

    public Integer getCurrentTime() {
        return this.currentPosition;
    }

    public void setCurrentTime(Integer currentTime) {
        this.currentPosition = currentTime;
    }

    public Integer getTotalTime() {
        return this.totalDuration;
    }

    public void setTotalTime(Integer totalTime) {
        this.totalDuration = totalTime;
    }

    /**
     * 获取本次学习增量秒数（优先 studySeconds，兼容旧字段 studyTime）
     */
    public Integer getEffectiveStudySeconds() {
        if (studySeconds != null && studySeconds > 0) {
            return studySeconds;
        }
        if (studyTime != null && studyTime > 0) {
            return studyTime;
        }
        return 0;
    }
}