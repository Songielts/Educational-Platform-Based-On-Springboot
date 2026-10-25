package com.smartedu.modules.course.controller;

import com.smartedu.common.result.Result;
import com.smartedu.modules.course.entity.Chapter;
import com.smartedu.modules.course.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/course/chapter")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    // 获取课程章节列表
    @GetMapping("/list/{courseId}")
    public Result getChapterList(@PathVariable Long courseId) {
        List<Chapter> list = chapterService.getChaptersByCourseId(courseId);
        return Result.success(list);
    }

    // 添加章节
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result addChapter(@RequestBody Chapter chapter) {
        // 设置默认值
        chapter.setParentId(chapter.getParentId() != null ? chapter.getParentId() : 0L);
        chapter.setChapterType(chapter.getChapterType() != null ? chapter.getChapterType() : 1);
        chapter.setSortOrder(chapter.getSortOrder() != null ? chapter.getSortOrder() : 0);
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());

        chapterService.save(chapter);
        return Result.success("添加成功");
    }

    // 删除章节
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Result deleteChapter(@PathVariable Long id) {
        chapterService.removeById(id);
        return Result.success("删除成功");
    }
}