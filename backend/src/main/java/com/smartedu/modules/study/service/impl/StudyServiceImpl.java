package com.smartedu.modules.study.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartedu.common.constants.SystemConstants;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.common.result.ResultCode;
import com.smartedu.common.utils.RedisUtils;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.entity.Lesson;
import com.smartedu.modules.course.mapper.CourseMapper;
import com.smartedu.modules.course.mapper.LessonMapper;
import com.smartedu.modules.study.dto.*;
import com.smartedu.modules.study.entity.CourseStat;
import com.smartedu.modules.study.entity.StudyNote;
import com.smartedu.modules.study.entity.VideoProgress;
import com.smartedu.modules.study.mapper.CourseStatMapper;
import com.smartedu.modules.study.mapper.StudyNoteMapper;
import com.smartedu.modules.study.mapper.VideoProgressMapper;
import com.smartedu.modules.study.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学习Service实现类
 */
@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final VideoProgressMapper videoProgressMapper;
    private final StudyNoteMapper studyNoteMapper;
    private final CourseStatMapper courseStatMapper;
    private final CourseMapper courseMapper;
    private final LessonMapper lessonMapper;
    private final RedisUtils redisUtils;

    // ==================== 视频学习进度 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVideoProgress(Long studentId, VideoProgressDTO progressDTO) {
        // 1. 查询现有进度
        VideoProgress progress = videoProgressMapper.selectByStudentAndLesson(studentId, progressDTO.getLessonId());

        // 2. 防刷课校验 - 检查播放速度是否异常
        String cacheKey = SystemConstants.RedisKey.VIDEO_PROGRESS_PREFIX + studentId + ":" + progressDTO.getLessonId();
        Object lastPosition = redisUtils.get(cacheKey);
        if (lastPosition != null) {
            Integer lastPos = (Integer) lastPosition;
            // 如果进度跳跃超过30秒，可能是拖拽
            if (progressDTO.getCurrentPosition() - lastPos > 30) {
                // 记录异常，但允许继续
                // TODO: 记录刷课行为
            }
        }

        // 3. 缓存当前进度到Redis（60秒过期）
        redisUtils.set(cacheKey, progressDTO.getCurrentPosition(), 60, TimeUnit.SECONDS);

        // 4. 计算进度百分比
        BigDecimal progressPercent = BigDecimal.ZERO;
        if (progressDTO.getTotalDuration() != null && progressDTO.getTotalDuration() > 0) {
            progressPercent = BigDecimal.valueOf(progressDTO.getCurrentPosition())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(progressDTO.getTotalDuration()), 2, RoundingMode.HALF_UP);
        }

        // 5. 判断是否完成（观看超过80%视为完成）
        boolean isCompleted = progressPercent.compareTo(BigDecimal.valueOf(80)) >= 0;

        if (progress == null) {
            // 创建新记录
            progress = new VideoProgress();
            progress.setStudentId(studentId);
            progress.setLessonId(progressDTO.getLessonId());
            progress.setCourseId(progressDTO.getCourseId());
            progress.setCurrentPosition(progressDTO.getCurrentPosition());
            progress.setTotalDuration(progressDTO.getTotalDuration());
            progress.setProgressPercent(progressPercent);
            progress.setIsCompleted(isCompleted ? 1 : 0);
            progress.setStudyCount(1);
            progress.setTotalStudyTime(progressDTO.getStudyTime() != null ? progressDTO.getStudyTime() : 0);
            progress.setLastStudyTime(LocalDateTime.now());
            videoProgressMapper.insert(progress);
        } else {
            // 更新记录
            progress.setCurrentPosition(progressDTO.getCurrentPosition());
            progress.setTotalDuration(progressDTO.getTotalDuration());
            progress.setProgressPercent(progressPercent);
            if (isCompleted) {
                progress.setIsCompleted(1);
            }
            progress.setStudyCount(progress.getStudyCount() + 1);
            if (progressDTO.getStudyTime() != null) {
                progress.setTotalStudyTime(progress.getTotalStudyTime() + progressDTO.getStudyTime());
            }
            progress.setLastStudyTime(LocalDateTime.now());
            videoProgressMapper.updateById(progress);
        }

        // 6. 更新课程统计
        updateCourseStat(studentId, progressDTO.getCourseId());
    }

    @Override
    public VideoProgressVO getVideoProgress(Long studentId, Long lessonId) {
        VideoProgress progress = videoProgressMapper.selectByStudentAndLesson(studentId, lessonId);
        if (progress == null) {
            return null;
        }
        return convertToProgressVO(progress);
    }

    @Override
    public List<VideoProgressVO> getCourseProgress(Long studentId, Long courseId) {
        List<VideoProgress> progressList = videoProgressMapper.selectByStudentAndCourse(studentId, courseId);
        return progressList.stream()
                .map(this::convertToProgressVO)
                .collect(Collectors.toList());
    }

    // ==================== 学习笔记 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addNote(Long studentId, StudyNoteDTO noteDTO) {
        StudyNote note = new StudyNote();
        BeanUtils.copyProperties(noteDTO, note);
        note.setStudentId(studentId);
        note.setIsFavorite(0);
        note.setDeleted(0);  // 添加这行

        studyNoteMapper.insert(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNote(Long studentId, StudyNoteDTO noteDTO) {
        StudyNote note = studyNoteMapper.selectById(noteDTO.getId());
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        if (!note.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        BeanUtils.copyProperties(noteDTO, note);
        studyNoteMapper.updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long studentId, Long noteId) {
        StudyNote note = studyNoteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        if (!note.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        studyNoteMapper.deleteById(noteId);
    }

    @Override
    public StudyNoteVO getNoteDetail(Long noteId) {
        StudyNote note = studyNoteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        return convertToNoteVO(note);
    }

    @Override
    public Page<StudyNoteVO> getNotePage(Long studentId, Long courseId, Long lessonId, String keyword, Long current, Long size) {
        Page<StudyNote> page = new Page<>(current, size);
        // 添加 lessonId 参数
        Page<StudyNote> notePage = (Page<StudyNote>) studyNoteMapper.selectStudentNotePage(page, studentId, courseId, lessonId, keyword);

        List<StudyNoteVO> records = notePage.getRecords().stream()
                .map(this::convertToNoteVO)
                .collect(Collectors.toList());

        Page<StudyNoteVO> resultPage = new Page<>();
        BeanUtils.copyProperties(notePage, resultPage);
        resultPage.setRecords(records);

        return resultPage;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleNoteFavorite(Long studentId, Long noteId) {
        StudyNote note = studyNoteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(ResultCode.NOTE_NOT_EXIST);
        }
        if (!note.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        note.setIsFavorite(note.getIsFavorite() == 1 ? 0 : 1);
        studyNoteMapper.updateById(note);
    }

    // ==================== 学习统计 ====================

    @Override
    public StudyStatVO getStudentStudyStat(Long studentId) {
        StudyStatVO statVO = new StudyStatVO();

        // 累计学习时长
        Integer totalStudyTime = courseStatMapper.sumStudyTimeByStudent(studentId);
        statVO.setTotalStudyTime(totalStudyTime != null ? totalStudyTime : 0);
        statVO.setTotalStudyTimeStr(formatDuration(statVO.getTotalStudyTime()));

        // 学习课程数
        List<CourseStat> courseStats = courseStatMapper.selectByStudent(studentId);
        statVO.setCourseCount(courseStats.size());

        // 已完成课程数
        Integer completedCount = courseStatMapper.countCompletedCourses(studentId);
        statVO.setCompletedCourseCount(completedCount != null ? completedCount : 0);

        // 完成率
        if (statVO.getCourseCount() > 0) {
            statVO.setCompletionRate(BigDecimal.valueOf(statVO.getCompletedCourseCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(statVO.getCourseCount()), 2, RoundingMode.HALF_UP));
        } else {
            statVO.setCompletionRate(BigDecimal.ZERO);
        }

        // TODO: 笔记数量、考试次数、平均分、连续学习天数

        return statVO;
    }

    @Override
    public CourseStatVO getCourseStat(Long studentId, Long courseId) {
        CourseStat stat = courseStatMapper.selectByStudentAndCourse(studentId, courseId);
        if (stat == null) {
            return null;
        }
        return convertToCourseStatVO(stat);
    }

    @Override
    public List<Map<String, Object>> getStudyTrend(Long studentId, Integer days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        // TODO: 从数据库查询每日学习时长
        // 这里返回模拟数据
        for (int i = days - 1; i >= 0; i--) {
            Map<String, Object> data = new HashMap<>();
            LocalDateTime date = endDate.minusDays(i);
            data.put("date", date.format(DateTimeFormatter.ofPattern("MM-dd")));
            data.put("studyTime", (int) (Math.random() * 3600));
            result.add(data);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getKnowledgeMastery(Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();

        // TODO: 从数据库查询知识点掌握情况
        // 这里返回模拟数据
        String[] knowledges = {"数学基础", "代数", "几何", "函数", "概率统计"};
        for (String knowledge : knowledges) {
            Map<String, Object> data = new HashMap<>();
            data.put("knowledge", knowledge);
            data.put("mastery", (int) (Math.random() * 100));
            result.add(data);
        }

        return result;
    }

    // ==================== 私有方法 ====================

    private void updateCourseStat(Long studentId, Long courseId) {
        // 查询课程的所有课时
        List<Lesson> lessons = lessonMapper.selectByCourseId(courseId);
        int totalLessons = lessons.size();

        // 查询已完成课时数
        List<VideoProgress> progressList = videoProgressMapper.selectByStudentAndCourse(studentId, courseId);
        int completedLessons = (int) progressList.stream()
                .filter(p -> p.getIsCompleted() != null && p.getIsCompleted() == 1)
                .count();

        // 计算累计学习时长
        int totalStudyTime = progressList.stream()
                .mapToInt(VideoProgress::getTotalStudyTime)
                .sum();

        // 计算完成率
        BigDecimal completionRate = BigDecimal.ZERO;
        if (totalLessons > 0) {
            completionRate = BigDecimal.valueOf(completedLessons)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
        }

        // 更新或创建统计记录
        CourseStat stat = courseStatMapper.selectByStudentAndCourse(studentId, courseId);
        if (stat == null) {
            stat = new CourseStat();
            stat.setStudentId(studentId);
            stat.setCourseId(courseId);
            stat.setTotalLessons(totalLessons);
            stat.setCompletedLessons(completedLessons);
            stat.setCompletionRate(completionRate);
            stat.setTotalStudyTime(totalStudyTime);
            stat.setLastStudyTime(LocalDateTime.now());
            courseStatMapper.insert(stat);
        } else {
            stat.setTotalLessons(totalLessons);
            stat.setCompletedLessons(completedLessons);
            stat.setCompletionRate(completionRate);
            stat.setTotalStudyTime(totalStudyTime);
            stat.setLastStudyTime(LocalDateTime.now());
            courseStatMapper.updateById(stat);
        }
    }

    private VideoProgressVO convertToProgressVO(VideoProgress progress) {
        VideoProgressVO vo = new VideoProgressVO();
        BeanUtils.copyProperties(progress, vo);

        // 查询课时名称
        Lesson lesson = lessonMapper.selectById(progress.getLessonId());
        if (lesson != null) {
            vo.setLessonName(lesson.getLessonName());
        }

        return vo;
    }

    private StudyNoteVO convertToNoteVO(StudyNote note) {
        StudyNoteVO vo = new StudyNoteVO();
        BeanUtils.copyProperties(note, vo);

        // 查询课程名称
        Course course = courseMapper.selectById(note.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
        }

        // 查询课时名称
        if (note.getLessonId() != null) {
            Lesson lesson = lessonMapper.selectById(note.getLessonId());
            if (lesson != null) {
                vo.setLessonName(lesson.getLessonName());
            }
        }

        // 格式化视频时间点
        if (note.getVideoPosition() != null) {
            int minutes = note.getVideoPosition() / 60;
            int seconds = note.getVideoPosition() % 60;
            vo.setVideoPositionStr(String.format("%02d:%02d", minutes, seconds));
        }

        return vo;
    }

    private CourseStatVO convertToCourseStatVO(CourseStat stat) {
        CourseStatVO vo = new CourseStatVO();
        BeanUtils.copyProperties(stat, vo);

        // 查询课程信息
        Course course = courseMapper.selectById(stat.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getCourseName());
            vo.setCoverImage(course.getCoverImage());
        }

        // 格式化学习时长
        vo.setTotalStudyTimeStr(formatDuration(stat.getTotalStudyTime()));

        // 格式化最后学习时间
        if (stat.getLastStudyTime() != null) {
            long days = ChronoUnit.DAYS.between(stat.getLastStudyTime(), LocalDateTime.now());
            if (days == 0) {
                vo.setLastStudyTimeStr("今天");
            } else if (days == 1) {
                vo.setLastStudyTimeStr("昨天");
            } else {
                vo.setLastStudyTimeStr(days + "天前");
            }
        }

        return vo;
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds == 0) {
            return "0小时";
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }
}
