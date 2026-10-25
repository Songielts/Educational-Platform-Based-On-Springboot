package com.smartedu.modules.study.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.modules.study.dto.VideoProgressDTO;
import com.smartedu.modules.study.entity.VideoProgress;
import com.smartedu.modules.study.mapper.VideoProgressMapper;
import com.smartedu.modules.study.service.VideoProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 视频学习进度服务实现（增量累加模式）
 */
@Slf4j
@Service
public class VideoProgressServiceImpl extends ServiceImpl<VideoProgressMapper, VideoProgress>
        implements VideoProgressService {

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    // ==================== 旧方法（保留兼容）====================

    @Override
    public void saveOrUpdateProgress(VideoProgress progress) {
        VideoProgress exist = lambdaQuery()
                .eq(VideoProgress::getStudentId, progress.getStudentId())
                .eq(VideoProgress::getLessonId, progress.getVideoId())
                .one();

        if (exist != null) {
            progress.setId(exist.getId());
            progress.setUpdateTime(LocalDateTime.now());
            updateById(progress);
        } else {
            progress.setCreateTime(LocalDateTime.now());
            save(progress);
        }
    }

    @Override
    public VideoProgress getProgress(Long studentId, Long videoId) {
        return lambdaQuery()
                .eq(VideoProgress::getStudentId, studentId)
                .eq(VideoProgress::getLessonId, videoId)
                .one();
    }

    // ==================== 核心修复：增量模式保存 ====================

    /**
     * 保存视频学习进度（增量模式）
     *
     * 核心逻辑：
     * 1. 从 DTO 获取 studySeconds（前端计算的增量秒数）
     * 2. 将增量秒数转为分钟数（数据库 total_study_time 单位为分钟）
     * 3. 使用原子 UPDATE 累加，避免并发覆盖
     * 4. studySeconds=0 时只更新播放位置，不累加时长
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateProgressWithDelta(Long studentId, VideoProgressDTO dto) {
        Long lessonId = dto.getLessonId();
        Long courseId = dto.getCourseId();
        Integer currentPosition = dto.getCurrentPosition() != null ? dto.getCurrentPosition() : 0;
        Integer totalDuration = dto.getTotalDuration() != null ? dto.getTotalDuration() : 0;
        // 获取增量秒数（优先 studySeconds，兼容旧字段 studyTime）
        Integer studySeconds = dto.getEffectiveStudySeconds();
        boolean completed = Integer.valueOf(1).equals(dto.getIsCompleted());

        // 增量分钟数（数据库 total_study_time 单位为分钟）
        // studySeconds=0 时 addMinutes=0，不再默认加30分钟
        int addMinutes = studySeconds > 0 ? Math.max(1, Math.round(studySeconds / 60f)) : 0;

        // 计算进度百分比
        BigDecimal progressPercent = BigDecimal.ZERO;
        if (totalDuration > 0) {
            progressPercent = new BigDecimal(currentPosition * 100)
                    .divide(new BigDecimal(totalDuration), 2, RoundingMode.HALF_UP);
        }

        // 查询现有记录
        VideoProgress exist = videoProgressMapper.selectByStudentAndLesson(studentId, lessonId);

        if (exist == null) {
            // 首次学习：插入新记录
            VideoProgress vp = new VideoProgress();
            vp.setStudentId(studentId);
            vp.setLessonId(lessonId);
            vp.setCourseId(courseId);
            vp.setCurrentPosition(currentPosition);
            vp.setTotalDuration(totalDuration);
            vp.setProgressPercent(progressPercent);
            vp.setIsCompleted(completed ? 1 : 0);
            vp.setStudyCount(1);
            vp.setTotalStudyTime(addMinutes);  // 首次只记录增量
            vp.setDeleted(0);
            vp.setLastStudyTime(LocalDateTime.now());
            vp.setCreateTime(LocalDateTime.now());
            vp.setUpdateTime(LocalDateTime.now());
            videoProgressMapper.insert(vp);

            log.info("[saveWithDelta] 首次学习 studentId={}, lessonId={}, studySeconds={}, addMinutes={}",
                    studentId, lessonId, studySeconds, addMinutes);
        } else {
            // 非首次：使用原子 UPDATE 累加
            int newIsCompleted = completed ? 1 : (exist.getIsCompleted() != null ? exist.getIsCompleted() : 0);

            if (addMinutes > 0) {
                // 有增量时：原子累加 total_study_time + addMinutes, study_count + 1
                videoProgressMapper.updateProgressAtomic(
                        exist.getId(),
                        currentPosition,
                        totalDuration > 0 ? totalDuration : exist.getTotalDuration(),
                        progressPercent,
                        newIsCompleted,
                        addMinutes,
                        LocalDateTime.now()
                );
                log.info("[saveWithDelta] 累加 studentId={}, lessonId={}, studySeconds={}, addMinutes={}, currentPos={}",
                        studentId, lessonId, studySeconds, addMinutes, currentPosition);
            } else {
                // 无增量时：只更新播放位置和进度，不累加时长和次数
                videoProgressMapper.updateProgressDirect(
                        exist.getId(),
                        currentPosition,
                        totalDuration > 0 ? totalDuration : exist.getTotalDuration(),
                        progressPercent,
                        newIsCompleted,
                        exist.getStudyCount() != null ? exist.getStudyCount() : 1,
                        exist.getTotalStudyTime() != null ? exist.getTotalStudyTime() : 0,
                        LocalDateTime.now()
                );
                log.info("[saveWithDelta] 仅更新位置 studentId={}, lessonId={}, currentPos={}",
                        studentId, lessonId, currentPosition);
            }
        }
    }
}