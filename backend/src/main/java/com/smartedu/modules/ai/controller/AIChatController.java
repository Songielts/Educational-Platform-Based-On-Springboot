package com.smartedu.modules.ai.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.ai.dto.AIAskDTO;
import com.smartedu.modules.ai.service.KimiService;
import com.smartedu.modules.course.entity.Course;
import com.smartedu.modules.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai/chat")
public class AIChatController {

    @Autowired
    private KimiService kimiService;

    @Autowired
    private CourseService courseService;

    /**
     * AI智能问答 - 学生提问
     */
    @PostMapping("/ask")
    @PreAuthorize("hasRole('STUDENT')")
    public Result askQuestion(@AuthenticationPrincipal Long studentId,
                              @RequestBody @Validated AIAskDTO dto) {
        // 获取课程信息作为上下文
        Course course = courseService.getById(dto.getCourseId());
        String courseContext = course != null ? course.getDescription() : "";

        String answer = kimiService.answerQuestion(dto.getQuestion(), courseContext);

        return Result.success(Map.of("answer", answer));
    }

    /**
     * AI学习推荐
     */
    @PostMapping("/recommend")
    @PreAuthorize("hasRole('STUDENT')")
    public Result getRecommendations(@RequestBody Map<String, String> params) {
        String studentProfile = params.get("profile");
        String weakPoints = params.get("weakPoints");

        String recommendation = kimiService.recommendStudy(studentProfile, weakPoints);

        return Result.success(Map.of("recommendation", recommendation));
    }
}