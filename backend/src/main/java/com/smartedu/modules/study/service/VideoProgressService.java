package com.smartedu.modules.study.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.study.dto.VideoProgressDTO;
import com.smartedu.modules.study.entity.VideoProgress;

/**
 * 视频学习进度服务接口
 */
public interface VideoProgressService extends IService<VideoProgress> {

    /**
     * 保存/更新进度（旧方法，保留兼容）
     */
    void saveOrUpdateProgress(VideoProgress progress);

    /**
     * 增量模式保存进度（核心修复）
     * 只累加 studySeconds 增量，不覆盖总时长
     */
    void saveOrUpdateProgressWithDelta(Long studentId, VideoProgressDTO dto);

    /**
     * 获取学习进度
     */
    VideoProgress getProgress(Long studentId, Long videoId);
}