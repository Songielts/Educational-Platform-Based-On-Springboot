package com.smartedu.modules.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartedu.common.result.Result;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.entity.Lesson;
import com.smartedu.modules.course.entity.StudentCourse;
import com.smartedu.modules.course.entity.StudyCourseStat;
import com.smartedu.modules.course.mapper.LessonMapper;
import com.smartedu.modules.course.mapper.StudyCourseStatMapper;
import com.smartedu.modules.course.service.CourseService;
import com.smartedu.modules.course.service.StudentCourseService;
import com.smartedu.modules.user.entity.User;
import com.smartedu.modules.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/student/course")
public class StudentCourseController {

    @Autowired
    private StudentCourseService studentCourseService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private LessonMapper lessonMapper;

    /**
     * 直接注入 StudyCourseStatMapper（course模块下没有对应的Service，只有Mapper）
     */
    @Autowired
    private StudyCourseStatMapper studyCourseStatMapper;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }

        if (principal instanceof Long) {
            return (Long) principal;
        }

        if (principal instanceof Integer) {
            return ((Integer) principal).longValue();
        }

        if (principal instanceof com.smartedu.modules.user.entity.User) {
            return ((com.smartedu.modules.user.entity.User) principal).getId();
        }

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.error("Cannot parse username to Long: {}", username);
            }
        }

        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                log.error("Cannot parse principal to Long: {}", principal);
            }
        }

        return null;
    }

    /**
     * 学生选课
     */
    @PostMapping("/select/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result selectCourse(@PathVariable Long courseId) {
        log.info("选课请求: courseId={}", courseId);

        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error("未登录或无法获取用户信息");
        }

        if (studentCourseService.isSelected(studentId, courseId)) {
            return Result.error("您已选过该课程");
        }

        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        sc.setStatus(1);
        sc.setSelectTime(LocalDateTime.now());
        sc.setCreateTime(LocalDateTime.now());
        sc.setUpdateTime(LocalDateTime.now());

        boolean success = studentCourseService.save(sc);
        log.info("选课结果: {}", success);

        return success ? Result.success("选课成功") : Result.error("选课失败");
    }

    /**
     * 获取我的课程列表（带课程详情和学习进度）
     * 修复：用 StudyCourseStatMapper 直接查 study_course_stat 表，不再硬编码0
     */
    @GetMapping("/my-list")
    @PreAuthorize("hasRole('STUDENT')")
    public Result getMyCourses() {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error("未登录");
        }

        List<StudentCourse> list = studentCourseService.lambdaQuery()
                .eq(StudentCourse::getStudentId, studentId)
                .orderByDesc(StudentCourse::getSelectTime)
                .list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentCourse sc : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("courseId", sc.getCourseId());
            map.put("status", sc.getStatus());
            map.put("selectTime", sc.getSelectTime());

            Course course = courseService.getById(sc.getCourseId());
            if (course != null) {
                map.put("courseName", course.getCourseName());
                map.put("coverImage", course.getCoverImage());

                User teacher = userService.getById(course.getTeacherId());
                map.put("teacherName", teacher != null ? teacher.getRealName() : "未知教师");

                long totalLessons = lessonMapper.selectCount(
                        new QueryWrapper<Lesson>()
                                .eq("course_id", sc.getCourseId())
                                .eq("deleted", 0)
                );
                map.put("totalLessons", totalLessons);

                // 用 Mapper 直接查 study_course_stat
                StudyCourseStat stat = studyCourseStatMapper.selectOne(
                        new QueryWrapper<StudyCourseStat>()
                                .eq("student_id", studentId)
                                .eq("course_id", sc.getCourseId())
                                .and(w -> w.eq("deleted", 0).or().isNull("deleted"))
                                .last("LIMIT 1")
                );

                if (stat != null) {
                    map.put("completedLessons", stat.getCompletedLessons() != null ? stat.getCompletedLessons() : 0);
                    map.put("completionRate", stat.getCompletionRate() != null ? stat.getCompletionRate() : 0);
                    map.put("lastStudyTimeStr",
                            stat.getLastStudyTime() != null ? stat.getLastStudyTime().toString() : "未学习");
                } else {
                    map.put("completedLessons", 0);
                    map.put("completionRate", 0);
                    map.put("lastStudyTimeStr", "未学习");
                }
            }

            result.add(map);
        }

        return Result.success(result);
    }

    /**
     * 取消选课
     */
    @DeleteMapping("/cancel/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public Result cancelCourse(@PathVariable Long courseId) {
        Long studentId = getCurrentUserId();
        if (studentId == null) {
            return Result.error("未登录");
        }

        studentCourseService.lambdaUpdate()
                .eq(StudentCourse::getStudentId, studentId)
                .eq(StudentCourse::getCourseId, courseId)
                .remove();

        return Result.success("取消选课成功");
    }
}