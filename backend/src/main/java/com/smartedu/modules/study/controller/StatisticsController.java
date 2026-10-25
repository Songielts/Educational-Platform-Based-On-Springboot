package com.smartedu.modules.study.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartedu.common.result.Result;
import com.smartedu.modules.course.dto.StudentCourseVO;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.mapper.CourseMapper;
import com.smartedu.modules.study.entity.CourseStat;
import com.smartedu.modules.study.entity.VideoProgress;
import com.smartedu.modules.study.mapper.CourseStatMapper;
import com.smartedu.modules.study.mapper.VideoProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习统计控制器（最终修复版）
 *
 * 核心修复：
 * 1. 所有接口改为以 edu_student_course（学生选课表）为基准查询
 * 2. 复用 CourseMapper.selectStudentCourses() 方法获取全部课程
 * 3. 没有学习记录的课程默认完成率为0，确保全部7门课程都显示
 */
@Slf4j
@RestController
@RequestMapping("/study/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    @Autowired
    private VideoProgressMapper videoProgressMapper;

    @Autowired
    private CourseStatMapper courseStatMapper;

    @Autowired
    private CourseMapper courseMapper;

    // ==================== 核心：获取学生全部课程（从选课表为基准） ====================

    /**
     * 获取学生的所有选课（含未学习的），按完成率降序排列
     * 复用 CourseMapper.selectStudentCourses() 方法
     */
    private List<StudentCourseVO> getAllStudentCourses(Long studentId) {
        List<StudentCourseVO> courses = courseMapper.selectStudentCourses(studentId);
        if (courses == null) {
            return new ArrayList<>();
        }
        // 按完成率降序排列（高的在前面）
        courses.sort((a, b) -> {
            int rateA = a.getCompletionRate() != null ? a.getCompletionRate().intValue() : 0;
            int rateB = b.getCompletionRate() != null ? b.getCompletionRate().intValue() : 0;
            return Integer.compare(rateB, rateA);
        });
        return courses;
    }

    // ==================== 1. 综合统计卡片（修复：基于全部选课） ====================

    @GetMapping("/overview")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getStudyOverview(@AuthenticationPrincipal Long studentId) {
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        Map<String, Object> overview = new HashMap<>();

        // 累计学习时长（从视频进度表查，保持不变）
        Integer totalMinutes = videoProgressMapper.sumStudyTimeByStudent(studentId);
        double hours = totalMinutes != null ? Math.round(totalMinutes / 60.0 * 10) / 10.0 : 0.0;
        overview.put("totalHours", hours);

        // ========== 修复：基于全部选课计算 ==========
        List<StudentCourseVO> allCourses = getAllStudentCourses(studentId);

        if (allCourses.isEmpty()) {
            overview.put("studyingCount", 0);
            overview.put("completedCount", 0);
            overview.put("avgCompletionRate", 0);
            log.info("[getStudyOverview] studentId={}, 无选课记录", studentId);
            return Result.success(overview);
        }

        int studyingCount = 0;   // 0 < 完成率 < 100
        int completedCount = 0;  // 完成率 >= 100
        int totalRate = 0;

        for (StudentCourseVO course : allCourses) {
            int rate = course.getCompletionRate() != null ? course.getCompletionRate().intValue() : 0;
            totalRate += rate;

            if (rate >= 100) {
                completedCount++;
            } else  {
                studyingCount++;
            }
            // rate == 0 表示未开始学习，不计入 studyingCount
        }

        int avgRate = allCourses.size() > 0 ? Math.round((float) totalRate / allCourses.size()) : 0;

        overview.put("studyingCount", studyingCount);
        overview.put("completedCount", completedCount);
        overview.put("avgCompletionRate", avgRate);

        log.info("[getStudyOverview] studentId={}, totalHours={}, studying={}, completed={}, avgRate={}%, totalCourses={}",
                studentId, hours, studyingCount, completedCount, avgRate, allCourses.size());

        return Result.success(overview);
    }

    // ==================== 2. 学习趋势（无需修改，与学习记录相关） ====================

    @GetMapping("/trend")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Map<String, Object>>> getStudyTrend(
            @AuthenticationPrincipal Long studentId,
            @RequestParam(defaultValue = "7") Integer days) {
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        if (days < 1) days = 7;
        if (days > 90) days = 90;

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        // 初始化近 days 天的空数据
        Map<String, Integer> dailyMinutes = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(formatter);
            dailyMinutes.put(date, 0);
        }

        // 查询学习记录
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        List<VideoProgress> progressList = videoProgressMapper.selectByStudentAndTimeRange(studentId, startTime);

        if (progressList != null) {
            for (VideoProgress vp : progressList) {
                if (vp.getLastStudyTime() != null && vp.getTotalStudyTime() != null) {
                    String date = vp.getLastStudyTime().format(formatter);
                    if (dailyMinutes.containsKey(date)) {
                        dailyMinutes.put(date, dailyMinutes.get(date) + vp.getTotalStudyTime());
                    }
                }
            }
        }

        dailyMinutes.forEach((date, minutes) -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", date);
            point.put("minutes", minutes);
            trend.add(point);
        });

        log.info("[getStudyTrend] studentId={}, days={}, dataPoints={}", studentId, days, trend.size());
        return Result.success(trend);
    }

    // ==================== 3. 课程完成进度（修复：返回全部选课） ====================

    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Map<String, Object>>> getCourseProgress(@AuthenticationPrincipal Long studentId) {
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        List<StudentCourseVO> allCourses = getAllStudentCourses(studentId);

        for (StudentCourseVO course : allCourses) {
            Map<String, Object> item = new HashMap<>();
            item.put("courseName", course.getCourseName());
            item.put("completionRate", course.getCompletionRate() != null ? course.getCompletionRate().intValue() : 0);
            item.put("completedLessons", course.getCompletedLessons() != null ? course.getCompletedLessons() : 0);
            item.put("totalLessons", course.getTotalLessons() != null ? course.getTotalLessons() : 0);
            result.add(item);
        }

        log.info("[getCourseProgress] studentId={}, courses={}", studentId, result.size());
        return Result.success(result);
    }

    // ==================== 4. 能力分析雷达图（修复：基于全部选课） ====================

    @GetMapping("/radar")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<Map<String, Object>>> getAbilityRadar(@AuthenticationPrincipal Long studentId) {
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        List<Map<String, Object>> radar = new ArrayList<>();
        List<StudentCourseVO> allCourses = getAllStudentCourses(studentId);

        for (StudentCourseVO course : allCourses) {
            Map<String, Object> item = new HashMap<>();
            String courseName = course.getCourseName() != null ? course.getCourseName() : "未知";

            // 取课程名前4个字作为维度名（避免过长）
            String dimension = courseName.length() > 4 ? courseName.substring(0, 4) : courseName;
            item.put("dimension", dimension);
            int score = course.getCompletionRate() != null ? course.getCompletionRate().intValue() : 0;
            item.put("score", score);
            radar.add(item);
        }

        log.info("[getAbilityRadar] studentId={}, dimensions={}", studentId, radar.size());
        return Result.success(radar);
    }

    // ==================== 5. 综合看板（修复） ====================

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Map<String, Object>> getStudyDashboard(@AuthenticationPrincipal Long studentId) {
        if (studentId == null) {
            return Result.error(401, "未登录");
        }

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("summary", buildSummary(studentId));
        dashboard.put("trend", buildTrend(studentId, 7));
        dashboard.put("courseProgress", buildCourseProgress(studentId));
        dashboard.put("abilityRadar", buildAbilityRadar(studentId));

        log.info("[getStudyDashboard] studentId={}", studentId);
        return Result.success(dashboard);
    }

    // ==================== 内部组装方法（修复版） ====================

    private Map<String, Object> buildSummary(Long studentId) {
        Map<String, Object> overview = new HashMap<>();

        Integer totalMinutes = videoProgressMapper.sumStudyTimeByStudent(studentId);
        double hours = totalMinutes != null ? Math.round(totalMinutes / 60.0 * 10) / 10.0 : 0.0;
        overview.put("totalHours", hours);

        List<StudentCourseVO> allCourses = getAllStudentCourses(studentId);

        int studyingCount = 0;
        int completedCount = 0;
        int totalRate = 0;

        for (StudentCourseVO course : allCourses) {
            int rate = course.getCompletionRate() != null ? course.getCompletionRate().intValue() : 0;
            totalRate += rate;
            if (rate >= 100) completedCount++;
            else {
                studyingCount++;
            };
        }

        int avgRate = allCourses.size() > 0 ? Math.round((float) totalRate / allCourses.size()) : 0;

        overview.put("studyingCount", studyingCount);
        overview.put("completedCount", completedCount);
        overview.put("avgCompletionRate", avgRate);

        return overview;
    }

    private List<Map<String, Object>> buildTrend(Long studentId, int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        Map<String, Integer> dailyMinutes = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(formatter);
            dailyMinutes.put(date, 0);
        }

        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        List<VideoProgress> progressList = videoProgressMapper.selectByStudentAndTimeRange(studentId, startTime);

        if (progressList != null) {
            for (VideoProgress vp : progressList) {
                if (vp.getLastStudyTime() != null && vp.getTotalStudyTime() != null) {
                    String date = vp.getLastStudyTime().format(formatter);
                    if (dailyMinutes.containsKey(date)) {
                        dailyMinutes.put(date, dailyMinutes.get(date) + vp.getTotalStudyTime());
                    }
                }
            }
        }

        dailyMinutes.forEach((date, minutes) -> {
            Map<String, Object> point = new HashMap<>();
            point.put("date", date);
            point.put("minutes", minutes);
            trend.add(point);
        });

        return trend;
    }

    private List<Map<String, Object>> buildCourseProgress(Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<StudentCourseVO> allCourses = getAllStudentCourses(studentId);

        for (StudentCourseVO course : allCourses) {
            Map<String, Object> item = new HashMap<>();
            item.put("courseName", course.getCourseName());
            item.put("completionRate", course.getCompletionRate() != null ? course.getCompletionRate().intValue() : 0);
            item.put("completedLessons", course.getCompletedLessons() != null ? course.getCompletedLessons() : 0);
            item.put("totalLessons", course.getTotalLessons() != null ? course.getTotalLessons() : 0);
            result.add(item);
        }

        return result;
    }

    private List<Map<String, Object>> buildAbilityRadar(Long studentId) {
        List<Map<String, Object>> radar = new ArrayList<>();
        List<StudentCourseVO> allCourses = getAllStudentCourses(studentId);

        for (StudentCourseVO course : allCourses) {
            Map<String, Object> item = new HashMap<>();
            String courseName = course.getCourseName() != null ? course.getCourseName() : "未知";
            String dimension = courseName.length() > 4 ? courseName.substring(0, 4) : courseName;
            item.put("dimension", dimension);
            item.put("score", course.getCompletionRate() != null ? course.getCompletionRate().intValue() : 0);
            radar.add(item);
        }

        return radar;
    }
}