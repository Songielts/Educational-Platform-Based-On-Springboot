package com.smartedu.modules.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartedu.modules.course.entity.Chapter;
import java.util.List;

public interface ChapterService extends IService<Chapter> {
    List<Chapter> getChaptersByCourseId(Long courseId);
}