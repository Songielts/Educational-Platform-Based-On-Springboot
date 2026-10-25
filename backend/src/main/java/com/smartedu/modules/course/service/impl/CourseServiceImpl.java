package com.smartedu.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartedu.common.exception.BusinessException;
import com.smartedu.common.result.ResultCode;
import com.smartedu.modules.course.dto.*;
import com.smartedu.modules.course.entity.Chapter;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.entity.Lesson;
import com.smartedu.modules.course.mapper.ChapterMapper;
import com.smartedu.modules.course.mapper.CourseMapper;
import com.smartedu.modules.course.mapper.LessonMapper;
import com.smartedu.modules.course.service.CourseService;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final ChapterMapper chapterMapper;
    private final LessonMapper lessonMapper;

    // ==================== 课程CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCourse(Long teacherId, CourseDTO courseDTO) {
        Course course = new Course();
        BeanUtils.copyProperties(courseDTO, course);
        course.setTeacherId(teacherId);
        course.setStatus(0);
        course.setIsPublic(0);
        course.setViewCount(0);
        course.setLikeCount(0);
        course.setFavoriteCount(0);
        course.setDeleted(0);
        courseMapper.insert(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(Long teacherId, CourseDTO courseDTO) {
        Course course = getById(courseDTO.getId());
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (course.getStatus() == 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "已发布的课程不能编辑，请先下架");
        }
        BeanUtils.copyProperties(courseDTO, course);
        course.setId(courseDTO.getId());
        courseMapper.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long teacherId, Long courseId) {
        Course course = getById(courseId);
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        courseMapper.deleteById(courseId);
    }

    @Override
    public CourseVO getCourseDetail(Long courseId) {
        Course course = getById(courseId);
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        return convertToVO(course);
    }

    // ==================== 教师课程查询 ====================

    @Override
    public Page<CourseVO> getTeacherCoursePage(Long teacherId, String keyword, Long current, Long size) {
        Page<Course> page = new Page<>(current, size);
        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        wrapper.eq("teacher_id", teacherId);
        wrapper.eq("deleted", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("course_name", keyword);
        }
        wrapper.orderByDesc("create_time");
        Page<Course> coursePage = courseMapper.selectPage(page, wrapper);

        List<CourseVO> records = coursePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<CourseVO> result = new Page<>();
        BeanUtils.copyProperties(coursePage, result);
        result.setRecords(records);
        return result;
    }

    // ==================== 公开课程查询 ====================

    @Override
    public IPage<CourseVO> getPublicCoursePage(String keyword, Long subjectId,
                                               Integer educationStage, Long current, Long size) {
        Page<Course> page = new Page<>(current, size);
        IPage<Course> coursePage = courseMapper.selectPublicCoursePage(page, keyword, subjectId, educationStage);

        List<CourseVO> records = coursePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<CourseVO> result = new Page<>();
        BeanUtils.copyProperties(coursePage, result);
        result.setRecords(records);
        return result;
    }

    // ==================== 管理员审核 ====================

    @Override
    public Page<CourseVO> getPendingAuditCourses(String keyword, Long current, Long size) {
        Page<Course> page = new Page<>(current, size);
        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.and(w -> w.eq("status", 0).or().eq("status", 1));
        if (StringUtils.hasText(keyword)) {
            wrapper.like("course_name", keyword);
        }
        wrapper.orderByDesc("create_time");
        Page<Course> coursePage = courseMapper.selectPage(page, wrapper);

        List<CourseVO> records = coursePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<CourseVO> result = new Page<>();
        BeanUtils.copyProperties(coursePage, result);
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCourse(Long courseId, boolean approved, String reason) {
        Course course = getById(courseId);
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        if (course.getStatus() != 0 && course.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该课程不在待审核状态");
        }
        if (approved) {
            course.setStatus(2);
            course.setIsPublic(1);
            course.setPublishTime(LocalDateTime.now());
        } else {
            course.setStatus(3);
        }
        courseMapper.updateById(course);
    }

    // ==================== 发布/下架 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishCourse(Long teacherId, Long courseId) {
        Course course = getById(courseId);
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        course.setStatus(1);
        courseMapper.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineCourse(Long teacherId, Long courseId) {
        Course course = getById(courseId);
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.EXAM_NOT_EXIST);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        course.setStatus(3);
        courseMapper.updateById(course);
    }

    // ==================== 查询所有已发布课程 ====================

    @Override
    public List<CourseVO> getAllPublishedCourses() {
        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 2);
        wrapper.eq("deleted", 0);
        wrapper.eq("is_public", 1);
        wrapper.orderByDesc("create_time");
        List<Course> courses = courseMapper.selectList(wrapper);
        return courses.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    // ==================== 章节管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addChapter(Long teacherId, ChapterDTO chapterDTO) {
        Course course = getById(chapterDTO.getCourseId());
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        // ==================== 修改：教师可以给任意课程添加章节 ====================
        // 原校验已移除
        Chapter chapter = new Chapter();
        chapter.setCourseId(chapterDTO.getCourseId());
        chapter.setChapterName(chapterDTO.getChapterName());
        chapter.setSortOrder(chapterDTO.getSortOrder());
        chapter.setParentId(chapterDTO.getParentId());
        chapter.setChapterType(chapterDTO.getChapterType());
        chapter.setDeleted(0);
        chapterMapper.insert(chapter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChapter(Long teacherId, ChapterDTO chapterDTO) {
        Chapter chapter = chapterMapper.selectById(chapterDTO.getId());
        if (chapter == null || chapter.getDeleted() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "章节不存在");
        }
        // ==================== 修改：不限制所有权 ====================
        chapter.setChapterName(chapterDTO.getChapterName());
        chapter.setSortOrder(chapterDTO.getSortOrder());
        chapterMapper.updateById(chapter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapter(Long teacherId, Long chapterId) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) return;
        // ==================== 修改：不限制所有权 ====================
        chapterMapper.deleteById(chapterId);
    }

    @Override
    public List<ChapterVO> getCourseChapters(Long courseId) {
        List<Chapter> chapters = chapterMapper.selectByCourseId(courseId);
        List<ChapterVO> result = new ArrayList<>();
        for (Chapter chapter : chapters) {
            ChapterVO vo = new ChapterVO();
            BeanUtils.copyProperties(chapter, vo);
            vo.setChapterName(chapter.getChapterName());

            List<Lesson> lessons = lessonMapper.selectByChapterId(chapter.getId());
            List<LessonVO> lessonVOs = new ArrayList<>();
            for (Lesson lesson : lessons) {
                LessonVO lvo = new LessonVO();
                BeanUtils.copyProperties(lesson, lvo);
                lvo.setLessonName(lesson.getLessonName());
                lvo.setIsCompleted(false);
                lessonVOs.add(lvo);
            }
            vo.setLessons(lessonVOs);
            result.add(vo);
        }
        return result;
    }

    // ==================== 课时管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLesson(Long teacherId, LessonDTO lessonDTO) {
        Chapter chapter = chapterMapper.selectById(lessonDTO.getChapterId());
        if (chapter == null || chapter.getDeleted() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "章节不存在");
        }
        Course course = getById(chapter.getCourseId());
        if (course == null || course.getDeleted() != 0) {
            throw new BusinessException(ResultCode.COURSE_NOT_EXIST);
        }
        // ==================== 修改：教师可以给任意课程添加课时 ====================
        // 原校验：if (course != null && !course.getTeacherId().equals(teacherId)) throw FORBIDDEN;
        // 现在：只检查课程是否存在，不限制所有权
        Lesson lesson = new Lesson();
        lesson.setChapterId(lessonDTO.getChapterId());
        lesson.setLessonName(lessonDTO.getLessonName());
        lesson.setLessonType(lessonDTO.getLessonType());
        lesson.setVideoUrl(lessonDTO.getVideoUrl());
        lesson.setVideoDuration(lessonDTO.getVideoDuration());
        lesson.setContent(lessonDTO.getContent());
        lesson.setSortOrder(lessonDTO.getSortOrder());
        lesson.setIsFree(lessonDTO.getIsFree());
        lesson.setCourseId(chapter.getCourseId());
        lesson.setDeleted(0);
        lessonMapper.insert(lesson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLesson(Long teacherId, LessonDTO lessonDTO) {
        Lesson lesson = lessonMapper.selectById(lessonDTO.getId());
        if (lesson == null || lesson.getDeleted() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "课时不存在");
        }
        // ==================== 修改：不限制所有权 ====================
        lesson.setLessonName(lessonDTO.getLessonName());
        lesson.setLessonType(lessonDTO.getLessonType());
        lesson.setVideoUrl(lessonDTO.getVideoUrl());
        lesson.setVideoDuration(lessonDTO.getVideoDuration());
        lesson.setContent(lessonDTO.getContent());
        lesson.setSortOrder(lessonDTO.getSortOrder());
        lesson.setIsFree(lessonDTO.getIsFree());
        lessonMapper.updateById(lesson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLesson(Long teacherId, Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) return;
        // ==================== 修改：不限制所有权 ====================
        lessonMapper.deleteById(lessonId);
    }

    @Override
    public LessonVO getLessonDetail(Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null || lesson.getDeleted() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "课时不存在");
        }
        LessonVO vo = new LessonVO();
        BeanUtils.copyProperties(lesson, vo);
        vo.setLessonName(lesson.getLessonName());
        return vo;
    }

    // ==================== 学生课程 ====================

    @Override
    public List<StudentCourseVO> getStudentCourses(Long studentId) {
        return courseMapper.selectStudentCourses(studentId);
    }

    @Override
    public boolean checkStudentCourse(Long studentId, Long courseId) {
        return courseMapper.countStudentCourse(studentId, courseId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void selectCourse(Long studentId, Long courseId) {
        courseMapper.insertStudentCourse(studentId, courseId);
    }

    // ==================== 私有转换方法 ====================

    private CourseVO convertToVO(Course course) {
        if (course == null) return null;
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(course, vo);

        switch (course.getStatus()) {
            case 0: vo.setStatusName("草稿"); break;
            case 1: vo.setStatusName("审核中"); break;
            case 2: vo.setStatusName("已发布"); break;
            case 3: vo.setStatusName("已下架"); break;
            default: vo.setStatusName("未知"); break;
        }

        switch (course.getEducationStage()) {
            case 1: vo.setEducationStageName("小学"); break;
            case 2: vo.setEducationStageName("初中"); break;
            case 3: vo.setEducationStageName("高中"); break;
            case 4: vo.setEducationStageName("大学"); break;
            default: vo.setEducationStageName("未知"); break;
        }

        switch (course.getDifficultyLevel()) {
            case 1: vo.setDifficultyLevelName("简单"); break;
            case 2: vo.setDifficultyLevelName("中等"); break;
            case 3: vo.setDifficultyLevelName("困难"); break;
            default: vo.setDifficultyLevelName("未知"); break;
        }

        vo.setSubjectName(getSubjectName(course.getSubjectId()));

        if (course.getTeacherId() != null) {
            User teacher = userMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getRealName());
            }
        }

        // 统计课程课时数
        int lessonCount = countLessonsByCourseId(course.getId());
        vo.setLessonCount(lessonCount);

        return vo;
    }

    /**
     * 统计课程的课时数量
     */
    private int countLessonsByCourseId(Long courseId) {
        if (courseId == null) return 0;
        List<Chapter> chapters = chapterMapper.selectByCourseId(courseId);
        int count = 0;
        for (Chapter chapter : chapters) {
            List<Lesson> lessons = lessonMapper.selectByChapterId(chapter.getId());
            count += lessons != null ? lessons.size() : 0;
        }
        return count;
    }

    private String getSubjectName(Long subjectId) {
        if (subjectId == null) return "未知";
        switch (subjectId.intValue()) {
            case 1: return "语文";
            case 2: return "数学";
            case 3: return "英语";
            case 4: return "物理";
            case 5: return "化学";
            case 6: return "生物";
            case 7: return "历史";
            case 8: return "地理";
            case 9: return "政治";
            default: return "其他";
        }
    }
}