package com.smartedu.modules.course.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.course.dto.*;
import com.smartedu.modules.course.entity.Course;

import java.util.List;

/**
 * 课程Service接口
 */
public interface CourseService extends IService<Course> {

    /**
     * 创建课程
     */
    void createCourse(Long teacherId, CourseDTO courseDTO);

    /**
     * 更新课程
     */
    void updateCourse(Long teacherId, CourseDTO courseDTO);

    /**
     * 删除课程
     */
    void deleteCourse(Long teacherId, Long courseId);

    /**
     * 获取课程详情
     */
    CourseVO getCourseDetail(Long courseId);

    /**
     * 分页查询公开课程
     */
    IPage<CourseVO> getPublicCoursePage(String keyword, Long subjectId, Integer educationStage, Long current, Long size);

    /**
     * 分页查询教师的课程（包含草稿、审核中、已发布、已下架）
     */
    Page<CourseVO> getTeacherCoursePage(Long teacherId, String keyword, Long current, Long size);

    /**
     * 发布课程（提交审核）
     */
    void publishCourse(Long teacherId, Long courseId);

    /**
     * 下架课程
     */
    void offlineCourse(Long teacherId, Long courseId);

    // ==================== 管理员审核 ====================

    /**
     * 查询待审核课程列表（管理员）
     */
    Page<CourseVO> getPendingAuditCourses(String keyword, Long current, Long size);

    /**
     * 审核课程（管理员）
     * @param approved true-通过 false-拒绝
     * @param reason 拒绝原因（拒绝时必填）
     */
    void auditCourse(Long courseId, boolean approved, String reason);

    // ==================== 章节管理 ====================

    /**
     * 添加章节
     */
    void addChapter(Long teacherId, ChapterDTO chapterDTO);

    /**
     * 更新章节
     */
    void updateChapter(Long teacherId, ChapterDTO chapterDTO);

    /**
     * 删除章节
     */
    void deleteChapter(Long teacherId, Long chapterId);

    /**
     * 获取课程的章节树
     */
    List<ChapterVO> getCourseChapters(Long courseId);

    // ==================== 课时管理 ====================

    /**
     * 添加课时
     */
    void addLesson(Long teacherId, LessonDTO lessonDTO);

    /**
     * 更新课时
     */
    void updateLesson(Long teacherId, LessonDTO lessonDTO);

    /**
     * 删除课时
     */
    void deleteLesson(Long teacherId, Long lessonId);

    /**
     * 获取课时详情
     */
    LessonVO getLessonDetail(Long lessonId);

    /**
     * 获取学生学习的课程列表
     */
    List<StudentCourseVO> getStudentCourses(Long studentId);

    /**
     * 检查学生是否已选课程
     */
    boolean checkStudentCourse(Long studentId, Long courseId);

    /**
     * 学生选课
     */
    void selectCourse(Long studentId, Long courseId);

    /**
     * 查询所有已发布的课程
     */
    List<CourseVO> getAllPublishedCourses();
}
